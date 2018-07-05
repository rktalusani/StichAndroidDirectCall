package com.nexmo.sdk.conversation.client;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.client.event.container.Invitation;
import com.nexmo.sdk.conversation.client.event.container.Receipt;
import com.nexmo.sdk.conversation.client.event.container.SynchronisingState;
import com.nexmo.sdk.conversation.config.Defaults;
import com.nexmo.sdk.conversation.core.client.request.CreateConversationRequest;
import com.nexmo.sdk.conversation.core.client.request.DeliveredReceiptRequest;
import com.nexmo.sdk.conversation.core.client.request.audio.RtcNewRequest;
import com.nexmo.sdk.conversation.core.networking.ImageDelete;
import com.nexmo.sdk.conversation.core.persistence.ImageStorage;
import com.nexmo.sdk.conversation.core.util.Log;

import com.nexmo.sdk.conversation.client.event.EventType;
import com.nexmo.sdk.conversation.client.event.misc.SessionError;
import com.nexmo.sdk.conversation.core.client.request.DeleteEventRequest;
import com.nexmo.sdk.conversation.core.client.request.GetConversationRequest;
import com.nexmo.sdk.conversation.core.client.request.GetEventsRequest;
import com.nexmo.sdk.conversation.core.client.request.InviteRequest;
import com.nexmo.sdk.conversation.core.client.request.JoinRequest;
import com.nexmo.sdk.conversation.core.client.request.SendImageMessageRequest;
import com.nexmo.sdk.conversation.core.client.request.SendTextMessageRequest;
import com.nexmo.sdk.conversation.core.client.request.TypingIndicatorRequest;
import com.nexmo.sdk.conversation.core.networking.ImageDownloader;
import com.nexmo.sdk.conversation.core.persistence.contract.ConversationContract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.nexmo.sdk.conversation.client.Call.MEMBER_CALL_STATE.ANSWERED;
import static com.nexmo.sdk.conversation.client.Call.MEMBER_CALL_STATE.HUNGUP;
import static com.nexmo.sdk.conversation.client.Call.MEMBER_CALL_STATE.REJECTED;
import static com.nexmo.sdk.conversation.core.networking.Constants.CALL_PREFIX_NAME;

/**
 * SocketEventHandler handles the response events from SocketClient
 * and OperationsManager, and forwards to SocketEventNotifier.
 *
 * @author chatitze moumin.
 *
 * @hide
 * @VisibleForTesting
 */
class SocketEventHandler {

    private static final String TAG = SocketEventHandler.class.getSimpleName();

    // list of joined/invited conversations.
    private final List<Conversation> conversationList = new ArrayList<>();
    // list of created conversations.
    private final List<Conversation> createdConversationList = new ArrayList<>();
    // list of ongoing calls.
    private final List<com.nexmo.sdk.conversation.client.Call> callList = Collections.synchronizedList(new ArrayList<com.nexmo.sdk.conversation.client.Call>());
    private SocketClient socketClient;
    private SocketEventNotifier socketEventNotifier;
    private final CacheDB cacheDb;

    SocketEventHandler (SocketClient socketClient){
        this.socketClient = socketClient;
        this.socketEventNotifier = socketClient.getConversationClient().getEventNotifier();
        this.cacheDb = CacheDB.getCacheDBInstance();
    }

    private Member onMemberJoinedAddMember(Conversation conversation, String memberId, User user, Date joinedTimestamp) {
        Member newMember = new Member(user.getUserId(), user.getName(), memberId, joinedTimestamp, null, null, Member.STATE.JOINED);
        conversation.addMember(newMember);
        newMember.setConversation(conversation);

        // if self set conversation.setself
        updateCacheAddMember(newMember);

        return newMember;
    }

    private void onMemberJoinedUpdateMember(Member member, Date joinedTimestamp) {
        member.updateState(Member.STATE.JOINED, joinedTimestamp);
        updateCacheOnMemberEvent(member);
    }

    // update internal state regardless of listeners being set
    void onMemberJoined(String cid, final String memberId, User user, Date joinedTimestamp) {
        Log.d(TAG, "onMemberJoined ");
        Conversation pendingConversation = findConversation(cid);
        if (pendingConversation != null) {
            //update member state and join date.
            Member member = pendingConversation.getMember(memberId);
            if (member != null)
                onMemberJoinedUpdateMember(member, joinedTimestamp);
            else
                member = onMemberJoinedAddMember(pendingConversation, memberId, user, joinedTimestamp);
            pendingConversation.memberJoinedEvent().notifySubscriptions(member);

            // notify member call state as well
            if (pendingConversation.getDisplayName().startsWith(CALL_PREFIX_NAME) && pendingConversation.isAudioEnabled()) {
                CallEvent callEvent = new CallEvent(ANSWERED, member);
                Log.d(TAG, "CALL event " + callEvent.toString());

                com.nexmo.sdk.conversation.client.Call ongoingCall = findCall(pendingConversation.getConversationId());
                if (ongoingCall != null)
                    ongoingCall.event().notifySubscriptions(callEvent);
            }
        } else { //assume self-join  to new conversation event
            getConversation(cid, new RequestHandler<Conversation>() {
                @Override
                public void onError(NexmoAPIError apiError) {
                    Log.e(TAG, apiError.getMessage());
                }
                @Override
                public void onSuccess(Conversation conversation) {
                    conversation.setConversationSignalingChannel(socketClient.getConversationClient().getSignallingChannel());
                    conversation.setSocketEventNotifier(socketEventNotifier);
                    updateCacheWithNewConversation(conversation);
                    socketClient.getConversationClient()
                            .joinedToNewConversationEvent()
                            .notifySubscriptions(conversation);
                }
            });

        }
    }

    void onSelfInvited(String cid, String cName, final Member selfAsMember, final String invitedByUsername, final boolean isAudioEnabled) {
        Log.d(TAG, "onSelfInvited to a new conversation");
        Conversation pendingConversation = findConversation(cid);
        if (pendingConversation == null) {
            Log.d(TAG, "User received an invitation to a new conversation");
            final Conversation invitedConversation = new Conversation(cName, cid, selfAsMember);
            selfAsMember.setConversation(invitedConversation);
            invitedConversation.setSelf(selfAsMember);
            Log.d(TAG, "User received an invitation to a new conversation, self: " + invitedConversation.getSelf());
            invitedConversation.setConversationSignalingChannel(this.socketClient.getConversationClient().getSignallingChannel());
            invitedConversation.setSocketEventNotifier(this.socketEventNotifier);
            conversationList.add(invitedConversation);

            // insert conversation details + self member
            updateCacheAddConversation(invitedConversation, new RequestHandler<Conversation>() {
                @Override
                public void onSuccess(Conversation conversation) {
                    invitedConversation.updateBasicDetails(conversation);

                    cacheDb.getConversationRepository().update(conversation, conversation.getConversationId());
                    cacheDb.getMemberRepository().insertAll(conversation.getConversationId(), conversation.getMembers());

                    if (conversation.getDisplayName().startsWith(CALL_PREFIX_NAME) && isAudioEnabled) {
                        com.nexmo.sdk.conversation.client.Call incomingCall = new com.nexmo.sdk.conversation.client.Call(conversation, invitedByUsername);
                        updateCallList(incomingCall);

                        socketClient.getConversationClient().callEvent().notifySubscriptions(incomingCall);
                    }
                    else {
                        Invitation invitation = new Invitation(invitedConversation, selfAsMember, invitedByUsername, isAudioEnabled);
                        socketClient.getConversationClient().invitedEvent().notifySubscriptions(invitation);
                    }
                }

                @Override
                public void onError(NexmoAPIError error) {
                    Log.e(TAG, error.getMessage());
                }
            });
        }
    }

    void onMemberInvited(String cid, Member invitedMember, String invitedByUsername) {
        Log.d(TAG, "onMemberInvited");
        Conversation pendingConversation = findConversation(cid);

        if (pendingConversation != null) {
            invitedMember.setConversation(pendingConversation);

            //add new member
            pendingConversation.addMember(invitedMember);
            updateCacheAddMember(invitedMember);

            Invitation invitation = new Invitation(pendingConversation, invitedMember, invitedByUsername);
            pendingConversation.memberInvitedEvent().notifySubscriptions(invitation);
        } else

            Log.e(TAG, "onMemberInvited internal error. Receiving events from an unknown conversation!");
    }

    void onMemberLeft(String cid, String memberId, User user, Date invited, Date joined, Date left) {
        Log.d(TAG, "onMemberLeft");
        Conversation pendingConversation = findConversation(cid);

        if (pendingConversation == null) return;

        //update member state.
        Member member = pendingConversation.getMember(user);
        if (member != null) {
            member.updateState(Member.STATE.LEFT, left);

            //update cached member. we don't want to remove the member, just update state.
            updateCacheOnMemberEvent(member);

            pendingConversation.memberLeftEvent().notifySubscriptions(member);

            // notify member call state as well
            if (pendingConversation.getDisplayName().startsWith(CALL_PREFIX_NAME) && pendingConversation.isAudioEnabled()) {
                CallEvent callEvent = new CallEvent(member.getJoinedAt() == null ? REJECTED : HUNGUP, member);
                Log.d(TAG, "CALL event " + callEvent.toString());

                com.nexmo.sdk.conversation.client.Call ongoingCall = findCall(pendingConversation.getConversationId());
                if (ongoingCall != null)
                    ongoingCall.event().notifySubscriptions(callEvent);
            }
        }
        else
            Log.e(TAG, "onMemberLeft internal error. Receiving events from an out-of-sync member!");
    }

    Text onTextSent(String textId, Date timestamp, final SendTextMessageRequest request) {
        Conversation pendingConversation = findConversation(request.cid);
        if (pendingConversation != null) {

            if (pendingConversation.findText(textId) == null) {
                final Text incomingText = new Text(request.message, textId, null, pendingConversation.getSelf(), pendingConversation);
                return incomingText;
            }
        }

        return null;
    }

    Image onImageSent(String imageId, Date timestamp, SendImageMessageRequest request) {
        Conversation pendingConversation = findConversation(request.cid);
        if (pendingConversation != null) {
            if (pendingConversation.findImage(imageId) == null) {
                 Image image = new Image(imageId, null, pendingConversation.getSelf(), pendingConversation,
                        request.original, request.medium, request.thumbnail);
                return image;
            }
        }
        return null;
    }

//    void onMarkedAsSeen(final ReceiptRecordRequest request) {
//        Conversation pendingConversation = findConversation(request.cid);
//        if (pendingConversation == null) return;
//
//        Message seenMessage = pendingConversation.findMessage(request.eventId);
//        if (seenMessage != null) {
//            SeenReceipt seenReceipt = new SeenReceipt(request.eventId, request.memberId, request.timestamp);
//            seenMessage.addSeenReceipt(seenReceipt);
//            updateCacheUpdateMessage(seenMessage, request.cid);
//
//            //TODO: be able to notify listeners on success.
//            //request.getListener().onSuccess();
//        }
//    }


    // Subscription event
    void onMessageDeleted(String cid, final String memberId, String eventId, String deleteEventId, Date timestamp) {
        // remove payload from the text and refresh conversation list.
        final Conversation pendingConversation = findConversation(cid);
        if (pendingConversation != null && pendingConversation.getMember(memberId) != null) {
            final Event deletedEvent = pendingConversation.getEvent(eventId);
            if (deletedEvent != null) {
                if (deletedEvent.getType() == EventType.IMAGE) {
                    Image deletedImage = (Image) deletedEvent;

                    // delete local cached images.
                    cacheDeleteImageRepresentations(socketClient.getConversationClient().getContext(), (Image) deletedEvent);
                    deletedImage.setRepresentations(null, null, null);
                }
                else if (deletedEvent.getType() == EventType.TEXT) {
                    ((Text) deletedEvent).setText(null);
                }

                deletedEvent.setDeletedTimestamp(timestamp);
                pendingConversation.updateLastEventId(deleteEventId);
                updateCacheUpdateMessage(deletedEvent);

                //set date deleted.
                pendingConversation.messageEvent().notifySubscriptions(deletedEvent);
            }
        }
        // else need to resync
    }

    void onEventSeen(String cid, String memberId, String eventId, Date timestamp) {
        //add seen receipt
        Conversation pendingConversation = findConversation(cid);
        if (pendingConversation != null) {
            Event seenEvent = pendingConversation.findEvent(eventId);
            Member seenBy = pendingConversation.getMember(memberId);
            if (seenEvent != null && seenBy != null) {
                SeenReceipt seenReceipt = new SeenReceipt(seenEvent, seenBy, timestamp);
                seenEvent.addSeenReceipt(seenReceipt);
                // update cache
                pendingConversation.updateLastEventId(seenEvent.id);
                updateCacheUpdateMessage(seenEvent);

                Receipt<SeenReceipt> receipt = new Receipt<>(seenEvent, pendingConversation.getMember(memberId), seenReceipt);
                pendingConversation.seenEvent().notifySubscriptions(receipt);
            }
        }
    }

    void onEventDelivered(String cid, String memberId, String eventId, Date timestamp) {
        Conversation pendingConversation = findConversation(cid);
        if (pendingConversation == null) return;

        Event deliveredEvent = pendingConversation.findEvent(eventId);
        Member deliveredTo = pendingConversation.getMember(memberId);

        if (deliveredEvent != null && deliveredTo != null) {
            DeliveredReceipt deliveredReceipt = new DeliveredReceipt(deliveredEvent, deliveredTo, timestamp);
            deliveredEvent.addDeliveredReceipt(deliveredReceipt);
            pendingConversation.updateLastEventId(deliveredEvent.id);
            updateCacheUpdateMessage(deliveredEvent);

            Receipt<DeliveredReceipt> receipt = new Receipt<>(deliveredEvent, pendingConversation.getMember(memberId), deliveredReceipt);
            pendingConversation.deliveryEvent().notifySubscriptions(receipt);
        }
    }

    void onMessageReceived(final String cid, final Event event) {
        Log.d(TAG, "onMessageReceived " + event.toString());
        final Conversation pendingConversation = findConversation(cid);

        if (pendingConversation != null) {
            if (event.isReadyForMarkedAsDelivered())
                issueDeliveryReceiptInBackground(event);

            updateCacheNewMessage(event);

            switch (event.getType()) {
                case TEXT:
                case MEMBER_MEDIA: {
                    pendingConversation.messageEvent().notifySubscriptions(event);

                    break;
                }
                case IMAGE: {
                    final Image relayedImage = (Image) event;

                    // attempt to download THUMBNAIL.
                    downloadImageRepresentation(relayedImage, ImageRepresentation.TYPE.THUMBNAIL, new RequestHandler<Void>() {
                        @Override
                        public void onError(NexmoAPIError apiError) {
                            Log.d(TAG, "onMessageReceived cannot be downloaded. Try later. " + apiError.toString());
                            pendingConversation.messageEvent().notifySubscriptions(event);
                        }

                        @Override
                        public void onSuccess(Void result) {
                            Log.d(TAG, "onMessageReceived Image THUMBNAIL was downloaded.");
                            pendingConversation.messageEvent().notifySubscriptions(event);
                        }
                    });
                    break;
                }
                default: {
                    Log.d(TAG, "unknown event type");
                    break;
                }
            }

        } else Log.d(TAG, "onMessageReceived for not-sync conversation");
        // completely new event, conversations not synced yet.
    }

    void onTypingOnReceived(String cid, String memberId) {
        dispatchMemberTypeEvent(cid, memberId, Member.TYPING_INDICATOR.ON);
    }

    void onTypingOffReceived(String cid, String memberId) {
        dispatchMemberTypeEvent(cid, memberId, Member.TYPING_INDICATOR.OFF);
    }

    private void onJoinUpdateConversation(Conversation conversation, Member member, JoinRequest request) {
        Log.d(TAG, " onJoinUpdateConversation ");
        // request was done on my behalf: invitation is accepted
        if (conversation.isSelf(member)) {
            conversation.getSelf().updateState(Member.STATE.JOINED, member.getJoinedAt());
            updateCacheOnMemberEvent(conversation.getSelf());
        }
        else {
            // update other member's status
            Member joinedMember = conversation.getMember(member.getMemberId());
            if (joinedMember != null) {
                joinedMember.updateState(Member.STATE.JOINED, member.getJoinedAt());
                updateCacheOnMemberEvent(joinedMember);
            }
            else {
                member.setConversation(conversation);
                conversation.addMember(member);
                updateCacheAddMember(member);
            }
        }
    }

    private Conversation onJoinNewConversation(final Member member, final JoinRequest request) {
        Log.d(TAG, " onJoinNewConversation ");
        final Conversation createdConversation = findCreatedConversation(request.cid);
        if (createdConversation != null) {
            /* Self joined conversation. Persist new conversation. */
            if (request.userId.equals(this.socketClient.self.getUserId()) ||
                    request.userName.equals(this.socketClient.self.getName())) {
                createdConversation.setSelf(member);
                member.setConversation(createdConversation);

                updateCacheAddConversation(createdConversation, new RequestHandler<Conversation>() {
                    @Override
                    public void onSuccess(Conversation conversation) {
                        createdConversation.updateBasicDetails(conversation);
                        member.setConversation(createdConversation);

                        cacheDb.getConversationRepository().update(conversation, conversation.getConversationId());
                        cacheDb.getMemberRepository().insertAll(conversation.getConversationId(), conversation.getMembers());
                    }

                    @Override
                    public void onError(NexmoAPIError error) {
                        Log.e(TAG, error.getMessage());
                    }
                });

                addOrUpdateConversationList(createdConversation);
                return createdConversation;
            }
            else {
                /* Conversation joined on behalf of different user. Conversation not persisted. */
                Conversation newConversation = new Conversation(request.cName, request.cid);
                member.setConversation(newConversation);

                newConversation.setConversationSignalingChannel(this.socketClient.getConversationClient().getSignallingChannel());
                newConversation.setSocketEventNotifier(this.socketEventNotifier);

                return newConversation;
            }

        }
        return null;
    }

    /**
     * Conversation created, keep created conversations in memory but do not persist them until user has joined/was invited to.
     * @param conversation The newly created conversation.
     */
    void onConversationCreated(Conversation conversation) {
        addOrUpdateCreatedConversationList(conversation);
    }

    void addNewConversationToMemory(Conversation conversation) {
        addOrUpdateCreatedConversationList(conversation);
    }

    void onNewConversationHelperSuccessCreated(final Conversation conversation, final CreateConversationRequest request, final Member member) {
        conversation.setSelf(member);
        member.setConversation(conversation);

        updateCacheAddConversation(conversation, new RequestHandler<Conversation>() {
            @Override
            public void onSuccess(Conversation updatedConversation) {
                conversation.updateBasicDetails(updatedConversation);
                member.setConversation(conversation);

                cacheDb.getConversationRepository().update(conversation, conversation.getConversationId());
                cacheDb.getMemberRepository().insertAll(conversation.getConversationId(), conversation.getMembers());
                request.getListener().onSuccess(conversation);
            }

            @Override
            public void onError(NexmoAPIError error) {
                Log.e(TAG, error.getMessage());
            }
        });

        addOrUpdateCreatedConversationList(conversation);
        Log.d(TAG, "onJoin " + member.toString());
    }

    String onRtcNew(RtcNewRequest request, RtcNewRequest.RtcNewResponse response) {
        Log.d(TAG, "onRtcNew " + response.rtc_id);
        Conversation pendingConversation = findConversation(request.cid);
        pendingConversation.updateRtcId(response.rtc_id);
        return response.rtc_id;
    }

    /**
     * onJoin callback can be a result of:
     * - self accepting an invite
     * - self joins a new conversation
     * - self has joined a different member to a new conversation he's not part of
     * - self has joined a different member to a conversation he's part of
     */
    void onJoin(Member member, JoinRequest request) {
        Log.d(TAG, "onJoin " + member.toString());
        Conversation pendingConversation = findConversation(request.cid);

        if (pendingConversation == null) {
            pendingConversation = onJoinNewConversation(member, request);
        }
        else
            onJoinUpdateConversation(pendingConversation, member, request);

        addOrUpdateConversationList(pendingConversation);
    }

    final List<Conversation> getConversationList() {
        return this.conversationList;
    }

    void onConversations(List<Conversation> conversations) {
        if (conversations.size() > Defaults.MAX_CONVERSATION_LIST_SIZE) {
            socketClient.getConversationClient().synchronisationEvent().notifySubscriptions(SynchronisingState.STATE.OUT_OF_SYNC);
            socketClient.getConversationClient().getSignallingChannel().updateSyncState(SynchronisingState.STATE.OUT_OF_SYNC);

            return;
        }

        socketClient.getConversationClient().synchronisationEvent().notifySubscriptions(SynchronisingState.STATE.CONVERSATIONS);
        socketClient.getConversationClient().getSignallingChannel().updateSyncState(SynchronisingState.STATE.CONVERSATIONS);

        for (Conversation conversation : conversations) {
            conversation.setConversationSignalingChannel(socketClient.getConversationClient().getSignallingChannel());
            conversation.setSocketEventNotifier(socketEventNotifier);
        }
        synchronized(this.conversationList) {
            this.conversationList.clear();
            this.conversationList.addAll(conversations);
        }

        // TODO: Caching might be handling in a different thread?
        updateCacheOnConversations();
    }

    Conversation onConversation(Conversation conversation, GetConversationRequest request) {
        // override entire conversation.
        addOrUpdateConversationList(conversation);
        return conversation;
    }

    Conversation onEventsHistory(List<Event> events, GetEventsRequest request) {
        //update list, make sure does not overlap with onConversations;
        //update conversation entry, don't remove the members
        Conversation pendingConversation = findConversation(request.cid);
        if (pendingConversation == null)
            return null; // TODO: user won't be notified! :(

        for (Event event : events) {
            event.setConversation(pendingConversation);

            String memberId = event.getMember().getMemberId();
            Member sender = pendingConversation.getMember(memberId);
            if (sender != null) {
                event.setMember(sender);

                for (SeenReceipt seenReceipt : event.getSeenReceipts()) {
                    seenReceipt.setEvent(event);

                    Member seenByMember = pendingConversation.getMember(seenReceipt.getMember().getMemberId());
                    if (seenByMember != null)
                        seenReceipt.setMember(seenByMember);

                }
                // for delivery receipt as well
                for (DeliveredReceipt deliveredReceipt : event.getDeliveredReceipts()) {
                    deliveredReceipt.setEvent(event);

                    Member deliveredToMember = pendingConversation.getMember(deliveredReceipt.getMember().getMemberId());
                    if (deliveredToMember != null)
                        deliveredReceipt.setMember(deliveredToMember);
                }

                if (event.isReadyForMarkedAsDelivered())
                    issueDeliveryReceiptInBackground(event);

                if (event.getType() == EventType.IMAGE
                        && event.deletedTimestamp == null)
                    downloadImageRepresentation((Image)event, ImageRepresentation.TYPE.THUMBNAIL, null);
            }
        }
        pendingConversation.setEvents(events);
        addOrUpdateConversationList(pendingConversation);

        if (pendingConversation.getSelf() != null)
            updateCacheOfMessageEvents(pendingConversation);

        return pendingConversation;
    }

    Member.TYPING_INDICATOR  onTyping(Member.TYPING_INDICATOR indicator, TypingIndicatorRequest request) {
        Conversation pendingConversation = findConversation(request.cid);
        if (pendingConversation != null) {
            Member sender = pendingConversation.getMember(request.memberId);
            if (sender != null) {
                sender.setTypingIndicator(indicator);
            }
            else
                request.listener.onError(NexmoAPIError.unexpectedResponse(request.getRequestId(), pendingConversation.getConversationId()));
        }
        return indicator;
    }

    Member onInvitationSent(String userId, String memberId, Date timestampInvited, InviteRequest request) {
        Conversation pendingConversation = findConversation(request.cid);
        if (pendingConversation != null) {
            Member invitedMember = new Member(userId, request.user, memberId);
            invitedMember.updateState(Member.STATE.INVITED, timestampInvited);
            return invitedMember;
        }
        return null;
    }

    void onSessionError(SessionError sessionError) {
        socketClient.getConversationClient().sessionErrorEvent().notifySubscriptions(sessionError);
    }

    //download from media service
    //if this is for the history download, we need different callback
    void downloadImageRepresentation(final Image image, final ImageRepresentation.TYPE type, final RequestHandler<Void> downloadListener) {
        final Callback downloadCallback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure download " + e.toString());

                if (downloadListener != null)
                    image.getConversation().getSignallingChannel().getConversationClient().callUserCallback(new Runnable() {
                        @Override
                        public void run() {
                            downloadListener.onError(NexmoAPIError.downloadFailure(image.getConversation().getConversationId()));
                        }
                    });
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                Log.d(TAG, "onResponse download:" + type);

                //recoverable error.
                if (!response.isSuccessful()) {
                    final NexmoAPIError downloadError = new NexmoAPIError(NexmoAPIError.DOWNLOAD_FAILURE, image.conversation.getConversationId(), "Unexpected code " + response.code());
                    if (downloadListener != null)
                        image.getConversation().getSignallingChannel().getConversationClient().callUserCallback(new Runnable() {
                            @Override
                            public void run() {
                                downloadListener.onError(downloadError);
                            }
                        });

                    response.body().close();
                    return;
                }

                Bitmap bitmap = ImageDownloader.decodeImage(response);
                    switch(type) {
                        case ORIGINAL: {
                            image.getOriginal().setBitmap(bitmap);
                            break;
                        }
                        case MEDIUM: {
                            image.getMedium().setBitmap(bitmap);
                            break;
                        }
                        case THUMBNAIL: {
                            image.getThumbnail().setBitmap(bitmap);
                            break;
                        }
                    }

                cacheImageRepresentation(socketClient.getConversationClient().getContext(), image, type, downloadListener);

                response.body().close();
            }
        };

        ImageDownloader.downloadImage(image.getImageRepresentationByType(type), downloadCallback, socketClient.getConversationClient().getToken());
    }

    void deleteImageRepresentations(Image image, RequestHandler listener) {
        long _ts = System.currentTimeMillis();
        Log.d(TAG, "Starting image delete.. ");
        if (image.getOriginal() != null)
            deleteImageRepresentation(image, image.getOriginal(), listener);
        if (image.getMedium() != null)
            deleteImageRepresentation(image, image.getMedium(), listener);
        if (image.getThumbnail() != null)
            deleteImageRepresentation(image, image.getThumbnail(), listener);
        Log.d(TAG, "Delete took " + (System.currentTimeMillis() - _ts) + "ms");
    }

    // delete all 3 representation files from media service
    private void deleteImageRepresentation(final Image image, final ImageRepresentation imageRepresentation, final RequestHandler<Void> listener) {
        final Callback deleteCallback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure " + e.getMessage());
                listener.onError(new NexmoAPIError(NexmoAPIError.IMAGE_DELETE_FAILURE, "Image delete rejected." + e.getMessage()));
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                Log.d(TAG, "onResponse " + response.toString());

                if (!response.isSuccessful()) {
                    listener.onError(new NexmoAPIError(NexmoAPIError.IMAGE_DELETE_FAILURE, "Image delete rejected." + response.code()));
                    response.body().close();
                    return;
                }
                imageRepresentation.updateUrl(null);

                // if all 3 are deleted remove event from CAPI
                if (TextUtils.isEmpty(image.getOriginal().getUrl()) && TextUtils.isEmpty(image.getMedium().getUrl())
                    && TextUtils.isEmpty(image.getThumbnail().getUrl())) {
                    DeleteEventRequest deleteEventRequest = new DeleteEventRequest(image.getConversation().getConversationId(),
                            image.getMember().getMemberId(), image.getId(), listener);

                    socketClient.deleteEvent(deleteEventRequest);
                }

                response.body().close();
            }
        };

        ImageDelete.deleteImage(imageRepresentation.getUrl(), deleteCallback, socketClient.getConversationClient().getToken());
    }

    private void cacheImageRepresentation(Context context, final Image image, ImageRepresentation.TYPE type, final RequestHandler downloadListener) {
        Log.d(TAG, " cacheImageRepresentation " + type);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            String representationPath = ImageStorage.saveFileToDisk(context, image, type);
            image.getImageRepresentationByType(type).updateLocalFilePath(representationPath);

            //update cache with path to local file
            updateCacheUpdateMessage(image);

            if (downloadListener != null)
                image.getConversation().getSignallingChannel().getConversationClient().callUserCallback(new Runnable() {
                    @Override
                    public void run() {
                        downloadListener.onSuccess(null);
                    }
                });
        }
        else if(downloadListener != null)
            image.getConversation().getSignallingChannel().getConversationClient().callUserCallback(new Runnable() {
                @Override
                public void run() {
                    downloadListener.onError(NexmoAPIError.permissionRequired(image.getConversation().getConversationId()));
                }
            });
    }

    private void cacheDeleteImageRepresentations(Context context, Image image) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            ImageStorage.deleteFilesFromDisk(context, image);
            image.updateLocalFilePaths(null);
        }
        //update cache
        updateCacheUpdateMessage(image);
    }

    private void dispatchMemberTypeEvent(String cid, String memberId, Member.TYPING_INDICATOR typing_indicator){
        Conversation pendingConversation = findConversation(cid);
        if (pendingConversation != null) {
            // find and update Member that is typing
            Member typingMember = pendingConversation.getMember(memberId);
            if(typingMember != null) {
                typingMember.setTypingIndicator(typing_indicator);
                pendingConversation.typingEvent().notifySubscriptions(typingMember);
            }
        } //else dispatch an internal error report to bugsnag
    }

    private void issueDeliveryReceiptInBackground(Event event) {
        DeliveredReceiptRequest deliveredReceiptRequest = new DeliveredReceiptRequest(event, new RequestHandler() {
            @Override
            public void onError(NexmoAPIError error) {
                Log.d(TAG, "issueDeliveryReceiptInBackground failed " + error.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "issueDeliveryReceiptInBackground done");
            }
        });
        this.socketClient.sendReceiptRecord(deliveredReceiptRequest);
    }

    com.nexmo.sdk.conversation.client.Call findCall(final String callId) {
        for (com.nexmo.sdk.conversation.client.Call call : callList) {
            if (call.getConversation().getConversationId().equals(callId))
                return call;
        }
        return null;
    }

    Conversation findConversation(final String cid) {
        synchronized(conversationList) {
            for (Conversation conversation : conversationList) {
                if (conversation.getConversationId().equals(cid))
                    return conversation;
            }
        }
        return null;
    }

    Conversation findCreatedConversation(final String cid) {
        synchronized(createdConversationList) {
            for (Conversation conversation : createdConversationList) {
                if (conversation.getConversationId().equals(cid))
                    return conversation;
            }
        }
        return null;
    }


    /**
     * Check if cached conversations need sync based on lastEventId.
     * If conversation not in cache yet, add it.
     * If conversation removed from CS means someone kicked us and we have to remove it from cache.
     *
     * @param fetchedConversations The fresh list.
     * @return  List of cids that require conversation details update.
     */
    private List<String> checkIfConversationsNeedSync(List<Conversation> fetchedConversations, User self) {
        Log.d(TAG, "checkIfConversationsNeedSync");
        List<String> cidsToUpdate = new ArrayList<>();

        final Map<String, Conversation> freshConversationMap = new HashMap<>();
        final Map<String, Conversation> cachedConversationsMap = this.cacheDb.getConversationRepository().getConversationsListAsMap(self);

        for (Conversation freshConversation : fetchedConversations) {
            freshConversationMap.put(freshConversation.getConversationId(), freshConversation);
            Conversation cachedConversation = cachedConversationsMap.get(freshConversation.getConversationId());

            if (cachedConversation == null) {
                Log.d(TAG, "checkIfConversationsNeedSync: Yes, conversation not cached yet");
                this.cacheDb.getConversationRepository().insert(freshConversation, freshConversation.getConversationId());

                cidsToUpdate.add(freshConversation.getConversationId());
            } else if (TextUtils.equals(cachedConversation.getLastEventId(), freshConversation.getLastEventId()))
                // just read the members from cache.
                freshConversation.setMembers(cachedConversation.getMembers());
            else {
                Log.d(TAG, "Cached conversation has to update members and messages");
                freshConversation.markAsDirty(true);
                // cache fresh members and messages.
                cidsToUpdate.add(freshConversation.getConversationId());
            }
        }

        ArrayList<String> idsToRemove = new ArrayList<>();
        for (Conversation cachedConversation : cachedConversationsMap.values()) {
            if (!freshConversationMap.containsKey(cachedConversation.getConversationId()))
                idsToRemove.add(cachedConversation.getConversationId());
        }
        this.cacheDb.getConversationRepository().delete(idsToRemove);

        return cidsToUpdate;
    }


    private void syncMessagesIfNeeded(Conversation conversation) {
        String cid = conversation.getConversationId();
        List<Event> messagesFromCAPI = conversation.getEvents();
        final List<String> cachedMessageIds = cacheDb.getEventRepository().getEventIds(conversation);
        Log.d(TAG, "syncMessagesIfNeeded - cachedMessageIds: " + cachedMessageIds.toString());

        for(Event freshEvent : messagesFromCAPI) {
            if (cachedMessageIds.contains(freshEvent.getId()))
                cachedMessageIds.remove(freshEvent.getId());
            else
                cacheDb.getEventRepository().insert(freshEvent, cid);
        }
        cacheDb.getEventRepository().delete(cachedMessageIds);
    }

    /**
     * Updating DB against fresh list of conversations.
     * If no conversations are persisted yet:
     *     - bulk insert all conversations and members.
     * If some conversations are persisted already:
     *     - check if conversations need sync.
     */
    private void updateCacheOnConversations() {
        List<String> cidsToUpdate = new ArrayList<>();
        if (cacheDb.tableContainsRows(ConversationContract.ConversationEntry.TABLE_NAME))
            cidsToUpdate =  (checkIfConversationsNeedSync(this.conversationList, this.socketClient.getConversationClient().getUser()));
        else {
            // only update conversations user is part of.
            for (Conversation conversation : this.conversationList)
                if (conversation.getSelf() != null)
                    cidsToUpdate.add(conversation.getConversationId());

            this.cacheDb.getConversationRepository().insertAll(this.conversationList);
        }

        final int totalItems = cidsToUpdate.size();
        final AtomicInteger iteration = new AtomicInteger();
        Log.d(TAG, "updateCacheOnConversations for total conversations: " + totalItems);

        if (totalItems == 0) {
            socketClient.getConversationClient().synchronisationEvent().notifySubscriptions(SynchronisingState.STATE.MEMBERS);
            socketEventNotifier.notifyConversationListListener(conversationList);
            socketClient.getConversationClient().getSignallingChannel().updateSyncState(SynchronisingState.STATE.MEMBERS);
            return;
        }

        // start fetching conversation details (members and message events).
        for (final String cid : cidsToUpdate) {
            final int counter = iteration.incrementAndGet();

            updateCacheOfMembersForConversation(cid, new RequestHandler<Conversation>() {
                @Override
                public void onSuccess(Conversation conversation) {
                    Conversation pendingConversation = findConversation(cid);
                    if (pendingConversation != null) {
                        pendingConversation.updateBasicDetails(conversation);

                        if (pendingConversation.getSelf() != null) {
                            cacheDb.getConversationRepository().update(conversation, conversation.getConversationId());
                            cacheDb.getMemberRepository().insertAll(conversation.getConversationId(),
                                    conversation.getMembers());
                        }

                        if (counter == totalItems) {
                            socketClient.getConversationClient().synchronisationEvent().notifySubscriptions(SynchronisingState.STATE.MEMBERS);
                            socketEventNotifier.notifyConversationListListener(conversationList);
                            socketClient.getConversationClient().getSignallingChannel().updateSyncState(SynchronisingState.STATE.MEMBERS);
                        }
                    }
                }

                @Override
                public void onError(NexmoAPIError error) {
                }
            });
        }
    }

    private  void updateCacheOfMessageEvents(Conversation conversation){
        // Persist (or remove if NOT exist anymore) each message of conversation if it is NOT done yet!
        if(cacheDb.getEventRepository().isAny(conversation.getConversationId()))
            // there are already some messages in the cache, update the cache if needed
            syncMessagesIfNeeded(conversation);
        else // NO any message persisted yet, so DO persist all
            cacheDb.getEventRepository().insertAll(conversation.getConversationId(), conversation.getEvents());
    }

    private void updateCacheOfMembersForConversation(final String cid, RequestHandler<Conversation> requestHandler){
        socketClient.getConversation(new GetConversationRequest(cid, requestHandler));
    }

    private void updateCacheOnMemberEvent(final Member member) {
        this.cacheDb.getMemberRepository().update(member, member.getConversation().getConversationId());
    }

    private void updateCacheAddMember(final Member member) {
        this.cacheDb.getMemberRepository().insert(member, member.getConversation().getConversationId());
    }

    // persist newly created conversation after join/invitation.
    private void updateCacheAddConversation(Conversation conversationToUpdate, RequestHandler<Conversation> requestHandler) {
        if (requestHandler == null) return;
        this.cacheDb.getConversationRepository().insert(conversationToUpdate, conversationToUpdate.getConversationId());

        socketClient.getConversation(new GetConversationRequest(conversationToUpdate.getConversationId(), requestHandler));
    }

    private void getConversation(String cid, RequestHandler<Conversation> requestHandler) {
        if (requestHandler == null) return;
        socketClient.getConversation(new GetConversationRequest(cid, requestHandler));
    }

    private void updateCacheWithNewConversation( Conversation conversationToUpdate ){
        cacheDb.getConversationRepository().insert(conversationToUpdate, conversationToUpdate.getConversationId());
        cacheDb.getMemberRepository().insertAll(conversationToUpdate.getConversationId(), conversationToUpdate.getMembers());
    }

    private void updateCacheNewMessage(Event event) {
        Conversation updatedConversation = event.getConversation();
        this.cacheDb.getConversationRepository().update(updatedConversation, updatedConversation.getConversationId());
        this.cacheDb.getEventRepository().insert(event, updatedConversation.getConversationId());
    }

    private void updateCacheUpdateMessage(Event event) {
        Conversation updatedConversation = event.getConversation();
        this.cacheDb.getConversationRepository().update(updatedConversation, updatedConversation.getConversationId());
        this.cacheDb.getEventRepository().update(event, updatedConversation.getConversationId());
    }

    public void addOrUpdateConversationList(Conversation joinedConversation) {
        synchronized (conversationList) {
            for(int i = 0; i < conversationList.size(); i++){
                if (conversationList.get(i).getConversationId().equals(joinedConversation.getConversationId())){
                    conversationList.set(i, joinedConversation); // replace it
                    return;
                }
            }

            this.conversationList.add(joinedConversation);
        }
    }

    public void updateCallList(com.nexmo.sdk.conversation.client.Call outgoingCall) {
        this.callList.add(outgoingCall);
    }

    private void addOrUpdateCreatedConversationList(Conversation createdConversation) {
        synchronized (createdConversationList) {
            for(int i = 0; i < createdConversationList.size(); i++){
                if (createdConversationList.get(i).getConversationId().equals(createdConversation.getConversationId())){
                    createdConversationList.set(i, createdConversation); // replace it
                    return;
                }
            }

            this.createdConversationList.add(createdConversation);
        }
    }
}

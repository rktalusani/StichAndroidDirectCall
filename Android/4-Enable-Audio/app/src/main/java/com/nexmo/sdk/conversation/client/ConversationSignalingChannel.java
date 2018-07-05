/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.client;

import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.nexmo.sdk.conversation.client.audio.AppRTCAudioManager;
import com.nexmo.sdk.conversation.client.audio.AudioCallEventListener;
import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.client.event.container.SynchronisingState;
import com.nexmo.sdk.conversation.core.client.request.InviteWithAudioRequest;
import com.nexmo.sdk.conversation.core.client.request.audio.AudioEarmuffRequest;
import com.nexmo.sdk.conversation.core.client.request.audio.AudioMuteRequest;
import com.nexmo.sdk.conversation.core.client.request.audio.AudioRingingRequest;
import com.nexmo.sdk.conversation.core.client.request.audio.RtcNewRequest;
import com.nexmo.sdk.conversation.core.client.request.SeenReceiptRequest;
import com.nexmo.sdk.conversation.core.client.request.audio.RtcTerminateRequest;
import com.nexmo.sdk.conversation.core.util.Log;

import com.nexmo.sdk.conversation.client.ImageRepresentation.TYPE;
import com.nexmo.sdk.conversation.client.event.EventType;
import com.nexmo.sdk.conversation.client.event.network.NetworkState;
import com.nexmo.sdk.conversation.core.client.request.CreateConversationRequest;
import com.nexmo.sdk.conversation.core.client.request.DeleteEventRequest;
import com.nexmo.sdk.conversation.core.client.request.GetConversationRequest;
import com.nexmo.sdk.conversation.core.client.request.GetEventsRequest;
import com.nexmo.sdk.conversation.core.client.request.GetUserInfoRequest;
import com.nexmo.sdk.conversation.core.client.request.InviteRequest;
import com.nexmo.sdk.conversation.core.client.request.JoinRequest;
import com.nexmo.sdk.conversation.core.client.request.LeaveRequest;
import com.nexmo.sdk.conversation.core.client.request.PushSubscribeRequest;
import com.nexmo.sdk.conversation.core.client.request.PushSubscriptionRequest;
import com.nexmo.sdk.conversation.core.client.request.PushUnsubscribeRequest;
import com.nexmo.sdk.conversation.core.client.request.Request;
import com.nexmo.sdk.conversation.core.client.request.SendImageMessageRequest;
import com.nexmo.sdk.conversation.core.client.request.SendTextMessageRequest;
import com.nexmo.sdk.conversation.core.client.request.TypingIndicatorRequest;
import com.nexmo.sdk.conversation.core.networking.ImageUploader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.nexmo.sdk.conversation.client.Conversation.MEDIA_TYPE.AUDIO;
import static com.nexmo.sdk.conversation.client.event.NexmoAPIError.noUserLoggedIn;
import static com.nexmo.sdk.conversation.core.networking.Constants.CALL_NAME_SEPARATOR;
import static com.nexmo.sdk.conversation.core.networking.Constants.CALL_PREFIX_NAME;
import static com.nexmo.sdk.conversation.core.networking.ImageUploader.uploadImage;

/**
 * ConversationSignalingChannel.
 *
 * @author emma tresanszki.
 *
 * @hide
 */
public class ConversationSignalingChannel  {

    private static final String TAG = ConversationSignalingChannel.class.getSimpleName();
    SocketClient socketClient;

    private ConversationClient conversationClient;
    private SynchronisingState.STATE syncState = SynchronisingState.STATE.OUT_OF_SYNC;

    ConversationSignalingChannel(ConversationClient conversationClient, SocketClient socketClient) {
        this.conversationClient = conversationClient;
        this.socketClient = socketClient;
    }

    void connect(RequestHandler<User> loginListener) {
        try {
            this.socketClient.connect(loginListener);
        } catch(IllegalStateException e) {
            loginListener.onError(NexmoAPIError.alreadyConnecting());
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    boolean isConnecting() {
        return this.socketClient.getConnectionStatus() == NetworkState.CONNECTED;
    }

    NetworkState getConnectionStatus() {
        return this.socketClient.getConnectionStatus();
    }

    SynchronisingState.STATE getSyncState() {
        return this.syncState;
    }

    synchronized void updateSyncState(SynchronisingState.STATE syncState) {
        this.syncState = syncState;
    }

    // Retrieve the current user of this socket session, if any.
    //will rename this to getSessionLoggedInUser to avoid confusion.
    public User getLoggedInUser() {
        return this.socketClient.getLoggedInUser();
    }

    void enableAllPushNotifications(boolean enable, RequestHandler pushEnableListener) {
        if (enable)
            this.socketClient.pushRegister(pushEnableListener);
        else
            this.socketClient.pushUnregister(pushEnableListener);
    }

    void enablePushNotifications(boolean enable, String cid, RequestHandler<Void> pushEnableListener) {
        PushSubscriptionRequest pushSubscribeRequest;
        if (enable)
            pushSubscribeRequest = new PushSubscribeRequest(cid, pushEnableListener);
        else
            pushSubscribeRequest = new PushUnsubscribeRequest(cid, pushEnableListener);

        this.socketClient.pushSubscribeToConversation(pushSubscribeRequest);
    }

    public void logout(RequestHandler logoutListener) {
        this.socketClient.logout(logoutListener);
    }

    public void newConversation(final String name, RequestHandler listener) {
        CreateConversationRequest createConversationRequest = new CreateConversationRequest(name, listener);
        this.socketClient.newConversation(createConversationRequest);
    }

    public void newConversationWithJoin(final String name, RequestHandler listener) {
        CreateConversationRequest createConversationRequest = new CreateConversationRequest(name, listener);
        this.socketClient.newConversationHelper(createConversationRequest);
    }

    public void createCall(final List<String> users, final RequestHandler<com.nexmo.sdk.conversation.client.Call> listener) {
        if (users == null || users.isEmpty())
            listener.onError(NexmoAPIError.missingParams());
        else {
            StringBuilder callName = new StringBuilder(CALL_PREFIX_NAME + getLoggedInUser().getName());
            for (String username : users) {
                callName.append(CALL_NAME_SEPARATOR);
                callName.append(username);
            }

            newConversationWithJoin(callName.toString(), new RequestHandler<Conversation>() {
                @Override
                public void onError(NexmoAPIError apiError) {
                    listener.onError(apiError);
                }

                @Override
                public void onSuccess(final Conversation result) {
                    Log.d(TAG, "Call conversation created and joined: " + result.getDisplayName());

                    inviteUsersToCall(result, users, listener);
                }
            });
        }
    }

    /**
     * Invite users to a call.
     * If there is one successful invitation being set, continue with the call.
     * If there is no successful invitation, do not enable audio.
     */
    private void inviteUsersToCall(final Conversation conversation, final List<String> users,
                                      final RequestHandler<com.nexmo.sdk.conversation.client.Call> listener) {
        final AtomicInteger invitationsSent = new AtomicInteger(0);
        for (final String user : users) {
            inviteWithAudio(conversation, null, user, false, false, new RequestHandler<Member>() {
                @Override
                public void onError(NexmoAPIError apiError) {
                    listener.onError(apiError);
                }

                @Override
                public void onSuccess(Member result) {
                    if (invitationsSent.incrementAndGet() == 1) {
                        socketClient.getSocketEventHandler().addOrUpdateConversationList(conversation);

                        conversation.media(AUDIO).enable(new AudioCallEventListener() {
                            @Override
                            public void onRinging() {
                                com.nexmo.sdk.conversation.client.Call outgoingCall =
                                        new com.nexmo.sdk.conversation.client.Call(conversation, getLoggedInUser().getName());
                                socketClient.getSocketEventHandler().updateCallList(outgoingCall);

                                listener.onSuccess(outgoingCall);
                            }

                            @Override
                            public void onCallConnected() {
                            }

                            @Override
                            public void onCallEnded() {
                            }

                            @Override
                            public void onGeneralCallError(NexmoAPIError apiError) {
                                listener.onError(apiError);
                            }

                            @Override
                            public void onAudioRouteChange(AppRTCAudioManager.AudioDevice device) {

                            }
                        });
                    }
                }
            });
        }
    }

    /**
     * Check if any of the current conversations has audio enabled.
     */
    public boolean isAudioEnabled() {
        for ( Conversation conversation : this.conversationClient.getConversationList()) {
            if (conversation.isAudioEnabled())
                return true;
        }

        return false;
    }

    public void disableAudio() {
        for ( Conversation conversation : this.conversationClient.getConversationList()) {
            if (conversation.isAudioEnabled()) {
                conversation.media(AUDIO).disable(new RequestHandler<Void>() {
                    @Override
                    public void onError(NexmoAPIError apiError) {
                        Log.d(TAG, "Cannot disable audio. Logout initiated");
                    }

                    @Override
                    public void onSuccess(Void result) {

                    }
                });
                return;
            }
        }
    }

    public void rtcNew(String cid, String memberId, String SDP, RequestHandler listener) {
        RtcNewRequest rtcNewRequest = new RtcNewRequest(cid, memberId, SDP, listener);

        this.socketClient.rtcNew(rtcNewRequest);
    }

    public void rtcTerminate(final String cid, final String memberId, String rtcId, final RequestHandler listener) {
        RtcTerminateRequest rtcTerminateRequest = new RtcTerminateRequest(cid, memberId, rtcId, new RequestHandler<String>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                listener.onError(apiError);
            }

            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "rtcTerminate onSuccess " + result);
                listener.onSuccess(result);
            }
        });

        this.socketClient.rtcTerminate(rtcTerminateRequest);
    }

    public void rtcMute(final String cid, final String memberId, boolean isMute, final RequestHandler listener) {
        AudioMuteRequest muteRequest = new AudioMuteRequest(cid, memberId, isMute, listener);

        this.socketClient.rtcMute(muteRequest);
    }

    public void rtcEarmuff(final String cid, final String memberId, boolean isEarmuffed, final RequestHandler listener) {
        AudioEarmuffRequest earmuffRequest = new AudioEarmuffRequest(cid, memberId, isEarmuffed, listener);

        this.socketClient.rtcEarmuff(earmuffRequest);
    }

    public void mediaRinging(final String cid, final String memberId, boolean isRinging, final RequestHandler listener) {
        AudioRingingRequest ringingRequest = new AudioRingingRequest(cid, memberId, isRinging, listener);

        this.socketClient.audioRinging(ringingRequest);
    }

    void joinConversation(String conversationId, String conversationName, String memberId, RequestHandler<Member> requestHandler) {
        if (getLoggedInUser() != null) {
            JoinRequest joinRequest = JoinRequest.createJoinRequestWithMemberId(conversationId, conversationName, memberId, requestHandler, getLoggedInUser().getUserId(),
                    getLoggedInUser().getName());

            this.socketClient.joinConversation(joinRequest);
        } else
            requestHandler.onError(NexmoAPIError.noUserLoggedInForConversation(conversationId));
    }

    //JoinRequest with username
    void joinUserToConversation(String conversationId, String conversationName, String username, RequestHandler<Member> requestHandler) {
        JoinRequest joinRequest = JoinRequest.createJoinRequestWithUsername(conversationId, conversationName,requestHandler, username);
        this.socketClient.joinConversation(joinRequest);
    }

    // JoinRequest with userId
    void joinUserToConversationWithId(String conversationId, String conversationName, String userId, RequestHandler<Member> requestHandler) {
        JoinRequest joinRequest =  JoinRequest.createJoinRequestWithUserId(conversationId, conversationName,requestHandler, userId);
        this.socketClient.joinConversation(joinRequest);
    }

    void leaveConversation(Conversation conversation, Member member, RequestHandler<Void> leaveListener) {
        // member is allowed to leave if he was invited.
        LeaveRequest leaveRequest = new LeaveRequest(Request.TYPE.LEAVE, conversation.getConversationId(), member, leaveListener);
        this.socketClient.leaveConversation(leaveRequest);
    }

    public void invite(String conversationId, String username, RequestHandler<Member> inviteSendListener) {
        InviteRequest inviteRequest = new InviteRequest(conversationId, username, inviteSendListener);
        this.socketClient.invite(inviteRequest);
    }

    void inviteWithAudio(Conversation conversation, String userId, String username, boolean isMuted, boolean isEarmuffed, RequestHandler<Member> listener) {
        InviteWithAudioRequest inviteWithAudioRequest = new InviteWithAudioRequest(conversation.getConversationId(), userId, username, isMuted, isEarmuffed, listener);

        this.socketClient.createCall(inviteWithAudioRequest);
    }


    // don't allow multiple retrievals just yet.
    // force update conversation list from CS
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    void updateConversationList() {
        this.socketClient.updateConversationList();
    }

    void updateConversationListFromCache() {
        List<Conversation> conversations = CacheDB.getCacheDBInstance().getConversationRepository()
                .read(socketClient.getConversationClient().getUser());

        SocketEventNotifier socketEventNotifier = socketClient.getConversationClient().getEventNotifier();
        SocketEventHandler socketEventHandler = socketClient.getSocketEventHandler();

        for (Conversation conversation : conversations) {
            conversation.setConversationSignalingChannel(socketClient.getConversationClient().getSignallingChannel());
            conversation.setSocketEventNotifier(socketEventNotifier);
        }

        synchronized (socketEventHandler.getConversationList()) {
            socketEventHandler.getConversationList().clear();
            socketEventHandler.getConversationList().addAll(conversations);
        }

        socketClient.getConversationClient().synchronisationEvent().notifySubscriptions(SynchronisingState.STATE.MEMBERS);
        socketEventNotifier.notifyConversationListListener(socketEventHandler.getConversationList());
        updateSyncState(SynchronisingState.STATE.MEMBERS);
    }

    public void getConversation(Conversation conversation, RequestHandler<Conversation> requestHandler) {
        // if there is a connection to the CAPI, send the request to CAPI, else return from cache
        String cid = conversation.getConversationId();
        if(isConnecting()){
            GetConversationRequest getConversationRequest = new GetConversationRequest(cid, requestHandler);

            this.socketClient.getConversation(getConversationRequest);
        }
        else if(conversationClient.getUser() != null){
            // There is no connection to CAPI, get members and add to conversation
            Conversation cachedConversation = CacheDB.getCacheDBInstance().getConversationRepository().read(cid);
            if (cachedConversation == null) {
                requestHandler.onSuccess(null);
                return;
            }

            SocketEventNotifier socketEventNotifier = socketClient.getConversationClient().getEventNotifier();
            conversation.setConversationSignalingChannel(socketClient.getConversationClient().getSignallingChannel());
            conversation.setSocketEventNotifier(socketEventNotifier);
            conversation.updateBasicDetails(cachedConversation);
            conversation.setSelf(cachedConversation.getSelf());

            List<Member> members = CacheDB.getCacheDBInstance().getMemberRepository().read(cid);
            for (Member member : members)
                conversation.addMember(member);

            requestHandler.onSuccess(conversation);
        }
        else
            requestHandler.onError(NexmoAPIError.noUserLoggedInForConversation(cid));
    }

    void getMembersBeforeGetEvents(Conversation conversation, final String startId, final String endId, final RequestHandler<Conversation> requestHandler) {
        Log.d(TAG, "getMembersBeforeGetEvents");

        getConversation(conversation, new RequestHandler<Conversation>() {
            @Override
            public void onSuccess(Conversation conversation) {
                getEvents(conversation, startId, endId, requestHandler);
            }

            @Override
            public void onError(NexmoAPIError error) {
                Log.e(TAG, "getMembersBeforeGetEvents: " + error.getMessage());
            }
        });
    }

    void getEvents(final Conversation conversation, String startId, String endId, RequestHandler<Conversation> requestHandler) {
        String lastCachedEventId = CacheDB.getCacheDBInstance()
                .getEventRepository().getLastEventId(conversation.getConversationId());
        String lastEventIdFromCAPI = (conversation.getLastMessageEventId() != null ? conversation.getLastMessageEventId() : conversation.getLastEventId());
        Log.d(TAG, "Conversation: LastCachedEventId: " + lastCachedEventId + " AND lastEventIdFromCAPI: " + lastEventIdFromCAPI);

        // if there is a connection to CAPI, and conversation.last_event_id from CAPI does NOT matches the one from cache
        // then go to CAPI, otherwise get from Cache
        if(isConnecting() && !TextUtils.equals(lastEventIdFromCAPI, lastCachedEventId)) {
            Log.d(TAG, "getEvents from CAPI");
            GetEventsRequest request = new GetEventsRequest(conversation, startId, endId, requestHandler);

            this.socketClient.getEvents(request);
        }
        else {
            CacheDB.getCacheDBInstance()
                    .getEventRepository().read(conversation.getConversationId(), conversation);
            requestHandler.onSuccess(conversation);
        }
    }

    public void sendText(Conversation conversation, final String message, RequestHandler listener) {
        SendTextMessageRequest sendMessageRequest = new SendTextMessageRequest(
                conversation.getConversationId(), conversation.getMemberId(), message, listener);

        this.socketClient.sendText(sendMessageRequest);
    }

    /**
     * Upload image that will result in 3 Image Representations:
     * <ul>
     *     <li>{@link Image#getOriginal()}</li>
     *     <li>{@link Image#getMedium()}</li>
     *     <li>{@link Image#getThumbnail()} </li>
     * </ul>
     *
     * <p> By default, the Thumbnail representation of the image will be downloaded.
     * For manually downloading other image representations use
     * {@link Image#download(TYPE, RequestHandler)} </p>
     *
     * <p>It's possible to cancel request which is in progress by calling cancel method on returned Call instance.</p>
     *
     * @param conversation
     * @param imagePath
     * @param listener
     * @return Call instance or null
     */
    public ImageUploader.CancelableCall sendImage(final Conversation conversation, final String imagePath, final RequestHandler listener) {
        final Callback uploadCallback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure upload " + e.toString());
                listener.onError(new NexmoAPIError(NexmoAPIError.UPLOAD_FAILURE, conversation.getConversationId(), e.toString()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "onResponse upload" + response.toString());

                if (!response.isSuccessful()) {
                    listener.onError(new NexmoAPIError(NexmoAPIError.UPLOAD_FAILURE, conversation.getConversationId(), "Unexpected code: " + response.code()));
                    response.body().close();
                    return;
                }

                try {
                    JSONObject body = new JSONObject(response.body().string());
                    handleResponse(body);
                } catch (JSONException e) {
                    e.printStackTrace();
                    NexmoAPIError.forward(listener, NexmoAPIError.unexpectedResponse(null, conversation.getConversationId()));
                }

                response.body().close();
            }

            void handleResponse(JSONObject body) throws JSONException {
                ImageRepresentation original = ImageRepresentation.fromJson(TYPE.ORIGINAL, body.getJSONObject("original"));
                ImageRepresentation medium = ImageRepresentation.fromJson(TYPE.MEDIUM, body.getJSONObject("medium"));
                ImageRepresentation thumbnail = ImageRepresentation.fromJson(TYPE.THUMBNAIL, body.getJSONObject("thumbnail"));

                Log.d(TAG, "upload response: " + body.toString());
                SendImageMessageRequest sendMessageRequest = new SendImageMessageRequest(conversation.getConversationId(), conversation.getMemberId(), imagePath, listener);
                sendMessageRequest.updateImages(original, medium, thumbnail, body);
                socketClient.sendImage(sendMessageRequest);
            }
        };

        return uploadImage(imagePath, uploadCallback, this.conversationClient.getConfig().getImageProcessingServiceUrl(), this.conversationClient.getToken());
    }

    void getUserInfo(String userId, RequestHandler<User> userInfoListener) {
        GetUserInfoRequest request = new GetUserInfoRequest(userId, userInfoListener);
        this.socketClient.getUser(request);
    }

    void sendSeenEvent(Event event, RequestHandler<SeenReceipt> listener) {
        SeenReceiptRequest seenReceiptRequest = new SeenReceiptRequest(event, event.getConversation().getSelf(), listener);
        this.socketClient.sendReceiptRecord(seenReceiptRequest);
    }

    void sendTypingIndicator(Conversation conversation, Member.TYPING_INDICATOR typingIndicator, RequestHandler listener){
        TypingIndicatorRequest request = TypingIndicatorRequest.forType(typingIndicator,
                conversation.getConversationId(), conversation.getMemberId(), listener);

        this.socketClient.sendTypingIndicator(request);
    }

    void deleteEvent(Conversation conversation, Event event, RequestHandler<Void> listener) {
        if (event.getType() == EventType.TEXT) {
            DeleteEventRequest deleteEventRequest = new DeleteEventRequest(conversation.getConversationId(),
                    conversation.getMemberId(), event.getId(), listener);

            this.socketClient.deleteEvent(deleteEventRequest);
        } else if (event.getType() == EventType.IMAGE)
            socketClient.getSocketEventHandler().deleteImageRepresentations((Image) event, listener);
    }

    ConversationClient getConversationClient(){
        return this.conversationClient;
    }

    /**
     * Checks the validity of inputs before generating a remote request.
     *
     * <p> The presence/absence of a converationId determines how if the conversation object will
     * be associated with the Error if/when one occurs.
     *
     *<p> It also implicitly checks if the session is associated with a {@link User}.
     *
     * @param listener        The completion listener in charge of dispatching Errors.
     * @param conversationId  Optional, Conversation ID, to be associated with generated Errors.
     * @return                The result of the validity of the check. Returns false if validity checks
     *                        fails and may dispatch Error to the completion listner if present. It will return true
     *                        if the validity check passes.
     */
    boolean isValidInput(RequestHandler listener, String conversationId) {
        boolean isValid = false;
        if (listener != null) {
            if (this.getLoggedInUser() != null)
                isValid = true;
            else
                noUserLoggedInError(conversationId, listener);
        } else {
            Log.d(TAG, "Listener is mandatory");
        }
        return isValid;
    }

    private void noUserLoggedInError(String conversationId, RequestHandler listener) {
        if (conversationId == null)
            listener.onError(noUserLoggedIn());
        else
            listener.onError(NexmoAPIError.noUserLoggedInForConversation(conversationId));
    }

    SocketClient getSocketClient() {
        return this.socketClient;
    }
}

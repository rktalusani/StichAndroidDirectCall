/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.client;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.nexmo.sdk.conversation.client.audio.AppRTCAudioManager;
import com.nexmo.sdk.conversation.client.audio.AudioCallEventListener;
import com.nexmo.sdk.conversation.client.audio.AudioCallManager;
import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.client.event.container.Invitation;
import com.nexmo.sdk.conversation.client.event.container.Receipt;
import com.nexmo.sdk.conversation.config.Defaults;
import com.nexmo.sdk.conversation.core.util.Log;

import com.nexmo.sdk.conversation.client.event.EventType;
import com.nexmo.sdk.conversation.client.event.ResultListener;
import com.nexmo.sdk.conversation.core.networking.Constants;
import com.nexmo.sdk.conversation.core.networking.ImageUploader;
import com.nexmo.sdk.conversation.core.util.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.nexmo.sdk.conversation.core.persistence.contract.ConversationContract.ConversationEntry.*;

/**
 * A container that you use to manage communications between Members,
 * the conversation history and context.
 *
 * <p>Users cannot manually create conversation objects, but creating a Conversation via
 * {@link ConversationClient#newConversation(String, RequestHandler<Conversation>)}
 * will dispatch a new {@link Conversation} object.
 * </p>
 *
 * <p>Every action on a conversation will dispatch a completion callback that
 * contains an updated Conversation object. For ex:
 * <pre>
 *     myConversation.sendText("payload", new RequestHandler<Event>() {
 *         &#64;Override
 *         public void onSuccess(Event text) {
 *              // Update the application UI here if needed with updated conversation.
 *         }
 *         &#64;Override
 *         public void onError(NexmoAPIError error) {
 *              // Update the application UI here if needed.
 *         }
 *     });
 * </pre>
 * </p>
 *
 * <p>Listen for incoming events {@link Conversation#messageEvent()} </p>
 * <p>Example usage:</p>
 * <pre>
 *   conversation.messageEvent().add(new ResultListener<Event>() {
 *    &#64;Override
 *    public void onSuccess(Event result) {
 *        notifyUI("New event from " + result.getMember());
 *    }
 *  });
 * </pre>
 *
 * <p>Sending a text message to a specific conversation is achieved using
 * {@link Conversation#sendText(String, RequestHandler<Event>)}</p>
 * <p>Example usage:</p>
 * <pre>
 *     myConversation.sendText("message payload...", new RequestHandler<Event>() {
 *         &#64;Override
 *         public void onSuccess(Event event) {
 *              // Update the application UI here if needed.
 *         }
 *         &#64;Override
 *         public void onError(NexmoAPIError error) {
 *              // Update the application UI here if needed.
 *         }
 *    });
 * </pre>
 *
 * <p>Marking an event as seen is achieved using
 * {@link Event#markAsSeen(RequestHandler)}</p>
 * <p>Example usage:</p>
 * <pre>
 *     textEvent.markAsSeen( new RequestHandler<SeenReceipt>() {
 *         &#64;Override
 *         public void onSuccess(SeenReceipt seenReceipt) {
 *              // Update the application UI here if needed.
 *         }
 *         &#64;Override
 *         public void onError(NexmoAPIError error) {
 *              // Update the application UI here if needed.
 *         }
 *    });
 * </pre>
 *
 * <p>Listen for typing indicator events {@link Conversation#typingEvent().add(ResultListener<Member)}  </p>
 * <p>Example usage:</p>
 * <pre>
 *     myConversation.typingEvent().add(new ResultListener<Member) {
 *         &#64;Override
 *         public void onSuccess(Member member);
 *              // Update the application UI to highlight when someone else is typing.
 *         }
 *     });
 * </pre>
 *
 * @author emma tresanszki.
 */
public class Conversation {
    private static final String TAG = Conversation.class.getSimpleName();

    public enum MEDIA_TYPE { AUDIO }
    private long typingTimeOutLength = Defaults.TYPING_TIMER_LENGTH;

    //TODO v2.0 key value map based on memberId to fast up the search
    private List<Member> members = Collections.synchronizedList(new ArrayList<Member>());
    private List<Event> events = Collections.synchronizedList(new ArrayList<Event>());
    //self as member of the conversation
    private Member self;

    private String displayName;
    private Date creationDate;
    private String conversationId;
    public AudioCallEventListener audioListener = null;

    private String lastEventId; // last known event conversationId, used for paginated access.

    private ConversationSignalingChannel conversationSignalingChannel;
    private SocketEventNotifier socketEventNotifier;
    // internally mark conversations asDirty when they require force sync.
    private boolean isDirty = false;

    private AudioCallManager audioCallManager;
    private boolean isTypingOn = false;
    private boolean isTypingOff = false;
    @NonNull
    private Handler typingHandler;

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Conversation(final String displayName) {
        this.displayName = displayName;
        createTypingThreads();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Conversation(final String displayName, final String cid) {
        this(displayName);
        this.conversationId = cid;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Conversation(final String displayName, final String cid, final String lastEventId) {
        this(displayName, cid);
        this.lastEventId = lastEventId;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Conversation(final String displayName, final String cid, final Member member) {
        this(displayName, cid);
        if (member != null)
            setSelf(member);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Conversation(final String displayName, final String cid, final Member member, final Date creationDate) {
        this(displayName, cid, member);
        this.creationDate = creationDate;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Conversation(final String name, final String cid, final String lastEventId, final Member member) {
        this(name, cid, lastEventId);
        if (member != null)
            setSelf(member);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Conversation(final String displayName, final String cid, final String lastEventId, final Member member,
                        final Date creationDate, List<Member> members) {
        this(displayName, cid, lastEventId, member, creationDate);
        this.members = members;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Conversation(final String name, final String cid, final String lastEventId, final Date creationDate) {
        this(name, cid, lastEventId);
        this.creationDate = creationDate;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Conversation(final String displayName, final String cid, final String lastEventId, final Member member, final Date creationDate) {
        this(displayName, cid, lastEventId, member);
        this.creationDate = creationDate;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Conversation(final String displayName, final String cid, final String lastEventId, final Member member, final Date creationDate,
            final List<Member> members, final List<Event> events) {
        this(displayName, cid, lastEventId, member, creationDate, members);
        this.events = events;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected void setTypingTimeOutLength(long typingTimeOutLength) {
        this.typingTimeOutLength = typingTimeOutLength;
    }

    protected Conversation(Conversation conversation) {
        this(conversation.getDisplayName(), conversation.getConversationId(), conversation.getLastEventId(),
                conversation.getSelf(),conversation.getCreationDate());
    }

    //**********************************************************************************************
    //*                                                                                            *
    //*                         Public Interface of Conversation Object                            *
    //*                                                                                            *
    //**********************************************************************************************

    /**
     * Retrieve full conversation information, like members and conversation details.
     * available information includes:
     * <ul>
     *     <li>{@link Conversation#getDisplayName()} </li>
     *     <li>{@link Conversation#getCreationDate()} </li>
     *     <li>{@link Conversation#getMembers()} </li>
     * </ul>
     *
     * <p>For retrieving the text/image events use {@link Conversation#updateEvents(String, String, RequestHandler<Conversation>)}.
     *
     * <p>Please bear in mind to refresh the members list on a regular basis to avoid events from 'unknown' members.
     *
     * <p>For the first time launching the app, the local cache will not present any results.
     *
     * @param requestHandler The handler in charge of dispatching the result.
     */
    public void update(RequestHandler<Conversation> requestHandler) {
        if(conversationSignalingChannel.isValidInput(requestHandler, conversationId) == false)
            return;

        this.conversationSignalingChannel.getConversation(this, requestHandler);
    }

    /**
     * Retrieve the event history of a {@link Conversation}.
     * available information includes:
     * <ul>
     *     <il>{@link Conversation#getTexts()} for text messages.</il>
     *     <li>{@link Conversation#getImages()} for image messages.</li>
     *     <li>{@link Conversation#getLastEventId()} for assessing when a sync is required.</li>
     * </ul>
     * @since 0.1.0 {@link Conversation#update(RequestHandler<Conversation>)} is done silently, to ensure
     * members information can be matched against new events.
     *
     * <p>Note: If  members are out of sync make sure to call {@link Conversation#update(RequestHandler<Conversation>)}
     * before this,in order to retrieve the already existing members inside this conversation.</p>
     *
     * @param startId              Optional, the first event id to get.
     * @param endId                Optional, the last event id to get.
     * @param requestHandler       The handler in charge of dispatching the result.
     */
    public void updateEvents(String startId, String endId, RequestHandler<Conversation> requestHandler) {
        //temporary. until we merge the caching implementation:
        //fetch the members in case conversation.update wasn't previously done by dev.
        if (!conversationSignalingChannel.isValidInput(requestHandler, conversationId)) return;

        if (this.members.size() != 0)
            Log.d(TAG, members.toString());

        if (this.members.size() == 0)
            this.conversationSignalingChannel.getMembersBeforeGetEvents(this, startId, endId, requestHandler);
        else
            this.conversationSignalingChannel.getEvents(Conversation.this, startId, endId, requestHandler);
    }

    /**
     * Current {@link User} joins a {@link Conversation}.
     *
     * <p>Note: any {@link User} that joins a {@link Conversation} becomes a {@link Member}.</p>
     * <p>The {@link User} will always be current user that has successfully logged-in.</p>
     *
     * @param requestHandler      The handler in charge of dispatching the result.
     */
    public void join(RequestHandler<Member> requestHandler) {
        if(!conversationSignalingChannel.isValidInput(requestHandler, conversationId)) return;


        this.conversationSignalingChannel.joinConversation(this.conversationId, this.displayName, ((this.self != null) ? this.self.getMemberId() : null), requestHandler);
    }

    private void joinWithAudio(final RequestHandler<Member> requestHandler) {
        if(!conversationSignalingChannel.isValidInput(requestHandler, conversationId))
            return;

        join(new RequestHandler<Member>() {
            @Override
            public void onError(NexmoAPIError apiError) {

            }

            @Override
            public void onSuccess(Member result) {

                final AudioCallEventListener audioCallEventListener = new AudioCallEventListener() {
                    @Override
                    public void onRinging() {

                    }

                    @Override
                    public void onCallConnected() {

                    }

                    @Override
                    public void onCallEnded() {

                    }

                    @Override
                    public void onGeneralCallError(NexmoAPIError apiError) {

                    }

                    @Override
                    public void onAudioRouteChange(AppRTCAudioManager.AudioDevice device) {

                    }
                };

                requestHandler.onSuccess(result);


                Context context = getSignallingChannel().socketClient.getConversationClient().getContext();
                Handler handler = new Handler(context.getMainLooper());
                Runnable runAudioCall = new Runnable() {
                    public void run() {
                        audio().enable(audioListener);
                    }
                };
                handler.post(runAudioCall);
            }
        });
    }

    /**
     * Current {@link User} allows another user to join a {@link Conversation}.
     *
     * <p>Note: any {@link User} that joins a {@link Conversation} becomes a {@link Member}.</p>
     *
     * @param username           The user id or name to join to a conversation.
     * @param requestHandler     The handler in charge of dispatching the result.
     */
    public void join(String username, RequestHandler<Member> requestHandler) {
        if(!conversationSignalingChannel.isValidInput(requestHandler, conversationId)) return;

        if (!TextUtils.isEmpty(username)) {
            // lookup invitations and reuse same memberId to accept invite instead of creating new member.
            String memberId = this.containsMember(username);
            if (!TextUtils.isEmpty(memberId))
                this.conversationSignalingChannel.joinConversation(this.conversationId, this.displayName, memberId, requestHandler);
            else
                this.conversationSignalingChannel.joinUserToConversation(this.conversationId, this.displayName, username, requestHandler);
        }
        else requestHandler.onError(new NexmoAPIError(NexmoAPIError.MISSING_PARAMS, this.conversationId,
                    "User name cannot be empty when joining a conversation."));
    }

    /**
     * Invite a user to a certain conversation.
     * The receiving  user can accept an invite {@link Conversation#join(RequestHandler)} or reject an invite
     * {@link Conversation#leave(RequestHandler)}.
     *
     * @param username            The user id or name.
     * @param inviteSendListener  The completion listener.
     */
    public void invite(String username, RequestHandler<Member> inviteSendListener) {
        if(!conversationSignalingChannel.isValidInput(inviteSendListener, conversationId)) return;

        if (TextUtils.isEmpty(username))
            inviteSendListener.onError(new NexmoAPIError(NexmoAPIError.MISSING_PARAMS, this.conversationId, "This invite cannot be sent to empty username"));
        else
            this.conversationSignalingChannel.invite(this.conversationId, username, inviteSendListener);
    }

    /**
     * Invite a user to an audio conversation, using preset params for muted and earmuff.
     * Invite by either userId or username, providing one of this params is mandatory.
     *
     * The receiving  user can accept an invite {@link Conversation#join(RequestHandler)} or reject an invite
     * {@link Conversation#leave(RequestHandler)}.
     *
     * <p>A user accepting an invite to a conversation with audio enabled will need to manually
     * enable audio after accepting invitation.</p>
     *
     * @param userId              The user id.
     * @param username            The user name.
     * @param muted               The user will join as muted or not.
     * @param isEarmuff           The user will join as earmuff or not.
     * @param requestHandler      The completion listener.
     */
    public void inviteWithAudio(String userId, String username, boolean muted, boolean isEarmuff, RequestHandler<Member> requestHandler) {
        if(!conversationSignalingChannel.isValidInput(requestHandler, conversationId))
            return;

        if (TextUtils.isEmpty(username) && TextUtils.isEmpty(userId))
            requestHandler.onError(new NexmoAPIError(NexmoAPIError.MISSING_PARAMS, this.conversationId, "This invite cannot be sent with an empty user"));
        else
            this.conversationSignalingChannel.inviteWithAudio(this, userId, username, muted, isEarmuff, requestHandler);
    }

    /**
     * Leave a conversation the {@link User} has joined, or is invited to.
     * No ownership is enforced on conversation, so any member may invite or remove others.
     *
     * @param leaveListener     The completion listener in charge of dispatching the result.
     */
    public void leave(RequestHandler<Void> leaveListener) {
        if(!conversationSignalingChannel.isValidInput(leaveListener, conversationId)) return;

        this.conversationSignalingChannel.leaveConversation(this, self, leaveListener);
    }

    /**
     * Send a typing indicator {{@link com.nexmo.sdk.conversation.client.Member.TYPING_INDICATOR#ON} event for the
     * current member of a conversation.
     *
     * @param listener  The listener in charge of dispatching the completion result.
     */
    public void startTyping(RequestHandler<Member.TYPING_INDICATOR> listener) {

        if (!isTypingOn) {
            isTypingOn = true;
            isTypingOff = false;
            sendTyping(Member.TYPING_INDICATOR.ON, listener);
        }

        postRunnables(true);

    }

    /**
     * Send a typing indicator {@link com.nexmo.sdk.conversation.client.Member.TYPING_INDICATOR#OFF} event for the
     * current member of a conversation.
     *
     * @param listener  The listener in charge of dispatching the completion result.
     */
    public void stopTyping(RequestHandler<Member.TYPING_INDICATOR> listener) {

        if (!isTypingOff) {
            isTypingOff = true;
            isTypingOn = false;
            sendTyping(Member.TYPING_INDICATOR.OFF, listener);
        }

        postRunnables(false);
    }

    /**
     * Send a Text event to a conversation.
     *
     * <p> For listening to incoming/sent events events, register using
     * {@link Conversation#messageEvent()} </p>
     *
     * @param message          The payload.
     * @param listener The listener in charge of dispatching the completion result.
     */
    public void sendText(String message, RequestHandler<Event> listener) {
        if (listener == null)
            Log.d(TAG, "Listener is mandatory");
        else if (this.getSelf() == null)
            listener.onError(NexmoAPIError.noUserLoggedInForConversation(this.getConversationId()));
        else
            this.conversationSignalingChannel.sendText(this, message, listener);
    }

    public void setAudioRoute(AppRTCAudioManager.AudioDevice audioRoute) {
        audioCallManager.setAudioRoute(audioRoute);
    }

    /**
     * Send/Upload an Image event to a conversation.
     *
     * <p>
     * Once the image is uploaded, you will be able to access all 3 representations:
     * <ul>
     *     <li>Original at 100% size via {@link Image#getOriginal()}</li>
     *     <li>Medium at 50% size via {@link Image#getMedium()}</li>
     *     <li>Thumbnail at 10% size via {@link Image#getThumbnail()} </li>
     * </ul>
     * </p>
     *
     * <p>Each {@link ImageRepresentation} contains a {@link ImageRepresentation#bitmap}
     * that can be used to update UI</p>
     *
     * <p>Please note that sending image does not get retried in case an error occurs. It is up to
     * the user to resend the image when convenient.</p>
     *
     * <p> For listening to incoming/sent events events, register using
     * {@link Conversation#messageEvent()} </p>
     *
     * Note: at this only the image path is allowed, but bitmap or file will soon be added.
     * @param imagePath         The image location, mandatory.
     * @param imageSendListener The completion listener, mandatory.
     * @return object to control upload process or null in case of error.
     */
    public ImageUploader.CancelableCall sendImage(String imagePath, RequestHandler<Event> imageSendListener) {
        if (!conversationSignalingChannel.isValidInput(imageSendListener, conversationId)) return null;

        Context context = this.conversationSignalingChannel.getConversationClient().getContext();
        boolean isPermissionGranted = context == null || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (TextUtils.isEmpty(imagePath))
            imageSendListener.onError(new NexmoAPIError(NexmoAPIError.MISSING_PARAMS, this.conversationId, "No imagePath was provided"));
        else if (!isPermissionGranted) {
            imageSendListener.onError(NexmoAPIError.permissionRequired(this.conversationId));
        } else {
            File file = new File(imagePath);
            if (!file.exists())
                imageSendListener.onError(new NexmoAPIError(NexmoAPIError.INVALID_PARAMS, this.conversationId, "Image file doesn't exist"));
            else if (file.length() > Constants.MAX_ALLOWED_FILESIZE)
                imageSendListener.onError(new NexmoAPIError(NexmoAPIError.UPLOAD_FAILURE, this.conversationId, "Image is too big, it should be 15mb max"));
            else
                return this.conversationSignalingChannel.sendImage(this, imagePath, imageSendListener);
        }
        return null;
    }

    /**
     * Delete a message event
     *
     * @param event The event that needs to be deleted.
     */
    public void deleteEvent(Event event, RequestHandler<Void> eventDeleteListener) {
        if (!conversationSignalingChannel.isValidInput(eventDeleteListener, conversationId)) return;

        if (event.getDeletedTimestamp() != null)
            eventDeleteListener.onError(new NexmoAPIError(NexmoAPIError.INVALID_ACTION, this.conversationId, "This message was already deleted"));
        else
            this.conversationSignalingChannel.deleteEvent(this, event, eventDeleteListener);
    }

    /**
     * Retrieves event source for given event type for THIS conversation
     * @param eventType
     * @param <T>
     * @return EventSource
     */
    private <T> EventSource<T> getEventSource(String eventType) {
        return conversationSignalingChannel
                .getConversationClient()
                .getEventSource(eventType + "-" + conversationId);
    }

    /**
     * Register for receiving member joined events.
     *
     * @return The listener in charge of dispatching the result.
     */
    public EventSource<Member> memberJoinedEvent() {
        return getEventSource("memberJoinedEvent");
    }

    /**
     * Register for receiving member invited events.
     * This applies for other members being invited to this conversation.
     *
     * @return The listener in charge of dispatching the result.
     */
    public EventSource<Invitation> memberInvitedEvent() {
        return getEventSource("memberInvitedEvent");
    }

    /**
     * Register for receiving member left events.
     *
     * @return The listener in charge of dispatching the result.
     */
    public EventSource<Member> memberLeftEvent() {
        return getEventSource("memberLeftEvent");
    }

    /**
     * Register for receiving typing indicator events from other members.
     *
     * @return The listener in charge of dispatching the result.
     */
    public EventSource<Member> typingEvent() {
        return getEventSource("typingEvent");
    }

    /**
     * Listen for incoming messages from this conversation.
     *
     * Event types:
     * <ul>
     *     <li>"text" Text event</li>
     *     <li>"image" Image event.</li>
     *     <li>"event:delete" Event delete of either Text or Image.</li>
     *     <li>"member:media" A member has either enabled or disabled audio.
     *          Typically when a member enables/disables audio with {@link AudioCallManager#enable(AudioCallEventListener)}
     *          and {@link AudioCallManager#disable(RequestHandler)}.</li>
     * </ul>
     *
     * <p> For incoming {@link Image} event type, the THUMBNAIL representation will be downloaded automatically.</p>
     *
     */
    public EventSource<Event> messageEvent() {
        return getEventSource("messageEvent");
    }
    public void setAudioEventListener(AudioCallEventListener listener){
        audioListener = listener;
    }
    /**
     * Register for receiving seen receipt events.
     * <p>When registering for receipt records, the current member will receive all of them, regardless
     * of whether he was the one sending the associated messages.</p>
     *
     * <p>Note: None of the receipt records can be un-done.</p>
     */
    public EventSource<Receipt<SeenReceipt>> seenEvent() {
        return getEventSource("seenEvent");
    }

    /**
     * Register for receiving delivered receipt events.
     *
     */
    public EventSource<Receipt<DeliveredReceipt>> deliveryEvent() {
        return getEventSource("deliveryEvent");
    }

    /**
     * The name of this conversation.
     *
     * @return The name used to create this conversation.
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * The id of this conversation.
     *
     * @return The auto-generated application unique ID of this conversation.
     */
    public String getConversationId() {
        return this.conversationId;
    }

    /**
     * Get the member ID of the current user in this conversation.
     *
     * @return The auto-generated memberId for current user, or null if user is not part of the conversation.
     */
    public String getMemberId() {
        return (this.self != null ? this.self.getMemberId() : null);
    }

    /**
     * Get a member based on it's member ID, synchronously.
     * If the member appears to be missing this conversation is out of sync, call
     * {@link Conversation#update(RequestHandler<Conversation>)} to force fetch all members.
     *
     * @param member_id The auto-generated memberID.
     * @return          The member or null if member is not found locally.
     */
    public Member getMember(final String member_id) {
        for (Member member : this.members) {
            if (TextUtils.equals(member.getMemberId(), member_id))
                return member;
        }
        return null;
    }

    /**
     * Get a member based on it's user details, synchronously.
     * If the member appears to be missing this conversation is out of sync, call
     * {@link Conversation#update(RequestHandler<Conversation>)} to force fetch all members.
     *
     * @param user      The current user.
     * @return          The member or null if member is not found locally.
     */
    public Member getMember(final User user) {
        for (Member member : this.members) {
            if (TextUtils.equals(member.getUserId(), user.getUserId()))
                return member;
        }
        return null;
    }

    /**
     * Get all members that are JOINED/INVITED/LEFT in this conversation, synchronously.
     *
     * @return The list of all members.
     */
    public List<Member> getMembers() {
        return this.members;
    }

    /**
     * Add or update member.
     * TODO hide
     *
     * @param member
     */
    void addMember(Member member) {
        synchronized(members) {
            for (int i=0 ; i < this.members.size(); i++) {
                if (TextUtils.equals(this.members.get(i).getMemberId(), member.getMemberId())) {
                    this.members.set(i, member);
                    return;
                }
            }
            this.members.add(member);
        }
    }

    /**
     * Get the member object for current user.
     * Once a user joins or gets invited to a conversation, a Member object will be created with same
     * username and id, and auto-generated member ID.
     *
     * <p>Note: If current user it not part of this conversation (neither joined or was invited to), then
     * this method will return null.</p>
     *
     * @return User's member in current conversation.
     */
    public Member getSelf() {
        return this.self;
    }

    boolean isSelf(Member member) {
        return (this.getSelf() != null && this.getSelf().getMemberId().equals(member.getMemberId()));
    }

    /**
     * Get an event based on it's ID, synchronously.
     *
     * @param event_id The event ID.
     * @return         The event object.
     */
    public Event getEvent(final String event_id) {
        for (Event event : this.events) {
            if (TextUtils.equals(event.getId(), event_id))
                return event;
        }
        return null;
    }

    /**
     * Get the list of events that were lastly retrieved, synchronously.
     *
     * @return The list of events in this conversation.
     */
    public List<Event> getEvents() {
        return this.events;
    }

    /**
     * Find a Text message based on event ID, synchronously.
     *
     * @param id The event id.
     * @return   The text object.
     */
    public Text findText(final String id) {
        return (Text) findEvent(id);
    }

    /**
     * Find an Image message based on event ID, synchronously.
     *
     * @param id The event id.
     * @return   The image object.
     */
    public Image findImage(final String id) {
        return (Image) findEvent(id);
    }

    /**
     * Get the list of image events <b>only</b> , synchronously.
     *
     * @return The list of image events in this conversation.
     */
    public List<Image> getImages() {
        ArrayList<Image> images = new ArrayList<>();
        for (Event event : this.events) {
            if (event.getType() == EventType.IMAGE)
                images.add((Image) event);
        }
        return images;
    }

    /**
     * Get the list of text events <b>only</b> , synchronously.
     *
     * @return The list of text events in this conversation.
     */
    public List<Text> getTexts() {
        ArrayList<Text> texts = new ArrayList<>();
        for (Event event : this.events) {
            if (event.getType() == EventType.TEXT)
                texts.add((Text) event);
        }
        return texts;
    }


    /**
     * Get the creation date of this conversation, synchronously.
     *
     * @return creation date.
     */
    public Date getCreationDate() {
        return this.creationDate;
    }

    /**
     * @Deprecated
     * The last event id of this conversation.
     *
     * @return event id.
     */
    public String getLastEventId() {
        return this.lastEventId;
    }

    public AudioCallManager media(MEDIA_TYPE  mediaType) {
        switch (mediaType) {
            case AUDIO: {
                return this.audio();
            }
            default:
                return this.audio();
        }
    }

    /**
     * Get Audio manager.
     * <p>In order to enable audio in a conversation perform {@link Conversation#media(MEDIA_TYPE)#enable(AudioCallEventListener)}</p>
     *
     * @return Audio manager.
     */
    private AudioCallManager audio() {
        if (audioCallManager == null) {
            Context context = this.conversationSignalingChannel.getConversationClient().getContext();
            audioCallManager = new AudioCallManager(context, this.getConversationId(), this.getMemberId(), this.conversationSignalingChannel);

            getSignallingChannel().socketClient.setRtcEventsListener(audioCallManager);
        }

        return this.audioCallManager;
    }

    boolean isAudioEnabled() {
        return (audioCallManager != null && audio().getState() != AudioCallManager.AudioCallState.ReadyForCall);
    }

    /**
     * Construct object from the network response.
     * @param body network response
     *
     * @throws JSONException if required fields don't exist
     */
    public static Conversation fromJson(JSONObject body) throws JSONException {
        String cname;
        if(body.has("display_name"))
            cname = body.getString("display_name");
        else
            cname = body.getString("name");

        String cid          = body.getString("id");
        String lastEventId  = body.getString("sequence_number");
        String memberId     = body.optString("member_id");
        String userId       = body.optString("user_id");
        String userName     = body.optString("user_name");
        String memberState  = body.optString("state");
        JSONObject dateBody = body.optJSONObject("timestamp");
        Date timestamp      = null;

        if (dateBody != null)
            timestamp = DateUtil.parseDateFromJson(dateBody, "created");

        if (TextUtils.isEmpty(memberId))
            return new Conversation(cname, cid, lastEventId, timestamp);

        Member member = new Member(userId, userName, memberId, Member.STATE.fromId(memberState));
        Conversation conversation = new Conversation(cname, cid, lastEventId, member, timestamp);
        member.setConversation(conversation);

        return conversation;
    }

    public static Conversation fromCursor(Cursor cursor) {
        if (cursor == null) return null;
        Member self = Member.fromCursor(cursor);

        Date dateCreated;
        try {
            dateCreated = DateUtil.formatIso8601DateString(cursor.getString(cursor.getColumnIndex(COLUMN_CREATED)));
        } catch (ParseException e) {
            dateCreated = null;
            Log.d(TAG, "getConversationsList: wrong date format");
        }
        String cid = cursor.getString(cursor.getColumnIndex(COLUMN_CID));
        return new Conversation(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                cid,
                cursor.getString(cursor.getColumnIndex(COLUMN_LAST_EVENT_ID)),
                self,
                dateCreated);
    }

    @Override
    public String toString() {
        return TAG + " cid: " + (this.conversationId != null ? this.conversationId : "") +
                " .display_name: " + (this.displayName != null ? this.displayName : "") +
                " .lastEventId: " + (this.lastEventId != null ? this.lastEventId : "") +
                " .member: " + (this.getSelf() != null ? this.getSelf() : "") +
                " .creation_time: " + (this.creationDate != null ? this.creationDate : "") +
                " .members:" + (this.members != null ? this.members.toString() : "")+
                " .events:" + (this.events != null ? this.events.toString() : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conversation)) return false;

        Conversation that = (Conversation) o;

        if (members != null ? !members.equals(that.members) : that.members != null) return false;
        if (events != null ? !events.equals(that.events) : that.events != null)
            return false;
        if (self != null ? !self.equals(that.self) : that.self != null) return false;
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;
        if (creationDate != null ? !creationDate.equals(that.creationDate) : that.creationDate != null)
            return false;
        if (conversationId != null ? !conversationId.equals(that.conversationId) : that.conversationId != null)
            return false;
        return lastEventId != null ? lastEventId.equals(that.lastEventId) : that.lastEventId == null;

    }

    @Override
    public int hashCode() {
        int result = members != null ? members.hashCode() : 0;
        result = 31 * result + (events != null ? events.hashCode() : 0);
        result = 31 * result + (self != null ? self.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        result = 31 * result + conversationId.hashCode();
        result = 31 * result + (lastEventId != null ? lastEventId.hashCode() : 0);
        return result;
    }

    //**********************************************************************************************
    //*                                                                                            *
    //*              Package level (access) & Private operations of Event Object                 *
    //*                                                                                            *
    //**********************************************************************************************

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    void setConversationSignalingChannel(ConversationSignalingChannel csc){
        this.conversationSignalingChannel = csc;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    void setSocketEventNotifier(SocketEventNotifier sen){
        this.socketEventNotifier = sen;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    void setMembers(List<Member> members) {
        this.members = members;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    void setEvents(List<Event> events) {
        this.events = events;
        if (!events.isEmpty())
            updateLastEventId(events.get(events.size()-1).getId());
    }

    void addEvent(Event event) {
        this.events.add(event);
        this.lastEventId = event.getId();
    }

    void setSelf(Member self) {
        this.self = self;
        if (self != null)
            addMember(self);
    }

    void updateLastEventId(String lastEventId) {
        this.lastEventId = lastEventId;
    }

    void updateBasicDetails(final Conversation conversation) {
        this.lastEventId = conversation.getLastEventId();
        this.creationDate = conversation.getCreationDate();
        this.displayName = conversation.getDisplayName();
        this.members = conversation.getMembers();
    }

    /**
     * Check if provided event is already part of conversation.
     * @param event The event to be checked.
     * @return      True if event is part of the conversation history.
     */
    boolean containsEvent(Event event) {
        for (Event message: this.events) {
            if (TextUtils.equals(event.getId(), message.getId()))
                return true;
        }
        return false;
    }

    /**
     * Check if current conversation contains a member based on username.
     *
     * @param username  Username of the user we are looking for.
     * @return          The memberId.
     */
    String containsMember(final String username) {
        for (Member member : this.members)
            if (TextUtils.equals(member.getName(),username))
                return member.getMemberId();

        return null;
    }

    Event findEvent(String eventId) {
        for (Event event : this.events)
            if (TextUtils.equals(event.getId(), eventId))
                return event;
        return null;
    }

    ConversationSignalingChannel getSignallingChannel(){
        return this.conversationSignalingChannel;
    }

    SocketEventNotifier getSocketEventNotifier(){
        return this.socketEventNotifier;
    }

    String getLastMessageEventId(){
        if(this.events.size() == 0) return null;

        int maxId = 0;
        for(Event event : this.events)
            maxId = Math.max(maxId, Integer.valueOf(event.getId()));

        return String.valueOf(maxId);
    }

    /**
     * Mark a conversation asDirty when requires force sync because cache missed events.
     * @param isDirty
     */
    void markAsDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    boolean isDirty() {
        return this.isDirty;
    }

    private void sendTyping(Member.TYPING_INDICATOR indicator, RequestHandler<Member.TYPING_INDICATOR> listener) {
        if(conversationSignalingChannel.isValidInput(listener, conversationId))
            this.conversationSignalingChannel.sendTypingIndicator(this, indicator, listener);
    }

    void updateRtcId(final String rtcId){

        this.audio().updateRtcId(rtcId);
    }

    void recycleEventBitmaps() {
        for (Event event : this.events) {
            if (event.getType() == EventType.IMAGE)
                ((Image) event).recycleBitmaps();
        }
    }

    private void createTypingThreads(){
        HandlerThread handlerThread = new HandlerThread("typingHandler");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        typingHandler = new Handler(looper);
    }

    private void postRunnables(boolean isStart){

        if(isStart) {

            if (typingHandler != null) {
                typingHandler.removeCallbacks(onTypingOnTimeout);
                typingHandler.postDelayed(onTypingOnTimeout, typingTimeOutLength);
            }

        } else {

            if (typingHandler != null) {
                typingHandler.removeCallbacks(onTypingOffTimeout);
                typingHandler.postDelayed(onTypingOffTimeout, typingTimeOutLength);
            }
        }


    }

    private Runnable onTypingOnTimeout = new Runnable() {
        @Override
        public void run() {
            if (!isTypingOn) return;
            isTypingOn = false;
        }
    };

    private Runnable onTypingOffTimeout = new Runnable() {
        @Override
        public void run() {
            if (!isTypingOff) return;
            isTypingOff = false;
        }
    };

}

/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.client;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.nexmo.sdk.conversation.client.audio.RtcEvents;
import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.client.event.ResultListener;
import com.nexmo.sdk.conversation.client.event.container.SynchronisingState;
import com.nexmo.sdk.conversation.client.event.misc.SessionError;
import com.nexmo.sdk.conversation.client.event.network.CAPIAwareListener;
import com.nexmo.sdk.conversation.client.event.network.CAPIInternalRequest;
import com.nexmo.sdk.conversation.client.event.network.NetworkState;
import com.nexmo.sdk.conversation.client.event.network.NetworkingStateListener;
import com.nexmo.sdk.conversation.client.event.network.SubscriptionListener;
import com.nexmo.sdk.conversation.core.client.Router;
import com.nexmo.sdk.conversation.core.client.Router.FlushFinishedListener;
import com.nexmo.sdk.conversation.core.client.request.*;
import com.nexmo.sdk.conversation.core.client.request.audio.AudioEarmuffRequest;
import com.nexmo.sdk.conversation.core.client.request.audio.AudioMuteRequest;
import com.nexmo.sdk.conversation.core.client.request.audio.AudioRingingRequest;
import com.nexmo.sdk.conversation.core.client.request.audio.RtcNewRequest;
import com.nexmo.sdk.conversation.core.client.request.audio.RtcTerminateRequest;
import com.nexmo.sdk.conversation.core.persistence.UserPreference;
import com.nexmo.sdk.conversation.core.util.DateUtil;
import com.nexmo.sdk.conversation.core.util.Log;
import com.nexmo.sdk.conversation.device.DeviceProperties;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.SessionDescription;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.*;

import io.socket.emitter.Emitter;

import static com.nexmo.sdk.conversation.client.ReceiptRecordUtil.parseDeliveredReceipt;
import static com.nexmo.sdk.conversation.client.ReceiptRecordUtil.parseSeenReceipt;
import static com.nexmo.sdk.conversation.core.client.request.DeleteEventRequest.EVENT_DELETE;
import static com.nexmo.sdk.conversation.core.client.request.DeliveredReceiptRequest.IMAGE_DELIVERED;
import static com.nexmo.sdk.conversation.core.client.request.DeliveredReceiptRequest.TEXT_DELIVERED;
import static com.nexmo.sdk.conversation.core.client.request.SeenReceiptRequest.IMAGE_SEEN;
import static com.nexmo.sdk.conversation.core.client.request.SeenReceiptRequest.TEXT_SEEN;
import static com.nexmo.sdk.conversation.core.client.request.SendImageMessageRequest.IMAGE_MESSAGE;
import static com.nexmo.sdk.conversation.core.client.request.SendTextMessageRequest.TEXT_MESSAGE;
import static com.nexmo.sdk.conversation.core.client.request.TypingIndicatorRequest.TEXT_TYPE_OFF;
import static com.nexmo.sdk.conversation.core.client.request.TypingIndicatorRequest.TEXT_TYPE_ON;
import static com.nexmo.sdk.conversation.core.client.request.audio.AudioMuteRequest.AUDIO_MUTE_OFF;
import static com.nexmo.sdk.conversation.core.client.request.audio.AudioMuteRequest.AUDIO_MUTE_ON;

/**
 * SocketClient responsible for signaling events.
 * User-aware event processing class
 *
 * @author emma tresanszki.
 *
 * @hide
 * @VisibleForTesting
 */
class SocketClient implements NetworkingStateListener {
    private static final String TAG = SocketClient.class.getSimpleName();
    private static final String RTC_MEMBER_MEDIA = "member:media";
    private static final String RTC_ANSWER = "rtc:answer";

    /**
     * conversation events
     */
    public static final String CONVERSATION_MEMBER_JOINED = "member:joined";


    public static final String MEMBER_INVITED = "member:invited";
    public static final String MEMBER_LEFT = "member:left";  //invitation decline, or leave joined conversation.

    /**
     * error events
     */
    public static final String EVENT_ERROR = "event:error";

    static boolean DEBUG_INCOMING_DATA = !false;

    private ConversationClient conversationClient;
    private SocketEventHandler socketEventHandler;

    private RequestHandler<User> loginListener;
    private Set<NetworkingStateListener> networkingStateListeners = Collections.synchronizedSet(new HashSet<NetworkingStateListener>());

    public Router router;
    User self;
    private RtcEvents rtcEventsListener;

    SocketClient(ConversationClient conversationClient) {
        this.conversationClient = conversationClient;
        this.router = new Router(conversationClient.getContext(), this, conversationClient.getConfig().isFlushPending());
        this.socketEventHandler = new SocketEventHandler(this);
        this.addConnectionListener(new NetworkingStateListener() {
            @Override
            public void onNetworkingState(NetworkState networkingState) {
                if (networkingState == NetworkState.CONNECTED) login();
            }
        });
        this.registerSubscriptions();
    }

    ConversationClient getConversationClient(){
        return this.conversationClient;
    }

    SocketEventHandler getSocketEventHandler(){
        return this.socketEventHandler;
    }

    public User getLoggedInUser() {
        return self;
    }

    private void registerSubscriptions() {
        //listen for new members once joined.
        this.router.on(CONVERSATION_MEMBER_JOINED, onMemberJoined);

        //listen for kicks
        this.router.on(MEMBER_LEFT, onMemberLeft);

        //listen for invites
        this.router.on(MEMBER_INVITED, onMemberInvited);

        //listen for text messages once joined.
        this.router.on(EVENT_DELETE, onMessageDeleted);
        this.router.on(TEXT_MESSAGE, onMessageReceived);

        //listen for text seen and delivered events.
        this.router.on(TEXT_SEEN, onTextSeen);
        this.router.on(TEXT_DELIVERED, onTextDelivered);

        //listen for image events
        this.router.on(IMAGE_MESSAGE, onMessageReceived);
        this.router.on(IMAGE_SEEN, onImageSeen);
        this.router.on(IMAGE_DELIVERED, onImageDelivered);

        //listen to typing
        this.router.on(TEXT_TYPE_ON, onTypeOn);
        this.router.on(TEXT_TYPE_OFF, onTypeOff);

        //listen for media events
        this.router.on(RTC_ANSWER, onRtcAnswer);
        this.router.on(RTC_MEMBER_MEDIA, onMemberMedia);
        this.router.on(AUDIO_MUTE_ON, onRtcMute);
        this.router.on(AUDIO_MUTE_OFF, onRtcMute);

        //listen for generic conversation errors.
        this.router.on(EVENT_ERROR, onEventError);

        for (SessionError error : SessionError.values())
            this.router.on(error.getEventName(), this.sessionError);
    }

    NetworkState getConnectionStatus() {
        return this.router.getConnectionStatus();
    }

    void connect(RequestHandler<User> loginListener) throws URISyntaxException {
        this.loginListener = loginListener;
        ConversationClient.ConversationClientConfig clientConfig = this.conversationClient.getConfig();
        String connectionUrl = clientConfig.getEnvironmentHost();
        String path = clientConfig.getEndpointPath();

        Log.d(TAG, "Connect to " + connectionUrl);
        this.router.connect(connectionUrl, path, clientConfig.autoReconnect);
    }

    private void login() {
        String deviceId = DeviceProperties.getUserid(this.conversationClient.getConfig().getContext());
        final LoginRequest loginRequest = new LoginRequest(this.conversationClient.getToken(), deviceId, null);

        sendRequest(new InternalResponseHelper<User, User>(loginRequest) {

            @Override
            User processParsedResponse(User me) {
                self = me;
                Log.d(TAG, "onLogin user " + self.toString());
                if (loginListener != null) {
                    loginListener.onSuccess(self);
                    loginListener = null;
                }
                flushPendingOperations();

                if (!UserPreference.containsUser(conversationClient.getContext())) {
                    Log.d(TAG, "Cache: no user found. saving user.");
                    CacheDB.getCacheDBInstance().clearDb();
                    UserPreference.saveUser(self, conversationClient.getContext());
                }
                else if (!UserPreference.isLastLoggedInUser(self, conversationClient.getContext())) {
                    Log.d(TAG,"Cache: other user found. overriding all cache.");
                    CacheDB.getCacheDBInstance().clearDb();
                    UserPreference.saveUser(self, conversationClient.getContext());
                }
                else
                    Log.d(TAG, "Cache: user found");

                // always get fresh conversation list from backend so we can match lastEventId
                updateConversationList();

// QUEST: Determine why listener is called here
                if (loginListener != null) {
                    loginListener.onSuccess(self);
                    loginListener = null;
                }


                return self;
            }

            @Override
            public void onError(String errorEventName, JSONObject data, String rid, String cid) {
                Log.e(TAG, "errorEventName: [" + errorEventName + "] data: [" + data + "] rid: [" +  rid + "] cid: [" + cid + "]");
                loginListener.onError(new NexmoAPIError(rid, errorEventName, cid, data.toString()));

                SocketClient.this.router.shutdown();
            }
        });
    }

    void pushRegister(RequestHandler<Void> pushEnableListener) {
        final PushRegisterRequest request = new PushRegisterRequest(conversationClient.getPushDeviceToken(), pushEnableListener);

        sendRequest(new InternalResponseHelper<Void, Void>(request) {
            @Override
            Void processParsedResponse(Void data) {
                return null;
            }
        });
    }

    void pushUnregister(RequestHandler<Void> listener) {
        String deviceId = DeviceProperties.getUserid(this.conversationClient.getContext());
        final PushUnregisterRequest request = new PushUnregisterRequest(deviceId, listener);

        sendRequest(new InternalResponseHelper<Void, Void>(request) {

            @Override
            Void processParsedResponse(Void data) {
                Log.d(TAG, "onPushUnregistered ");
                //on response - clear the token
                DeviceProperties.resetUserId(conversationClient.getContext());

                return null;
            }
        });
    }

    void pushSubscribeToConversation(final PushSubscriptionRequest subscriptionRequest) {

       sendRequest(new InternalResponseHelper<Void, Void>(subscriptionRequest) {
           @Override
           Void processParsedResponse(Void data) {
               return null;
           }
       });
    }

    /**
     * Cleanup any locally stored data associated for the user.
     * Recycle bitmaps.
     * Cleanup in-memory data.
     * Disable and terminate audio.
     */
    void disconnectCleanup() {
        //remove user info
        self = null;
        UserPreference.clearUser(conversationClient.getContext());

        CacheDB cacheDB = CacheDB.getCacheDBInstance();
        if (cacheDB != null) {
            cacheDB.clearDb();
            cacheDB.closeDatabase();
        }

        ConversationSignalingChannel signalingChannel = getConversationClient().getSignallingChannel();
        if (signalingChannel.isAudioEnabled())
            // disable and terminate audio
            signalingChannel.disableAudio();

        signalingChannel.updateSyncState(SynchronisingState.STATE.OUT_OF_SYNC);

        //recycle bitmaps
        for (Conversation conversation : socketEventHandler.getConversationList())
            conversation.recycleEventBitmaps();
        socketEventHandler.getConversationList().clear();
    }

    void logout(final RequestHandler logoutListener) {
        conversationClient.getEventNotifier().removeAllListeners();

        getConversationClient().getSignallingChannel().disableAudio();

        // unregister for push
        pushUnregister(new RequestHandler<Void>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                sendLogoutRequest(logoutListener);
            }

            @Override
            public void onSuccess(Void result) {
                sendLogoutRequest(logoutListener);
            }
        });
    }

    private void sendLogoutRequest(final RequestHandler logoutListener) {
        sendRequest(new AsyncInternalResponseHelper<Void, User>(new LogoutRequest()) {

            NetworkingStateListener disconnectedStateListener = new NetworkingStateListener() {
                @Override
                public void onNetworkingState(NetworkState networkingState) {
                    User logedOutUser = new User(self);
                    disconnectCleanup();

                    logoutListener.onSuccess(logedOutUser);
                }
            };

            @Override
            void processParsedResponseAsync(Void data, ResultListener<User> callback) {
                Log.d(TAG, "Logout success");
                SocketClient.this.addOnceConnectionListener(NetworkState.DISCONNECTED, disconnectedStateListener);
                SocketClient.this.router.shutdown();
            }
        });
    }

    void newConversation(final CreateConversationRequest request) {

        sendRequest(new InternalResponseHelper<String, Conversation>(request) {
            @Override
            Conversation processParsedResponse(String conversationId) {
                                Conversation conversation = new Conversation(request.displayName, conversationId);
                conversation.setConversationSignalingChannel(conversationClient.getSignallingChannel());
                conversation.setSocketEventNotifier(conversationClient.getEventNotifier());

                socketEventHandler.onConversationCreated(conversation);
                return conversation;
            }
        });
    }

    void newConversationHelper(final CreateConversationRequest request) {
        sendRequest(new AsyncInternalResponseHelper<String, Conversation>(request) {
            @Override
            void processParsedResponseAsync(String conversationId, ResultListener<Conversation> callback) {
                Conversation conversation = new Conversation(request.displayName, conversationId);
                conversation.setConversationSignalingChannel(conversationClient.getSignallingChannel());
                conversation.setSocketEventNotifier(conversationClient.getEventNotifier());

                socketEventHandler.addNewConversationToMemory(conversation);

                // create a join request without a listener, username is used here, but userid can also be used
                JoinRequest joinRequestNoHandler = JoinRequest.createJoinRequestWithUsername(conversationId, request.displayName, null, self.getName());
                joinNewConversationHelper(joinRequestNoHandler, conversation, request);
            }
        });
    }

    void joinNewConversationHelper(final JoinRequest joinRequest, final Conversation conversation,  final CreateConversationRequest newConversationRequest) {
        sendRequest(new AsyncInternalResponseHelper<Member, Member>(joinRequest) {
            @Override
            void processParsedResponseAsync(Member member, ResultListener<Member> callback) {
                if(isMemberSelf(member))
                    member.setName(getConversationClient().getUser().getName());
                socketEventHandler.onNewConversationHelperSuccessCreated(conversation, newConversationRequest, member);
            }
        });
    }

    // R is raw unprocessed response from socket - type before Parsing ( we normally specify a parse Type (R) to handle the response when we are creating the
    //      the initial Request.
    // U/L is data - type after Parsing (type erasure mitigates the need to have same letter), what the listener is expecting back

    /** Rtc related events */
    void createCall(final InviteWithAudioRequest request) {
        sendRequest(new InternalResponseHelper<InviteRequest.InviteContainer, Member>(request) {
            @Override
            Member processParsedResponse(InviteRequest.InviteContainer result) {
                Log.d(TAG, "conversation:invite with audio success " + result.toString());
                return socketEventHandler.onInvitationSent(result.userId, result.memberId, result.timestampInvited, request);
            }
        });
    }

    void rtcNew(final RtcNewRequest request) {
        sendRequest(new InternalResponseHelper<RtcNewRequest.RtcNewResponse, String>(request) {
            @Override
            String processParsedResponse(RtcNewRequest.RtcNewResponse result) {
                System.out.println(TAG + " rtc:new:success " + result);
                return socketEventHandler.onRtcNew(request, result);
            }
        });
    }

    void rtcTerminate(final RtcTerminateRequest rtcTerminateRequest) {
        sendRequest(new InternalResponseHelper<Void, String>(rtcTerminateRequest) {
            @Override
            String processParsedResponse(Void result) {
                System.out.println(TAG + " rtc:terminate:success " + result);
                return null;
            }
        });
    }

    void rtcMute(final AudioMuteRequest muteRequest) {
        sendRequest(new InternalResponseHelper<Void, String>(muteRequest) {
            @Override
            String processParsedResponse(Void result) {
                System.out.println(TAG + " audio:mute: " + result);
                return null;
            }
        });
    }

    void rtcEarmuff(final AudioEarmuffRequest earmuffRequest) {
        sendRequest(new InternalResponseHelper<Void, String>(earmuffRequest) {
            @Override
            String processParsedResponse(Void result) {
                System.out.println(TAG + " audio:earmuff: " + result);
                return null;
            }
        });
    }

    void audioRinging(final AudioRingingRequest audioRingingRequest) {
        sendRequest(new InternalResponseHelper<Void, String>(audioRingingRequest) {
            @Override
            String processParsedResponse(Void result) {
                return null;
            }
        });
    }

    void joinConversation(final JoinRequest joinRequest) {
        sendRequest(new InternalResponseHelper<Member, Member>(joinRequest) {
            @Override
            Member processParsedResponse(Member member) {
                if(isMemberSelf(member))
                    member.setName(getConversationClient().getUser().getName());
                socketEventHandler.onJoin(member, joinRequest);
                return member;
            }
        });
    }

    void leaveConversation(final LeaveRequest leaveRequest) {

        sendRequest(new InternalResponseHelper<LeaveRequest.TimestampsContainer, Void>(leaveRequest) {

            @Override
            Void processParsedResponse(LeaveRequest.TimestampsContainer data) {
                Conversation pendingConversation = conversationClient.getConversation(leaveRequest.cid);
                Member member = pendingConversation.getMember(leaveRequest.member.getMemberId());
                //update Member with left timestamp
                if (member != null)
                    member.updateState(Member.STATE.LEFT, data.leftTimestamp);

                pendingConversation.recycleEventBitmaps();
                //Quest: not sure about this..
                return null;
            }
        });
    }

    void invite(final InviteRequest inviteRequest) {
        sendRequest(new InternalResponseHelper<InviteRequest.InviteContainer, Member>(inviteRequest) {
            @Override
            Member processParsedResponse(InviteRequest.InviteContainer result) {
                Log.d(TAG, "onInviteSent " + result.toString());
                return socketEventHandler.onInvitationSent(result.userId, result.memberId, result.timestampInvited, inviteRequest);
                //return null;
            }
        });
    }

    void getConversation(final GetConversationRequest getRequest) {
        sendRequest(new InternalResponseHelper<GetConversationRequest.Container, Conversation>(getRequest) {
            @Override
            Conversation processParsedResponse(GetConversationRequest.Container result) {
                Log.d(TAG, "onConversationUpdated ");
                Conversation conversation = result.conversation;
                for (Member member : result.members) {
                    member.setConversation(conversation);
                    if (member.getUserId().equals(self.getUserId()))
                        conversation.setSelf(member);
                    else
                        conversation.addMember(member);
                }

                Log.d(TAG, "onGet get members size: " + conversation.getMembers().size()
                        + " :" + conversation.getMembers().toString());

                conversation.setConversationSignalingChannel(conversationClient.getSignallingChannel());
                conversation.setSocketEventNotifier(conversationClient.getEventNotifier());

                return socketEventHandler.onConversation(conversation, getRequest);
            }
        });
    }

    void updateConversationList() {
        GetConversationsRequest request = new GetConversationsRequest(conversationClient.getUser());

        sendRequest(new InternalResponseHelper<List<GetConversationsRequest.Container>, List<Conversation>>(request) {
            @Override
            List<Conversation> processParsedResponse(List<GetConversationsRequest.Container> result) {
                ArrayList<Conversation> conversations = new ArrayList<>(result.size());
                for (GetConversationsRequest.Container container : result) {
                    container.conversation.setSelf(container.member);
                    conversations.add(container.conversation);
                }
                socketEventHandler.onConversations(conversations);
                return conversations; // no actual result to be delivered - it's internal call
            }
        });
    }

    void flushPendingOperations() {
        FlushFinishedListener readyToCallUserCallback = new FlushFinishedListener() {
            @Override
            public void onFlushFinished() {
                retryPendingOperations();
            }
        };

        if (conversationClient.getConfig().isFlushPending()) {
            Log.d(TAG, "Login succeed and flushPending is enabled - trying to flush");
            router.loadAndRemoveAndFlush(router, readyToCallUserCallback);
        } else
            readyToCallUserCallback.onFlushFinished();
    }

    // declare that self has seen the file
    void sendReceiptRecord(final SeenReceiptRequest receiptRequest) {
        sendRequest(new InternalResponseHelper<SeenReceiptRequest.Container, SeenReceipt>(receiptRequest) {
            @Override
            SeenReceipt processParsedResponse(SeenReceiptRequest.Container result) {
                Log.d(TAG, "SeenReceiptRequest success");
               return new SeenReceipt(receiptRequest.event, receiptRequest.member, result.timestamp);
            }
        });
    }

    //Quest: sendReceiptRecord DeliveredReceiptRequest
    void sendReceiptRecord(final DeliveredReceiptRequest receiptRequest) {
        sendRequest(new InternalResponseHelper<Void, Void>(receiptRequest) {
            @Override
            Void processParsedResponse(Void data) {
                //Log.d(TAG, "DeliveredReceiptRequest success");
//             // no fallback mechanism for failed delivered receipts.
                return null;
            }
        });
    }

    void sendTypingIndicator(final TypingIndicatorRequest request) {
        sendRequest(new InternalResponseHelper<Void, Member.TYPING_INDICATOR>(request) {
            @Override
            Member.TYPING_INDICATOR processParsedResponse(Void data) {
                Log.d(TAG, "onTypeSuccess ");
                return socketEventHandler.onTyping(request.typingIndicator, request);
            }
        });
    }

    void deleteEvent(final DeleteEventRequest request) {
        sendRequest(new InternalResponseHelper<Void, Void>(request) {
            @Override
            Void processParsedResponse(Void data) {
                Log.d(TAG, "onEventDeletedSuccess eventId: " + request.messageId);
                return null;
            }
        });
    }

    void sendText(final SendTextMessageRequest sendMessageRequest) {
        sendRequest(new InternalResponseHelper<SendTextMessageRequest.Container, Text>(sendMessageRequest) {
            @Override
            Text processParsedResponse(SendTextMessageRequest.Container container) {
                Log.d(TAG, "onTextSent " );
                return socketEventHandler.onTextSent(container.messageId, container.timestamp, sendMessageRequest);
            }
        });
    }

    void sendImage(final SendImageMessageRequest sendImageRequest) {
        sendRequest(new InternalResponseHelper<SendTextMessageRequest.Container, Image>(sendImageRequest) {
            @Override
            Image processParsedResponse(SendTextMessageRequest.Container container) {
                return  socketEventHandler.onImageSent(container.messageId, container.timestamp, sendImageRequest);
            }
        });
    }

    void getUser(final GetUserInfoRequest userRequest) {
        sendRequest(new InternalResponseHelper<User, User>(userRequest) {
            @Override
            User processParsedResponse(User user) {
                Log.d(TAG, "onUserInfo: " + user);
                return user;
            }
        });
    }

    void getEvents(final GetEventsRequest getRequest) {
        sendRequest(new InternalResponseHelper<List<Event>, Conversation>(getRequest) {
            @Override
            Conversation processParsedResponse(List<Event> events) {

                Log.d(TAG, "onConversationEvents " + events);
                return socketEventHandler.onEventsHistory(events, getRequest);
            }
        });
    }

    private Emitter.Listener onMessageDeleted = new Listener() {
        @Override
        public void onData(JSONObject data, String rid) throws Throwable {
            Log.d(TAG, "onDeleted " + data.toString());
            String from = data.getString("from");
            String cid = data.getString("cid");
            Date timestamp = null;
            try {
                timestamp = DateUtil.formatIso8601DateString(data.getString("timestamp"));
            } catch (ParseException e) {
            }

            JSONObject body = data.getJSONObject("body");
            socketEventHandler.onMessageDeleted(cid, from,
                    body.getString("event_id"), data.getString("id"), timestamp);
        }
    };

    private Emitter.Listener onMessageReceived = new Listener() {
        @Override
        public void onData(JSONObject data, String rid) throws Throwable {
            Log.d(TAG, "onMessage " + data.toString());
            String cid = data.getString("cid");
            Conversation conversation = socketEventHandler.findConversation(cid);
            if (conversation != null)
                socketEventHandler.onMessageReceived(cid, Event.fromJson(conversation, data));
            else
                Log.d(TAG, "onMessageReceived: outOfSync, unknown conversation"); // will be new Error type: "message-conversation:outOfSync"
        }
    };

    private Emitter.Listener onTypeOn = new Listener() {
        @Override
        public void onData(JSONObject data, String rid) throws Throwable {
            Log.d(TAG, "onTypeOn received " + data.toString());
            String cid = data.getString("cid");
            String memberId = data.getString("from");

            socketEventHandler.onTypingOnReceived(cid, memberId);
        }
    };

    private Emitter.Listener onTypeOff = new Listener() {
        @Override
        public void onData(JSONObject data, String rid) throws Throwable {
            Log.d(TAG, "onTypeOn received " + data.toString());
            String cid = data.getString("cid");
            String memberId = data.getString("from");

            socketEventHandler.onTypingOffReceived(cid, memberId);
        }
    };

    private Emitter.Listener onImageSeen = new Listener() {
        @Override
        public void onData(JSONObject data, String rid) throws Throwable {
            Log.d(TAG, "onImageSeen " + data.toString());

            parseSeenReceipt(data, socketEventHandler);
        }
    };

    private Emitter.Listener onTextDelivered = new Listener() {
        @Override
        public void onData(JSONObject data, String rid) throws Throwable {
            Log.d(TAG, "onTextDelivered" + data.toString());

            parseDeliveredReceipt(data, socketEventHandler);
        }
    };

    private Emitter.Listener onImageDelivered = new Listener() {
        @Override
        public void onData(JSONObject data, String rid) throws Throwable {
            Log.d(TAG, "onImageDelivered" + data.toString());

            parseDeliveredReceipt(data, socketEventHandler);
        }
    };

    private Emitter.Listener onTextSeen = new Listener() {
        @Override
        public void onData(JSONObject data, String rid) throws Throwable {
            Log.d(TAG, "onTextSeen" + data.toString());

            parseSeenReceipt(data, socketEventHandler);
        }
    };

    private Emitter.Listener onMemberInvited = new Listener() {
        @Override
        public void onData(JSONObject data, String rid) throws Throwable {
            Log.d(TAG, "onInvitation received " + data.toString());
            String cid = data.getString("cid");

            JSONObject body = data.getJSONObject("body");
            String cName = body.getString("cname");
            String senderUsername = body.getString("invited_by");

            JSONObject time = body.getJSONObject("timestamp");
            Date timestampInvitedAt = null;
            try {
                timestampInvitedAt = DateUtil.formatIso8601DateString(time.getString("invited"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            JSONObject user = body.getJSONObject("user");
            Member invitedMember = new Member(
                    user.getString("user_id"),
                    user.getString("name"),
                    user.getString("member_id"),
                    null,
                    timestampInvitedAt,
                    null,
                    Member.STATE.INVITED);

            boolean hasMediaEnabled = false;
            if (user.has("media"))
                hasMediaEnabled = user.getJSONObject("media").has("audio");

            if (isMemberSelf(invitedMember)) {
                socketEventHandler.onSelfInvited(cid, cName, invitedMember, senderUsername, hasMediaEnabled);
            }
            else
                socketEventHandler.onMemberInvited(cid, invitedMember, senderUsername);
        }
    };

    private boolean isMemberSelf(Member member) {
        return TextUtils.equals(getConversationClient().getUser().getUserId(), member.getUserId());
    }

    private Emitter.Listener onMemberLeft = new Listener() {
        @Override
        public void onData(JSONObject data, @Nullable String rid) throws Throwable {
            Log.d(TAG, "onMemberLeft " + data.toString());
            String cid = data.getString("cid");
            String memberId = data.getString("from");

            JSONObject body = data.getJSONObject("body");
            JSONObject timestamp = body.getJSONObject("timestamp");
            Date joinedTimestamp = null, leftTimestamp = null, invitedTimestamp = null; //optional
            try {
                if (timestamp.has("joined"))
                    joinedTimestamp = DateUtil.formatIso8601DateString(timestamp.getString("joined"));
                if (timestamp.has("left"))
                    leftTimestamp = DateUtil.formatIso8601DateString(timestamp.getString("left"));
                if (timestamp.has("invited"))
                    invitedTimestamp = DateUtil.formatIso8601DateString(timestamp.getString("invited"));
            } catch (ParseException e) {
            }
            User user = User.fromJson(body.getJSONObject("user"));

            socketEventHandler.onMemberLeft(cid, memberId, user, invitedTimestamp, joinedTimestamp, leftTimestamp);
        }
    };

    private Emitter.Listener onMemberJoined = new Listener() {
        @Override
        public void onData(JSONObject data, @Nullable String rid) throws Throwable {
            Log.d(TAG, "onMemberJoined " + data.toString());
            String from = data.getString("from");
            String cid = data.getString("cid");

            JSONObject body = data.getJSONObject("body");
            JSONObject userObject = body.getJSONObject("user");
            User user = User.fromJson(userObject);
            JSONObject timestamp = body.getJSONObject("timestamp");

            Date joinedTimestamp = null;
            try {
                joinedTimestamp = DateUtil.formatIso8601DateString(timestamp.getString("joined"));
            } catch (ParseException e) {
            }

            socketEventHandler.onMemberJoined(cid, from, user, joinedTimestamp);
        }
    };

    private SubscriptionListener sessionError = new SubscriptionListener() {
        @Override
        public void onData(String eventName, JSONObject jsonObject) throws JSONException {
            Log.d(TAG, "onSessionError (" + eventName + "): " + jsonObject);
            router.shutdown();
            socketEventHandler.onSessionError(SessionError.getByEventName(eventName));
        }
    };

    private Emitter.Listener onEventError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d("onEventError ", data.toString());
        }
    };

    /** RTC events **/
    private Emitter.Listener onRtcAnswer = new Listener() {
        @Override
        public void onData(JSONObject data, String rid) throws Throwable {
            Log.d(TAG + " onRtcAnswer ", data.toString());
            try {
                JSONObject body = data.getJSONObject("body");

                if (rtcEventsListener != null)
                    rtcEventsListener.onAnswer(new SessionDescription(SessionDescription.Type.PRANSWER, body.getString("answer")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public Emitter.Listener onRtcMute = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, AUDIO_MUTE_ON + " : " + data.toString());
        }
    };

    public Emitter.Listener onMemberMedia = new Listener() {
        @Override
        public void onData(JSONObject data, String rid) throws Throwable {
            Log.d(TAG, RTC_MEMBER_MEDIA + " : " + data.toString());
            String cid = data.getString("cid");
            Conversation conversation = socketEventHandler.findConversation(cid);

            if (conversation != null) {
                String memberId = data.getString("from");
                Member member = conversation.getMember(memberId);
                if (member == null) {
                    Log.d(TAG, "Event.fromJson out-of-sync members");
                }
                String eventId = data.getString("id");
                Date timestamp = DateUtil.parseDateFromJson(data, "timestamp");

                boolean audio = false;

                try {
                    JSONObject body = data.getJSONObject("body");
                    audio = body.optBoolean("audio");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                MemberMedia newEvent = new MemberMedia(eventId, member, conversation, audio, timestamp);
                conversation.addEvent(newEvent);
                socketEventHandler.onMessageReceived(cid, newEvent);
            }
            else
                Log.d(TAG, "onMessageReceived: outOfSync, unknown conversation"); // will be new Error type: "message-conversation:outOfSync"
        }
    };

    // Set new rtc event listener that overrides the old one each time.
    public void setRtcEventsListener(RtcEvents rtcEventsListener) {
        this.rtcEventsListener = rtcEventsListener;
    }

    public void addOnceConnectionListener(final NetworkState expectedState, final NetworkingStateListener listener) {
        networkingStateListeners.add(new NetworkingStateListener() {
            @Override
            public void onNetworkingState(NetworkState newState) {
                if (expectedState != newState) return;
                removeConnectionListener(this);
                listener.onNetworkingState(newState);
            }
        });
    }

    public void addConnectionListener(NetworkingStateListener listener) {
        networkingStateListeners.add(listener);
    }

    public void removeConnectionListener(NetworkingStateListener listener) {
        networkingStateListeners.remove(listener);
    }

    public void removeAllConnectionListeners() {
        networkingStateListeners.clear();
    }

    void explicitDisconnect() {
        getConversationClient().getSignallingChannel().disableAudio();

        router.updateConnectionStatus(NetworkState.DISCONNECTED);
        onNetworkingState(NetworkState.DISCONNECTED);

        router.shutdown();
        disconnectCleanup();
        Log.d(TAG,"Explicitly Disconnected: Connection state: " + router.getConnectionStatus().toString());
    }

    @Override
    public void onNetworkingState(final NetworkState networkingState) {
        for(final NetworkingStateListener networkingStateListener : new ArrayList<>(this.networkingStateListeners))
            conversationClient.callUserCallback(new Runnable() {
                @Override
                public void run() {
                    networkingStateListener.onNetworkingState(networkingState);
                }
            });
    }

    /**
     * Helper class which wraps SocketIO Emitter.Listener for convenience
     */
    abstract class Listener implements Emitter.Listener {
        /**
         * Handles response
         *
         * @param data returned data
         * @param rid  field "rid" from data, can be either empty string or response id key
         */
        public abstract void onData(JSONObject data, String rid) throws Throwable;

        /**
         * Handles error during response processing (for ex JsonException).
         * Default behavior is print stacktrace
         */
        public void onError(Throwable throwable) {
        }

        @Override
        public final void call(Object... args) {
            String rid = "", cid = "";
            try {
                JSONObject jsonObject = (JSONObject) args[0];
                rid = jsonObject.optString("rid", "");
                cid = jsonObject.optString("cid", "");
                onData(jsonObject, rid);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    // R is raw unprocessed response from socket - type before Parsing
    // U/L is data - type after Parsing (type erasure mitigates the need to have same letter

    private abstract class InternalResponseHelper<R, U> extends CAPIRequest<R, U> {
        InternalResponseHelper(Request<RequestHandler<U>, R> requestParser) {
            super(requestParser);
        }

        @Override
        final void onParsedResponseData(R result) {
            final U data = processParsedResponse(result);
            if (this.requestResponseHandler.getListener() == null) return;

            applyOnRightThread(data);
        }

        abstract U processParsedResponse(R data);
    }

    private abstract class AsyncInternalResponseHelper<R, U> extends CAPIRequest<R, U> {
        AsyncInternalResponseHelper(Request<RequestHandler<U>, R> requestParser) {
            super(requestParser);
        }

        @Override
        final void onParsedResponseData(R result) {
            processParsedResponseAsync(result, new ResultListener<U>() {
                @Override
                public void onSuccess(final U result) {
                    if (requestResponseHandler.getListener() == null) return;
                    applyOnRightThread(result);
                }
            });
        }

        abstract void processParsedResponseAsync(R data, ResultListener<U> callback);
    }

    /**
     * Helper class which implements default error handler and does parsing using provided Request
     */
    private abstract class CAPIRequest<R, U> implements CAPIAwareListener {
        //Request<?, R> requestParser;
        protected Request<RequestHandler<U>, R> requestResponseHandler;

//        <L extends RequestHandler> CAPIRequest(Request<L, R> requestParser) {
//            this.requestParser = requestParser;
//        }

        CAPIRequest(Request<RequestHandler<U>, R> requestParser) {
            this.requestResponseHandler = requestParser;
        }

        @Override
        public final void onRawUnprocessResponseData(JSONObject data, String rid, String cid) throws JSONException {
            JSONObject body = data.optJSONObject("body");
            onParsedResponseData(requestResponseHandler.parse(data, body != null ? body : empty));
        }

        @Override
        public void onError(String errorEventName, JSONObject data, String rid, String cid) {
            Log.w(SocketClient.TAG, String.format("Caught error %s for request %s: %s", errorEventName, requestResponseHandler.getRequestName(), data));

            RequestHandler listener = requestResponseHandler.listener;
            if (listener == null) return;
            //NexmoAPIError.forward(listener, new NexmoAPIError(rid, errorEventName, cid, data.toString()));
            errorOnRightThread(new NexmoAPIError(rid, errorEventName, cid, data.toString()));
        }

        abstract void onParsedResponseData(R result);

        protected void applyOnRightThread(final U data) {
            if (requestResponseHandler.getListener() == null) return;

            if (conversationClient.handlerForCallbacks == null) {
                requestResponseHandler.getListener().onSuccess(data);
                return;
            }

            conversationClient.handlerForCallbacks.post(new Runnable() {
                @Override
                public void run() {
                    requestResponseHandler.getListener().onSuccess(data);
                }
            });
        }

        protected void errorOnRightThread(final NexmoAPIError errorDetails) {
            if (requestResponseHandler.getListener() == null) return;

            if (conversationClient.handlerForCallbacks == null) {
//                errorDetails.forward(req.getListener());
                NexmoAPIError.forward(requestResponseHandler.getListener(), errorDetails);
                return;
            }

            conversationClient.handlerForCallbacks.post(new Runnable() {
                @Override
                public void run() {
//                    errorDetails.forward(req.getListener());
                    NexmoAPIError.forward(requestResponseHandler.getListener(), errorDetails);
                }
            });
        }
    }

// Quest: mark for deletion?
    static JSONObject empty = new JSONObject();
    static JSONObject emptyJson = new JSONObject();

    private <R, U> void sendRequest(CAPIRequest<R, U> requestProcessor) {
        this.router.sendRequest(requestProcessor.requestResponseHandler, requestProcessor);
    }

    /**
     * When coming back online retry following operations:
     * <ul>retry:
     *     <li>create conversation</li>
     *     <li>join</li>
     *     <li>invite</li>
     *     <li>kick member</li>
     *     <li>delete event</li>
     *     <li>send event/li>
     * </ul>
     */
    private void retryPendingOperations() {
        Map<String, CAPIInternalRequest> ongoingRequests = router.getPendingRequests();
        Log.d(TAG, "retryPendingOperations. of size: " + ongoingRequests.size());
        for (Map.Entry<String, CAPIInternalRequest> entry : ongoingRequests.entrySet()) {
            Request request = entry.getValue().getRequest();
            switch (request.type) {
                case CREATE:
                    newConversation((CreateConversationRequest) request);
                    break;
                case JOIN:
                    joinConversation((JoinRequest) request);
                    break;
                case INVITE:
                    invite((InviteRequest) request);
                    break;
                case LEAVE:
                    leaveConversation((LeaveRequest) request);
                    break;
                case SEND_TEXT:
                    sendText((SendTextMessageRequest) request);
                    break;
                case SEND_IMAGE:
                    sendImage((SendImageMessageRequest) request);
                    break;
                case DELETE_EVENT:
                    deleteEvent((DeleteEventRequest) request);
                    break;
            }
        }
    }

}

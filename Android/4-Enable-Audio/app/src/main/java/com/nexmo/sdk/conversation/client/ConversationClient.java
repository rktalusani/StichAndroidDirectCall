/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.client;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.nexmo.enableaudio.BuildConfig;
import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.client.event.container.SynchronisingState;

import com.nexmo.sdk.conversation.client.event.misc.SessionError;
import com.nexmo.sdk.conversation.core.util.Log;

//import com.bugsnag.android.Bugsnag;
//import com.nexmo.sdk.conversation.BuildConfig;
import com.nexmo.sdk.conversation.client.event.container.Invitation;
import com.nexmo.sdk.conversation.client.event.ConversationClientException;
import com.nexmo.sdk.conversation.client.event.network.NetworkState;
import com.nexmo.sdk.conversation.client.event.network.NetworkingStateListener;
import com.nexmo.sdk.conversation.core.client.Router;
import com.nexmo.sdk.conversation.core.networking.Constants;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * You use a <i>ConversationClient</i> instance to utilise the services provided by Conversation API in your app.
 *
 * A session is the period during which your app is connected to Conversation API.
 * Sessions are established for the length of time given when the authToken was created.
 *
 * Tokens also have a lifetime and can optionally be one-shot which will allow a single login only, before
 * the authToken becomes invalid for another login attempt. If the authToken is revoked while a session is active the
 * session may be terminated by the server.
 * It is only possible to have a single session active over a socket.io connection at a time.
 * Session multiplexing is not supported.</p>
 *
 * <strong>Note</strong>: The connection uses socket.io for both web and mobile clients.
 * Upon a successful socket.io connection the client needs to authenticate itself.
 * This is achieved by sending a login request via {@link ConversationClient#login(String, RequestHandler<User>)}</p>
 *
 * <p>Unless otherwise specified, all the methods invoked by this client are executed asynchronously.</p>
 *
 * <p>For the security of your Nexmo account, you should not embed directly your Conversation credential authToken as strings in the app you submit to the Google Play Store.</p>
 *
 * First step is to acquire a {@link ConversationClient} instance based on user credentials.
 * <p>To construct a {@link ConversationClient} the required parameters are:</p>
 * <ul>
 *     <li>applicationContext:  The application context.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 *     ConversationClient myClient = new ConversationClient.ConversationClientBuilder()
 *                     .context(applicationContext)
 *                     .build();
 *
 *     myClient.login("authToken", new RequestHandler<User> {
 *         &#64;Override
 *         public void onSuccess(User user) {
 *              // Update the application UI here if needed.
 *         }
 *         &#64;Override
 *         public void onError(NexmoAPIError code error) {
 *              // Update the application UI here if needed.
 *         }
 *    });
 * </pre>
 *
 * <p>Conversations synchronisation is done during login, for listening to sync event subscribe
 * to a {@link ConversationClient#synchronisationEvent()}. When sync is done conversation list can be retrieved
 * synchronously from {@link ConversationClient#getConversationList()}}</p>
 * <pre>
 *     myClient.synchronisationEvent().add(new ResultListener<SynchronisingState.STATE state>) {
 *         &#64;Override
 *         public void onSuccess(SynchronisingState.STATE state() {
 *              // Update the application UI here if needed with entire list.
 *         }
 *         &#64;Override
 *         public void onError(NexmoAPIError error) {
 *              // Unrecoverable error, update UI with some information from error.
 *         }
 *    });
 * </pre>
 *
 * <p>The list of active conversations can be retrieved asynchronously via
 * {@link ConversationClient#getConversations(RequestHandler<List<Conversation>>)}.</p>
 *
 * <p> Example usage, performing a {@link ConversationClient#getConversations(RequestHandler<List<Conversation>>)}
 * for the logged in user:</p>
 *
 * <pre>
 *     myClient.getConversations(new RequestHandler<List<Conversation>> () {
 *         &#64;Override
 *         public void onSuccess(List<Conversation> conversations) {
 *              // Update the application UI here if needed.
 *         }
 *         &#64;Override
 *         public void onError(NexmoAPIError error) {
 *              // Update the application UI here if needed.
 *         }
 *    });
 * </pre>
 *
 * <p>Joining a conversation is done via {@link Conversation#join(RequestHandler<Member>)}</p>
 * Note: any {@link User} that joins a {@link Conversation} becomes a {@link Member}.
 * <p> Example usage:</p>
 *
 * <pre>
 *     myConversation.join(new RequestHandler<Member>() {
 *         &#64;Override
 *         public void onSuccess(Member member) {
 *              // Update the application UI here if needed. From now on the member may send messages in this conversation.
 *         }
 *         &#64;Override
 *         public void onError(NexmoAPIError error) {
 *              // Update the application UI here if needed.
 *         }
 *    });
 * </pre>
 *
 * Remember to logout when needed in order to remove current user and disconnect from the underlying socket connection.
 * <p>Example usage:</p>
 * <pre>
 *     myClient.logout(new RequestHandler() {
 *         &#64;Override
 *         public void onSuccess(User user) {
 *              // Update the application UI here if needed.
 *         }
 *         &#64;Override
 *         public void onError(NexmoAPIError error) {
 *              // Update the application UI here if needed.
 *         }
 *    });
 * </pre>
 *
 * Remember to disconnect when needed in order to disconnect from the underlying socket connection.
 * Use this method when your app is not being used.
 * In order to revoke a disconnect, a new Login must be performed.
 * <p>Example usage:</p>
 * <pre>
 *     myClient.disconnect();
 * </pre>
 *
 * @author emma tresanszki.
 */
public class ConversationClient {

    private static final String TAG = ConversationClient.class.getSimpleName();

    private ConversationClientConfig config;
    private String authToken;

    private ConversationSignalingChannel signalingChannel;
    private SocketEventNotifier socketEventNotifier;

    //push setting persisted
    private boolean pushEnabledForAllConversations;
    private String pushDeviceToken;
    private RequestHandler<Void> pushEnableListener;

    private ConcurrentMap<String, EventSource<?>> eventSourceMap = new ConcurrentHashMap<>();
    Handler handlerForCallbacks = null;

    private ConversationClient(ConversationClientConfig config) {
        this.config = config;
        /*if (this.config.enableCrashReporting) {
            Bugsnag.init(config.getContext());
            Bugsnag.setAppVersion(BuildConfig.SDK_REVISION_CODE);
        }*/
        CacheDB.initializeCacheDBInstance(config.getContext());

        this.socketEventNotifier = new SocketEventNotifier();
        this.signalingChannel = new ConversationSignalingChannel(this, new SocketClient(this));
        this.handlerForCallbacks = config.isOnMainThread() ? new Handler(config.getContext().getMainLooper()) : null;
    }

    /**
     * Login this {@link ConversationClient} instance based on the builder information and login details.
     * <p> Only one {@link ConversationClient#login(String, RequestHandler)} action can be performed at a time.
     * In case there is a user already logged in {@link NexmoAPIError#onUserAlreadyLoggedIn()} will be fired,
     * and explicit {@link ConversationClient#logout(RequestHandler)} needs to be performed.</p>
     *
     * <p>Depending on the validity of the supplied token, a {@link NexmoAPIError} can be fired for:
     * <ul>
     *     <li>{@link SessionError#INVALID_TOKEN}. Supplied token is not valid, your application needs to retrieve another one in order to complete login.</li>
     *     <li>{@link SessionError#EXPIRED_TOKEN}. Supplied token has expired, your application needs to retrieve another one in order to complete login.</li>
     * </ul>
     * </p>
     *
     * <p>A network connection will be established with the Conversation service, make sure you listen to connection events
     *  {@link ConversationClient#listenToConnectionEvents(NetworkingStateListener)}</p>
     *
     * Required parameters are:
     * <ul>
     *      <li>authToken:              The backend authorization authToken.</li>
     *      <li>listener                The completion listener.</li>
     * </ul>
     *
     * @param token    The backend authorization jwt.
     * @param loginListener The completion listener.
     */
    public void login(final String token, final RequestHandler<User> loginListener) {
        if (loginListener != null) {
            if (TextUtils.isEmpty(token))
                loginListener.onError(NexmoAPIError.missingParams());
            else if (getSignallingChannel().getLoggedInUser() != null)
                loginListener.onError(NexmoAPIError.onUserAlreadyLoggedIn());
            else if (this.signalingChannel.isConnecting())
                loginListener.onError(new NexmoAPIError(NexmoAPIError.CONNECT_ALREADY_IN_PROGRESS, "Already connecting"));
            else {
                this.authToken = token;

                /**
                 * Ask to establish a network connection with the Conversation service.
                 * If another connection is attempted already, an error is received.
                 */
                this.signalingChannel.connect(loginListener);
            }
        } else Log.d(TAG, "LoginListener is mandatory!");
    }

    /**
     * Logout current user.
     * Logout also:
     * <ul>
     *     <li>disconnects from the socket connection</li>
     *     <li>disables and terminates any audio stream.</li>
     *     <li>removes any event listeners and</li>
     *     <li>deletes any locally stored data.</li>
     * </ul>
     * <p> Only one {ConversationClient#logout(RequestHandler)} can be performed at a time. </p>
     *
     * @param logoutListener The {@link RequestHandler}.
     */
    public void logout(RequestHandler logoutListener) {
        if (logoutListener != null) {
            if (getSignallingChannel().getLoggedInUser() != null) {
                this.signalingChannel.logout(logoutListener);
                this.authToken = null;
            } else logoutListener.onError(NexmoAPIError.noUserLoggedIn());
        } else Log.d(TAG, "LogoutListener is mandatory!");
    }

    /**
     * Set/Reset the push registration authToken whenever there is a new one available, via FirebaseInstanceIdService, prior to
     * {@link ConversationClient#login(String, RequestHandler)}.
     *
     * <p>User must perform another login if a new push pushDeviceToken is issued.</p>
     *
     * @param pushDeviceToken The instance ID that uniquely identifies an app/device pairing for push purposes. Remember to update the
     *                        pushDeviceToken provided by a FirebaseInstanceIdService each time  the InstanceID pushDeviceToken is updated.
     */
    public void setPushDeviceToken(final String pushDeviceToken) {
        //cache it internally until login.
        this.pushDeviceToken = pushDeviceToken;
    }

    /**
     * Enable or Disable any push notifications triggered by any of the available conversations, using Firebase Cloud Messaging.
     *
     * <p></p>User must be logged in and set {@link ConversationClient#setPushDeviceToken} obtained by extending a FirebaseInstanceIdService.</p>
     *
     * <p>By default push notifications are disabled, until {@link ConversationClient#setPushDeviceToken(String)} and
     * any of the {@link ConversationClient#enableAllPushNotifications(boolean, RequestHandler)} or
     *{@link ConversationClient#enablePushNotificationsForConversation(boolean, String, RequestHandler)} are set.</p>
     *
     * @param enable             True or False.
     * @param pushEnableListener Completion listener.
     */
    public void enableAllPushNotifications(boolean enable, RequestHandler<Void> pushEnableListener) {
        if (pushEnableListener != null) {
            this.pushEnableListener = pushEnableListener;

            if (getSignallingChannel().getLoggedInUser() != null) {
                //enable/disable push regardless of logged in state.
                this.pushEnabledForAllConversations = enable;
                this.signalingChannel.enableAllPushNotifications(enable, pushEnableListener);
            }
            else
                this.pushEnableListener.onError(NexmoAPIError.noUserLoggedIn());
        }
        else
            Log.d(TAG, "PushEnableListener is mandatory!");
    }

    /**
     * NOTE This is not yet implemented service-side!
     *
     * Enable or Disable push notifications for certain conversations.
     * Push notifications must be enabled first via {@link ConversationClient#enableAllPushNotifications(boolean, RequestHandler)}.
     *
     * @param enable             True or False.
     * @param conversationId     The conversation id.
     * @param pushEnableListener Completion listener.
     */
    public void enablePushNotificationsForConversation(boolean enable, String conversationId, RequestHandler<Void> pushEnableListener) {
        if (pushEnableListener != null) {
            this.pushEnableListener = pushEnableListener;
            //enable/disable push regardless of logged in state.
            this.pushEnabledForAllConversations = (enable && this.pushEnabledForAllConversations);
            this.signalingChannel.enablePushNotifications(enable, conversationId, pushEnableListener);
        }
        else
            Log.d(TAG, "PushEnableListener is mandatory!");
    }

    /**
     * Create a new {@link Conversation} for a given name.
     *
     * <p>  In order for a {@link User} to be able to send and receive events from a {@link Conversation}, he must either:
     * <ul>
     *     <li>explicitly {@link Conversation#join(RequestHandler<Member>)} and receive a {@link Member} instance.</li>
     *     <li>accept an invitation.
     * Listen for incoming invitations using {@link Conversation#memberInvitedEvent()} } </li>
     * </ul> </p>
     *
     * @param conversationName           The optional conversation name. Must be unique, but it's case-insensitive.
     * @param conversationCreateListener The request completion listener.
     */
    public void newConversation(final String conversationName, RequestHandler<Conversation> conversationCreateListener) {
        if (getSignallingChannel().isValidInput(conversationCreateListener, null))
            createConversationWithJoin(false, conversationName, conversationCreateListener);
    }

    /**
     * Helper to create a new {@link Conversation} for a given name.
     *
     * <p> This helper optionally combines {@link #newConversation(String, RequestHandler)} and
     * {@link Conversation#join(RequestHandler)} into a single method call. </p>
     *
     * @param withJoin                   Specify if the new conversation should be automatically joined after creation.
     * @param conversationName           The optional conversation name. Must be unique, but it's case-insensitive.
     * @param conversationCreateListener The request completion listener.
     */
    public void newConversation(boolean withJoin, final String conversationName, RequestHandler<Conversation> conversationCreateListener) {
           if (getSignallingChannel().isValidInput(conversationCreateListener, null))
               createConversationWithJoin(withJoin, conversationName, conversationCreateListener);
    }

    /**
     * Helper method to create a {@link Conversation} and invite multiple users to an Audio Call.
     *
     * @param users              The optional users to get invited to the new {@link Call}.
     * @param callCreateListener The request completion listener.
     */
    public void call(List<String> users, RequestHandler<Call> callCreateListener) {
        if (getSignallingChannel().isValidInput(callCreateListener, null))
            this.signalingChannel.createCall(users, callCreateListener);
    }

    /**
     * Reload conversation list.
     * Retrieve a full list of conversations the logged in user is a Member of asynchronously.
     *
     * <p>A {@link Member} is part of a {@link Conversation} if:
     *  <li>
     *      <ul> he gets invited by some other Member. </ul>
     *      <ul> simply joins because he knows the conversation id. </ul>
     *  </li>
     *
     * @param conversationListListener The listener in charge of dispatching the result.
     */
    public void getConversations(RequestHandler<List<Conversation>> conversationListListener) {
        if (conversationListListener != null) {
            if (getSignallingChannel().socketClient.getConnectionStatus() == NetworkState.CONNECTED) {
                this.socketEventNotifier.setConversationListListener(conversationListListener);
                this.signalingChannel.updateConversationList();
            } else if (getSignallingChannel().getLoggedInUser() != null) {
                this.socketEventNotifier.setConversationListListener(conversationListListener);
                this.signalingChannel.updateConversationListFromCache();
            } else
                conversationListListener.onError(NexmoAPIError.noUserLoggedIn());
        } else
            Log.d(TAG, "ConversationListListener is mandatory");
    }

    /**
     * Get list of active conversation where user has been either invited, or joined, synchronously.
     * Subscribe to {@link ConversationClient#synchronisationEvent()} to get notified once all
     * conversations are synchronised.
     *
     * @return List of conversations.
     */
    public List<Conversation> getConversationList(){
        return this.signalingChannel.socketClient.getSocketEventHandler().getConversationList();
    }

    /**
     * Get conversation based on unique ID, synchronously.
     * The list is composed of conversations we last synchronised.
     * <p>If the conversation list becomes out of sync, force a re-sync with
     * {@link ConversationClient#getConversations(RequestHandler<List<Conversation>>)}</p>
     *
     * @param cid The unique conversation ID.
     * @return    The conversation, if any.
     */
    public Conversation getConversation(String cid){
        return this.signalingChannel.socketClient.getSocketEventHandler().findConversation(cid);
    }

    /**
     * Get the current logged in User if available.
     *
     * @return The current user.
     */
    public final User getUser() {
        return signalingChannel.getLoggedInUser();
    }

    /**
     * Check whether a User has been successfully logged in.
     *
     * @return boolean.
     */
    public boolean isLoggedIn() {
        return getSignallingChannel().getLoggedInUser() != null;
    }

    /**
     * Get user info for given userId.
     *
     * @param userId id of user
     * @param userInfoListener listener to be notified
     */
    public void getUser(String userId, RequestHandler<User> userInfoListener) {
        if (userInfoListener == null) {
            Log.d(TAG, "ConversationListListener is mandatory");
            return;
        }
        if (getSignallingChannel().getLoggedInUser() == null) {
            userInfoListener.onError(NexmoAPIError.noUserLoggedIn());
        } else signalingChannel.getUserInfo(userId, userInfoListener);
    }

    /**
     * Check whether the {@link ConversationClient} is trying to connect to the backend socket or not.
     * Use this method whenever you want a single fast check of the connection status.
     * Nevertheless, {@link NetworkingStateListener} does notify upon all of the different connection statuses as well.
     *
     * @return The socket connected status. One of the {@link NetworkState}
     */
    public NetworkState getNetworkingState() {
        return this.signalingChannel.getConnectionStatus();
    }

    /**
     * Get the context we used for building {@link ConversationClient#config}.
     *
     * @return a context.
     */
    public Context getContext() {
        return config.getContext();
    }

    /**
     * Get the last used jwt for login.
     *
     * @return Last authentication token.
     */
    public String getToken() { return authToken;}

    /**
     * Retrieve the push registration token that was once set.
     *
     * @return The Firebase push registration token.
     */
    public String getPushDeviceToken() {
        return this.pushDeviceToken;
    }

    public RequestHandler<Void> getPushEnableListener() {
        return this.pushEnableListener;
    }

    /**
     * Get config used to build {@link ConversationClient}.
     *
     * @return the config
     */
    public ConversationClientConfig getConfig() {
        return config;
    }

    /**
     * <p>If you rely on your own mechanism for downloading images - instead of
     * {@link Image#download(ImageRepresentation.TYPE, RequestHandler)} -
     * bear those custom headers in mind, so your request is authorized.</p>
     * <p>To access any static content on Nexmo IPS user must attach headers returned by this method</p>
     *
     * @return List of (key, value) user should put to server
     */
    public List<IPSHeader> ipsHeaders() {
        return Arrays.asList(
                new IPSHeader(Constants.CUSTOM_HEADER_AUTHORIZATION, "Bearer " + getToken())
        );
    }

    <T> EventSource<T> getEventSource(String id) {
        if (!this.eventSourceMap.containsKey(id))
            this.eventSourceMap.putIfAbsent(id, new EventSource<T>(this.handlerForCallbacks));
        return (EventSource<T>) this.eventSourceMap.get(id);
    }

    /**
     * Register for receiving invitation events from new conversations.
     * @return Invitation which holds: the conversation you've been invited to, your new member details
     *         and the sender of the invite.
     */
    public EventSource<Invitation> invitedEvent() {
        return getEventSource("invitedEvent");
    }

    /**
     * Register for receiving session events.
     * @return Any session error, due to either socket getting disconnected or token expiry events.
     */
    public EventSource<SessionError> sessionErrorEvent() {
        return getEventSource("sessionErrorEvent");
    }

    public EventSource<Conversation> joinedToNewConversationEvent() {
        return getEventSource("joinedToNewConversationEvent");
    }

    /**
     * Register for receiving incoming {@link Call} events.
     * <p> Call properties:
     * <ul>
     *     <li>Call name: {@link Call#getName()}.</li>
     *     <li>Username of the user who initiated the call: {@link Call#from()}.</li>
     *     <li>All members in the call: {@link Call#to()}.</li>
     * </ul>
     * </p>
     *
     * <p>< Upon receiving incoming call, following options are available:
     * <ul>
     *     <li>Answer {@link Call#answer(RequestHandler)}.</li>
     *     <li>Reject {@link Call#reject(RequestHandler)}.</li>
     *     <li>Hangup {@link Call#hangup(RequestHandler)} </li>
     * </ul>
     * /p>
     *
     */
    public EventSource<Call> callEvent() {
        return getEventSource("callEvent");
    }

    /**
     * Attach a listener for connection events.
     * Any application should listen for those events as the socket might get disconnected/reconnecting at any stage
     * without prior notice.
     *
     * @param listener The {@link NetworkingStateListener}.
     */
    public void listenToConnectionEvents(NetworkingStateListener listener) {
        this.signalingChannel.socketClient.addConnectionListener(listener);
    }

    public void removeConnectionListener(NetworkingStateListener listener) {
        this.signalingChannel.socketClient.removeConnectionListener(listener);
    }

    /**
     * Get the current synchronising State.
     * Use this method whenever you want a single fast check of the sync status.
     * Nevertheless, {@link ConversationClient#synchronisationEvent()} does notify upon all the
     * different synchronisation statuses as well.
     *
     * @return The synchronisation most recent status. One of the {@link SynchronisingState.STATE}.
     *         <p>The order of sync events is important, depending on which STATE has been synced,
     *          UI will be ready for update with fresh info.</p>
     *          <ul>
     *              <li>CONVERSATIONS: Conversations preview has been synced.
     *              At this point all members can be displayed in the UI.
     *              Wait for the next state for updating members information in the UI.</li>
     *              <li>MEMBERS: all information about the members has been synced.
     *              At this point all members can be displayed in the UI.</li>
     *          </ul>
     */
    public SynchronisingState.STATE getSynchronisingState() {
        return signalingChannel.getSyncState();
    }

    /**
     * Register for receiving synchronisation events.
     *
     * <p>The order of sync events is important, depending on which STATE has been synced,
     * UI will be ready for update with fresh info.</p>
     * <ul>
     *     <li>CONVERSATIONS: Conversations preview has been synced.
     *     At this point all members can be displayed in the UI.
     *     Wait for the next state for updating members information in the UI.</li>
     *     <li>MEMBERS: all information about the members has been synced. At this point all members can be displayed in the UI.</li>
     * </ul>
     *
     * <p> The Conversation Client holds the current state information. Fetch it at any point via
     * {@link ConversationClient#getSynchronisingState()} ()} </p>
     *
     * @return The most recent synchronisation state.
     */
    public EventSource<SynchronisingState.STATE> synchronisationEvent() {
        return getEventSource("synchronisationEvent");
    }

    @Override
    public String toString() {
        return TAG + " Token: " + (this.authToken != null ? this.authToken : "");
    }

    /**
     * Returns the current version of the Nexmo Conversation SDK.
     *
     *  @return The current version of the Nexmo Conversation SDK.
     */
    public static String getSDKversion() {
        return "";//BuildConfig.SDK_REVISION_CODE;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    ConversationSignalingChannel getSignallingChannel() {
        return this.signalingChannel;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    SocketEventNotifier getEventNotifier(){
        return this.socketEventNotifier;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    Router getRouter() {
        return signalingChannel.socketClient.router;
    }

    /**
     * Explicitly disconnect the socket connection.
     * <p>Use this when your app is not being in use.
     * In order to reconnect the socket back, user must re-login.</p>
     *
     * Explicit disconnect also:
     * <ul>
     *     <li>disables and terminates any audio stream</li>
     *     <li>removes any event listeners and</li>
     *     <li>deletes any locally stored data.</li>
     * </ul>
     *
     * <p>If there is no ongoing socket connection the method will return.</p>
     * @experimental
     */
    public void disconnect(){
        if (getSignallingChannel().socketClient.getConnectionStatus() != NetworkState.DISCONNECTED)
            getSignallingChannel().socketClient.explicitDisconnect();
        else Log.w(TAG, "Cannot disconnect socket connection, there is no connection in use.");
    }

    void callUserCallback(Runnable subject) {
        if (handlerForCallbacks == null) {
            subject.run();
            return;
        }
        handlerForCallbacks.post(subject);
    }

    private void createConversationWithJoin(boolean withJoin, String conversationName, RequestHandler<Conversation> conversationCreateListener) {
        if (withJoin) {
            this.signalingChannel.newConversationWithJoin(conversationName, conversationCreateListener);
        } else {
            this.signalingChannel.newConversation(conversationName, conversationCreateListener);
        }
    }

    /**
     * HTTP headers to be provided to access IPS service's media
     */
    public static class IPSHeader {
        private String key;
        private String value;

        public IPSHeader(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * POJO which contains current configuration (has to be built by Builder)
     * First step is to acquire a ```ConversationClient``` instance based on a context.
     *
     * <pre>
     *     ConversationClient client = new ConversationClient.ConversationClientBuilder()
     *     .context(this)
     *     .build();
     * </pre>
     * Optional parameters for ConversationClientBuilder are:
     * <pre>.environmentHost(endpoint)      // default Config.ENDPOINT_PRODUCTION = "https://ws.nexmo.com"</pre>
     * <pre.endpointPath(endpointPath)     // default Config.ENDPOINT_PATH = "/rtc/"></pre>
     * <pre>.imageProcessingServiceUrl(ips) // default Config.IPS_ENDPOINT_PRODUCTION = "https://api.nexmo.com/v1/image"</pre>
     * <pre>.flushPending(true)             // default true. Pending operations from previous app start will be flushed on login process</pre>
     * <pre>.logLevel(Log.ASSERT)           // default Log.ASSERT(minimal output). For verbose logs use Log.VERBOSE</pre>
     * <pre>.autoReconnect(true)            // default true. Set automatic reconnect policy if the connectivity gets lost.</pre>
     *
     */
     public static class ConversationClientConfig {
        protected Context context;
        protected String environmentHost = "https://ws.nexmo.com";
        protected String endpointPath = "/rtc/";
        protected boolean enableCrashReporting = false; // by default false.
        protected String imageProcessingServiceUrl = "https://api.nexmo.com/v1/image";
        protected boolean flushPending;
        protected int logLevel = android.util.Log.ASSERT;
        protected boolean autoReconnect = true; // automatically-reconnect policy when the socket gets disconnected.
        protected boolean onMainThread = true;


        ConversationClientConfig() { }

        public Context getContext() { return context;}

        public String getEnvironmentHost() {
            return environmentHost;
        }

        public boolean isEnableCrashReporting() {
            return enableCrashReporting;
        }

        public String getImageProcessingServiceUrl() {
            return imageProcessingServiceUrl;
        }

        public String getEndpointPath() {
            return endpointPath;
        }

        public boolean isFlushPending() {
            return flushPending;
        }

        public boolean isAutoReconnect() {
            return this.autoReconnect;
        }

        public boolean isOnMainThread() {
            return onMainThread;
        }
     }


    /**
     * API for creating {@link ConversationClient} instances.
     *
     * Acquire a ConversationClient, based on the following mandatory params:
     * <ul>
     *      <li>applicationContext: The application context.</li>
     * </ul>
     * <p> Example usage:</p>
     * <pre>
     *     // Create a ConversationClient using the ConversationClientBuilder.
     *     ConversationClient myConversationClient = new ConversationClient.ConversationClientBuilder()
     *        .context(myAppContext)
     *        .build();
     * </pre>
     */
    public static class ConversationClientBuilder extends ConversationClient.ConversationClientConfig {
        /**
         * Build a {@link ConversationClient}, based on the following mandatory params:
         * <ul>
         * <li>applicationContext: The application context.</li>
         * </ul>
         *
         * @return an instance of {@link ConversationClient}.
         * @throws ConversationClientException A {@link ConversationClientException} if any of the mandatory params are not supplied.
         */
        public ConversationClient build()  {
            StringBuilder builder = new StringBuilder();
            if (context == null) {
                ConversationClientException.appendExceptionCause(builder, "Context");
                throw new ConversationClientException("Building a ConversationClient instance has failed due to missing parameters: "
                        + builder.toString());
            }

            if (this.environmentHost == null)
                throw new ConversationClientException("environmentHost can't be null");

            if (this.imageProcessingServiceUrl == null)
                throw new ConversationClientException("imageProcessingServiceUrl can't be null");

            if (this.endpointPath == null)
                throw new ConversationClientException("endpointPath can't be null");

            if (this.logLevel > android.util.Log.ASSERT)
                throw new ConversationClientException("Maximum log level is ASSERT");

            if (this.logLevel < android.util.Log.VERBOSE)
                throw new ConversationClientException("Minimum log level is VERBOSE");

            Log.setLevel(this.logLevel);

            return new ConversationClient(this);
        }

        public ConversationClientBuilder context(@NonNull final Context context) {
            this.context = context;
            return this;
        }

        public ConversationClientBuilder environmentHost(@NonNull final String environmentHost) {
            this.environmentHost = environmentHost;
            return this;
        }

        public ConversationClientBuilder enableCrashReporting(boolean enableCrashReporting) {
            this.enableCrashReporting = enableCrashReporting;
            return this;
        }

        public ConversationClientBuilder imageProcessingServiceUrl(@NonNull String ipsUrl) {
            this.imageProcessingServiceUrl = ipsUrl;
            return this;
        }

        public ConversationClientBuilder endpointPath(@NonNull String path) {
            this.endpointPath = path;
            return this;
        }

        /**
         * When new session is being created pending operations from previous app start
         * will be flushed on login process
         * @param flushPending true if pending operations have to be flushed. Default is false
         */
        public ConversationClientBuilder flushPending(boolean flushPending) {
            this.flushPending = flushPending;
            return this;
        }

        /**
         * Set log level for SDK. Default is {@link android.util.Log#ASSERT} - minimal output.
         * For maximum verbosity use {@link android.util.Log#VERBOSE}
         * @param logLevel required log level constant from Log {@link android.util.Log}
         */
        public ConversationClientBuilder logLevel(int logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Set automatic reconnect policy.
         *
         * <p>By default if the connection gets lost, SDK is automatically reconnecting exponentially.</p>
         *
         * @param autoReconnect
         */
        public ConversationClientBuilder autoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        /**
         * User can specify thread on which callback will be fired
         * @param useMainThread if true main application thread will be used
         */
        public ConversationClientBuilder onMainThread(boolean useMainThread) {
            this.onMainThread = useMainThread;
            return this;
        }

    }

}

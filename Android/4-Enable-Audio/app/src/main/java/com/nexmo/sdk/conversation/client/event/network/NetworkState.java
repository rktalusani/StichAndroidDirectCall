package com.nexmo.sdk.conversation.client.event.network;

import com.nexmo.sdk.conversation.client.ConversationClient;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.client.User;


/**
 * Representation connection state of the SDK
 */
public enum NetworkState {
    /**
     * Connection completion.
     */
    CONNECTED,
    /**
     * User has been disconnected. Possible cause may be connectivity issues.
     * Also occurs after an explicit {@link ConversationClient#logout(RequestHandler<User>)}.
     */
    DISCONNECTED,
    RECONNECT,
    CONNECT_TIMEOUT,
    CONNECT_ERROR,
    /**
     * Reconnection error.
     */
    RECONNECT_ERROR,
}

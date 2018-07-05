/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.client.event.network;

import com.nexmo.sdk.conversation.client.ConversationClient;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.client.User;

/**
 * Socket Connection listener.
 * Any application should listen for those events as the socket might get disconnected at any stage
 * without prior notice.
 *
 * @author emma tresanszki.
 * @hide
 */
public interface NetworkingStateListener {

    /**
     * User connection has been changed.
     * User has been disconnected. Possible cause may be connectivity issues.
     * onDisconnected also occurs after an explicit {@link ConversationClient#logout(RequestHandler<User>)}.
     *
     * @param networkingState Session networking state has been changed.
     */
    void onNetworkingState(NetworkState networkingState);

}

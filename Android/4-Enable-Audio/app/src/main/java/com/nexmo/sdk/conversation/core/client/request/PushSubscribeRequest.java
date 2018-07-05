/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.event.RequestHandler;

/**
 *
 * @author emma tresanszki.
 *
 * @hide
 */
public class PushSubscribeRequest extends PushSubscriptionRequest {

    public PushSubscribeRequest(String cid, RequestHandler pushEnableListener){
        super(Request.TYPE.PUSH_SUBSCRIBE, cid, pushEnableListener);
    }

    @Override
    public String getRequestName() {
        return PushSubscriptionRequest.PUSH_SUBSCRIBE;
    }

    @Override
    public String getSuccessEventName() {
        return PUSH_SUBSCRIBE_SUCCESS;
    }
}

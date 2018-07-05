/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.client.request;

import android.text.TextUtils;

import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get conversation details request.
 *
 * @author emma tresanszki.
 * @hide
 */
public abstract class GetEventsBaseRequest<R> extends ConversationRequestBase<RequestHandler<Conversation>, R>{
    private String startId;
    private String endId;

    protected GetEventsBaseRequest(TYPE type, String cid, String startId, String endId, RequestHandler<Conversation> listener) {
        super(type, cid, listener);
        this.startId = startId;
        this.endId = endId;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        JSONObject bodyObj = new JSONObject();
        if (!TextUtils.isEmpty(this.startId))
            bodyObj.put("start_id", this.startId);
        if (!TextUtils.isEmpty(this.endId))
            bodyObj.put("end_id", this.endId);

        return newConversationTaggedResponse()
            .put("body", bodyObj);
    }
}

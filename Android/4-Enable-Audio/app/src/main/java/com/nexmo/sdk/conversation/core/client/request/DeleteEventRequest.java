/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author emma tresanszki.
 * @hide
 */
public class DeleteEventRequest extends ConversationRequestBase<RequestHandler<Void>, Void> {
    public static final String EVENT_DELETE = "event:delete";
    public static final String EVENT_DELETE_SUCCESS = "event:delete:success";
    public String cid;
    public String messageId;
    public String memberId;

    public DeleteEventRequest(String cid, String memberId, String messageId, RequestHandler<Void> listener) {
        super(TYPE.DELETE_EVENT, cid, listener);
        this.cid = cid;
        this.memberId = memberId;
        this.messageId = messageId;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newConversationTaggedResponse()
                .put("from", this.memberId)
                .put("body", jsonObject("event_id", this.messageId));
    }

    @Override
    public String getRequestName() {
        return EVENT_DELETE;
    }

    @Override
    public String getSuccessEventName() {
        return EVENT_DELETE_SUCCESS;
    }

    @Override
    public boolean isPersistable() {
        return true;
    }

    @Override
    public Void parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        return null;
    }
}

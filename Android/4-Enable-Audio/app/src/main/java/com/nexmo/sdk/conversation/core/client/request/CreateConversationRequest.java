/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Conversation create request.
 *
 * @hide
 */
public class CreateConversationRequest extends Request<RequestHandler<Conversation>, String>  {
    public static final String CONVERSATION_NEW_REQUEST = "new:conversation";
    public static final String CONVERSATION_NEW_SUCCESS = "new:conversation:success";
    public String displayName;

    public CreateConversationRequest(String displayName, RequestHandler<Conversation> listener) {
        super(TYPE.CREATE, listener);
        this.displayName = displayName;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newTaggedResponse()
            .put("body", jsonObject("display_name", this.displayName));
    }

    @Override
    public String getRequestName() {
        return CONVERSATION_NEW_REQUEST;
    }

    @Override
    public String getSuccessEventName() {
        return CONVERSATION_NEW_SUCCESS;
    }

    @Override
    public boolean isPersistable() {
        return true;
    }

    @Override
    public String parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        return body.getString("id");
    }
}

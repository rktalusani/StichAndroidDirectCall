/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.client.request;

import org.json.JSONException;
import org.json.JSONObject;

import com.nexmo.sdk.conversation.client.Event;
import com.nexmo.sdk.conversation.client.Text;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.util.DateUtil;

import java.util.Date;

import static com.nexmo.sdk.conversation.core.client.request.Request.TYPE.SEND_TEXT;

/**
 * Send text request.
 *
 * @author emma tresanszki.
 *
 * @hide
 */
public class SendTextMessageRequest extends ConversationRequestBase<RequestHandler<Text>, SendTextMessageRequest.Container> {
    public static final String TEXT_MESSAGE = "text";
    public static final String TEXT_MESSAGE_SUCCESS = "text:success";
    public String message;
    public String memberId;


    public SendTextMessageRequest(String cid, String memberId, String message, RequestHandler<Text> listener) {
        super(SEND_TEXT, cid, listener);
        this.memberId = memberId;
        this.message = message;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newConversationTaggedResponse()
            .put("from", this.memberId)
            .put("body", jsonObject("text", this.message));
    }

    @Override
    public String getRequestName() {
        return TEXT_MESSAGE;
    }

    @Override
    public String getSuccessEventName() {
        return TEXT_MESSAGE_SUCCESS;
    }

    @Override
    public boolean isPersistable() {
        return true;
    }

    @Override
    public Container parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        return new Container(body.getString("id"), DateUtil.parseDateFromJson(body, "timestamp"));
    }

    public static class Container {
        public String messageId;
        public Date timestamp;

        public Container(String messageId, Date timestamp) {
            this.messageId = messageId;
            this.timestamp = timestamp;
        }
    }
}

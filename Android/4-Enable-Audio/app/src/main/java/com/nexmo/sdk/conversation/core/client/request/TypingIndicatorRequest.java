/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author emma tresanszki.
 *
 * @hide
 */
public abstract class TypingIndicatorRequest extends ConversationRequestBase<RequestHandler<Member.TYPING_INDICATOR>, Void> {
    public static final String TEXT_TYPE_ON = "text:typing:on";
    static final String TEXT_TYPE_ON_SUCCESS = "text:typing:on:success";
    public static final String TEXT_TYPE_OFF = "text:typing:off";
    static final String TEXT_TYPE_OFF_SUCCESS = "text:typing:off:success";
    public String memberId;
    public Member.TYPING_INDICATOR typingIndicator;

    protected TypingIndicatorRequest(Member.TYPING_INDICATOR typingIndicator, String cid, String memberId, RequestHandler<Member.TYPING_INDICATOR> listener) {
        super(TYPE.TYPING, cid, listener);
        this.memberId = memberId;
        this.typingIndicator = typingIndicator;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newConversationTaggedResponse()
            .put("from", this.memberId)
            .put("body", jsonObject("activity", this.typingIndicator));

    }

    @Override
    public Void parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        return null;
    }

    public static class TypingOff extends TypingIndicatorRequest {
        public TypingOff(Member.TYPING_INDICATOR typingIndicator, String cid, String memberId, RequestHandler listener) {
            super(typingIndicator, cid, memberId, listener);
        }

        @Override
        public String getRequestName() {
            return TEXT_TYPE_OFF;
        }

        @Override
        public String getSuccessEventName() {
            return TEXT_TYPE_OFF_SUCCESS;
        }
    }

    public static class TypingOn extends TypingIndicatorRequest {
        public TypingOn(Member.TYPING_INDICATOR typingIndicator, String cid, String memberId,  RequestHandler listener) {
            super(typingIndicator, cid, memberId, listener);
        }

        @Override
        public String getRequestName() {
            return TEXT_TYPE_ON;
        }

        @Override
        public String getSuccessEventName() {
            return TEXT_TYPE_ON_SUCCESS;
        }
    }

    public static TypingIndicatorRequest forType(Member.TYPING_INDICATOR typingIndicator, String cid, String memberId, RequestHandler listener) {
        if (typingIndicator == Member.TYPING_INDICATOR.ON)
            return new TypingOn(typingIndicator, cid, memberId, listener);
        else if (typingIndicator == Member.TYPING_INDICATOR.OFF)
            return new TypingOff(typingIndicator, cid, memberId, listener);

        throw new IllegalArgumentException("Never can happen");
    }
}

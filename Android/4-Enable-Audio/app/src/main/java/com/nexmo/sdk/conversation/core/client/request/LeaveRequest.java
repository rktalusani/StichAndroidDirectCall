/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.util.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Leave conversation request.
 *
 * @author emma tresanszki.
 *
 * @hide
 */
public class LeaveRequest extends ConversationRequestBase<RequestHandler<Void>, LeaveRequest.TimestampsContainer> {
    static final String CONVERSATION_LEAVE = "conversation:member:delete";
    static final String CONVERSATION_LEAVE_SUCCESS = "conversation:member:delete:success";
    public final Member member;

    public LeaveRequest(TYPE type, String cid, Member member, RequestHandler leaveListener) {
        super(type, cid, leaveListener);
        this.member = member;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newConversationTaggedResponse()
                .put("body", jsonObject("member_id", member!=null ? member.getMemberId() : ""));
    }

    @Override
    public String getRequestName() {
        return CONVERSATION_LEAVE;
    }

    @Override
    public String getSuccessEventName() {
        return CONVERSATION_LEAVE_SUCCESS;
    }

    @Override
    public boolean isPersistable() {
        return true;
    }

    @Override
    public TimestampsContainer parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        JSONObject timestampJson = body.getJSONObject("timestamp");
        Date joinedTimestamp = null, leftTimestamp = null, invitedTimestamp = null;

        if (timestampJson.has("joined"))
            joinedTimestamp = DateUtil.parseDateFromJson(timestampJson, "joined");
        if (timestampJson.has("left"))
            leftTimestamp = DateUtil.parseDateFromJson(timestampJson, "left");
        if (timestampJson.has("invited"))
            invitedTimestamp = DateUtil.parseDateFromJson(timestampJson, "invited");
        return new TimestampsContainer(joinedTimestamp, leftTimestamp, invitedTimestamp);
    }

    /**
     * Created by rux on 06/03/17.
     */
    public static class TimestampsContainer {
        public Date joinedTimestamp = null, leftTimestamp = null, invitedTimestamp = null;

        public TimestampsContainer(Date joinedTimestamp, Date leftTimestamp, Date invitedTimestamp) {
            this.joinedTimestamp = joinedTimestamp;
            this.leftTimestamp = leftTimestamp;
            this.invitedTimestamp = invitedTimestamp;
        }
    }
}

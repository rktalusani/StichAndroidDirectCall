/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.client.request;

import android.text.TextUtils;

import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.User;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Join conversation request.
 * @author emma tresanszki.
 *
 * @hide
 */
public class JoinRequest extends ConversationRequestBase<RequestHandler<Member>, Member> {
    static final String CONVERSATION_JOIN_REQUEST = "conversation:join";
    static final String CONVERSATION_JOIN_SUCCESS = "conversation:join:success";
    public String cName;
    public String memberId;
    public String userId;
    public String userName;


    private JoinRequest(String cid, String cName, RequestHandler<Member> listener, String userId, String userName)
    {
        super(Request.TYPE.JOIN, cid, listener);
        this.cName = cName;
        this.userId = userId;
        this.userName = userName;
    }

    private JoinRequest(String cid, String cName, String memberId, RequestHandler<Member> listener, String userId, String userName) {
        this(cid, cName, listener, userId, userName);
        this.memberId = memberId;
    }

    public static JoinRequest createJoinRequestWithUsername(String cid,
                                                String cName,
                                                RequestHandler<Member> listener,
                                                String userName) {
        return new JoinRequest(cid, cName, listener, null, userName);
    }

    public static JoinRequest createJoinRequestWithUserId(String cid,
                                                            String cName,
                                                            RequestHandler<Member> listener,
                                                            String userId) {
        return new JoinRequest(cid, cName, listener, userId, null);
    }


    public static JoinRequest createJoinRequestWithMemberId(String cid,
                                                            String cName,
                                                            String memberId,
                                                            RequestHandler<Member> listener,
                                                            String userId,
                                                            String userName) {
        return new JoinRequest(cid, cName, memberId, listener, userId, userName);
    }


    @Override
    protected JSONObject makeJson() throws JSONException {
        JSONObject bodyObj = new JSONObject();
        if(this.userId != null)
            bodyObj.put("user_id", this.userId);
        if(this.userName != null)
            bodyObj.put("user_name", this.userName);
        if (!TextUtils.isEmpty(this.memberId))
            bodyObj.put("member_id", this.memberId);

        return newConversationTaggedResponse()
                .put("body", bodyObj);
    }

    @Override
    public String getRequestName() {
        return CONVERSATION_JOIN_REQUEST;
    }

    @Override
    public String getSuccessEventName() {
        return CONVERSATION_JOIN_SUCCESS;
    }

    @Override
    public boolean isPersistable() {
        return true;
    }

    @Override
    public Member parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        JSONObject memberJson = new JSONObject();
        memberJson.put("user_id", body.getString("user_id"));
        memberJson.put("member_id", body.getString("id"));
        memberJson.put("timestamp", body.getJSONObject("timestamp"));
        memberJson.put("state", Member.STATE.JOINED.getId());
        memberJson.put("name", this.userName);

        return Member.fromJson(memberJson);
    }
}

/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.util.Log;

import com.nexmo.sdk.conversation.core.util.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Invite request.
 *
 * @author emma tresanszki.
 * @hide
 */
public class InviteRequest extends Request<RequestHandler<Member>, InviteRequest.InviteContainer> implements ResultParser<InviteRequest.InviteContainer> {
    public static final String TAG = InviteRequest.class.getSimpleName();

    static final String INVITE_REQUEST = "conversation:invite";
    static final String INVITE_SUCCESS = "conversation:invite:success";
    public String cid;
    public String user;


    public InviteRequest(String cid, String user, RequestHandler<Member> listener){
        super(TYPE.INVITE, listener);
        this.cid = cid;
        this.user = user;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newTaggedResponse()
            .put("cid", cid)
            .put("body", jsonObject("user_name", user));
    }

    @Override
    public String getRequestName() {
        return INVITE_REQUEST;
    }

    @Override
    public String getSuccessEventName() {
        return INVITE_SUCCESS;
    }

    @Override
    public boolean isPersistable() {
        return true;
    }

    @Override
    public InviteContainer parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        if (body.getString("state").equals("INVITED")) {
            String userId = body.getString("user_id");
            String memberId = body.getString("id");
            Date timestampInvited = DateUtil.parseDateFromJson(body.getJSONObject("timestamp"), "invited");
            return new InviteContainer(userId, memberId, timestampInvited);
        } else
            Log.d(TAG, "onInvite:success event error, member is not INVITED"); // corner-case

        return null;
    }


    public static class InviteContainer {
        public String userId, memberId;
        public Date timestampInvited;

        public InviteContainer(String userId, String memberId, Date timestampInvited) {
            this.userId = userId;
            this.memberId = memberId;
            this.timestampInvited = timestampInvited;
        }
    }
}


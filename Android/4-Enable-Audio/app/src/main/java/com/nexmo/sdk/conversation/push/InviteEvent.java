package com.nexmo.sdk.conversation.push;

import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.core.util.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

public class InviteEvent {
    public static final String MESSAGE_KEY_INVITE = "invited";
    private static final String MESSAGE_KEY_USER = "user";
    private static final String MESSAGE_KEY_USERNAME = "name";
    private static final String MESSAGE_KEY_STATE = "state";
    private static final String MESSAGE_KEY_CNAME = "cname";
    private static final String MESSAGE_KEY_INVITE_BY = "invited_by";
    private static final String MESSAGE_KEY_TIMESTAMP = "timestamp";

    String senderMemberId;
    String cName;
    String conversationId;
    String senderUsername;
    JSONObject time;
    Date timestampInvited;
    Member invitedMember;

    public String getSenderMemberId() {
        return senderMemberId;
    }

    public String getcName() {
        return cName;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public JSONObject getTime() {
        return time;
    }

    public Date getTimestampInvited() {
        return timestampInvited;
    }

    public Member getInvitedMember() {
        return invitedMember;
    }

    private InviteEvent(String senderMemberId, String cName, String senderUsername, JSONObject time, Date timestampInvited, Member invitedMember, String conversationId) {

        this.senderMemberId = senderMemberId;
        this.cName = cName;
        this.senderUsername = senderUsername;
        this.time = time;
        this.timestampInvited = timestampInvited;
        this.invitedMember = invitedMember;
        this.conversationId = conversationId;
    }

    public static InviteEvent createInviteEvent(JSONObject body, String senderId, String conversationId) throws JSONException {

            JSONObject time = body.getJSONObject(MESSAGE_KEY_TIMESTAMP);
            Date timestampInvited = null;
            try {
                timestampInvited = DateUtil.formatIso8601DateString(time.getString(MESSAGE_KEY_INVITE));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            // Building a JSON user
            JSONObject user = body.getJSONObject(MESSAGE_KEY_USER);
            user.put(MESSAGE_KEY_USERNAME, user.getString(MESSAGE_KEY_USERNAME));
            user.put(MESSAGE_KEY_STATE, Member.STATE.INVITED.getId());
            user.put(MESSAGE_KEY_TIMESTAMP, time);
            user.put(MESSAGE_KEY_INVITE, timestampInvited);

            Member invitedMember = Member.fromJson(user);

            InviteEvent inviteEvent = new InviteEvent(senderId, body.getString(MESSAGE_KEY_CNAME), body.getString(MESSAGE_KEY_INVITE_BY), time, timestampInvited, invitedMember, conversationId);

            return inviteEvent;
    }
}

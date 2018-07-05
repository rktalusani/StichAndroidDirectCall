/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.client;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;

import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.util.Log;

import com.nexmo.sdk.conversation.core.util.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_INVITEDAT;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_JOINEDAT;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_LEFTAT;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_MEMBER_ID;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_STATE;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_USERNAME;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_USER_ID;

/**
 * Use this class to retrieve information about a Member and handle state. For example, when a
 * Member has joined a Conversation, or when he or she is typing.
 *
 * The following code example shows how to see which Member sent a text message:
 * <pre>
 *   conversation.messageEvent().add(new ResultListener<Event>() {
 *    &#64;Override
 *    public void onSuccess(Event result) {
 *        notifyUI("New event from " + result.getMember());
 *    }
 *  });
 * </pre>
 *
 * @author emma tresanszki.
 */
public class Member implements Parcelable {
    private static final String TAG = Member.class.getSimpleName();

    private String memberId;
    private String userId;
    private String name;
    private STATE state = STATE.UNKNOWN;
    private TYPING_INDICATOR typingIndicator = TYPING_INDICATOR.OFF;
    // STATE timestamp is returned by get events history.
    private Date joinedAt;
    private Date invitedAt;
    private Date leftAt;
    private Conversation conversation;

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Member(String memberId) {
        this.memberId = memberId;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Member(String memberId, Conversation conversation) {
        this(memberId);
        this.conversation = conversation;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Member(String userId, String name, String memberId) {
        this(memberId);
        this.userId = userId;
        this.name = name;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Member(String userId, String name, String memberId, STATE state) {
        this(userId, name, memberId);
        this.state = state;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Member(String userId, String name, String memberId,
                  Date joinedAt, Date invitedAt, Date leftAt, STATE state) {
        this(userId, name, memberId, state);
        this.invitedAt = invitedAt;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
    }

    protected Member(User user) {
        this.userId = user.getUserId();
        this.name = user.getName();
    }

    protected Member(Member member) {
        this(member.getUserId(), member.getName(), member.getMemberId(), member.getJoinedAt(),
                member.getInvitedAt(), member.getLeftAt(), member.getState());
        this.typingIndicator = member.getTypingIndicator();
    }

    protected Member(Parcel in) {
        this.userId = in.readString();
        this.name = in.readString();
        this.memberId = in.readString();
        this.state = STATE.values()[in.readInt()];
        this.joinedAt = (Date) in.readSerializable();
        this.leftAt = (Date) in.readSerializable();
        this.invitedAt = (Date) in.readSerializable();
        this.typingIndicator = in.readInt() == 1 ? TYPING_INDICATOR.ON : TYPING_INDICATOR.OFF;
    }

    protected void setName(String name) {
        this.name = name;
    }

    //**********************************************************************************************
    //*                                                                                            *
    //*                         Public Interface of Member Object                                  *
    //*                                                                                            *
    //**********************************************************************************************

    public String getName() {
        return this.name;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getMemberId() {
        return this.memberId;
    }

    public STATE getState() {
        return this.state;
    }

    public TYPING_INDICATOR getTypingIndicator() {
        return this.typingIndicator;
    }

    public Date getJoinedAt() {
        return this.joinedAt;
    }

    public Date getInvitedAt() {
        return this.invitedAt;
    }

    public Date getLeftAt() {
        return this.leftAt;
    }

    public Conversation getConversation() { return this.conversation; }

    /**
     * Remove a member from a conversation.
     *
     * @param leaveListener The completion listener in charge of dispatching the result.
     */
    public void kick(RequestHandler<Void> leaveListener) {
        if (leaveListener == null)
            Log.e(TAG, "Leave Listener is mandatory in order to kick a member from a conversation");
        else if (this.conversation == null)
            leaveListener.onError(new NexmoAPIError(NexmoAPIError.MISSING_PARAMS, "The conversation cannot be NULL"));
        else if (conversation.getSignallingChannel().getLoggedInUser() == null)
            leaveListener.onError(NexmoAPIError.noUserLoggedIn());
         else
            conversation.getSignallingChannel().leaveConversation(conversation, this, leaveListener);
    }

    public static final Creator<Member> CREATOR = new Creator<Member>() {
        @Override
        public Member createFromParcel(Parcel in) {
            return new Member(in);
        }

        @Override
        public Member[] newArray(int size) {
            return new Member[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(name);
        dest.writeString(memberId);
        dest.writeInt(state.ordinal());
        dest.writeSerializable(joinedAt);
        dest.writeSerializable(leftAt);
        dest.writeSerializable(invitedAt);
        dest.writeInt(typingIndicator == TYPING_INDICATOR.ON ? 1 : 0);
    }

    @Override
    public String toString(){
        return TAG +
                " name: " + (this.name!= null ? this.name: "") +
                " .userId: " + (this.userId != null ? this.userId : "") +
                " .memberId: " + (this.memberId != null ? this.memberId : "") +
                " .state: " + (this.state.getId()) +
                " .typing: " + (this.typingIndicator != null ? this.typingIndicator.toString() : "") +
                " .invitedAt: " + (this.invitedAt != null ? this.invitedAt.toString() : "") +
                " .joinedAt: " + (this.joinedAt != null ? this.joinedAt.toString() : "") +
                " .leftAt: " + (this.leftAt != null ? this.leftAt.toString() : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member)) return false;

        Member member = (Member) o;

        if (memberId != null ? !memberId.equals(member.memberId) : member.memberId != null)
            return false;
        if (userId != null ? !userId.equals(member.userId) : member.userId != null) return false;
        if (name != null ? !name.equals(member.name) : member.name != null) return false;
        if (state != member.state) return false;
        if (joinedAt != null ? !joinedAt.equals(member.joinedAt) : member.joinedAt != null)
            return false;
        if (invitedAt != null ? !invitedAt.equals(member.invitedAt) : member.invitedAt != null)
            return false;
        return leftAt != null ? leftAt.equals(member.leftAt) : member.leftAt == null;

    }

    @Override
    public int hashCode() {
        int result = memberId != null ? memberId.hashCode() : 0;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + state.hashCode();
        result = 31 * result + (joinedAt != null ? joinedAt.hashCode() : 0);
        result = 31 * result + (invitedAt != null ? invitedAt.hashCode() : 0);
        result = 31 * result + (leftAt != null ? leftAt.hashCode() : 0);
        return result;
    }


    public enum TYPING_INDICATOR {
        ON,
        OFF
    }

    public enum STATE {
        JOINED("JOINED"),
        INVITED("INVITED"),
        LEFT("LEFT"),
        UNKNOWN("");

        private String id;

        STATE(String id) {
            this.id = id;
        }

        /**
         * Get string Id representation
         */
        public String getId() {
            return id;
        }

        /**
         * Returns state from given string representation or UNKNOWN if no representation found
         */
        public static STATE fromId(String id) {
            for (STATE state : STATE.values())
                if (id.equals(state.getId())) return state;
            return UNKNOWN;
        }

    }

    public static Member fromCursor(Cursor cursor) {
        if (cursor == null) return null;

        Date dateInvited = null, dateLeft = null, dateJoined = null;
        try {
            dateInvited = DateUtil.formatIso8601DateString(cursor.getString(cursor.getColumnIndex(COLUMN_INVITEDAT)));
            dateLeft = DateUtil.formatIso8601DateString(cursor.getString(cursor.getColumnIndex(COLUMN_LEFTAT)));
            dateJoined = DateUtil.formatIso8601DateString(cursor.getString(cursor.getColumnIndex(COLUMN_JOINEDAT)));
        } catch (ParseException e) {
            Log.d(TAG, "Member: wrong date format");
        }

        return new Member(cursor.getString(cursor.getColumnIndex(COLUMN_USER_ID)),
                cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME)),
                cursor.getString(cursor.getColumnIndex(COLUMN_MEMBER_ID)),
                dateJoined,
                dateInvited,
                dateLeft,
                Member.STATE.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_STATE))));
    }

    /**
     * Construct object from the network response.
     * @param memberJSON network response
     *
     * @throws JSONException if required fields don't exist
     */
    public static Member fromJson(JSONObject memberJSON) throws JSONException {

        Date joinedTimestamp = null, leftTimestamp = null, invitedTimestamp = null;

        JSONObject timestampJson = memberJSON.optJSONObject("timestamp");
        if (timestampJson != null) {
            if (timestampJson.has("joined"))
                joinedTimestamp = DateUtil.parseDateFromJson(timestampJson, "joined");
            if (timestampJson.has("left"))
                leftTimestamp = DateUtil.parseDateFromJson(timestampJson, "left");
            if (timestampJson.has("invited"))
                invitedTimestamp = DateUtil.parseDateFromJson(timestampJson, "invited");
        }

        Member member = new Member(memberJSON.getString("user_id"), memberJSON.optString("name"),
                memberJSON.getString("member_id"), joinedTimestamp, invitedTimestamp,
                leftTimestamp, Member.STATE.fromId(memberJSON.getString("state")));

        return member;
    }

    //**********************************************************************************************
    //*                                                                                            *
    //*              Package level (access) & Private operations of Member Object                  *
    //*                                                                                            *
    //**********************************************************************************************

    void setConversation(Conversation conversation) { this.conversation = conversation; }

    synchronized void setTypingIndicator(TYPING_INDICATOR typingIndicator) {
        this.typingIndicator = typingIndicator;
    }

    synchronized void updateState(STATE state, Date date) {
        switch(state) {
            case INVITED: {
                setState(STATE.INVITED);
                invitedAt = date;
                break;
            }
            case JOINED: {
                setState(STATE.JOINED);
                joinedAt = date;
                break;
            }
            case LEFT: {
                setState(STATE.LEFT);
                leftAt = date;
                break;
            }
        }
    }

    private void setState(STATE state) {
        this.state = state;
    }


}

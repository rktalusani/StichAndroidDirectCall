/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.persistence.contract;

import android.content.ContentValues;
import android.provider.BaseColumns;

import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.core.util.DateUtil;

import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_INVITEDAT;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_JOINEDAT;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_LEFTAT;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_MEMBER_ID;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_STATE;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_USERNAME;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_USER_ID;

/**
 *  Contract for {@link com.nexmo.sdk.conversation.client.Member} object.
 *
 *  @author emma tresanszki.
 *
 * @hide
 */
public final class MemberContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public MemberContract() {}

    public static abstract class MemberEntry implements BaseColumns {
        public static final String TABLE_NAME = "member";
        public static final String COLUMN_MEMBER_ID = "id";
        public static final String COLUMN_USERNAME = "username";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_STATE = "state";
        public static final String COLUMN_INVITEDAT = "invited_at";
        public static final String COLUMN_JOINEDAT = "joined_at";
        public static final String COLUMN_LEFTAT = "left_at";
    }

    public static ContentValues contentValues(final Member member, final String cid) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MEMBER_ID, member.getMemberId());
        values.put(COLUMN_USERNAME, member.getName());
        values.put(COLUMN_USER_ID, member.getUserId());
        values.put(COLUMN_STATE, member.getState().getId());
        try {
            values.put(COLUMN_INVITEDAT, DateUtil.formatIso8601DateString(member.getInvitedAt()));
            values.put(COLUMN_JOINEDAT, DateUtil.formatIso8601DateString(member.getJoinedAt()));
            values.put(COLUMN_LEFTAT, DateUtil.formatIso8601DateString(member.getLeftAt()));
        }catch (Exception e){

        }
        values.put(ConversationContract.ConversationEntry.COLUMN_CID, cid);

        return values;
    }

}

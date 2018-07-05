/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.persistence.contract;

import android.content.ContentValues;
import android.provider.BaseColumns;

import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.core.util.DateUtil;

/**
 * Contract for {@link com.nexmo.sdk.conversation.client.Conversation} object.
 *
 * @author emma tresanszki.
 *
 * @hide
 */
public final class ConversationContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public ConversationContract() {}

    /* Inner class that defines the table contents */
    public static abstract class ConversationEntry implements BaseColumns {
        public static final String TABLE_NAME = "conversation";
        public static final String COLUMN_CID = "cid";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_CREATED = "created";
        public static final String COLUMN_LAST_EVENT_ID = "lastEventId";
        public static final String COLUMN_MEMBER_ID = "selfMemberId";
    }

    public static ContentValues contentValues(final Conversation conversation) {
        ContentValues values = new ContentValues();
        values.put(ConversationEntry.COLUMN_CID, conversation.getConversationId());
        values.put(ConversationEntry.COLUMN_NAME, conversation.getDisplayName());
        values.put(ConversationEntry.COLUMN_CREATED, DateUtil.formatIso8601DateString(conversation.getCreationDate()));
        values.put(ConversationEntry.COLUMN_LAST_EVENT_ID, conversation.getLastEventId());
        values.put(ConversationEntry.COLUMN_MEMBER_ID, conversation.getSelf().getMemberId());

        return values;
    }
}
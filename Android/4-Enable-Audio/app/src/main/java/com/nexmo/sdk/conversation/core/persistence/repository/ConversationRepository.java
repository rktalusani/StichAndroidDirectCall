/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.persistence.repository;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.nexmo.sdk.conversation.client.CacheDB;
import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.User;
import com.nexmo.sdk.conversation.core.persistence.contract.ConversationContract;
import com.nexmo.sdk.conversation.core.persistence.contract.MemberContract;
import com.nexmo.sdk.conversation.core.persistence.dao.ConversationDAO;
import com.nexmo.sdk.conversation.core.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Conversation manager responsible for updating and maintaining conversations.
 * @hide
 */
public class ConversationRepository implements ConversationDAO {
    private static final String TAG = ConversationRepository.class.getSimpleName();
    private CacheDB cacheDB;

    // Prevent direct instantiation
    public ConversationRepository(CacheDB cacheDB){
        this.cacheDB = cacheDB;
    }

    @Override
    public Conversation read(final String cid) {
        SQLiteDatabase db = this.cacheDB.openDatabase();
        Conversation conversation = null;

        String[] projection = {
                ConversationContract.ConversationEntry.COLUMN_NAME,
                ConversationContract.ConversationEntry.TABLE_NAME + "." + ConversationContract.ConversationEntry.COLUMN_CID,
                ConversationContract.ConversationEntry.COLUMN_LAST_EVENT_ID,
                ConversationContract.ConversationEntry.COLUMN_MEMBER_ID,
                ConversationContract.ConversationEntry.COLUMN_CREATED,
                MemberContract.MemberEntry.COLUMN_MEMBER_ID,
                MemberContract.MemberEntry.TABLE_NAME + "." + ConversationContract.ConversationEntry.COLUMN_CID,
                MemberContract.MemberEntry.COLUMN_USER_ID,
                MemberContract.MemberEntry.COLUMN_USERNAME,
                MemberContract.MemberEntry.COLUMN_STATE,
                MemberContract.MemberEntry.COLUMN_INVITEDAT,
                MemberContract.MemberEntry.COLUMN_JOINEDAT,
                MemberContract.MemberEntry.COLUMN_LEFTAT
        };

        String[] selectionArgs = { String.valueOf(cid) };

        Cursor cursor = db.query(
                ConversationContract.ConversationEntry.TABLE_NAME + "," + MemberContract.MemberEntry.TABLE_NAME,
                projection,
                ConversationContract.ConversationEntry.TABLE_NAME + "." + ConversationContract.ConversationEntry.COLUMN_CID + " = ?" + " AND " +
                        ConversationContract.ConversationEntry.TABLE_NAME + "." + ConversationContract.ConversationEntry.COLUMN_CID + " = " +
                        MemberContract.MemberEntry.TABLE_NAME + "." + ConversationContract.ConversationEntry.COLUMN_CID,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst())
                conversation = Conversation.fromCursor(cursor);

            cursor.close();
        }
        return conversation;
    }

    // re-sync, insertAll all conversations and self member.
    @Override
    public void insertAll(final List<Conversation> conversationList) {
        Log.d(TAG, "insertAll");
        SQLiteDatabase db = this.cacheDB.openDatabase();

        db.beginTransaction();

        try {
            for (Conversation conversation : conversationList) {
                db.insert(
                        ConversationContract.ConversationEntry.TABLE_NAME,
                        null,
                        ConversationContract.contentValues(conversation));

                db.insert(
                        MemberContract.MemberEntry.TABLE_NAME,
                        null,
                        MemberContract.contentValues(conversation.getSelf(), conversation.getConversationId()));
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    // insertAll basic conversation details
    @Override
    public void insert(final Conversation conversation, String cid) {
        Log.d(TAG, "insertAll");
        SQLiteDatabase db = this.cacheDB.openDatabase();

        // Insert the new row, returning the primary key value of the new row
        long pk = db.insertWithOnConflict(
                ConversationContract.ConversationEntry.TABLE_NAME,
                null,
                ConversationContract.contentValues(conversation),
                SQLiteDatabase.CONFLICT_REPLACE);

        db.insert(
                MemberContract.MemberEntry.TABLE_NAME,
                null,
                MemberContract.contentValues(conversation.getSelf(), cid));
    }

    @Override
    public boolean delete(final String cid){
        return delete(Collections.singletonList(cid));
    }

    @Override
    public boolean delete(final Collection<String> cIds ){
        Log.d(TAG, "delete .cids " + cIds.toString());
        SQLiteDatabase db = this.cacheDB.openDatabase();

        String placeholders = TextUtils.join(",", Collections.nCopies(cIds.size(), "?"));

        return (db.delete(
                ConversationContract.ConversationEntry.TABLE_NAME,
                ConversationContract.ConversationEntry.COLUMN_CID + " IN (" + placeholders + ") ",
                cIds.toArray(new String[0])) != 0);
    }

    @Override
    public void update(final Conversation conversation, String cid) {
        Log.d(TAG, "update. cid " + conversation.getConversationId());
        SQLiteDatabase db = this.cacheDB.openDatabase();

        db.update(
                ConversationContract.ConversationEntry.TABLE_NAME,
                ConversationContract.contentValues(conversation),
                ConversationContract.ConversationEntry.COLUMN_CID + " = ? ",
                new String[] {cid});
    }

    //including members info.
    @Override
    public List<Conversation> read(final User self) {
        SQLiteDatabase db = this.cacheDB.openDatabase();
        List<Conversation> cachedConversations = new ArrayList<>();

        String[] projection = {
                ConversationContract.ConversationEntry.COLUMN_NAME,
                ConversationContract.ConversationEntry.TABLE_NAME + "." + ConversationContract.ConversationEntry.COLUMN_CID,
                ConversationContract.ConversationEntry.COLUMN_LAST_EVENT_ID,
                ConversationContract.ConversationEntry.COLUMN_MEMBER_ID,
                ConversationContract.ConversationEntry.COLUMN_CREATED,
                MemberContract.MemberEntry.COLUMN_MEMBER_ID,
                MemberContract.MemberEntry.TABLE_NAME + "." + ConversationContract.ConversationEntry.COLUMN_CID,
                MemberContract.MemberEntry.COLUMN_USER_ID,
                MemberContract.MemberEntry.COLUMN_USERNAME,
                MemberContract.MemberEntry.COLUMN_STATE,
                MemberContract.MemberEntry.COLUMN_INVITEDAT,
                MemberContract.MemberEntry.COLUMN_JOINEDAT,
                MemberContract.MemberEntry.COLUMN_LEFTAT
        };

        String[] selectionArgs = { String.valueOf(self.getUserId()) };

        Cursor cursor = db.query(
                ConversationContract.ConversationEntry.TABLE_NAME + "," + MemberContract.MemberEntry.TABLE_NAME,
                projection,
                MemberContract.MemberEntry.COLUMN_USER_ID + " = ?" + " AND " +
                        ConversationContract.ConversationEntry.TABLE_NAME + "." + ConversationContract.ConversationEntry.COLUMN_CID + " = " +
                        MemberContract.MemberEntry.TABLE_NAME + "." + ConversationContract.ConversationEntry.COLUMN_CID,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Conversation conversation = Conversation.fromCursor(cursor);
                cachedConversations.add(conversation);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return cachedConversations;
    }

    // Utility: get list as map for faster search.
    @Override
    public Map<String, Conversation> getConversationsListAsMap(final User self) {
        Map<String, Conversation> cachedConversations = new HashMap<>();

        List<Conversation> conversations = read(self);
        for (Conversation conversation : conversations)
            cachedConversations.put(conversation.getConversationId(), conversation);

        return cachedConversations;
    }
}


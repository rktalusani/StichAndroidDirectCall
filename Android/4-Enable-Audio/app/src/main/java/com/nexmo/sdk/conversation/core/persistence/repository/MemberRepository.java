/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */

package com.nexmo.sdk.conversation.core.persistence.repository;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.nexmo.sdk.conversation.core.persistence.dao.MemberDAO;
import com.nexmo.sdk.conversation.core.util.Log;

import com.nexmo.sdk.conversation.client.CacheDB;
import com.nexmo.sdk.conversation.client.Member;

import com.nexmo.sdk.conversation.core.persistence.contract.ConversationContract.ConversationEntry;
import com.nexmo.sdk.conversation.core.persistence.contract.MemberContract;
import com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Member manager responsible for managing and updating members.
 *
 * @author emma tresanszki.
 *
 * @hide
 */
public class MemberRepository implements MemberDAO {
    private static final String TAG = MemberRepository.class.getSimpleName();

    private CacheDB cacheDB;

    public MemberRepository(CacheDB cacheDB){
        this.cacheDB = cacheDB;
    }

    @Override
    public void insertAll(final String cid, final List<Member> members) {
        Log.d(TAG, "bulkInsertMembersToConversation. cid:  " + cid + " members. " + members.toString());
        delete(cid);

        SQLiteDatabase db = this.cacheDB.openDatabase();
        db.beginTransaction();

        try {
            for (Member member : members) {
                db.insert(
                        MemberEntry.TABLE_NAME,
                        null,
                        MemberContract.contentValues(member, cid));
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    @Override
    public boolean delete(final String cid) {
        Log.d(TAG, "removeMembersFromConversation. cid " + cid);
        SQLiteDatabase db = this.cacheDB.openDatabase();

        return (db.delete(
                MemberEntry.TABLE_NAME,
                ConversationEntry.COLUMN_CID + " = ?",
                new String[] {cid}) != 0);
    }

    @Override
    public boolean delete(Collection<String> ids) {
        return false;
    }

    @Override
    public void update(final Member member, final String cid) {
        Log.d(TAG, "updateMemberOfConversation. cid " + cid);
        SQLiteDatabase db = this.cacheDB.openDatabase();

        db.update(
                MemberEntry.TABLE_NAME,
                MemberContract.contentValues(member, cid),
                ConversationEntry.COLUMN_CID + " = ? AND " + MemberEntry.COLUMN_MEMBER_ID + " = ?",
                new String[] {cid, member.getMemberId()});
    }

    @Override
    public List<Member> read(final String cid) {
        Log.d(TAG, "read for cid " + cid);
        List<Member> members = new ArrayList<>();
        SQLiteDatabase db = this.cacheDB.openDatabase();

        if (DatabaseUtils.queryNumEntries(db, MemberEntry.TABLE_NAME) == 0)
            return null;

        String[] projection = {
                ConversationEntry.COLUMN_CID,
                MemberEntry.COLUMN_MEMBER_ID,
                MemberEntry.COLUMN_USERNAME,
                MemberEntry.COLUMN_USER_ID,
                MemberEntry.COLUMN_STATE,
                MemberEntry.COLUMN_INVITEDAT,
                MemberEntry.COLUMN_JOINEDAT,
                MemberEntry.COLUMN_LEFTAT
        };

        String[] selectionArgs = { String.valueOf(cid) };

        Cursor cursor = db.query(
                MemberEntry.TABLE_NAME,
                projection,
                ConversationEntry.COLUMN_CID + " = ?",
                selectionArgs,
                null,
                null,
                null
        );


        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Member member = Member.fromCursor(cursor);
                members.add(member);
                cursor.moveToNext();
            }
            cursor.close();
        }
        Log.d(TAG, "getMembers: " + members.toString());
        return members;
    }

    // insertAll member when member events occur.
    @Override
    public void insert(final Member member, final String cid) {
        Log.d(TAG, "insert.cid " + cid);
        SQLiteDatabase db = this.cacheDB.openDatabase();

        // Insert the new row, returning the primary key value of the new row
        long pk = db.insertWithOnConflict(
                MemberEntry.TABLE_NAME,
                null,
                MemberContract.contentValues(member, cid),
                SQLiteDatabase.CONFLICT_REPLACE);
    }
}
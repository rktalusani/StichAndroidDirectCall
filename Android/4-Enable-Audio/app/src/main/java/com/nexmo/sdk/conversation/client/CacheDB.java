/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.client;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import com.nexmo.sdk.conversation.core.util.Log;

import com.nexmo.sdk.conversation.core.persistence.repository.ConversationRepository;
import com.nexmo.sdk.conversation.core.persistence.repository.MemberRepository;
import com.nexmo.sdk.conversation.core.persistence.repository.EventRepository;
import com.nexmo.sdk.conversation.core.persistence.contract.ConversationContract.ConversationEntry;
import com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry;
import com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Database helper for updating and accessing the cached conversations.
 *
 * @hide
 *
 * @author emma tresanszki.
 */
public class CacheDB extends SQLiteOpenHelper {
    public static final String TAG = CacheDB.class.getSimpleName();

    private static CacheDB instance;
    private static SQLiteDatabase sDatabase;
    private AtomicInteger mOpenCounter = new AtomicInteger();

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "ConversationCache.db";
    private static final String TEXT_TYPE = " TEXT";
    private static final String DATE_TYPE = " TIMESTAMP";
    private static final String COMMA_SEP = ",";
    private static ConversationRepository conversationRepository;
    private static MemberRepository memberRepository;
    private static EventRepository eventRepository;

    private static final String SQL_CREATE_CONVERSATION_ENTRIES =
            "CREATE TABLE " + ConversationEntry.TABLE_NAME + " (" +
                    ConversationEntry.COLUMN_CID + TEXT_TYPE + " PRIMARY KEY" + COMMA_SEP + //will create int PK based on hashcode
                    ConversationEntry.COLUMN_NAME + TEXT_TYPE + COMMA_SEP +
                    ConversationEntry.COLUMN_CREATED + DATE_TYPE + COMMA_SEP +
                    ConversationEntry.COLUMN_LAST_EVENT_ID + TEXT_TYPE + COMMA_SEP +
                    ConversationEntry.COLUMN_MEMBER_ID + TEXT_TYPE +
                    " )";

    private static final String SQL_CREATE_MEMBER_ENTRIES =
            "CREATE TABLE " + MemberEntry.TABLE_NAME + " (" +
                    MemberEntry.COLUMN_MEMBER_ID + TEXT_TYPE + COMMA_SEP +
                    MemberEntry.COLUMN_USERNAME + TEXT_TYPE + COMMA_SEP +
                    MemberEntry.COLUMN_USER_ID + TEXT_TYPE + COMMA_SEP +
                    MemberEntry.COLUMN_STATE + TEXT_TYPE + COMMA_SEP +
                    MemberEntry.COLUMN_INVITEDAT + DATE_TYPE + COMMA_SEP +
                    MemberEntry.COLUMN_JOINEDAT + DATE_TYPE + COMMA_SEP +
                    MemberEntry.COLUMN_LEFTAT + DATE_TYPE + COMMA_SEP +
                    ConversationEntry.COLUMN_CID + TEXT_TYPE + COMMA_SEP +
                    " PRIMARY KEY (" + MemberEntry.COLUMN_MEMBER_ID + COMMA_SEP + ConversationEntry.COLUMN_CID + ")" + COMMA_SEP +
                    " FOREIGN KEY(" + ConversationEntry.COLUMN_CID +
                    ") REFERENCES " + ConversationEntry.TABLE_NAME + "(" + ConversationEntry.COLUMN_CID + ")" +
                    " ON DELETE CASCADE" +
                    " );" +
                    "CREATE INDEX idx_member_cid ON " + MemberEntry.TABLE_NAME + "(" + ConversationEntry.COLUMN_CID + ")";

    private static final String SQL_CREATE_MESSAGE_ENTRIES =
            "CREATE TABLE " + EventEntry.TABLE_NAME + " (" +
                    EventEntry.COLUMN_EVENT_ID + TEXT_TYPE + COMMA_SEP +
                    EventEntry.COLUMN_CID + TEXT_TYPE + COMMA_SEP +
                    EventEntry.COLUMN_MESSAGE_TYPE + TEXT_TYPE + COMMA_SEP +
                    EventEntry.COLUMN_TEXT + TEXT_TYPE + COMMA_SEP +
                    EventEntry.COLUMN_MEMBER_ID + TEXT_TYPE + COMMA_SEP +
                    EventEntry.COLUMN_TIMESTAMP + DATE_TYPE + COMMA_SEP +
                    EventEntry.COLUMN_DELETED_TIMESTAMP + DATE_TYPE + COMMA_SEP +
                    EventEntry.COLUMN_IMAGE_REPRESENTATIONS + TEXT_TYPE + COMMA_SEP +
                    EventEntry.COLUMN_DELIVERED_RECEIPTS + TEXT_TYPE + COMMA_SEP +
                    EventEntry.COLUMN_SEEN_RECEIPTS + TEXT_TYPE + COMMA_SEP +
                    EventEntry.COLUMN_MEMBER_MEDIA_ENABLED + " INT , " +
                    " PRIMARY KEY (" + EventEntry.COLUMN_EVENT_ID + COMMA_SEP + EventEntry.COLUMN_CID + ")" + COMMA_SEP +
                    " FOREIGN KEY(" + EventEntry.COLUMN_MEMBER_ID + COMMA_SEP + EventEntry.COLUMN_CID +
                    ") REFERENCES " + MemberEntry.TABLE_NAME + "(" + MemberEntry.COLUMN_MEMBER_ID + COMMA_SEP + ConversationEntry.COLUMN_CID + ")" +
                    " ON DELETE CASCADE" +
                    " );" +
                    "CREATE INDEX idx_message_cid ON " + EventEntry.TABLE_NAME + "(" + EventEntry.COLUMN_CID + ")";

    private static final String SQL_DELETE_CONVERSATION_ENTRIES =
            "DROP TABLE IF EXISTS " + ConversationEntry.TABLE_NAME;
    private static final String SQL_DELETE_MEMBER_ENTRIES =
            "DROP TABLE IF EXISTS " + MemberEntry.TABLE_NAME;
    private static final String SQL_DELETE_MESSAGE_ENTRIES =
            "DROP TABLE IF EXISTS " + EventEntry.TABLE_NAME;

    static synchronized void initializeCacheDBInstance(Context context){
        if (instance == null) {
            instance = new CacheDB(context);
            initializeRepositories();
        }
    }

    static synchronized CacheDB getCacheDBInstance(){
        return instance;
    }

    public synchronized SQLiteDatabase openDatabase(){
        if(mOpenCounter.incrementAndGet() == 1 || sDatabase == null) {
            // Opening new database
            sDatabase = instance.getWritableDatabase();
        }
        return sDatabase;
    }

    synchronized void closeDatabase(){
        if(mOpenCounter.decrementAndGet() >= 0) {
            mOpenCounter.set(0);
            // Closing database
            sDatabase.close();
            sDatabase = null;
        }
    }

    private CacheDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static void initializeRepositories() {
        conversationRepository = new ConversationRepository(getCacheDBInstance());
        memberRepository = new MemberRepository(getCacheDBInstance());
        eventRepository = new EventRepository(getCacheDBInstance());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "ConversationCache.db onCreate");
        db.execSQL(SQL_CREATE_CONVERSATION_ENTRIES);
        db.execSQL(SQL_CREATE_MEMBER_ENTRIES);
        db.execSQL(SQL_CREATE_MESSAGE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "ConversationCache.db onUpgrade Old version: " + oldVersion + " to new version: " + newVersion);
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        switch(oldVersion) {
            case 1:
                //fall through
            default:
                db.execSQL(SQL_DELETE_CONVERSATION_ENTRIES);
                db.execSQL(SQL_DELETE_MEMBER_ENTRIES);
                db.execSQL(SQL_DELETE_MESSAGE_ENTRIES);
                onCreate(db);
                break;
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            setForeignKeyConstraintsEnabled(db);
        }
    }

    void setForeignKeyConstraintsEnabled(SQLiteDatabase db) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            db.execSQL("PRAGMA foreign_keys=ON;");
        else
            setForeignKeyConstraintsEnabledPostJellyBean(db);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setForeignKeyConstraintsEnabledPostJellyBean(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
        Log.d(TAG, "ConversationCache.db onDowngrade from new version: " + newVersion + " to Old version: " + oldVersion);
    }

    boolean tableContainsRows(String tableName) {
        long count = DatabaseUtils.queryNumEntries(getCacheDBInstance().openDatabase(), tableName);
        Log.d(TAG, "tableContainsRows. tableName: " + tableName + " .count: " + count);

        return (count > 0);
    }

    MemberRepository getMemberRepository() {
        return memberRepository;
    }

    EventRepository getEventRepository() {
        return eventRepository;
    }

    ConversationRepository getConversationRepository() {
        return conversationRepository;
    }

    void updateMembersForConversations(Map<String, List<Member>> conversationMembersMap) {
        for (Map.Entry<String, List<Member>> entry : conversationMembersMap.entrySet())
            memberRepository.insertAll(entry.getKey(), entry.getValue());
    }

    /**
     * Clear cache manually or on explicit logout
     *
     * @VisibleForTesting
     */
    void clearDb() {
        SQLiteDatabase db = getCacheDBInstance().openDatabase();
        db.execSQL("delete from "+ ConversationEntry.TABLE_NAME);
        db.execSQL("delete from " + MemberEntry.TABLE_NAME);
        db.execSQL("delete from " + EventEntry.TABLE_NAME);
    }

    /**
     * @VisibleForTesting
     */
    private void dropDB() {
        Log.d(TAG, "dropDB");
        SQLiteDatabase db = getCacheDBInstance().openDatabase();
        db.execSQL(SQL_DELETE_CONVERSATION_ENTRIES);
        db.execSQL(SQL_DELETE_MEMBER_ENTRIES);
        db.execSQL(SQL_DELETE_MESSAGE_ENTRIES);
        getCacheDBInstance().closeDatabase();
    }

}

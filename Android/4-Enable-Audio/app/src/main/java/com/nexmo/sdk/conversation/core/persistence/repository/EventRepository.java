package com.nexmo.sdk.conversation.core.persistence.repository;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.nexmo.sdk.conversation.client.Event;
import com.nexmo.sdk.conversation.core.persistence.dao.EventDAO;
import com.nexmo.sdk.conversation.core.util.Log;

import com.nexmo.sdk.conversation.client.CacheDB;
import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.core.persistence.contract.MemberContract;
import com.nexmo.sdk.conversation.core.persistence.contract.EventContract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Event manager responsible for managing and updating message events. 
 *
 * @author chatitze moumin. 
 *
 * @hide
 */

public class EventRepository implements EventDAO {
    private static final String TAG = EventRepository.class.getSimpleName();

    private CacheDB cacheDB;

    public EventRepository(CacheDB cacheDB){
        this.cacheDB = cacheDB;
    }

    // insertAll a single event details
    @Override
    public void insert(final Event event, final String cid) {
        Log.d(TAG, "insertAll ID: " + event.getId());
        SQLiteDatabase db = this.cacheDB.openDatabase();

        // Insert the new row, returning the primary key value of the new row
        long pk = db.insertWithOnConflict(
                EventContract.EventEntry.TABLE_NAME,
                null,
                EventContract.contentValues(event, cid),
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    // update a single event
    @Override
    public void update(final Event event, final String cid) {
        Log.d(TAG, "update of eventId: " + event.getId());
        SQLiteDatabase db = this.cacheDB.openDatabase();

        db.update(
                EventContract.EventEntry.TABLE_NAME,
                EventContract.contentValues(event, cid),
                EventContract.EventEntry.COLUMN_CID + " = ? AND " + EventContract.EventEntry.COLUMN_EVENT_ID + " = ?",
                new String[] {cid, event.getId()});
    }

    @Override
    public boolean delete(String id) {
        return false;
    }

    // remove a list of messages
    @Override
    public boolean delete(final Collection<String> ids ){
        SQLiteDatabase db = this.cacheDB.openDatabase();

        String placeholders = TextUtils.join(",", Collections.nCopies(ids.size(), "?"));

       return (db.delete(
                EventContract.EventEntry.TABLE_NAME,
                EventContract.EventEntry.COLUMN_EVENT_ID + " IN (" + placeholders + ") ",
                ids.toArray(new String[0])) != 0);
    }

    @Override
    public void insertAll(final String cid, final List<Event> eventList) {
        SQLiteDatabase db = this.cacheDB.openDatabase();
        db.beginTransaction();
        int rowsInserted = 0;
        try {
            for (Event event : eventList) {
                long _id = db.insert(
                        EventContract.EventEntry.TABLE_NAME,
                        null,
                        EventContract.contentValues(event, cid));
                if(_id != -1)
                    rowsInserted++;
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
        Log.d(TAG, "insertAll: # of messages persisted: " + rowsInserted);
    }

    @Override
    public List<Event> read(final String cid, Conversation conversation) {
        List<Event> eventList = new ArrayList<>();
        SQLiteDatabase db = this.cacheDB.openDatabase();

        long count = DatabaseUtils.queryNumEntries(db, EventContract.EventEntry.TABLE_NAME, EventContract.EventEntry.COLUMN_CID + " = ?",
                new String[]{cid});
        if (count == 0)
            return eventList;

        String[] projection = {
                MemberContract.MemberEntry.COLUMN_MEMBER_ID,
                MemberContract.MemberEntry.COLUMN_USERNAME,
                MemberContract.MemberEntry.COLUMN_USER_ID,
                MemberContract.MemberEntry.COLUMN_STATE,
                MemberContract.MemberEntry.COLUMN_INVITEDAT,
                MemberContract.MemberEntry.COLUMN_JOINEDAT,
                MemberContract.MemberEntry.COLUMN_LEFTAT,
                EventContract.EventEntry.COLUMN_MESSAGE_TYPE,
                EventContract.EventEntry.COLUMN_TIMESTAMP,
                EventContract.EventEntry.COLUMN_DELETED_TIMESTAMP,
                EventContract.EventEntry.COLUMN_EVENT_ID,
                EventContract.EventEntry.COLUMN_DELIVERED_RECEIPTS,
                EventContract.EventEntry.COLUMN_SEEN_RECEIPTS,
                EventContract.EventEntry.COLUMN_TEXT,
                EventContract.EventEntry.COLUMN_IMAGE_REPRESENTATIONS,
                EventContract.EventEntry.COLUMN_MEMBER_MEDIA_ENABLED
        };

        String[] selectionArgs = { String.valueOf(cid) };

        Cursor cursor = db.query(
                EventContract.EventEntry.TABLE_NAME + " , "
                        + MemberContract.MemberEntry.TABLE_NAME,
                projection,
                EventContract.EventEntry.COLUMN_CID + " = ?" + " AND "
                        + MemberContract.MemberEntry.COLUMN_MEMBER_ID + "="
                        + EventContract.EventEntry.COLUMN_MEMBER_ID,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Event event = Event.fromCursor(cursor, conversation);

                eventList.add(event);
                cursor.moveToNext();
            }
            cursor.close();
        }
        Log.d(TAG, "queryConversationMessages: " + eventList.size());
        return eventList;
    }

    @Override
    public boolean isAny(String cid){
        SQLiteDatabase db = this.cacheDB.openDatabase();

        long count = DatabaseUtils.queryNumEntries(db, EventContract.EventEntry.TABLE_NAME, EventContract.EventEntry.COLUMN_CID + " = ?",
                new String[]{cid});
        Log.d(TAG, "Number of persisted Events for Conversation: " + count);
        return (count > 0);
    }

    @Override
    public String getLastEventId(String cid) {
        SQLiteDatabase db = this.cacheDB.openDatabase();

        String[] projection = {
                " MAX(" + EventContract.EventEntry.COLUMN_EVENT_ID + ") as LastEventID"
        };

        String lastEventId = null;

        String[] selectionArgs = { String.valueOf(cid) };

        Cursor cursor = db.query(
                EventContract.EventEntry.TABLE_NAME,
                projection,
                EventContract.EventEntry.COLUMN_CID + " = ?",
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null) {
            cursor.moveToFirst();
            lastEventId = cursor.getString(cursor.getColumnIndex("LastEventID"));
            Log.d(TAG, "Last Event ID: " + lastEventId);
            cursor.close();
        }
        return lastEventId;
    }

    // get message id's
    @Override
    public List<String> getEventIds(Conversation conversation){

        SQLiteDatabase db = this.cacheDB.openDatabase();
        List<String> cachedMessageIds = new ArrayList<>();

        String[] projection = {
                EventContract.EventEntry.COLUMN_EVENT_ID
        };
        String[] selectionArgs = { String.valueOf(conversation.getConversationId()) };

        Cursor cursor = db.query(
                EventContract.EventEntry.TABLE_NAME,
                projection,
                EventContract.EventEntry.COLUMN_CID + " = ?",
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                cachedMessageIds.add(cursor.getString(cursor.getColumnIndex(EventContract.EventEntry.COLUMN_EVENT_ID)));
                cursor.moveToNext();
            }
            Log.d(TAG, "Conversation: " + conversation.getDisplayName() + " - Number of messages in DB: " + cursor.getCount());
            cursor.close();
        }
        Collections.sort(cachedMessageIds, new Comparator<String>() {
            public int compare(String a, String b) {
                return Integer.parseInt(a) - Integer.parseInt(b);
            }
        });

        return cachedMessageIds;
    }
}

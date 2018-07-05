package com.nexmo.sdk.conversation.core.persistence.contract;

import android.content.ContentValues;
import android.provider.BaseColumns;

import com.nexmo.sdk.conversation.client.Event;
import com.nexmo.sdk.conversation.client.Image;
import com.nexmo.sdk.conversation.client.MemberMedia;
import com.nexmo.sdk.conversation.client.ReceiptRecordUtil;
import com.nexmo.sdk.conversation.client.Text;
import com.nexmo.sdk.conversation.core.util.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;

import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_CID;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_DELETED_TIMESTAMP;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_DELIVERED_RECEIPTS;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_EVENT_ID;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_IMAGE_REPRESENTATIONS;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_MEMBER_ID;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_MEMBER_MEDIA_ENABLED;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_SEEN_RECEIPTS;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_TEXT;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_TIMESTAMP;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_MESSAGE_TYPE;

/**
 * Contract for {@link com.nexmo.sdk.conversation.client.Event} object.
 *
 * @author chatitze moumin.
 *
 * @hide
 */

// This class will never be extended, so it is marked as final.
public final class EventContract {

    // Suppress default constructor for non-instantiability
    private EventContract() {
        throw new AssertionError();
    }

    /* Inner class that defines the table contents */
    public static abstract class EventEntry implements BaseColumns {
        public static final String TABLE_NAME                   = "message";
        public static final String COLUMN_EVENT_ID              = "event_id";
        public static final String COLUMN_CID                   = "conversation_id";
        public static final String COLUMN_MEMBER_ID             = "member_id";
        public static final String COLUMN_TIMESTAMP             = "timestamp";
        public static final String COLUMN_DELETED_TIMESTAMP     = "deleted_timestamp";
        public static final String COLUMN_MESSAGE_TYPE          = "message_type";
        public static final String COLUMN_DELIVERED_RECEIPTS    = "delivered_receipts";
        public static final String COLUMN_SEEN_RECEIPTS         = "seen_receipts";
        // fields dependant on TYPE.
        public static final String COLUMN_TEXT                  = "text";
        // 3 Image representation: id, url, size, local_path
        public static final String COLUMN_IMAGE_REPRESENTATIONS = "image_representations";
        public static final String COLUMN_MEMBER_MEDIA_ENABLED = "audio_enabled";
    }

    public static ContentValues contentValues(final Event event, final String cid) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_CID, cid);
        values.put(COLUMN_EVENT_ID, event.getId());
        values.put(COLUMN_MEMBER_ID, event.getMember().getMemberId());
        values.put(COLUMN_TIMESTAMP, DateUtil.formatIso8601DateString(event.getTimestamp()));
        values.put(COLUMN_DELETED_TIMESTAMP, (event.getDeletedTimestamp() != null) ?
                DateUtil.formatIso8601DateString(event.getDeletedTimestamp()) : null);

        if (!event.getDeliveredReceipts().isEmpty())
            try {
                values.put(COLUMN_DELIVERED_RECEIPTS, ReceiptRecordUtil.deliveredReceiptsToJson(event).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

        if (!event.getSeenReceipts().isEmpty())
            try {
                values.put(COLUMN_SEEN_RECEIPTS, ReceiptRecordUtil.seenReceiptsToJson(event).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

        values.put(COLUMN_MESSAGE_TYPE, event.getType().toString());
        switch (event.getType()) {
            case TEXT: {
                values.put(COLUMN_TEXT, ((Text) event).getText());
                values.putNull(COLUMN_IMAGE_REPRESENTATIONS);
                break;
            }
            case IMAGE: {
                values.putNull(COLUMN_TEXT);
                Image image = (Image) event;
                if (image.getOriginal() != null && image.getThumbnail() !=  null && image.getMedium() != null)
                    try {
                         values.put(COLUMN_IMAGE_REPRESENTATIONS, imageRepresentationsToJson(image).toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                else
                    values.putNull(COLUMN_IMAGE_REPRESENTATIONS);

                break;
            }
            case MEMBER_MEDIA: {
                values.put(COLUMN_MEMBER_MEDIA_ENABLED, ((MemberMedia) event).isAudioEnabled() ? 1 : 0);
                break;
            }
        }

        return values;
    }

    static JSONObject imageRepresentationsToJson(final Image image) throws JSONException {
        JSONObject parentJson = new JSONObject();
        JSONObject representationsJson = new JSONObject();
        representationsJson.put("original", image.getOriginal().toJSON());
        representationsJson.put("medium", image.getMedium().toJSON());
        representationsJson.put("thumbnail", image.getThumbnail().toJSON());

        parentJson.put("representations", representationsJson);
        return parentJson;
    }

}

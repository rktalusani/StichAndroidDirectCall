package com.nexmo.sdk.conversation.client;

import android.support.annotation.Nullable;
import com.nexmo.sdk.conversation.core.util.Log;

import com.nexmo.sdk.conversation.core.util.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author emma tresanszki.
 * @date 17/10/16.
 *
 * @hide
 */
public class ReceiptRecordUtil {
    private static final String TAG = ReceiptRecordUtil.class.getSimpleName();

    /**
     * Convert seen receipts in json form, for writing them to cache.
     */
    public static JSONObject seenReceiptsToJson(final Event event) throws JSONException {
        JSONObject stateObject = new JSONObject();
        JSONObject seenObject = new JSONObject();
        for (SeenReceipt receipt : event.getSeenReceipts())
            seenObject.put(receipt.getMember().getMemberId(), DateUtil.formatIso8601DateString(receipt.getTimestamp()));

        stateObject.put("seen_by", seenObject);

        return stateObject;
    }

    public static JSONObject deliveredReceiptsToJson(final Event event) throws JSONException {
        JSONObject stateObject = new JSONObject();
        JSONObject deliveredObject = new JSONObject();
        for (DeliveredReceipt receipt : event.getDeliveredReceipts())
            deliveredObject.put(receipt.getMember().getMemberId(), DateUtil.formatIso8601DateString(receipt.getTimestamp()));

        stateObject.put("delivered_to", deliveredObject);

        return stateObject;
    }

    static List<DeliveredReceipt> parseDeliveryReceiptHistory(Conversation conversation, String eventId, JSONObject stateJson) {
        List<DeliveredReceipt> deliveredReceiptList = new ArrayList<>();

        try {
            if (stateJson.has("delivered_to") && stateJson.get("delivered_to") instanceof JSONObject){
                JSONObject deliveredTo = stateJson.getJSONObject("delivered_to");
                Iterator<String> keys= deliveredTo.keys();

                while (keys.hasNext())
                {
                    String keyValue = keys.next();
                    String valueString = deliveredTo.getString(keyValue);
                    Date timestamp = DateUtil.formatIso8601DateString(valueString);

                    Event event = conversation.findEvent(eventId);
                    Member deliveredToMember = conversation.getMember(keyValue);

                    if (event != null && deliveredToMember != null)
                        deliveredReceiptList.add(new DeliveredReceipt(event, deliveredToMember, timestamp));
                }
            }
        } catch (JSONException | ParseException e) {
            e.printStackTrace();
        }

        return deliveredReceiptList;
    }

    static List<SeenReceipt> parseSeenReceiptHistory(Conversation conversation, String eventId, JSONObject stateJson) {
        List<SeenReceipt> seenReceiptList = new ArrayList<>();

        try {
            if (stateJson.has("seen_by") && stateJson.get("seen_by") instanceof JSONObject) {
                JSONObject seenBy = stateJson.getJSONObject("seen_by");
                Iterator<String> keys= seenBy.keys();

                while (keys.hasNext()) {
                    String keyValue = keys.next();
                    String valueString = seenBy.getString(keyValue);
                    Date timestamp = DateUtil.formatIso8601DateString(valueString);

                    Event event = conversation.findEvent(eventId);
                    Member seenByMember = conversation.getMember(keyValue);

                    if (event != null && seenByMember != null)
                        seenReceiptList.add(new SeenReceipt(event, seenByMember, timestamp));
                }
            }
        } catch (JSONException | ParseException e) {
            e.printStackTrace();
        }

        return seenReceiptList;
    }

    static void parseSeenReceipt(JSONObject data, SocketEventHandler socketEventHandler) throws JSONException {
        String cid = data.getString("cid");
        String memberId = data.getString("from");
        Date timestamp = null;
        try {
            timestamp = DateUtil.formatIso8601DateString(data.getString("timestamp"));
        } catch (ParseException e) {
            Log.d(TAG, "parseSeenReceipt: wrong date format");
        }

        socketEventHandler.onEventSeen(cid, memberId, getEventId(data), timestamp);
    }

    static void parseDeliveredReceipt(JSONObject data, SocketEventHandler socketEventHandler) throws JSONException {
        String cid = data.getString("cid");
        String memberId = data.getString("from");
        Date timestamp = null;
        try {
            timestamp = DateUtil.formatIso8601DateString(data.getString("timestamp"));
        } catch (ParseException e) {
            Log.d(TAG, "parseDeliveredReceipt: wrong date format");
        }

        socketEventHandler.onEventDelivered(cid, memberId, getEventId(data), timestamp);
    }

    /**
     * Extracts event_id or message_id value from internal body structure from given rootObject
     * @return event_id or message_id value if any, null otherwise
     * @throws JSONException if body is absent in provided document
     */
    private static @Nullable String getEventId(JSONObject rootObject) throws JSONException {
        JSONObject body = rootObject.getJSONObject("body");

        String eventId = body.optString("event_id", null);
        if (eventId == null)
            eventId = body.optString("message_id", null);
        return eventId;
    }
}

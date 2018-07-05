package com.nexmo.sdk.conversation.push;

import org.json.JSONException;
import org.json.JSONObject;

class EventExtractor {
    private static final String MESSAGE_KEY_CID = "conversation_id";
    private static final String MESSAGE_KEY_SENDER_ID = "from";
    private static final String MESSAGE_KEY_BODY = "body";
    private static final String MESSAGE_KEY_EVENT_TYPE = "event_type";
    private static final String MESSAGE_KEY_EVENT_ID = "id";

    private JSONObject eventJson;
    private String cid;
    private String senderId;
    private JSONObject body;
    private String type;
    private String eventId;

    public EventExtractor(JSONObject eventJson) {
        this.eventJson = eventJson;
    }

    public String getCid() {
        return cid;
    }

    public String getSenderId() {
        return senderId;
    }

    public JSONObject getBody() {
        return body;
    }

    public JSONObject getJsonBody() {
        return eventJson;
    }

    public String getType() {
        return type;
    }

    public String getEventId() {
        return eventId;
    }

    public EventExtractor fromJson() throws JSONException {

        cid = eventJson.getString(MESSAGE_KEY_CID);
        senderId = eventJson.getString(MESSAGE_KEY_SENDER_ID);

        body = eventJson.getJSONObject(MESSAGE_KEY_BODY);

        type = eventJson.getString(MESSAGE_KEY_EVENT_TYPE);
        eventId = eventJson.getString(MESSAGE_KEY_EVENT_ID);

        return this;
    }

}

package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.Event;
import com.nexmo.sdk.conversation.client.MemberMedia;
import com.nexmo.sdk.conversation.client.event.EventType;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.util.DateUtil;
import com.nexmo.sdk.conversation.core.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @hide
 */
public class GetEventsRequest extends GetEventsBaseRequest<List<Event>> {
    public static final String TAG = GetEventsRequest.class.getSimpleName();

    public static final String CONVERSATION_GET_EVENTS = "conversation:events";
    public static final String CONVERSATION_GET_EVENTS_SUCCESS = "conversation:events:success";
    private Conversation conversation;

    public GetEventsRequest(Conversation conversation, String startId, String endId, RequestHandler<Conversation> listener) {
        super(TYPE.GET_EVENTS, conversation.getConversationId(), startId, endId, listener);
        this.conversation = conversation;
    }

    @Override
    public String getRequestName() {
        return CONVERSATION_GET_EVENTS;
    }

    @Override
    public String getSuccessEventName() {
        return CONVERSATION_GET_EVENTS_SUCCESS;
    }

    @Override
    public List<Event> parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        JSONArray messagesArray = jsonObject.getJSONArray("body");
        List<Event> events = new ArrayList<>();

        for(int index = 0; index < messagesArray.length(); index++) {
            JSONObject messageObject = messagesArray.getJSONObject(index);

            Event event = Event.fromJson(conversation, messageObject);
            if (event != null)
                events.add(event);
        }
        Log.d(TAG, "onMessages.conversation: " + events.toString());
        return events;
    }
}

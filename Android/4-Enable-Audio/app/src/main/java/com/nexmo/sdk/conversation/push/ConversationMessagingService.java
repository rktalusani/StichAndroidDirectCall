package com.nexmo.sdk.conversation.push;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nexmo.sdk.conversation.core.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * @author emma tresanszki
 */
public class ConversationMessagingService extends FirebaseMessagingService {
    public static final String TAG = ConversationMessagingService.class.getSimpleName();
    private static final String MESSAGE_PAYLOAD_KEY = "event";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        final Map data = message.getData();
        Log.d(ConversationMessagingService.TAG, "Remote message Data: " + data.toString());
        JSONObject messagePayload = getMessagePayload(data);

        if(messagePayload != null)
            sendBroadcast(new HandleFirebaseMessage().invoke(messagePayload));
    }

    JSONObject getMessagePayload(final Map data) {
        return new Object() {
            public JSONObject invoke() {
                JSONObject eventJson = null;
                if (data.containsKey(MESSAGE_PAYLOAD_KEY)) {
                    String eventString = (String) data.get(MESSAGE_PAYLOAD_KEY);
                    try {
                        eventJson = new JSONObject(eventString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return eventJson;
            }
        }.invoke();
    }

}

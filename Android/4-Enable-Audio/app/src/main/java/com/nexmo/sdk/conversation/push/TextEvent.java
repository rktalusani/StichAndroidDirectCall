package com.nexmo.sdk.conversation.push;

import com.nexmo.sdk.conversation.client.Text;
import com.nexmo.sdk.conversation.core.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class TextEvent {
    String conversationId;
    Text text;

    public String getConversationId() {
        return conversationId;
    }

    public Text getText() {
        return text;
    }

    private TextEvent(String conversationId, Text text) {
        this.conversationId = conversationId;
        this.text = text;
    }

    public static TextEvent createTextEvent(String senderId, String eventId, JSONObject body, String conversationId) throws JSONException{
        Text incomingText = Text.fromPush(senderId, eventId, body);
        Log.d(ConversationMessagingService.TAG, "text " + incomingText.toString());
        return new TextEvent(conversationId, incomingText);
    }

    public static TextEvent createTextEventFromBundle(String conversationId, Text text){
        return new TextEvent(conversationId, text);
    }

}

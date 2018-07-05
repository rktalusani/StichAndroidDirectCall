package com.nexmo.sdk.conversation.push;

import com.nexmo.sdk.conversation.client.Image;
import com.nexmo.sdk.conversation.core.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class ImageEvent {
    String conversationId;
    Image incomingImage;

    public String getConversationId() {
        return conversationId;
    }

    public Image getIncomingImage() {
        return incomingImage;
    }

    private ImageEvent(Image incomingImage, String conversationId) {
        this.incomingImage = incomingImage;
        this.conversationId = conversationId;
    }

    public static ImageEvent createImageEvent(String senderId, String eventId, JSONObject body, String conversationId) throws JSONException {
        Image incomingImage = Image.fromPush(senderId, eventId, body);
        Log.d(ConversationMessagingService.TAG, "new image" + incomingImage.toString());
        return new ImageEvent(incomingImage, conversationId);
    }

    public static ImageEvent createImageEventFromBundle(String conversationId, Image image){
        return new ImageEvent(image, conversationId);
    }
}

package com.nexmo.sdk.conversation.client;

import com.nexmo.sdk.conversation.client.event.RequestHandler;

import java.util.List;

/**
 * SocketEventNotifier is a subscriber for receiving the notifications defined in listeners.
 * It maintains its own collection of subscribers that want to be notified of those same events.
 *
 * @author chatitze moumin.
 *
 * @hide
 */
public class SocketEventNotifier {
    // user callbacks.
    private RequestHandler<List<Conversation>> conversationListListener;


    public void setConversationListListener(RequestHandler<List<Conversation>> conversationListListener){
        this.conversationListListener = conversationListListener;
    }

    //notify Listeners
    void notifyConversationListListener(List<Conversation> conversations){
        if (this.conversationListListener != null) {
            this.conversationListListener.onSuccess(conversations);
            this.conversationListListener = null;
        }
    }

    void removeAllListeners() {
        conversationListListener = null;
    }
}

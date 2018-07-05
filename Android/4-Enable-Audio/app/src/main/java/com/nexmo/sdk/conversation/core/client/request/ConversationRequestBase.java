package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Same as Request but it has additional field cid - for conversationId
 *
 * @hide
 */

public abstract class ConversationRequestBase<T extends RequestHandler, R> extends Request<T, R> {
    public String cid;

    protected ConversationRequestBase(TYPE type, String cid, T listener) {
        super(type, listener);
        this.cid = cid;
    }

    /**
     * Helper method which creates JSON object with
     * pre-filled fields {@link #tid} and {@link #cid}
     */
    protected JSONObject newConversationTaggedResponse() throws JSONException {
        return newTaggedResponse()
                .put("cid", cid);
    }

    public String getConversationId() {
        return cid;
    }
}

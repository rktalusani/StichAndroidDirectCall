package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for push subscribe and unsubscribe commands
 *
 * @hide
 */

public abstract class PushSubscriptionRequest extends ConversationRequestBase<RequestHandler<Void>, Void> {
    static final String PUSH_SUBSCRIBE = "push:subscribe";
    static final String PUSH_SUBSCRIBE_SUCCESS = "push:subscribe:success";

    protected PushSubscriptionRequest(TYPE type, String cid, RequestHandler listener) {
        super(type, cid, listener);
    }


    @Override
    protected JSONObject makeJson() throws JSONException {
        return newConversationTaggedResponse()
                .put("body", jsonObject("cid", this.cid));
    }

    @Override
    public Void parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        return null;
    }
}

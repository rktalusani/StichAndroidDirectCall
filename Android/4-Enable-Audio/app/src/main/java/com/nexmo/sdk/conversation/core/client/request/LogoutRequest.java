package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.User;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @hide
 */
public class LogoutRequest extends Request<RequestHandler<User>, Void> {
    static final String LOGOUT_REQUEST = "session:logout";
    static final String LOGOUT_SUCCESS = "session:logged-out";

    public LogoutRequest() {
        super(TYPE.OTHER);
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newTaggedResponse();
    }

    @Override
    public String getRequestName() {
        return LOGOUT_REQUEST;
    }

    @Override
    public String getSuccessEventName() {
        return LOGOUT_SUCCESS;
    }

    @Override
    public Void parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        return null;
    }
}

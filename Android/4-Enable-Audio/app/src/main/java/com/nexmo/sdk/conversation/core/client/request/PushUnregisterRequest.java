package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

import static com.nexmo.sdk.conversation.core.client.request.Request.TYPE.OTHER;

/**
 *
 * @hide
 */
public class PushUnregisterRequest extends Request<RequestHandler<Void>, Void> {
    static final String PUSH_UNREGISTER = "push:unregister";
    static final String PUSH_UNREGISTER_SUCCESS = "push:unregister:success";
    private final String deviceId;

    public PushUnregisterRequest(String deviceId, RequestHandler pushEnableListener) {
        super(OTHER, pushEnableListener);
        this.deviceId = deviceId;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newTaggedResponse()
                .put("body", jsonObject("device_id", deviceId));
    }

    @Override
    public String getRequestName() {
        return PUSH_UNREGISTER;
    }

    @Override
    public String getSuccessEventName() {
        return PUSH_UNREGISTER_SUCCESS;
    }

    @Override
    public Void parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        return null;
    }
}

package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

import static com.nexmo.sdk.conversation.core.client.request.Request.TYPE.OTHER;

/**
 * @hide
 */

public class PushRegisterRequest extends Request<RequestHandler<Void>, Void> {
    public static final String PUSH_REGISTER = "push:register";
    public static final String PUSH_REGISTER_SUCCESS = "push:register:success";
    private final String devicePushToken;

    public PushRegisterRequest(String devicePushToken, RequestHandler<Void> pushEnableListener) {
        super(OTHER, pushEnableListener);
        this.devicePushToken = devicePushToken;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        JSONObject bodyObj = new JSONObject();
        bodyObj.put("device_id", "687756674556745");
        bodyObj.put("device_token", devicePushToken);
        bodyObj.put("device_type", "android");

        return newTaggedResponse()
            .put("body", bodyObj);
    }

    @Override
    public String getRequestName() {
        return PUSH_REGISTER;
    }

    @Override
    public String getSuccessEventName() {
        return PUSH_REGISTER_SUCCESS;
    }

    @Override
    public Void parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        return null;
    }
}

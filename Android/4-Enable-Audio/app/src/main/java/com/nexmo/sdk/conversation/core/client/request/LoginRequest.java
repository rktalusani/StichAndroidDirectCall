package com.nexmo.sdk.conversation.core.client.request;

//import com.nexmo.sdk.conversation.BuildConfig;
import com.nexmo.sdk.conversation.client.User;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by rux on 20/02/17.
 *
 * @hide
 */
public class LoginRequest extends Request<RequestHandler<User>, User> {
    static final String LOGIN_REQUEST = "session:login";
    static final String LOGIN_SUCCESS = "session:success";
    private final String token;
    private final String deviceId;

    public LoginRequest(String token, String deviceId, RequestHandler<User> listener) {
        super(TYPE.OTHER, listener);
        this.token = token;
        this.deviceId = deviceId;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        JSONObject bodyObj = new JSONObject()
            .put("token", token)
            .put("sdk", "0.22.0")
            .put("device_id", deviceId)
            .put("device_type", "android");

        return newTaggedResponse()
                .put("body", bodyObj);
    }

    @Override
    public String getRequestName() {
        return LOGIN_REQUEST;
    }

    @Override
    public String getSuccessEventName() {
        return LOGIN_SUCCESS;
    }

    @Override
    public User parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        return User.fromJson(body);
    }

}

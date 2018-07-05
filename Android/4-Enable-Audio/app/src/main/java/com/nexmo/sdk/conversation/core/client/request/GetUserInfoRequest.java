package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.User;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by rux on 19/01/17.
 * @hide
 */

public class GetUserInfoRequest extends Request<RequestHandler<User>, User> {
    /**
     * User events
     */
    static final String USER_INFO = "user:get";
    static final String USER_INFO_SUCCESS = "user:get:success";
    private String userId;

    public GetUserInfoRequest(String userId, RequestHandler<User> listener) {
        super(TYPE.GET_USER, listener);
        this.userId = userId;
    }


    public String getUserId() {
        return userId;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newTaggedResponse()
                .put("user_id", this.getUserId());
    }

    @Override
    public String getRequestName() {
        return USER_INFO;
    }

    @Override
    public String getSuccessEventName() {
        return USER_INFO_SUCCESS;
    }

    @Override
    public User parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        return User.fromJson(body);
    }
}

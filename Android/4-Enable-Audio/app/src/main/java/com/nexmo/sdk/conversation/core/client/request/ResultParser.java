package com.nexmo.sdk.conversation.core.client.request;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @hide
 */

public interface ResultParser<R> {
    public R parse(JSONObject jsonObject, JSONObject body) throws JSONException;
}

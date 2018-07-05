package com.nexmo.sdk.conversation.client.event.network;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by rux on 24/02/17.
 * @hide
 */
public interface SubscriptionListener {
    public abstract void onData(String eventName, JSONObject jsonObject) throws JSONException;

}

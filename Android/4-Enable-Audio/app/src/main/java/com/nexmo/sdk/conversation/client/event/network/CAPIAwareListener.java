package com.nexmo.sdk.conversation.client.event.network;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Event handler for low-level CAPI protocol.
 * @hide
 */
public interface CAPIAwareListener {
    public void onRawUnprocessResponseData(JSONObject data, String rid, String cid) throws JSONException;

    public void onError(String errorEventName, JSONObject data, String rid, String cid);
}

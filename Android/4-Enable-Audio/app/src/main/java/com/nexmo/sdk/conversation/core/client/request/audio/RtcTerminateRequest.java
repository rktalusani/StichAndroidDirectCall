package com.nexmo.sdk.conversation.core.client.request.audio;

import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.client.request.ConversationRequestBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Send Answer with rtc_id
 * @author emma tresanszki.
 */
public class RtcTerminateRequest extends ConversationRequestBase<RequestHandler<String>, Void> {
    public static final String RTC_TERMINATE_REQUEST = "rtc:terminate";
    public static final String RTC_TERMINATE_SUCCESS = "rtc:terminate:success";
    public String rtc_id;
    public String memberId;

    public RtcTerminateRequest(String cid,String memberId, String rtc_id, RequestHandler<String> listener) {
        super(TYPE.RTC_TERMINATE, cid, listener);
        this.memberId = memberId;
        this.rtc_id = rtc_id;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newConversationTaggedResponse().put("rtc_id", rtc_id).put("from", this.memberId);
    }

    @Override
    public String getRequestName() {
        return RTC_TERMINATE_REQUEST;
    }

    @Override
    public String getSuccessEventName() {
        return RTC_TERMINATE_SUCCESS;
    }

    @Override
    public Void parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        return null;
    }

}
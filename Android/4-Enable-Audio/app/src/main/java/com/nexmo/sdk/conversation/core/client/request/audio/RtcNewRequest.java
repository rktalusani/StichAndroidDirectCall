package com.nexmo.sdk.conversation.core.client.request.audio;

import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.client.request.ConversationRequestBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Send SDP offer to CAPI, get an rtc_id back
 *
 * @author emma tresanszki.
 */
public class RtcNewRequest extends ConversationRequestBase<RequestHandler<String>, RtcNewRequest.RtcNewResponse> {
    public static final String RTC_NEW_REQUEST = "rtc:new";
    public static final String RTC_NEW_SUCCESS = "rtc:new:success";
    public String memberId;
    public String sdpOffer;

    public RtcNewRequest(String cid, String memberId, String SDP, RequestHandler<String> listener) {
        super(TYPE.RTC_NEW, cid, listener);
        this.memberId = memberId;
        this.sdpOffer = SDP;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        JSONObject bodyObj = new JSONObject();
        JSONObject sdp = new JSONObject();
        sdp.put("sdp", this.sdpOffer);
        bodyObj.put("offer", sdp);
        bodyObj.put("label", "");

        JSONObject json = newTaggedResponse().put("cid", cid).put("from", memberId).put("body", bodyObj);
        System.out.println("rtc:new " + json.toString());
        return json;
    }

    @Override
    public String getRequestName() {
        return RTC_NEW_REQUEST;
    }

    @Override
    public String getSuccessEventName() {
        return RTC_NEW_SUCCESS;
    }

    @Override
    public RtcNewResponse parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        System.out.println(getClass().getSimpleName() + " rtc:new response: " + body.toString());

        return new RtcNewResponse(body.getString("rtc_id"), body.optString("stream_id"));
    }

    public static class RtcNewResponse {
        public String rtc_id;
        public String stream_id;

        public RtcNewResponse(String rtc_id, String stream_id) {
            this.rtc_id = rtc_id;
            this.stream_id = stream_id;
        }
    }
}
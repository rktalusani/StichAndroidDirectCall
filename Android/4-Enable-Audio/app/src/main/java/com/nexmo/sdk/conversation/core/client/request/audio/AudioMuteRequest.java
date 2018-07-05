package com.nexmo.sdk.conversation.core.client.request.audio;

import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.client.request.ConversationRequestBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Send Mute notification after user has muted himself on the client.
 * @author emma tresanszki.
 */
public class AudioMuteRequest extends ConversationRequestBase<RequestHandler<String>, Void> {
    public static final String AUDIO_MUTE_ON = "audio:mute:on";
    public static final String AUDIO_MUTE_ON_SUCCESS = "audio:mute:on:success";
    public static final String AUDIO_MUTE_OFF = "audio:mute:off";
    public static final String AUDIO_MUTE_OFF_SUCCESS = "audio:mute:off:success";
    public String memberId;
    public boolean muted;

    public AudioMuteRequest(String cid, String memberId, boolean muted, RequestHandler<String> listener) {
        super(TYPE.RTC_MUTE, cid, listener);
        this.memberId = memberId;
        this.muted = muted;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        JSONObject bodyObj = new JSONObject();

        JSONObject json = newTaggedResponse().put("cid", cid).put("to", memberId).put("body", bodyObj);
        System.out.println(muted + " : " + json.toString());
        return json;
    }

    @Override
    public String getRequestName() {
        return (this.muted ? AUDIO_MUTE_ON : AUDIO_MUTE_OFF);
    }

    @Override
    public String getSuccessEventName() {
        return (this.muted ? AUDIO_MUTE_ON_SUCCESS : AUDIO_MUTE_OFF_SUCCESS);
    }

    @Override
    public Void parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        System.out.println(getClass().getSimpleName() + " rtc:mute response: " + body.toString());

        return null;

    }

}
package com.nexmo.sdk.conversation.core.client.request.audio;

import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.client.request.ConversationRequestBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author emma tresanszki.
 */
public class AudioEarmuffRequest extends ConversationRequestBase<RequestHandler<String>, Void> {
    public static final String AUDIO_EARMUFF_ON_REQUEST = "audio:earmuff:on";
    public static final String AUDIO_EARMUFF_ON_SUCCESS = "audio:earmuff:on:success";
    public static final String AUDIO_EARMUFF_OFF_REQUEST = "audio:earmuff:off";
    public static final String AUDIO_EARMUFF_OFF_SUCCESS = "audio:earmuff:off:success";
    public String memberId;
    public boolean earmuffed;

    public AudioEarmuffRequest(String cid, String memberId, boolean earmuffed, RequestHandler<String> listener) {
        super(TYPE.RTC_EARMUFF, cid, listener);
        this.memberId = memberId;
        this.earmuffed = earmuffed;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        JSONObject bodyObj = new JSONObject();

        JSONObject json = newTaggedResponse().put("cid", cid).put("to", memberId).put("body", bodyObj);
        System.out.println(earmuffed + " : " + json.toString());
        return json;
    }

    @Override
    public String getRequestName() {
        return (this.earmuffed ? AUDIO_EARMUFF_ON_REQUEST : AUDIO_EARMUFF_OFF_REQUEST);
    }

    @Override
    public String getSuccessEventName() {
        return (this.earmuffed ? AUDIO_EARMUFF_ON_SUCCESS : AUDIO_EARMUFF_OFF_SUCCESS);
    }

    @Override
    public Void parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        System.out.println(getClass().getSimpleName() + " audio:earmuff response: " + body.toString());

        return null;

    }

}

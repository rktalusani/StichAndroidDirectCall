package com.nexmo.sdk.conversation.core.client.request.audio;

import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.client.request.ConversationRequestBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author emma tresanszki.
 */
public class AudioRingingRequest extends ConversationRequestBase<RequestHandler<String>, Void> {
    public static final String AUDIO_RINGING_ON_REQUEST = "audio:ringing:start";
    public static final String AUDIO_RINGING_ON_SUCCESS = "audio:ringing:start:success";
    public static final String AUDIO_RINGING_OFF_REQUEST = "audio:ringing:stop";
    public static final String AUDIO_RINGING_OFF_SUCCESS = "audio:ringing:stop:success";
    public String memberId;
    public boolean isRinging;

    public AudioRingingRequest(String cid, String memberId, boolean isRinging, RequestHandler<String> listener) {
        super(TYPE.AUDIO_RINGING, cid, listener);
        this.memberId = memberId;
        this.isRinging = isRinging;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        JSONObject bodyObj = new JSONObject();

        JSONObject json = newTaggedResponse().put("cid", cid).put("from", memberId).put("body", bodyObj);
        return json;
    }

    @Override
    public String getRequestName() {
        return (this.isRinging ? AUDIO_RINGING_ON_REQUEST : AUDIO_RINGING_OFF_REQUEST);
    }

    @Override
    public String getSuccessEventName() {
        return (this.isRinging ? AUDIO_RINGING_ON_SUCCESS : AUDIO_RINGING_OFF_SUCCESS);
    }

    @Override
    public Void parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        return null;

    }
}
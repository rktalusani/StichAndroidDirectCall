package com.nexmo.sdk.conversation.core.client.request;

import android.text.TextUtils;

import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Invite with audio enabled.
 *
 * @author emma tresanszki.
 */
public class InviteWithAudioRequest extends InviteRequest {
    public String userId;
    public String username;
    public boolean isMuted;
    public boolean isEarmuff;
    public AudioInvitation audioInvitation;


    public InviteWithAudioRequest(String cid, String userId, String username, boolean isMuted, boolean isEarmuff, RequestHandler<Member> listener) {
        super(cid, userId, listener);
        this.cid = cid;
        this.listener = listener;
        this.userId = userId;
        this.username = username;
        this.audioInvitation = new AudioInvitation(isMuted, isEarmuff);
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        JSONObject bodyObj = new JSONObject();
        if (!TextUtils.isEmpty(this.userId))
            bodyObj.put("user_id", this.userId);
        else if(!TextUtils.isEmpty(this.username))
            bodyObj.put("user_name", this.username);

        JSONObject media = new JSONObject();
        JSONObject audio = new JSONObject();
        audio.put("muted", this.isMuted);
        audio.put("earmuffed", this.isEarmuff);
        media.put("audio", audio);
        bodyObj.put("media", media);

        return newTaggedResponse()
                .put("cid", cid)
                .put("body", bodyObj);
    }

    public static class AudioInvitation {
        boolean isMuted;
        boolean isEarmuff;

        public AudioInvitation(boolean isMuted, boolean isEarmuff) {
            this.isMuted = isMuted;
            this.isEarmuff = isEarmuff;
        }
    }
}
package com.nexmo.sdk.conversation.client.audio;

import org.webrtc.SessionDescription;

/**
 * @author emma tresanszki.
 */
public interface RtcEvents {

    void onAnswer(SessionDescription sdp);
}

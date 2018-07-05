package com.nexmo.sdk.conversation.client.audio;

import com.nexmo.sdk.conversation.client.event.NexmoAPIError;

/**
 * Created by siltus on 20/09/2017.
 */

public interface AudioCallEventListener {

    void onRinging();
    void onCallConnected();
    void onCallEnded();

    void onGeneralCallError(NexmoAPIError apiError);

    void onAudioRouteChange(AppRTCAudioManager.AudioDevice device);
}

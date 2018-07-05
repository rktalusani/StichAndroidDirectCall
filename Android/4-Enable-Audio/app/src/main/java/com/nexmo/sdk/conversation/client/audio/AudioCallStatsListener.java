package com.nexmo.sdk.conversation.client.audio;

import org.webrtc.StatsReport;

/**
 * Created by rtalusani on 14/05/18.
 */

public interface AudioCallStatsListener {
    void onStatsAvailable(StatsReport[] report);
}

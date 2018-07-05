/*
 *  Copyright 2013 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.nexmo.sdk.conversation.client.audio;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.List;

/**
 * AppRTCClient is the interface representing an AppRTC client.
 */
public interface AppRTCClient {


  class SignalingParameters {
    public final List<PeerConnection.IceServer> turnServers;
    public final boolean initiator;
    public final SessionDescription offerSdp;
    public final List<IceCandidate> iceCandidates;

    public SignalingParameters(List<PeerConnection.IceServer> turnServers, boolean initiator,
                               SessionDescription offerSdp, List<IceCandidate> iceCandidates) {
      this.turnServers = turnServers;
      this.initiator = initiator;
      this.offerSdp = offerSdp;
      this.iceCandidates = iceCandidates;
    }
  }

}

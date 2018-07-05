//package com.nexmo.sdk.conversation.client;
//
//import android.content.Context;
//import com.nexmo.sdk.conversation.core.util.Log;
//
//import org.webrtc.AudioSource;
//import org.webrtc.DataChannel;
//import org.webrtc.IceCandidate;
//import org.webrtc.MediaConstraints;
//import org.webrtc.MediaStream;
//import org.webrtc.PeerConnection;
//import org.webrtc.PeerConnectionFactory;
//import org.webrtc.SdpObserver;
//import org.webrtc.SessionDescription;
//import org.webrtc.VideoCapturer;
//import org.webrtc.VideoCapturerAndroid;
//import org.webrtc.VideoRenderer;
//import org.webrtc.VideoRendererGui;
//import org.webrtc.VideoSource;
//import org.webrtc.VideoTrack;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Experimental phase.
// *
// * @author emma
// * @date 17/11/15.
// */
//public class Session {
//
//    private static final String TAG = Session.class.getSimpleName();
//    private Context appContext;
//    private boolean audioOnly = true;
//    private PeerConnectionFactory peerConnectionFactory;
//    private SdpObserver sdpObserver;
//    private PeerConnection.Observer peerObserver;
//    private PeerConnection peerConnection;
//    private MediaConstraints mediaConstraints = new MediaConstraints();
//
//    public Session(Context appContext) {
//        this.appContext = appContext;
//        initializePeerConnectionFactory();
//        setupObservers();
//    }
//
//    public void setMode(boolean audioOnly) {
//        this.audioOnly = audioOnly;
//    }
//
//    public void initializePeerConnectionFactory() {
//        this.mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
//        this.mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
//        boolean factoryInitialized = PeerConnectionFactory.initializeAndroidGlobals(
//                this.appContext,
//                audioOnly,
//                true,
//                true,
//                // generate yuv420 frames instead of texture frames
//                null);
//
//        try {
//            Log.d(TAG, "before init");
//            this.peerConnectionFactory = new PeerConnectionFactory();
//            Log.d(TAG, "after init");
//        } catch (RuntimeException e) {
//            Log.d("peer", "Failed to initialize PeerConnectionFactory!");
//        }
//    }
//
//    public VideoCapturer initializeVideoCapturer() {
//        Log.d(TAG, String.valueOf(VideoCapturerAndroid.getDeviceCount()));
//        Log.d(TAG, String.valueOf(VideoCapturerAndroid.getNameOfFrontFacingDevice()));
//        Log.d(TAG, String.valueOf(VideoCapturerAndroid.getNameOfBackFacingDevice()));
//
//        return VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfBackFacingDevice());
//    }
//
//    public VideoTrack createVideoTrack() {
//        // Create VideoSource
//        VideoSource videoSource = this.peerConnectionFactory.createVideoSource(initializeVideoCapturer(), this.mediaConstraints);
//
//        VideoTrack videoTrack = peerConnectionFactory.createVideoTrack("FIRST_TRACK_ID", videoSource);
//        //video renderer from activity
//        VideoRenderer.Callbacks localRenderer= VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
//
//        videoTrack.addRenderer(new VideoRenderer(localRenderer));
//        return videoTrack;
//    }
//
//    public org.webrtc.AudioTrack createAudioTrack() {
//        AudioSource audioSource = this.peerConnectionFactory.createAudioSource(this.mediaConstraints);
//        return peerConnectionFactory.createAudioTrack("FIRST_AUDIO_TRACK_ID", audioSource);
//    }
//
//    public void startVideo() {
//        MediaStream mediaStream = this.peerConnectionFactory.createLocalMediaStream("FIRST_VIDEO_STREAM_ID");
//        mediaStream.addTrack(createVideoTrack());
//
//        List<PeerConnection.IceServer> iceServerList = new ArrayList<>();
//        iceServerList.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
//        iceServerList.add(new PeerConnection.IceServer("turn:numb.viagenie.ca", "webrtc@live.com", "muazkh"));
//        //        iceServerList.add(new PeerConnection.IceServer("stun:stun1.l.google.com:19302"));
//        //        iceServerList.add(new PeerConnection.IceServer("stun:stun2.l.google.com:19302"));
//        //        iceServerList.add(new PeerConnection.IceServer("stun:stun3.l.google.com:19302"));
//        //        iceServerList.add(new PeerConnection.IceServer("stun:stun4.l.google.com:19302"));
//        this.peerConnection = this.peerConnectionFactory.createPeerConnection(iceServerList, this.mediaConstraints, this.peerObserver);
//        //this.peerConnection.setLocalDescription(this.sdpObserver, new SessionDescription(SessionDescription.Type.OFFER, "first offer session local description"));
//        this.peerConnection.setRemoteDescription(this.sdpObserver, new SessionDescription(SessionDescription.Type.ANSWER, "first offer session remote description"));
//        // or
//        peerConnection.setLocalDescription(this.sdpObserver, new SessionDescription(SessionDescription.Type.ANSWER, "first answer session description"));
//
//        boolean streamAdded = peerConnection.addStream(mediaStream);
//        Log.d(TAG, "video streamAdded to peerConnection " + streamAdded);
//        //create offer needed?
//        //peerConnection.createOffer(this.sdpObserver, this.mediaConstraints);
//
//        //peerConnection.createAnswer(this.sdpObserver, this.mediaConstraints);
//
//        Log.d(TAG, "remote description: " + peerConnection.getRemoteDescription());
//        Log.d(TAG, "local description: " + peerConnection.getLocalDescription());
//
//        // to do handle all sdp-s
//        //if (localSdp == null || remoteDesc == null) {
//        //            Log.d(TAG, "Switching camera before the negotiation started.");
//        //            return;
//        //        }
//    }
//
//    public void startAudio() {
//        MediaStream mediaStream = this.peerConnectionFactory.createLocalMediaStream("FIRST_AUDIO_STREAM_ID");
//        mediaStream.addTrack(createAudioTrack());
//
//        // offerToRecieveAudio  offerToRecieveVideo
//
//        List<PeerConnection.IceServer> iceServerList = new ArrayList<>();
//        iceServerList.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
//        PeerConnection peerConnection = this.peerConnectionFactory.createPeerConnection(iceServerList, this.mediaConstraints, this.peerObserver);
//        peerConnection.setRemoteDescription(this.sdpObserver, new SessionDescription(SessionDescription.Type.OFFER, "second offer session description"));
//        //        peerConnection.setLocalDescription(this.sdpObserver, new SessionDescription(SessionDescription.Type.ANSWER, "first answer session description"));
//
//        boolean streamAdded = peerConnection.addStream(mediaStream);
//        Log.d(TAG, "streamAdded to peerConnection " + streamAdded);
//
//        peerConnection.createOffer(this.sdpObserver, mediaConstraints);
//    }
//
//    private void setupObservers() {
//
//        this.peerObserver = new PeerConnection.Observer() {
//            @Override
//            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
//                Log.d(TAG, "onSignalingChange "  + signalingState);
//
//            }
//
//            @Override
//            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
//                Log.d(TAG, "onIceConnectionChange " + iceConnectionState);
//                switch (iceConnectionState) {
//                    case CONNECTED: {
//                        // time to update the  VideoRenderedGui.
//
//                    }
//                }
//            }
//
//            @Override
//            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
//                Log.d(TAG,"onIceGatheringChange " + iceGatheringState);
//
//            }
//
//            @Override
//            public void onIceCandidate(IceCandidate iceCandidate) {
//                Log.d(TAG, "onIceCandidate " + iceCandidate.toString());
//                // broadcast it and get the other peers's iceCandidate as they become available
//                // add their iceCandidate to the peerConnection
//                peerConnection.addIceCandidate(iceCandidate);
//
//            }
//
//            @Override
//            public void onAddStream(MediaStream mediaStream) {
//                Log.d(TAG, "onAddStream " + mediaStream.toString());
//                // show it in the webview
//
//            }
//
//            @Override
//            public void onRemoveStream(MediaStream mediaStream) {
//                Log.d(TAG, "onRemoveStream " + mediaStream.toString());
//
//            }
//
//            @Override
//            public void onDataChannel(DataChannel dataChannel) {
//                Log.d(TAG, "onDataChannel " + dataChannel.toString());
//
//            }
//
//            @Override
//            public void onRenegotiationNeeded() {
//                Log.d(TAG, "onRenegotiationNeeded");
//
//            }
//        };
//
//        this.sdpObserver = new SdpObserver() {
//            @Override
//            public void onCreateSuccess(SessionDescription sessionDescription) {
//                Log.d(TAG, "onCreateSuccess " + sessionDescription.toString());
//
//                peerConnection.setRemoteDescription(sdpObserver, sessionDescription);
//                peerConnection.setLocalDescription(sdpObserver, sessionDescription);
//                peerConnection.createAnswer(sdpObserver, mediaConstraints);
//
//            }
//
//            @Override
//            public void onSetSuccess() {
//                Log.d(TAG, "onSetSuccess");
//
//            }
//
//            @Override
//            public void onCreateFailure(String s) {
//                Log.d(TAG, "onCreateFailure " + s);
//
//            }
//
//            @Override
//            public void onSetFailure(String s) {
//                Log.d(TAG, "onSetFailure " + s);
//
//            }
//        };
//    }
//}

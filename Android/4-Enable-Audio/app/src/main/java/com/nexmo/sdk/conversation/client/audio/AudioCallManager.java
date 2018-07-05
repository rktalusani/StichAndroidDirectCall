package com.nexmo.sdk.conversation.client.audio;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;

import com.nexmo.enableaudio.BuildConfig;
import com.nexmo.sdk.conversation.core.util.Log;
import android.widget.Toast;

//import com.nexmo.sdk.conversation.BuildConfig;
import com.nexmo.sdk.conversation.client.ConversationSignalingChannel;
import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Call Manager. Supports one ongoing call/conversation for now.
 *
 * Created by siltus on 17/09/2017.
 *
 */

public class AudioCallManager implements PeerConnectionClient.PeerConnectionEvents, RtcEvents {

    private static final String TAG = "Rtc " + AudioCallManager.class.getSimpleName();

    public enum AudioCallState {
        ReadyForCall,
        Trying,
        Ringing,
        Connected
    }

    // List of mandatory application permissions.
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};

    // Peer connection statistics callback period in ms.
    private static final int STAT_CALLBACK_PERIOD = 1000;

    private PeerConnectionClient peerConnectionClient = null;
    private AppRTCClient.SignalingParameters signalingParameters;
    private AppRTCAudioManager audioManager = null;
    private Toast logToast;
    private PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
    private boolean iceConnected;
    private boolean isError;
    private long callStartedTimeMs = 0;

    private AudioCallState state = AudioCallState.ReadyForCall;
    private final Context context;
    private final Handler handler;
    private String conversationId = null;

    private String memberId;
    private String rtc_id;

    private AudioCallEventListener delegate = null;
    private AudioCallStatsListener statsDelegate = null;
    private ConversationSignalingChannel conversationSignalingChannel;
    private SessionDescription offer;

    public AudioCallManager(Context context, final String conversationId, final String memberId,
                            ConversationSignalingChannel conversationSignalingChannel) {
        Log.d(TAG, "AudioCallManager() ctor");
        this.conversationId = conversationId;
        this.memberId = memberId;
        this.context = context;
        this.handler = new Handler(context.getMainLooper());
        this.state = AudioCallState.ReadyForCall;
        this.conversationSignalingChannel = conversationSignalingChannel;
    }

    //**********************************************************************************************
    //*                                                                                            *
    //*                         Public Interface for Conversation.audio()                          *
    //*                                                                                            *
    //**********************************************************************************************

    /**
     * Enables sending audio call stats to the calling application
     */

    public void enableCallStats(final AudioCallStatsListener listener)
    {
        if(listener == null){
            Log.e("AudioCallManager:enableCallStats","listener mandatory");
        }
        this.statsDelegate = listener;
    }

    /**
     * Enable Audio calls in the current conversation.
     * Audio capabilities require user permissions: <strong>"android.permission.MODIFY_AUDIO_SETTINGS",
     * "android.permission.RECORD_AUDIO", "android.permission.INTERNET"</strong>,
     * therefore request would fail if they are REJECTED.
     *
     * It is recommended to check and ask for these Permissions to be GRANTED before using any Audio feature
     * @see <a href="https://developer.android.com/training/permissions/requesting.html">Official doc</a>
     *
     * <p>Only one audio conversation is supported at a time, so trying to enable audio simultaneously
     * will fail with {@link NexmoAPIError#audioAlreadyInProgress()}</p>
     *
     * @param listener Mandatory completion listener in charge of dispatching the result.
     */
    public void enable(final AudioCallEventListener listener) {
        Log.d(TAG, "enable audio " + this.state);

        if (listener == null) {
            Log.d(TAG, "Listener is mandatory");
            return;
        }

        if (this.memberId == null) {
            listener.onGeneralCallError(NexmoAPIError.noUserLoggedInForConversation(this.conversationId));
            return;
        }

        // Check for mandatory permissions.
        for (String permission : MANDATORY_PERMISSIONS) {
            if (context.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission " + permission + " is not granted");
                listener.onGeneralCallError(NexmoAPIError.audioMissingPermissions());
                return;
            }
        }

        if (state != AudioCallState.ReadyForCall || conversationSignalingChannel.isAudioEnabled()) {
            listener.onGeneralCallError(NexmoAPIError.audioAlreadyInProgress());
            return;
        }

        if (this.conversationId == null || this.conversationId.length() == 0) {
            Log.e(TAG, "Incorrect room ID!");
            listener.onGeneralCallError(NexmoAPIError.missingParams());
            return;
        }
        startCall(listener);
    }

    /**
     * Disable audio calls in the current conversation.
     * Audio capabilities require user permissions: <strong>"android.permission.MODIFY_AUDIO_SETTINGS",
     * "android.permission.RECORD_AUDIO", "android.permission.INTERNET"</strong>,
     * therefore request would fail if they are REJECTED.
     * The audio has to be enabled {@link AudioCallManager#enable(AudioCallEventListener)} prior to
     * any disable action.
     *
     * @param listener Mandatory completion listener in charge of dispatching the result.
     */
    public void disable(final RequestHandler listener) {
        Log.d(TAG, "audio disable");

        if (listener == null) {
            Log.e(TAG, "Listener must be provided");
            return;
        }

        if (this.memberId == null) {
            listener.onError(NexmoAPIError.noUserLoggedInForConversation(this.conversationId));
            return;
        }

        // Check for mandatory permissions.
        for (String permission : MANDATORY_PERMISSIONS) {
            if (context.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission " + permission + " is not granted");
                listener.onError(NexmoAPIError.audioMissingPermissions());
                return;
            }
        }

        if (state != AudioCallState.Connected) {
            listener.onError(NexmoAPIError.audioGeneralCallError("Audio not connected"));
            return;
        }
        else
            this.conversationSignalingChannel.rtcTerminate(conversationId, memberId, rtc_id, listener);

        endAudioCall(listener);
    }

    /**
     * Mute self in the current audio call.
     * Audio has to be flowing before muting/un muting, by enabling audio
     * {@link AudioCallManager#enable(AudioCallEventListener)} before.
     *
     *
     * @param isMute true or false
     * @param listener Mandatory completion listener in charge of dispatching the result.
     */
    public void mute(boolean isMute, RequestHandler listener) {
        Log.d(TAG, "muteSelf : isMute = " + isMute);

        if (listener == null) {
            Log.e(TAG, "Listener must be provided");
            return;
        }

        if (this.memberId == null) {
            listener.onError(NexmoAPIError.noUserLoggedInForConversation(this.conversationId));
            return;
        }

        if (state != AudioCallState.Connected) {
            listener.onError(NexmoAPIError.audioGeneralCallError("Audio not connected"));
            return;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.setAudioEnabled(!isMute);

            this.conversationSignalingChannel.rtcMute(conversationId, memberId, isMute, listener);
        }
    }

    /**
     * Earmuff self in the current audio call.
     * Audio has to be flowing before by enabling audio
     * {@link AudioCallManager#enable(AudioCallEventListener)} before.
     *
     *
     * @param isEarmuff true or false.
     * @param listener Mandatory completion listener in charge of dispatching the result.
     */
    public void earmuff(boolean isEarmuff, RequestHandler listener) {
        Log.d(TAG, "earmuffSelf : isEarmuff = " + isEarmuff);

        if (listener == null) {
            Log.e(TAG, "Listener must be provided");
            return;
        }

        if (this.memberId == null) {
            listener.onError(NexmoAPIError.noUserLoggedInForConversation(this.conversationId));
            return;
        }

        if (state != AudioCallState.Connected) {
            listener.onError(NexmoAPIError.audioGeneralCallError("Audio not connected"));
            return;
        }


        this.conversationSignalingChannel.rtcEarmuff(conversationId, memberId, isEarmuff, listener);
    }

    /**
     * Send a start ringing event.
     *
     *  @param listener Mandatory completion listener in charge of dispatching the result.
     */
    public void startRinging(RequestHandler listener) {
        Log.d(TAG, "startRinging");

        if (listener == null) {
            Log.e(TAG, "Listener must be provided");
            return;
        }

        if (this.memberId == null) {
            listener.onError(NexmoAPIError.noUserLoggedInForConversation(this.conversationId));
            return;
        }

        if (state != AudioCallState.Connected) {
            listener.onError(NexmoAPIError.audioGeneralCallError("Audio not connected"));
            return;
        }


        this.conversationSignalingChannel.mediaRinging(conversationId, memberId, true, listener);
    }

    /**
     * Send a stop ringing event.
     *
     *  @param listener Mandatory completion listener in charge of dispatching the result.
     */
    public void stopRinging(RequestHandler listener) {
        Log.d(TAG, "stopRinging");

        if (listener == null) {
            Log.e(TAG, "Listener must be provided");
            return;
        }

        if (this.memberId == null) {
            listener.onError(NexmoAPIError.noUserLoggedInForConversation(this.conversationId));
            return;
        }

        if (state != AudioCallState.Connected) {
            listener.onError(NexmoAPIError.audioGeneralCallError("Audio not connected"));
            return;
        }


        this.conversationSignalingChannel.mediaRinging(conversationId, memberId, false, listener);
    }

    /**  Internal implementation **/

    void startCall(final AudioCallEventListener listener) {
        this.delegate = listener;
        this.state = AudioCallState.Ringing;
        delegate.onRinging();

        iceConnected = false;
        signalingParameters = null;

        // Create peer connection client.
        peerConnectionClient = new PeerConnectionClient();

        boolean tracing = false;

        peerConnectionParameters =
                new PeerConnectionClient.PeerConnectionParameters(
                        tracing,
                        0,
                        null,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false);

        peerConnectionClient.createPeerConnectionFactory(
                this.context, peerConnectionParameters, AudioCallManager.this);

        callStartedTimeMs = System.currentTimeMillis();

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(this.context);
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Starting the audio manager...");
        audioManager.start(new AppRTCAudioManager.AudioManagerEvents() {
            // This method will be called each time the number of available audio
            // devices has changed.
            @Override
            public void onAudioDeviceChanged(
                    AppRTCAudioManager.AudioDevice audioDevice, Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
                onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
            }
        });

        final AppRTCClient.SignalingParameters signallingParameters = setupSignallingParameters();

        Runnable runHttp = new Runnable() {
            public void run() {
                initiatePeerConnection(signallingParameters);
            }
        };
        new Thread(runHttp).start();
    }

    private void endAudioCall(final RequestHandler<Void> listener) {
        disconnect(listener);
    }

    public AudioCallState getState() {
        return state;
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect(final RequestHandler<Void> disableListener) {
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
        if (iceConnected && !isError) {
            this.delegate.onCallEnded();
        } else {
            this.delegate.onGeneralCallError(NexmoAPIError.audioGeneralCallError("While disconnecting"));
            if (disableListener != null)
                disableListener.onError(NexmoAPIError.audioGeneralCallError("While disconnecting"));
        }

        this.state = AudioCallState.ReadyForCall;
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        this.delegate.onGeneralCallError(NexmoAPIError.audioGeneralCallError(errorMessage));
        disconnect(null);
    }

    @Override
    public void onAnswer(SessionDescription sdp) {
        if (peerConnectionClient != null)
            peerConnectionClient.setRemoteDescription(sdp);
    }

    private AppRTCClient.SignalingParameters setupSignallingParameters() {
            List<PeerConnection.IceServer> turnServers = new ArrayList<>();
            PeerConnection.IceServer turnServer =
                    PeerConnection.IceServer.builder(Config.TURN_SERVER_URL)
                            .setUsername("bar")
                            .setPassword("foo2")
                            .createIceServer();
            turnServers.add(turnServer);

            return new AppRTCClient.SignalingParameters(
                    turnServers, true, null, null);
    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        if (peerConnectionClient == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);

        this.state = AudioCallState.Connected;
        delegate.onCallConnected();
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
        delegate.onAudioRouteChange(device);
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect() {
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
        if (iceConnected && !isError) {
            this.delegate.onCallEnded();
        } else {
            this.delegate.onGeneralCallError(NexmoAPIError.audioGeneralCallError("While disconnecting"));
        }

        this.state = AudioCallState.ReadyForCall;
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.e(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    private void reportError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    isError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }

    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.
    private void initiatePeerConnection(final AppRTCClient.SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        signalingParameters = params;
        //logAndToast("Creating peer connection, delay=" + delta + "ms");

        peerConnectionClient.createPeerConnection(signalingParameters);

        if (signalingParameters.initiator) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAndToast("Creating OFFER...");
                }
            });
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.

            peerConnectionClient.createOffer();
        } else {
            if (params.offerSdp != null) {
                peerConnectionClient.setRemoteDescription(params.offerSdp);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logAndToast("Creating ANSWER...");
                    }
                });
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }
            if (params.iceCandidates != null) {
                // Add remote ICE candidates from room.
                for (IceCandidate iceCandidate : params.iceCandidates) {
                    peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                }
            }
        }
    }

    // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        Log.d(TAG, "onLocalDescription: " + sdp.description);
        this.offer = sdp;
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        Log.d(TAG, "onIceCandidate: " + candidate.toString());

//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (appRtcClient != null) {
//                    appRtcClient.sendLocalIceCandidate(candidate);
//                }
//            }
//        });
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        Log.d(TAG, "onIceCandidatesRemoved: " + candidates.toString());

//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (appRtcClient != null) {
//                    appRtcClient.sendLocalIceCandidateRemovals(candidates);
//                }
//            }
//        });
    }

    @Override
    public void onIceConnected() {
        Log.d(TAG, "onIceConnected");
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE connected, delay=" + delta + "ms");
                iceConnected = true;
                callConnected();
            }
        });
    }

    @Override
    public void onIceDisconnected() {
        Log.d(TAG, "onIceDisconnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE disconnected");
                iceConnected = false;
                disconnect();
            }
        });
    }

    @Override
    public void onPeerConnectionClosed() {
        Log.d(TAG, "onPeerConnectionClosed");
    }

    @Override
    public void onPeerConnectionStatsReady(final StatsReport[] reports) {
        //Log.d(TAG, "onPeerConnectionStatsReady: " + reports.toString());

        if(statsDelegate != null){
            statsDelegate.onStatsAvailable(reports);
        }
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //logAndToast(reports[0].toString());

            }
        });*/

    }

    @Override
    public void onPeerConnectionError(final String description) {
        Log.d(TAG, "onPeerConnectionError: " + description);

        reportError(description);
    }

    @Override
    public void onIceGatheringDone() {
        Log.d(TAG, "onIceGatheringDone");
        Log.d(TAG, "rtc:new sdp=" + peerConnectionClient.getLocalDescription().description);

        this.conversationSignalingChannel.rtcNew(conversationId, this.memberId, peerConnectionClient.getLocalDescription().description, new RequestHandler() {
            @Override
            public void onError(NexmoAPIError apiError) {
                state = AudioCallState.ReadyForCall;
                delegate.onGeneralCallError(apiError);
            }

            @Override
            public void onSuccess(Object result) {
            }
        });
    }

    private void runOnUiThread(Runnable r) {
        handler.post(r);
    }

    public Set<AppRTCAudioManager.AudioDevice> getAvailableAudioRoutes() {
        if (audioManager == null)  {
            Log.e(TAG, "AudioManager is not initialised");
            return null;
        }

        return audioManager.getAudioDevices();
    }

    public void setAudioRoute(AppRTCAudioManager.AudioDevice audioRoute) {
        if (audioManager == null) {
            Log.e(TAG, "AudioManager is not initialised");
            return;
        }

        audioManager.selectAudioDevice(audioRoute);
    }

    public void updateRtcId(final String rtcId){
        this.rtc_id = rtcId;
    }
}

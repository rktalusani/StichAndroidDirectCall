package com.nexmo.sdk.conversation.client;

import android.support.annotation.VisibleForTesting;

import com.nexmo.sdk.conversation.client.audio.AppRTCAudioManager;
import com.nexmo.sdk.conversation.client.audio.AudioCallEventListener;
import com.nexmo.sdk.conversation.client.audio.AudioCallManager;
import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.util.Log;

import java.util.List;

import static com.nexmo.sdk.conversation.client.Conversation.MEDIA_TYPE.AUDIO;


/**
 * Calls can be made towards specified users.
 *
 * <p>Example usage:</p>
 * <pre>
 *     conversationClient.call(userArray, new RequestHandler<Call>() {
 *         &#64;Override
 *         public void onSuccess(Call call) {
 *              // Update the application UI here if needed with the ongoing call.
 *         }
 *         &#64;Override
 *         public void onError(NexmoAPIError error) {
 *              // Update the application UI here if needed.
 *         }
 *    });
 * </pre>
 *
 * <p>Subscribe for member events in a call:
 * <pre>
 *         ResultListener<CallEvent> callEventListener = new ResultListener<CallEvent>() {
 *         &#64;Override
 *         public void onSuccess(CallEvent event) {
 *             // Update the app UI with new event: message.toString();
 *         }
 *    });
 *
 *     call.event().add(callEventListener);
 * </pre>
 * </p>
 *
 * <p>
 *     Call available actions:
 *     <ul>
 *         <li>answer {@link Call#answer(RequestHandler)}.</li>
 *         <li>reject {@link Call#reject(RequestHandler)}.</li>
 *         <li>hangup {@link Call#hangup(RequestHandler)}.</li>
 *     </ul>
 * </p>
 * @author emma tresanszki.
 */
public class Call {
    private static final String TAG = Call.class.getSimpleName();

    /**
     * Enum for Call Member states.
     * @constant
     * @enum {object}
     */
    public enum MEMBER_CALL_STATE {
        /** A Member hung up the call */
        HUNGUP,
        /** A Member answered the call */
        ANSWERED,
        /** A Member rejected the call */
        REJECTED
    };

    private Conversation conversation;
    private String from;

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Call(Conversation conversation) {
        this.conversation = conversation;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Call(Conversation conversation, String from) {
        this.conversation = conversation;
        this.from = from;
    }

    public String getName() {
        return this.conversation.getDisplayName();
    }

    public final Conversation getConversation() {
        return this.conversation;
    }

    /**
     * Get all the members on the call.
     *
     * @return List of members.
     */
    public final List<Member> to() {
        return this.conversation.getMembers();
    }

    /**
     * Get the username of user who initiated call.
     */
    public String from() {
        return this.from;
    }
    /**
     * Answer an incoming call.
     * If the call is already answered, an error will be dispatched.
     *
     * Note: answering the call means accepting invitation to conversation and enable audio.
     *
     * @param listener The completion listener in charge of dispatching the result.
     */
    public void answer(final RequestHandler<Void> listener){
        if (listener == null) {
            Log.e(TAG, "Listener must be provided");
            return;
        }

        if (conversation.isAudioEnabled())
            listener.onError(NexmoAPIError.audioAlreadyInProgress());
        else
            conversation.join(new RequestHandler<Member>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                listener.onError(apiError);
            }

            @Override
            public void onSuccess(Member result) {
                conversation.media(AUDIO).enable(new AudioCallEventListener() {
                    @Override
                    public void onRinging() {
                    }

                    @Override
                    public void onCallConnected() {
                        listener.onSuccess(null);
                    }

                    @Override
                    public void onCallEnded() {
                    }

                    @Override
                    public void onGeneralCallError(NexmoAPIError apiError) {
                        listener.onError(apiError);
                    }

                    @Override
                    public void onAudioRouteChange(AppRTCAudioManager.AudioDevice device) {
                    }
                });

            }
        });
    }

    /**
     * Hangs up the call while leaving the generated conversation.
     * The audio is disabled.
     *
     * Note: when there is only one member left in the call, everyone gets removed from the call.
     *
     * @param listener The completion listener in charge of dispatching the result.
     */
    public void hangup(final RequestHandler<Void> listener) {
        if (listener == null) {
            Log.e(TAG, "Listener must be provided");
            return;
        }

        conversation.media(AUDIO).disable(new RequestHandler<Void>() {
            @Override
            public void onError(NexmoAPIError apiError) {
                listener.onError(apiError);
            }

            @Override
            public void onSuccess(Void result) {
                if (conversation.getMembers().size() == 1)
                    conversation.getMembers().get(0).kick(listener);
                else
                    listener.onSuccess(null);
            }
        });

    }

    /**
     * Reject an incoming call.
     *
     * <p> Note: User cannot reject a call that is already answered {@link Call#answer(RequestHandler)}.
     * If the call is already in progress user can hangup {@link Call#hangup(RequestHandler)} instead.
     * </p>
     *
     * @param listener The completion listener in charge of dispatching the result.
     */
    public void reject(final RequestHandler<Void> listener) {
        if (listener == null) {
            Log.e(TAG, "Listener must be provided");
            return;
        }

        if (this.conversation.media(AUDIO).getState() != AudioCallManager.AudioCallState.ReadyForCall) {
            listener.onError(NexmoAPIError.audioAlreadyInProgress());
            return;
        }

        conversation.leave(listener);
    }

    /**
     * Listen for incoming member events for this call.
     *
     * CallEvent types:
     * <ul>
     *     <li>HUNGUP A member hung up the call.</li>
     *     <li>ANSWERED A member answered the call.</li>
     *     <li>REJECTED A member rejected the call.</li>
     * </ul>
     *
     */
    public EventSource<CallEvent> event() {
        return this.conversation.getSignallingChannel()
                .getConversationClient()
                .getEventSource("callEvent" + "-" + this.conversation.getConversationId());
    }

    @Override
    public String toString() {
        return TAG + " display_name: " + this.conversation.getDisplayName() + " .from: " + this.from() +
                " .to: " + this.to().toString();
    }
}

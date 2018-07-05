package com.nexmo.sdk.conversation.client;

import android.os.Parcel;
import android.support.annotation.VisibleForTesting;

import com.nexmo.sdk.conversation.client.event.EventType;

/**
 * Incoming events from a call.
 *
 * @author emma tresanszki.
 */
public class CallEvent extends Event {
    private static final String TAG = CallEvent.class.getSimpleName();
    private Call.MEMBER_CALL_STATE state;

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected CallEvent(){}

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected CallEvent(Call.MEMBER_CALL_STATE state, Member member){
        this.state = state;
        this.member = member;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected CallEvent(Parcel in) {
        super(in);
        this.state = (Call.MEMBER_CALL_STATE) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeSerializable(this.state);
    }

    @Override
    public EventType getType() {
        return EventType.MEMBER_CALL_STATE;
    }

    /**
     * Get the member state.
     *
     * @return The state.
     */
    public Call.MEMBER_CALL_STATE getState() {
        return this.state;
    }

    @Override
    public String toString(){
        return TAG + " state: " + this.state +
                " .member: " + (this.member != null ? this.member.toString() : "");
    }

    public static final Creator<CallEvent> CREATOR = new Creator<CallEvent>() {
        @Override
        public CallEvent createFromParcel(Parcel in) {
            return new CallEvent(in);
        }

        @Override
        public CallEvent[] newArray(int size) {
            return new CallEvent[size];
        }
    };
}

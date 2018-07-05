package com.nexmo.sdk.conversation.client;

import android.os.Parcel;
import android.support.annotation.VisibleForTesting;

import com.nexmo.sdk.conversation.client.event.EventType;

import java.util.Date;

/**
 * MemberMedia contains an immutable media state for any member.
 * By default any member will have {@link MemberMedia#audioEnabled} false.
 *
 * @author emma tresanszki.
 */
public class MemberMedia extends Event {
    private static final String TAG = MemberMedia.class.getSimpleName();
    private boolean audioEnabled = false;

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected MemberMedia(Member member, boolean audioEnabled, Date timestamp) {
        this.member = member;
        this.audioEnabled = audioEnabled;
        this.timestamp = timestamp;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected MemberMedia(String eventId, Member member, Conversation conversation, boolean audioEnabled, Date timestamp) {
        this(member, audioEnabled, timestamp);
        this.conversation = conversation;
        this.id = eventId;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected MemberMedia(Parcel in) {
        super(in);
        this.audioEnabled = in.readInt() != 0;
    }

    public boolean isAudioEnabled() {
        return this.audioEnabled;
    }

    @Override
    public EventType getType() {
        return EventType.MEMBER_MEDIA;
    }

    @Override
    public String toString() {
        return TAG + " member: " + (this.member != null ? this.member.toString() : "") +
                " .id: " + (this.id != null ? this.id : "") +
                " .timestamp: " + (this.timestamp != null ? this.timestamp.toString() : "") +
                " .audioEnabled:" + this.audioEnabled;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest,flags); //writes the id, timestamp, member
        dest.writeInt(this.audioEnabled ? 1 : 0);
    }

    public static final Creator<MemberMedia> CREATOR = new Creator<MemberMedia>() {
        @Override
        public MemberMedia createFromParcel(Parcel in) {
            return new MemberMedia(in);
        }

        @Override
        public MemberMedia[] newArray(int size) {
            return new MemberMedia[size];
        }
    };
}

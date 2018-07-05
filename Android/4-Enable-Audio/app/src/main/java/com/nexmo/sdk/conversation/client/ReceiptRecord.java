package com.nexmo.sdk.conversation.client;

import android.os.Parcel;
import android.os.Parcelable;

import com.nexmo.sdk.conversation.client.event.EventType;

import java.util.Date;

/**
 * Receipt Record holds data for: SeenReceipt and DeliveryReceipt.
 * Receipt records are irrevocable ( ex: you cannot un-do a markAsSeen event)
 *
 * @author emma tresanszki.
 * @date 17/10/16.
 */
public class ReceiptRecord implements Parcelable {
    private static final String TAG = ReceiptRecord.class.getSimpleName();
    private Date timestamp;
    private Event event;
    private Member member;

    protected ReceiptRecord(Event event, Member member, Date timestamp) {
        this.event = event;
        this.member = member;
        this.timestamp = timestamp;
    }

    public Member getMember() {
        return this.member;
    }

    public Event getEvent() {
        return this.event;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    protected void setMember(Member member) {
        this.member = member;
    }

    protected void setEvent(Event event) {
        this.event = event;
    }

    protected ReceiptRecord(Parcel in) {
        this.member = new Member(in.readString());
        String eventId = in.readString();
        EventType eventType =  EventType.values()[in.readInt()];
        if (eventType == EventType.TEXT)
            this.event = new Text(eventId);
        else this.event = new Image(eventId);
        this.timestamp = (Date) in.readSerializable();
    }

    public static final Creator<ReceiptRecord> CREATOR = new Creator<ReceiptRecord>() {
        @Override
        public ReceiptRecord createFromParcel(Parcel in) {
            return new ReceiptRecord(in);
        }

        @Override
        public ReceiptRecord[] newArray(int size) {
            return new ReceiptRecord[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getMember().getMemberId());
        dest.writeString(getEvent().getId());
        dest.writeInt(getEvent().getType().ordinal());
        dest.writeSerializable(this.timestamp);
    }

    @Override
    public String toString() {
        return TAG + ".event : " + (this.event != null ? this.event.toString() : "") +
                ".member: " + (this.member != null ? this.member.toString() : "") +
                ".timestamp: " + (this.timestamp != null ? this.timestamp : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReceiptRecord that = (ReceiptRecord) o;

        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null)
        if (getEvent() != null ? !getEvent().equals(that.getEvent()) : that.getEvent() != null)
            return false;
        if (getMember() != null ? !getMember().equals(that.getMember()) : that.getMember() != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = timestamp != null ? timestamp.hashCode() : 0;
        result = 31 * result + (event.getId() != null ? event.getId().hashCode() : 0);
        result = 31 * result + (member != null ? member.getMemberId().hashCode() : 0);
        return result;
    }
}

/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.client;

import android.os.Parcel;
import android.support.annotation.VisibleForTesting;

import com.nexmo.sdk.conversation.client.event.EventType;
import com.nexmo.sdk.conversation.core.util.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Date;

/**
 * Send a message to a Conversation and handle activity receipts.
 *
 * The following code example shows how to send a text message to a Conversation:
 * <pre>
 * conversation.sendText("text", new RequestHandler<Event>() {
 *       &#64;Override
 *       public void onSuccess(Event text) {
 *       }
 *
 *       &#64;Override
 *       public void onError(NexmoAPIError error) {
 *       }
 *   });
 * </pre>
 *
 * Events that are sent by other members can be marked as seen:
 * <pre>
 * text.markAsSeen( new MarkedAsSeenListener() {
 *       &#64;Override
 *       public void onMarkedAsSeen(Conversation conversation) {
 *       }
 *
 *       &#64;Override
 *       public void onError(NexmoAPIError error) {
 *       }
 *   });
 * </pre>
 *
 * @author emma tresanszki.
 */
public class Text extends Event {
    private static final String TAG = Text.class.getSimpleName();
    private String text;

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Text(){}

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Text(final String text) {
        this.text = text;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public Text(final String text, final Conversation conversation) {
        this(text);
        this.conversation = conversation;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Text(final String text, final String id, final Date timestamp){
        this(text);
        this.id = id;
        this.timestamp = timestamp;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Text(final String text, final String id, final Date timestamp, Member member){
        this(text, id, timestamp);
        this.member = member;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Text(final String text, final String id, final Date timestamp, Member member, Conversation conversation){
        this(text, id, timestamp);
        this.member = member;
        this.conversation = conversation;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Text(final String text, final String id, final Date timestamp, Member member, Date deletedTimestamp){
        this(text, id, timestamp, member);
        this.deletedTimestamp = deletedTimestamp;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Text(final String text, final String id, final Date timestamp, Member member, Date deletedTimestamp, Conversation conversation){
        this(text, id, timestamp, member, deletedTimestamp);
        this.conversation = conversation;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Text(final String text, final String id, final Date timestamp, Member member,
                Date deletedTimestamp, Collection<SeenReceipt> seenReceiptList, Collection<DeliveredReceipt> deliveredReceiptList){
        this(text, id, timestamp, member, deletedTimestamp);
        setSeenReceipts(seenReceiptList);
        setDeliveryReceipts(deliveredReceiptList);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Text(final String text, final String id, final Date timestamp, Member member, Conversation conversation,
                Date deletedTimestamp, Collection<SeenReceipt> seenReceiptList, Collection<DeliveredReceipt> deliveredReceiptList){
        this(text, id, timestamp, member, deletedTimestamp, seenReceiptList, deliveredReceiptList);
        this.conversation = conversation;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Text(Text message) {
        this(message.getText(), message.getId(), message.getTimestamp(), message.getMember(), message.getConversation(),
                message.getDeletedTimestamp(), message.seenReceiptList, message.deliveredReceipts);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Text(Parcel in) {
        super(in);
        this.text = in.readString();
    }

    /**
     * Get the text of this event. For images text is not provided.
     *
     * @return The text text.
     */
    public String getText() {
        return this.text;
    }

    @Override
    public EventType getType() {
        return EventType.TEXT;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.text);
    }

    @Override
    public String toString(){
        return TAG + " text: " + (this.text != null ? this.text : "") + " .id: " + (this.id != null ? this.id : "") +
                " .timestamp: " + (this.timestamp != null ? this.timestamp.toString() : "") +
                " .member: " + (this.member != null ? this.member.toString() : "" +
                " .deletedTimestamp: " + (this.deletedTimestamp != null ? this.deletedTimestamp : " No deletion."));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Text)) return false;
        if (!super.equals(o)) return false;

        Text text = (Text) o;

        return this.text != null ? this.text.equals(text.text) : text.text == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }

    public static Text fromPush(final String senderId, final String eventId, final JSONObject messageObject) throws JSONException {
        Date timestamp = DateUtil.parseDateFromJson(messageObject, "timestamp");
        JSONObject textPayload = messageObject.optJSONObject("body");
        String payload = textPayload.optString("text");
        return new Text(payload, eventId, timestamp, new Member(senderId));
    }

    public static final Creator<Text> CREATOR = new Creator<Text>() {
        @Override
        public Text createFromParcel(Parcel in) {
            return new Text(in);
        }

        @Override
        public Text[] newArray(int size) {
            return new Text[size];
        }
    };

    void setText(String text) {
        this.text = text;
    }
}

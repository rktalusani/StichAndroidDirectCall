package com.nexmo.sdk.conversation.client;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.nexmo.sdk.conversation.client.event.EventType;
import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.util.DateUtil;
import com.nexmo.sdk.conversation.core.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_DELETED_TIMESTAMP;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_DELIVERED_RECEIPTS;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_EVENT_ID;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_IMAGE_REPRESENTATIONS;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_MEMBER_MEDIA_ENABLED;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_MESSAGE_TYPE;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_SEEN_RECEIPTS;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_TEXT;
import static com.nexmo.sdk.conversation.core.persistence.contract.EventContract.EventEntry.COLUMN_TIMESTAMP;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_INVITEDAT;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_JOINEDAT;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_LEFTAT;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_MEMBER_ID;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_STATE;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_USERNAME;
import static com.nexmo.sdk.conversation.core.persistence.contract.MemberContract.MemberEntry.COLUMN_USER_ID;

/**
 * Event is a foundation for different types of messages
 * @author rux
 */
public abstract class Event implements Parcelable {

    private static final String TAG = Event.class.getSimpleName();

    protected String id;
    protected Date timestamp;
    protected Date deletedTimestamp;

    protected Member member;
    protected Set<SeenReceipt> seenReceiptList =
            Collections.synchronizedSet(new HashSet<SeenReceipt>());
    protected Set<DeliveredReceipt> deliveredReceipts =
            Collections.synchronizedSet(new HashSet<DeliveredReceipt>());

    protected Conversation conversation;

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Event() {
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Event(String id, Date timestamp, Date deletedTimestamp) {
        this.id = id;
        this.timestamp = timestamp;
        this.deletedTimestamp = deletedTimestamp;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Event(Parcel parcel) {
        this.id = parcel.readString();
        this.timestamp = (Date)parcel.readSerializable();
        this.deletedTimestamp = (Date) parcel.readSerializable();
        this.member = parcel.readParcelable(Member.class.getClassLoader());
        this.setDeliveryReceipts(Arrays.asList(parcel.createTypedArray(DeliveredReceipt.CREATOR)));
        this.setSeenReceipts(Arrays.asList(parcel.createTypedArray(SeenReceipt.CREATOR)));
    }

    //**********************************************************************************************
    //*                                                                                            *
    //*                         Public Interface of Event Object                                 *
    //*                                                                                            *
    //**********************************************************************************************

    public abstract EventType getType();

    /**
     * Get the event id of this message.
     *
     * @return unique id of this event.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the timestamp at which this message was received.
     *
     * @return The received timestamp.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Get the delete timestamp of this message,
     * if message was deleted already, otherwise is NULL.
     *
     * @return The deleted timestamp.
     */
    public Date getDeletedTimestamp() {
        return deletedTimestamp;
    }

    /**
     * Get the list of seen receipts for this event.
     * Search the list for certain {@link SeenReceipt#getMember()} if needed.
     *
     * @return A list of seen receipts.
     */
    public Collection<SeenReceipt> getSeenReceipts() {
        return this.seenReceiptList;
    }

    /**
     * Get the list of delivered receipts for this event.
     *
     * @return A list of delivery receipts.
     */
    public Collection<DeliveredReceipt> getDeliveredReceipts() {
        return this.deliveredReceipts;
    }

    /**
     * Get the sender of this message.
     *
     * @return The member that has sent this message.
     */
    public Member getMember() {
        return this.member;
    }

    /**
     * Get the parent conversation of this event.
     *
     * @return The conversation.
     */
    public Conversation getConversation() {
        return this.conversation;
    }

    /**
     * Marks a message event as seen.
     * Flag an {@link Event} as seen by the current member.
     * Event cannot be un-seen.
     *
     * @param listener     The listener in charge of dispatching the completion result.
     */
    public void markAsSeen(RequestHandler<SeenReceipt> listener) {
        if (listener != null) {
            if (this.deletedTimestamp != null|| isMarkedAsSeen())
                listener.onError(
                        new NexmoAPIError(NexmoAPIError.INVALID_ACTION, this.getConversation().getConversationId(), "User cannot mark as seen this event anymore."));
            else if (conversation.getSignallingChannel().getLoggedInUser() == null)
                listener.onError(NexmoAPIError.noUserLoggedInForConversation(this.getConversation().getConversationId()));
            else
                conversation.getSignallingChannel().sendSeenEvent(this, listener);
        }
    }

    /**
     * Delete a message.
     * @param eventDeleteListener The listener in charge of dispatching the completion result.
     */
    public void delete(RequestHandler<Void> eventDeleteListener){
        if (eventDeleteListener == null)
            Log.d(TAG, "Listener is mandatory");
        else if (this.conversation.getSelf() == null)
            eventDeleteListener.onError(NexmoAPIError.noUserLoggedInForConversation(this.getConversation().getConversationId()));
        else if (this.deletedTimestamp != null)
            eventDeleteListener.onError(new NexmoAPIError(NexmoAPIError.INVALID_ACTION, this.getConversation().getConversationId(), "This message was already deleted"));
        else
            this.conversation.getSignallingChannel().deleteEvent(this.conversation, this, eventDeleteListener);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeSerializable(getTimestamp());
        dest.writeSerializable(getDeletedTimestamp());
        dest.writeParcelable(getMember(), 0);
        dest.writeTypedArray(getDeliveredReceipts().toArray(new DeliveredReceipt[0]), 0);
        dest.writeTypedArray(getSeenReceipts().toArray(new SeenReceipt[0]), 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event)) return false;

        Event Event = (Event) o;

        if (!id.equals(Event.id)) return false;

        if ((timestamp != null && Event.timestamp != null && !timestamp.equals(Event.timestamp)))
            return false;
        if (deletedTimestamp != null ? !deletedTimestamp.equals(Event.deletedTimestamp) : Event.deletedTimestamp != null)
            return false;
        if (!member.equals(Event.member)) return false;
        return conversation.equals(Event.conversation);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (deletedTimestamp != null ? deletedTimestamp.hashCode() : 0);
        if(member!= null) result = 31 * result + member.hashCode();
        if(conversation != null) result = 31 * result + conversation.hashCode();
        return result;
    }

    public static Event fromCursor(Cursor cursor, Conversation conversation) {
        if (cursor == null) return null;

        Date timestamp = null, deletedTimestamp = null, joinedAt = null, invitedAt = null, leftAt = null;

        try {
            timestamp = DateUtil.formatIso8601DateString(cursor.getString(cursor.getColumnIndex(COLUMN_TIMESTAMP)));
        } catch (ParseException e) {
            Log.d(TAG, "Event fromCursor: wrong date format - created timestamp");
        }

        try{
            deletedTimestamp = DateUtil.formatIso8601DateString(cursor.getString(cursor.getColumnIndex(COLUMN_DELETED_TIMESTAMP)));
        } catch (ParseException e) {
            Log.d(TAG, "Event fromCursor: wrong date format - deleted timestamp");
        }
        
        try{
            joinedAt = DateUtil.formatIso8601DateString(cursor.getString(cursor.getColumnIndex(COLUMN_JOINEDAT)));
        } catch (ParseException e) {
            Log.d(TAG, "Event fromCursor: wrong date format - joined timestamp");
        }

        try{
            invitedAt = DateUtil.formatIso8601DateString(cursor.getString(cursor.getColumnIndex(COLUMN_INVITEDAT)));
        } catch (ParseException e) {
            Log.d(TAG, "Event fromCursor: wrong date format - invited timestamp");
        }

        try{
            leftAt = DateUtil.formatIso8601DateString(cursor.getString(cursor.getColumnIndex(COLUMN_LEFTAT)));
        } catch (ParseException e) {
            Log.d(TAG, "Event fromCursor: wrong date format - left timestamp");
        }

        Member.STATE state = Member.STATE.fromId(cursor.getString(cursor.getColumnIndex(COLUMN_STATE)));
        Member self = new Member(cursor.getString(cursor.getColumnIndex(COLUMN_USER_ID)),
                cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME)),
                cursor.getString(cursor.getColumnIndex(COLUMN_MEMBER_ID)),
                joinedAt, invitedAt, leftAt, state);

        String text = cursor.getString(cursor.getColumnIndex(COLUMN_TEXT));
        String eventId = cursor.getString(cursor.getColumnIndex(COLUMN_EVENT_ID));
        String typeString = cursor.getString(cursor.getColumnIndex(COLUMN_MESSAGE_TYPE));

        String deliveredReceiptsJson = cursor.getString(cursor.getColumnIndex(COLUMN_DELIVERED_RECEIPTS));
        String seenReceiptsJson = cursor.getString(cursor.getColumnIndex(COLUMN_SEEN_RECEIPTS));

        List<SeenReceipt> seenReceipts = new ArrayList<>();
        List<DeliveredReceipt> deliveredReceipts = new ArrayList<>();
        if (!TextUtils.isEmpty(deliveredReceiptsJson))
            try {
                deliveredReceipts = ReceiptRecordUtil.parseDeliveryReceiptHistory(conversation, eventId, new JSONObject(deliveredReceiptsJson));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(TAG, "cannot parse delivery receipt records for this event");
            }

        if (!TextUtils.isEmpty(seenReceiptsJson))
            try {
                seenReceipts = ReceiptRecordUtil.parseSeenReceiptHistory(conversation, eventId, new JSONObject(seenReceiptsJson));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(TAG, "cannot parse seen receipt records for this event");
            }

        switch(EventType.valueOf(typeString)) {
            case TEXT: {
                return new Text(text, eventId, timestamp, self, conversation, deletedTimestamp, seenReceipts, deliveredReceipts);
            }
            case IMAGE: {
                if (deletedTimestamp != null) {
                    return new Image(eventId, timestamp, self, deletedTimestamp, conversation, seenReceipts, deliveredReceipts);
                } else {
                    try {
                        JSONObject representationsJson = new JSONObject(cursor.getString(cursor.getColumnIndex(COLUMN_IMAGE_REPRESENTATIONS)));

                        JSONObject representations = representationsJson.getJSONObject("representations");
                        JSONObject originalJson = representations.getJSONObject("original");
                        JSONObject mediumJson = representations.getJSONObject("medium");
                        JSONObject thumbnailJson = representations.getJSONObject("thumbnail");

                        ImageRepresentation original = ImageRepresentation.fromJson(ImageRepresentation.TYPE.ORIGINAL, originalJson);
                        ImageRepresentation medium = ImageRepresentation.fromJson(ImageRepresentation.TYPE.MEDIUM, mediumJson);
                        ImageRepresentation thumbnail = ImageRepresentation.fromJson(ImageRepresentation.TYPE.THUMBNAIL, thumbnailJson);

                        return new Image(eventId, timestamp, self, null, conversation, original, medium, thumbnail, seenReceipts, deliveredReceipts);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return new Image(eventId, timestamp, self, null, conversation, seenReceipts, deliveredReceipts);
                    }
                }
            }
            case MEMBER_MEDIA: {
                boolean isAudioEnabled = cursor.getInt(cursor.getColumnIndex(COLUMN_MEMBER_MEDIA_ENABLED)) == 1;
                return new MemberMedia(eventId, self, conversation, isAudioEnabled, timestamp);
            }
            default:
                return null;
        }
    }

    public static Event fromJson(Conversation conversation, JSONObject messageObject) throws JSONException {
        JSONObject body = messageObject.getJSONObject("body");

        String eventId = messageObject.getString("id");
        String memberId = messageObject.getString("from");
        Member member = conversation.getMember(memberId);
        if (member == null) {
            Log.d(TAG, "Event.fromJson out-of-sync members");
            return null;
        }

        Date timestamp = DateUtil.parseDateFromJson(messageObject, "timestamp");
        List<SeenReceipt> seenReceiptList = new ArrayList<>();
        List<DeliveredReceipt> deliveredReceiptList = new ArrayList<>();

        Date deletedTimestamp = null;
        if(body.has("timestamp") && body.get("timestamp") instanceof JSONObject && ((JSONObject) body.get("timestamp")).has("deleted")) {
            JSONObject timestampJson = body.getJSONObject("timestamp");
            deletedTimestamp = DateUtil.parseDateFromJson(timestampJson, "deleted");
        }

        String eventType = messageObject.optString("type");
        if (TextUtils.isEmpty(eventType)) {
            if (body.has("text"))
                eventType = "text";
            else
                eventType = "image";
         }

        Event newEvent;
         switch(eventType) {
             case "text": {
                 String payload = body.optString("text");
                 newEvent = new Text(payload, eventId, timestamp, member, conversation, deletedTimestamp,
                         seenReceiptList, deliveredReceiptList);
                 break;
             }
             case "image": {
                 if (deletedTimestamp != null) {
                     newEvent = new Image(eventId, timestamp, member, deletedTimestamp, conversation, seenReceiptList, deliveredReceiptList);
                 } else {
                     JSONObject representations = body.getJSONObject("representations");
                     JSONObject originalJson = representations.getJSONObject("original");
                     JSONObject mediumJson = representations.getJSONObject("medium");
                     JSONObject thumbnailJson = representations.getJSONObject("thumbnail");

                     ImageRepresentation original = ImageRepresentation.fromJson(ImageRepresentation.TYPE.ORIGINAL, originalJson);
                     ImageRepresentation medium = ImageRepresentation.fromJson(ImageRepresentation.TYPE.MEDIUM, mediumJson);
                     ImageRepresentation thumbnail = ImageRepresentation.fromJson(ImageRepresentation.TYPE.THUMBNAIL, thumbnailJson);

                     newEvent =  new Image(eventId, timestamp, member, null, conversation,
                             original, medium, thumbnail, seenReceiptList, deliveredReceiptList);
                 }
                 break;
             }
             case "member:media": {
                 boolean isAudioEnabled = body.optBoolean("audio");
                 newEvent = new MemberMedia(eventId, member, conversation, isAudioEnabled, timestamp);
                 break;
             }
             default: {
                 Log.d(TAG, "Invalid message type: " + eventType);
                 return null;
             }
        }
        conversation.addEvent(newEvent);

        if (messageObject.has("state") && messageObject.get("state") instanceof JSONObject) {
            JSONObject stateJson = messageObject.getJSONObject("state");

            seenReceiptList = ReceiptRecordUtil.parseSeenReceiptHistory(conversation, eventId, stateJson);
            deliveredReceiptList = ReceiptRecordUtil.parseDeliveryReceiptHistory(conversation, eventId, stateJson);

            newEvent.setSeenReceipts(seenReceiptList);
            newEvent.setDeliveryReceipts(deliveredReceiptList);
        }
        else
            Log.d(TAG, "no receipt records for this event");


        return newEvent;
    }

    //**********************************************************************************************
    //*                                                                                            *
    //*              Package level (access) & Private operations of Event Object                 *
    //*                                                                                            *
    //**********************************************************************************************

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    synchronized void setMember(Member member) {
        this.member = member;
    }

    void setId(String id) {
        this.id = id;
    }

    void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    synchronized void setDeletedTimestamp(Date deletedTimestamp){
        this.deletedTimestamp = deletedTimestamp;
    }

    void addSeenReceipt(SeenReceipt seenReceipt) {
        seenReceiptList.add(seenReceipt);
    }

    void addDeliveredReceipt(DeliveredReceipt deliveredReceipt) {
        deliveredReceipts.add(deliveredReceipt);
    }

    void setDeliveryReceipts(Collection<DeliveredReceipt> deliveryReceipts) {
        if (deliveryReceipts != null)
            this.deliveredReceipts = new HashSet<>(deliveryReceipts);
    }

    void setSeenReceipts(Collection<SeenReceipt> seenReceipts) {
        if (seenReceipts !=  null)
            this.seenReceiptList = new HashSet<>(seenReceipts);
    }

    void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    //mark event as DELIVERED only once.
    //mark event as DELIVERED if it belongs to other members.
    boolean isReadyForMarkedAsDelivered() {
        return (!isEventMarkedAsDeliveredByUser() &&
                !isOwnMessage() &&
                (this.getType() == EventType.TEXT || this.getType() == EventType.IMAGE));
    }

    /**
     * Check locally if this event was already marked as delivered.
     *
     * @return true if event was already marked as DELIVERED by self.
     */
    private boolean isEventMarkedAsDeliveredByUser() {
        if (this.deliveredReceipts != null) {
            for(DeliveredReceipt receipt : deliveredReceipts) {
                Member self = getConversation().getSelf();
                if (self != null && TextUtils.equals(receipt.getMember().getMemberId(), self.getMemberId()))
                    return true;
            }
        }

        return false;
    }

    /**
     * Check locally if event belongs to user.
     *
     * @return true if event belong to current user.
     */
    private boolean isOwnMessage() {
        User self = conversation.getSignallingChannel().getLoggedInUser();
        if (self != null && getMember() != null)//sometimes userId = null
            return (getMember().getUserId().equals(self.getUserId()));

        return false;
    }

    private boolean isMarkedAsSeen() {
        synchronized(seenReceiptList) {
            for (SeenReceipt seenReceipt : seenReceiptList) {
                if (TextUtils.equals(seenReceipt.getMember().getMemberId(), conversation.getMemberId()))
                    return true;
            }
        }

        return false;
    }

}

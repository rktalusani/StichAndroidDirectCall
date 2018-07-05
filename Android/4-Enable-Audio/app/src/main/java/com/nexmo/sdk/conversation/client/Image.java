/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.client;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.nexmo.sdk.conversation.client.event.NexmoAPIError;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import com.nexmo.sdk.conversation.client.event.EventType;
import com.nexmo.sdk.conversation.core.util.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Date;

/**
 * Send an image to a Conversation and handle activity receipts.
 *
 * The following code example shows how to send an image to a Conversation:
 * <pre>
 * conversation.sendImage("imagePath", new ImageSendListener() {
 *          &#64;Override
 *          public void onSuccess(Event image) {
 *          }
 *
 *          &#64;Override
 *          public void onError(NexmoAPIError error) {
 *          }
 *      });
 * </pre>
 *
 * <p>
 * Once the image is uploaded, you will be able to access all 3 representations:
 * <ul>
 *     <li>Original at 100% size via {@link Image#getOriginal()}</li>
 *     <li>Medium at 50% size via {@link Image#getMedium()}</li>
 *     <li>Thumbnail at 10% size via {@link Image#getThumbnail()} </li>
 * </ul></p>
 *
 * <p>Each {@link ImageRepresentation}can be asynchronously downloaded using
 * {@link Image#download(ImageRepresentation.TYPE, RequestHandler)}</p>
 * <p> Alternatively, you can use any other 3rd party image handling library to download the
 * image, based on {@link ImageRepresentation#getUrl()}</p>
 *
 * <p> For listening to incoming/sent messages events, register using
 * {@link Conversation#messageEvent()} </p>
 *
 * <p>Note: No payload is supported for images.
 * Use {@link Text} instead for sending text messages.</p>
 *
 * @author emma tresanszki.
 */
public class Image extends Event implements Parcelable {
    private static final String TAG = Image.class.getSimpleName();

    private String localPath;
    private ImageRepresentation original;
    private ImageRepresentation medium;
    private ImageRepresentation thumbnail;

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Image(String localPath) {
        super();
        this.localPath = localPath;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Image(final String id, final Date timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Image(final String id, final Date timestamp, Member member){
        this(id, timestamp);
        this.member = member;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Image(final String id, final Date timestamp, Member member, Conversation conversation){
        this(id, timestamp, member);
        this.conversation = conversation;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Image(final String id, final Date timestamp, Member member, Conversation conversation,
                 ImageRepresentation original, ImageRepresentation medium, ImageRepresentation thumbnail){
        this(id, timestamp, member, conversation);
        setRepresentations(original, medium, thumbnail);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Image(final String id, final Date timestamp, Member member, Date deletedTimestamp){
        this(id, timestamp, member);
        this.deletedTimestamp = deletedTimestamp;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Image(final String id, final Date timestamp, Member member, Date deletedTimestamp, Conversation conversation){
        this(id, timestamp, member, deletedTimestamp);
        this.conversation = conversation;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Image(final String id, final Date timestamp, Member member, Date deletedTimestamp,
                 Collection<SeenReceipt> seenReceipts){
        this(id, timestamp, member, deletedTimestamp);
        this.setSeenReceipts(seenReceipts);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Image(final String id, final Date timestamp, Member member, Date deletedTimestamp, Conversation conversation,
                 Collection<SeenReceipt> seenReceipts){
        this(id, timestamp, member, deletedTimestamp, conversation);
        this.setSeenReceipts(seenReceipts);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Image(final String id, final Date timestamp, Member member, Date deletedTimestamp, Conversation conversation,
                 ImageRepresentation original, ImageRepresentation medium, ImageRepresentation thumbnail,
                 Collection<SeenReceipt> seenReceipts, Collection<DeliveredReceipt> deliveredReceipts){
        this(id, timestamp, member, conversation, original, medium, thumbnail);
        setSeenReceipts(seenReceipts);
        setDeliveryReceipts(deliveredReceipts);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Image(final String id, final Date timestamp, Member member, Date deletedTimestamp, Conversation conversation,
                 Collection<SeenReceipt> seenReceipts, Collection<DeliveredReceipt> deliveredReceipts){
        this(id, timestamp, member, deletedTimestamp, conversation, seenReceipts);
        this.setDeliveryReceipts(deliveredReceipts);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Image(Image message) {
        this(message.getId(), message.getTimestamp(), message.getMember(),
                message.getDeletedTimestamp(), message.getSeenReceipts());
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected Image(Parcel in) {
        super(in);
        this.original = in.readParcelable(ImageRepresentation.class.getClassLoader());
        this.medium = in.readParcelable(ImageRepresentation.class.getClassLoader());
        this.thumbnail = in.readParcelable(ImageRepresentation.class.getClassLoader());
    }

    public ImageRepresentation getOriginal(){
        return this.original;
    }

    public ImageRepresentation getMedium() {
        return this.medium;
    }

    public ImageRepresentation getThumbnail(){
        return this.thumbnail;
    }

    public ImageRepresentation getImageRepresentationByType(ImageRepresentation.TYPE type) {
        switch(type) {
            case ORIGINAL:
                return this.getOriginal();
            case MEDIUM:
                return this.getMedium();
            case THUMBNAIL:
                return this.getThumbnail();
            default:
                return null;
        }
    }

    void updateLocalFilePaths(final String localPath) {
        this.original.updateLocalFilePath(localPath);
        this.medium.updateLocalFilePath(localPath);
        this.thumbnail.updateLocalFilePath(localPath);
    }

    /**
     * Check if THUMBNAIL image representation was already downloaded on local storage.
     *
     * <p>Path: "storage/emulated/0/AppPackageName/Media/Images/IMG-imageTimestamp-imageRepresentationID-imageRepresentationType.jpg"
     * Where imageTimestamp is {@link Image#getTimestamp()} ,
     * imageRepresentationID is {@link ImageRepresentation#id} and imageRepresentationType is {@link ImageRepresentation#type}
     * </p>
     *
     */
    public boolean isDownloaded() {
        return (this.thumbnail != null && !TextUtils.isEmpty(this.thumbnail.getLocalFilePath()));
    }

    /**
     * Start downloading an image representation. The downloaded image will be cached to local storage.
     * Next time download is called, image will be fetched from local storage, if file still exists.
     * @param type Any of {@link ImageRepresentation.TYPE#ORIGINAL},
     * {@link ImageRepresentation.TYPE#MEDIUM}, or {@link ImageRepresentation.TYPE#THUMBNAIL}
     *
     * @param downloadListener The listener in charge of dispatching the completion result.
     *                         When onSuccess() is triggered, either the file, or the associated bitmap
     *                         can be accessed to update UI:
     *                         <ul>
     *                         <li>{@link ImageRepresentation#getBitmap()}. Note: bitmaps are recycled on
     *                         disconnection only, so make sure to recycle them when not used to avoid OutOfMemory.</li>
     *                         <li>{@link ImageRepresentation#getLocalFilePath()} to decode from file.</li>
     *                         </ul>
     */
    public void download(ImageRepresentation.TYPE type, final RequestHandler downloadListener) {
        SocketEventHandler socketEventHandler = this.conversation.getSignallingChannel().socketClient.getSocketEventHandler();

        /** Online/offline. Image was deleted, throw error. **/
        if (this.deletedTimestamp != null) {
            conversation.getSignallingChannel().getConversationClient().callUserCallback(new Runnable() {
                @Override
                public void run() {
                    downloadListener.onError(NexmoAPIError.invalidAction(conversation.getConversationId(), "Image was deleted"));
                }
            });
            return;}

        /** Image already cached. **/
        if (this.getImageRepresentationByType(type).localFileExists()) {
            conversation.getSignallingChannel().getConversationClient().callUserCallback(new Runnable() {
                @Override
                public void run() {
                    downloadListener.onSuccess(null);
                }
            });
        } /** Online work: image not cached, attempt to fetch from service **/
        else if (conversation.getSignallingChannel().getLoggedInUser() != null)
            socketEventHandler.downloadImageRepresentation(this, type, downloadListener);
        else
            conversation.getSignallingChannel().getConversationClient().callUserCallback(new Runnable() {
                @Override
                public void run() {
                    downloadListener.onError(NexmoAPIError.noUserLoggedInForConversation(conversation.getConversationId()));
                }
            });
    }

    @Override
    public EventType getType() {
        return EventType.IMAGE;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest,flags); //writes the id, timestamp, member and receipts
        dest.writeParcelable(this.original, flags);
        dest.writeParcelable(this.medium, flags);
        dest.writeParcelable(this.thumbnail, flags);
    }

    @Override
    public String toString() {
        return TAG +
                " .id: " + (this.id != null ? this.id : "") +
                " .Member: " + (this.getMember() != null ? this.getMember().toString() : "") +
                " .Timestamp: " + (this.getTimestamp() != null ? this.getTimestamp() : "") +
                " .Original representation: " + (this.original != null ? this.original.toString() : "") +
                " .Medium representation: " + (this.medium != null ? this.medium.toString() : "") +
                " .Thumbnail representation: " + (this.thumbnail != null ? this.thumbnail.toString() : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Image)) return false;
        if (!super.equals(o)) return false;

        Image image = (Image) o;

        if (original != null ? !original.equals(image.original) : image.original != null)
            return false;
        if (medium != null ? !medium.equals(image.medium) : image.medium != null) return false;
        return thumbnail != null ? thumbnail.equals(image.thumbnail) : image.thumbnail == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (original != null ? original.hashCode() : 0);
        result = 31 * result + (medium != null ? medium.hashCode() : 0);
        result = 31 * result + (thumbnail != null ? thumbnail.hashCode() : 0);
        return result;
    }

    public static Image fromPush(final String senderId, final String eventId, final JSONObject messageObject) throws JSONException {
        Date timestamp = DateUtil.parseDateFromJson(messageObject, "timestamp");
        JSONObject representations = messageObject.getJSONObject("body").getJSONObject("representations");

        JSONObject originalJson = representations.getJSONObject("original");
        JSONObject mediumJson = representations.getJSONObject("medium");
        JSONObject thumbnailJson = representations.getJSONObject("thumbnail");

        ImageRepresentation original = ImageRepresentation.fromJson(ImageRepresentation.TYPE.ORIGINAL, originalJson);
        ImageRepresentation medium = ImageRepresentation.fromJson(ImageRepresentation.TYPE.MEDIUM, mediumJson);
        ImageRepresentation thumbnail = ImageRepresentation.fromJson(ImageRepresentation.TYPE.THUMBNAIL, thumbnailJson);

        return new Image(eventId, timestamp, new Member(senderId), null, original, medium, thumbnail);
    }

    public static final Creator<Image> CREATOR = new Creator<Image>() {
        @Override
        public Image createFromParcel(Parcel in) {
            return new Image(in);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };

    void setRepresentations(ImageRepresentation original, ImageRepresentation medium,
                            ImageRepresentation thumbnail) {
        this.original = original;
        this.medium = medium;
        this.thumbnail = thumbnail;
    }

    void recycleBitmaps() {
      if (this.original != null) this.original.recycleBitmap();
      if (this.medium != null) this.medium.recycleBitmap();
      if (this.thumbnail != null ) this.thumbnail.recycleBitmap();
    }

}

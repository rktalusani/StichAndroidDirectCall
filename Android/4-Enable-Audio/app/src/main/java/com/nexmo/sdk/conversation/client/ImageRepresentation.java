/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.client;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Each image representation can be defined by type: {@link TYPE}.
 *
 * @author emma tresanszki.
 */
public class ImageRepresentation implements Parcelable {
    private static final String TAG = ImageRepresentation.class.getSimpleName();

    public enum TYPE {
        ORIGINAL,
        MEDIUM,
        THUMBNAIL
    }
    public TYPE type;
    private String id;
    private String url;
    private long size;
    private Bitmap bitmap;
    private String localFilePath;

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected ImageRepresentation(TYPE type, String id, String url, long size, String localFilePath) {
        this.type = type;
        this.id = id;
        this.url = url;
        this.size = size;
        this.localFilePath = localFilePath;
    }

    protected ImageRepresentation(Parcel in) {
        this.type = TYPE.valueOf(in.readString());
        this.id = in.readString();
        this.url = in.readString();
        this.size = in.readLong();
        this.localFilePath = in.readString();
    }

    /**
     * Get the Image representation url.
     *
     * <p>Image can be downloaded explicitly via
     * <ul>
     *     <li><{@link Image#download(TYPE, RequestHandler)} that uses okhttp3 library under the hood.</li>
     *     <li>Alternatively, download from this link using an http library of your own choosing.
     *     Attach mandatory Nexmo custom authorization headers to your request in order to download:
     *     {@link ConversationClient#ipsHeaders()}</li>
     * </ul>
     * </p>
     * @return The image representation url.
     */
    public String getUrl() {
        return this.url;
    }

    public long getSize() {
        return this.size;
    }

    /**
     * @hide
     * @return Image representation id.
     */
    public String getId() {
        return this.id;
    }

    /**
     * If image representation was downloaded, the bitmap can be used to update UI.
     * @return The bitmap of the image representation.
     */
    public Bitmap getBitmap() {
        return this.bitmap;
    }

    public String getLocalFilePath() {
        return this.localFilePath;
    }

    boolean localFileExists() {
        if (TextUtils.isEmpty(localFilePath)) return false;
        File file = new File(localFilePath);
        return file.exists();
    }

    void updateLocalFilePath(final String localFilePath) {
        this.localFilePath = localFilePath;
    }

    void updateUrl(final String url) {
        this.url = url;
    }

    void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public static final Creator<ImageRepresentation> CREATOR = new Creator<ImageRepresentation>() {
        @Override
        public ImageRepresentation createFromParcel(Parcel in) {
            return new ImageRepresentation(in);
        }

        @Override
        public ImageRepresentation[] newArray(int size) {
            return new ImageRepresentation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.type.toString());
        dest.writeString(this.id);
        dest.writeString(this.url);
        dest.writeLong(this.size);
        dest.writeString(this.localFilePath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageRepresentation)) return false;

        ImageRepresentation that = (ImageRepresentation) o;

        if (size != that.size) return false;
        if (type != that.type) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        return localFilePath != null ? localFilePath.equals(that.localFilePath) : that.localFilePath == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (localFilePath != null ? localFilePath.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return TAG + " type: " + this.type.toString() + ". id: " + (this.id != null ? this.id : "") +
                ".url: " + (this.url != null ? this.url : "") +
                ".size: " + this.size +
                ".localFilePath: " + (this.localFilePath != null ? this.localFilePath : "");
    }

    static ImageRepresentation fromJson(TYPE type, JSONObject body) throws JSONException {
        return new ImageRepresentation(type, body.getString("id"), body.getString("url"), body.getLong("size"), body.optString("localFilePath"));
    }

    /**
     * @hide
     * @return
     * @throws JSONException
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject representationJson = new JSONObject();
        representationJson.put("id", this.id);
        representationJson.put("size", this.size);
        representationJson.put("url", this.url);
        representationJson.put("localFilePath", this.localFilePath);

        return representationJson;
    }

    void recycleBitmap() {
        if (this.bitmap != null)
            this.bitmap.recycle();
    }
}

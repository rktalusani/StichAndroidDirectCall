/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.client;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A user is a person associated with your Application.
 * You populate the application users from your customer database using Application API.
 * You use this SDK to add and remove users to a Conversation. A member is a User who has joined a Conversation.
 * The following code example shows how to add a User to a Conversation:
 * <pre>
 * client.login(token, new RequestHandler<User> {
 *         &#64;Override
 *         public void onSuccess(User user) {
 *             Log.d(TAG, "onSuccess " + user.toString());
 *             self = user;
 *             // can update UI user info for ex.
 *         }
 *
 *         &#64;Override
 *         public void onError(NexmoAPIError error) {
 *         }
 *     });
 * </pre>
 * The login method accepts any unique String as a User ID. You need to validate each user against
 * your user DB before adding them to a Conversation.
 *
 * @author emma tresanszki.
 */
public class User implements Parcelable {

    private static final String TAG = User.class.getSimpleName();
    private String userId;
    private String name;
    private String displayName;
    private String imageUrl;

    private User(final String userId, final String name, final String displayName, final String imageUrl) {
        this.userId = userId;
        this.name = name;
        this.displayName = displayName;
        this.imageUrl = imageUrl;
    }

    public User(final String userId, final String name) {
        this(userId, name, "", "");
    }

    public User(User user) {
        this(user.getUserId(), user.getName(), user.displayName != null ? user.displayName : "", user.imageUrl != null ? user.imageUrl : "");
    }

    protected User(Parcel in) {
        this.userId = in.readString();
        this.name = in.readString();
        this.displayName = in.readString();
        this.imageUrl = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    protected User() {
    }

    public static User createUser(final String userId, final String name, final String displayName, final String imageUrl) {
        return new User(userId, name, displayName, imageUrl);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(name);
        dest.writeString(displayName);
        dest.writeString(imageUrl);
    }

    public String getName() {
        return this.name;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String toString(){
        return TAG +
                " name: " + (this.name!= null ? this.name: "") +
                ". userId: " + (this.userId != null ? this.userId : "") +
                ". displayName: " + (this.displayName != null ? this.displayName : "") +
                ". imageUrl: " + (this.imageUrl != null ? this.imageUrl : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return userId.equals(user.userId) && name.equals(user.name) && displayName.equals(user.displayName) && imageUrl.equals(user.imageUrl);
    }

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    /**
     * Construct User object from the network response
     * @param userObject network response
     * @throws JSONException if required fields aren't exist
     */
    public static User fromJson(JSONObject userObject) throws JSONException {
        return createUser(userObject.optString("user_id", userObject.optString("id")),
                userObject.getString("name"),
                userObject.optString("display_name"),
                userObject.optString("image_url"));
    }
}

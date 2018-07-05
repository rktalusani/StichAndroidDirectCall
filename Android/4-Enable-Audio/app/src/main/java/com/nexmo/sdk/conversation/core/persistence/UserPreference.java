package com.nexmo.sdk.conversation.core.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import com.nexmo.sdk.conversation.core.util.Log;

import com.nexmo.sdk.conversation.client.User;

/**
 * Retrieve and store current logged in user information to shared preference file.
 *
 * @author emma tresanszki.
 *
 * @hide
 */
public class UserPreference {
    static final String PREFERENCE_FILE = UserPreference.class.getSimpleName();
    static final String USER_ID = "userId";
    static final String USERNAME = "username";

    // override last persisted user so we can unlock the cache after login.
    public static void saveUser(final User user, Context context) {
        Log.d(PREFERENCE_FILE, "saveUser");
        if (context == null)
            return;

        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(PREFERENCE_FILE, 0);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(USER_ID, user.getUserId());
        editor.putString(USERNAME, user.getName());
        editor.commit();
        Log.d(PREFERENCE_FILE, "saved: " + getLoggedInUser(context).toString());
    }

    public static void clearUser(Context context) {
        if (context == null)
            return;

        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(PREFERENCE_FILE, 0);
        SharedPreferences.Editor editor = preferences.edit();

        editor.remove(USER_ID);
        editor.remove(USERNAME);
        editor.commit();
    }

    public static User getLoggedInUser(Context context) {
        if (context == null)
            return null;

        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(PREFERENCE_FILE, 0);
        if (containsUser(context))
            return new User(preferences.getString(USER_ID, null), preferences.getString(USERNAME, null));

        return null;
    }

    public static boolean isLastLoggedInUser(final User user, Context context) {
        if (context == null)
            return false;

        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(PREFERENCE_FILE, 0);

        if (containsUser(context))
            return (preferences.getString(USER_ID, null).equals(user.getUserId())) &&
                    (preferences.getString(USERNAME, null).equals(user.getName()));

        return false;
    }

    public static boolean containsUser(Context context) {
        if (context == null)
            return false;

        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(PREFERENCE_FILE, 0);

        return preferences.contains(USER_ID) && preferences.contains(USERNAME);
    }
}

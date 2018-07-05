/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.Locale;
import java.util.UUID;

/**
 * Utility class for accessing device properties.
 *
 * @author emma tresanszki.
 */
public class DeviceProperties {

    private static final String TAG = DeviceProperties.class.getSimpleName();
    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";

    /**
     * Get the Android API version.
     *
     * @return The Android API version.
     */
    public static String getApiLevel() {
        return Build.VERSION.RELEASE;
    }

    /**
     * A randomly generated 64-bit number on the device's first boot that remains constant
     * for the lifetime of the device.
     *
     * @return The device ANDROID_ID, or null if the context is not supplied.
     * @deprecated
     */
    public static String getAndroid_ID(Context context) {
        if (context != null)
            return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return null;
    }

    /**
     * Get the user's preferred locale language. The format is language code and country code, separated by dash.
     * <p> Since the user's locale changes dynamically, avoid caching this value.
     *
     * @return The user's preferred language.
     */
    public static String getLanguage() {
        String language = Locale.getDefault().toString();
        if (!TextUtils.isEmpty(language) && language.indexOf("_") > 1) {
            return language.replace("_", "-");
        }
        return null;
    }

    public static String getUserid(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.apply();
            }
        }
        return uniqueID;
    }

    //reset userid, expected to be called as part of Logout clean up.
    public static void resetUserId(Context context) {
        uniqueID = null;
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                PREF_UNIQUE_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.remove(PREF_UNIQUE_ID);
        editor.apply();
    }

}

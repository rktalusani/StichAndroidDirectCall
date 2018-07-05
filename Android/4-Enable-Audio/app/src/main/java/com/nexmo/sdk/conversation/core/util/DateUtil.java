/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Common date utilities.
 *
 * @author emma tresanszki.
 *
 * @hide
 */
public class DateUtil {

    static final DateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());

    static final DateFormat SIMPLIFIED_DATE_FORMAT =
            new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

    static final ThreadLocal<DateFormat> ISO_8601_DATE_FORMAT =
            new ThreadLocal<DateFormat>() {
                @Override
                protected DateFormat initialValue() {
                    DATE_FORMAT.setLenient(false);
                    DATE_FORMAT.setTimeZone(Calendar.getInstance().getTimeZone());
                    return DATE_FORMAT;
                }
            };

    static final ThreadLocal<DateFormat> IMAGE_NAMING_DATE_FORMAT =
            new ThreadLocal<DateFormat>() {
                @Override
                protected DateFormat initialValue() {
                    SIMPLIFIED_DATE_FORMAT.setLenient(false);
                    SIMPLIFIED_DATE_FORMAT.setTimeZone(Calendar.getInstance().getTimeZone());
                    return SIMPLIFIED_DATE_FORMAT;
                }
            };

    public static Date formatIso8601DateString(String timestamp) throws ParseException {
        if (timestamp == null)
            return null;
        return ISO_8601_DATE_FORMAT.get().parse(timestamp);
    }

    public static String formatIso8601DateString(Date date) {
        return (date != null ? ISO_8601_DATE_FORMAT.get().format(date) : null);
    }

    public static Date parseDateFromJson(final JSONObject body, final String key) throws JSONException {
        try {
            return formatIso8601DateString(body.getString(key));
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String formatImageNamingDateString(Date date) {
        return (date != null ? IMAGE_NAMING_DATE_FORMAT.get().format(date) : null);
    }
}

package com.nexmo.sdk.conversation.core.util;

/**
 * Based on {@link android.util.Log}
 * @hide
 */
public final class Log  {
    private static int logLevel = android.util.Log.DEBUG;

    private Log() {
    }

    private static boolean shouldIgnore(int level) {
        return (level <= logLevel);
    }

    public static void setLevel(int level) {
        Log.logLevel = level;
    }

    /**
     * Send a {@link android.util.Log#VERBOSE} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void v(String tag, String msg) {
        if (shouldIgnore(android.util.Log.VERBOSE)) return;
        android.util.Log.v(tag, msg);
    }

    /**
     * Send a {@link android.util.Log#VERBOSE} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void v(String tag, String msg, Throwable tr) {
        if (shouldIgnore(android.util.Log.VERBOSE)) return;
        android.util.Log.v(tag, msg, tr);
    }

    /**
     * Send a {@link android.util.Log#DEBUG} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void d(String tag, String msg) {
        if (shouldIgnore(android.util.Log.DEBUG)) return;
        android.util.Log.d(tag, msg);
    }

    /**
     * Send a {@link android.util.Log#DEBUG} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void d(String tag, String msg, Throwable tr) {
        if (shouldIgnore(android.util.Log.DEBUG)) return;
        android.util.Log.v(tag, msg, tr);
    }

    /**
     * Send an {@link android.util.Log#INFO} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void i(String tag, String msg) {
        if (shouldIgnore(android.util.Log.INFO)) return;
        android.util.Log.i(tag, msg);
    }

    /**
     * Send a {@link android.util.Log#INFO} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void i(String tag, String msg, Throwable tr) {
        if (shouldIgnore(android.util.Log.INFO)) return;
        android.util.Log.i(tag, msg);
    }

    /**
     * Send a {@link android.util.Log#WARN} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void w(String tag, String msg) {
        if (shouldIgnore(android.util.Log.WARN)) return;
        android.util.Log.w(tag, msg);
    }

    /**
     * Send a {@link android.util.Log#WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void w(String tag, String msg, Throwable tr) {
        if (shouldIgnore(android.util.Log.WARN)) return;
        android.util.Log.w(tag, msg);
    }


    /*
     * Send a {@link android.util.Log#WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    public static void w(String tag, Throwable tr) {
        if (shouldIgnore(android.util.Log.WARN)) return;
        android.util.Log.w(tag, tr);
    }

    /**
     * Send an {@link android.util.Log#ERROR} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void e(String tag, String msg) {
        if (shouldIgnore(android.util.Log.ERROR)) return;
        android.util.Log.e(tag, msg);
    }

    /**
     * Send a {@link android.util.Log#ERROR} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void e(String tag, String msg, Throwable tr) {
        if (shouldIgnore(android.util.Log.ERROR)) return;
        android.util.Log.e(tag, msg, tr);
    }

    /**
     * What a Terrible Failure: Report a condition that should never happen.
     * The error will always be logged at level ASSERT with the call stack.
     * Depending on system configuration, a report may be added to the
     * {@link android.os.DropBoxManager} and/or the process may be terminated
     * immediately with an error dialog.
     * @param tag Used to identify the source of a log message.
     * @param msg The message you would like logged.
     */
    public static void wtf(String tag, String msg) {
        if (shouldIgnore(android.util.Log.ERROR)) return;
        android.util.Log.wtf(tag, msg);
    }

    /**
     * What a Terrible Failure: Report an exception that should never happen.
     * Similar to {@link android.util.Log#wtf(String, String)}, with an exception to log.
     * @param tag Used to identify the source of a log message.
     * @param tr An exception to log.
     */
    public static void wtf(String tag, Throwable tr) {
        if (shouldIgnore(android.util.Log.ERROR)) return;
        android.util.Log.wtf(tag, tr);
    }

    /**
     * What a Terrible Failure: Report an exception that should never happen.
     * Similar to {@link android.util.Log#wtf(String, Throwable)}, with a message as well.
     * @param tag Used to identify the source of a log message.
     * @param msg The message you would like logged.
     * @param tr An exception to log.  May be null.
     */
    public static void wtf(String tag, String msg, Throwable tr) {
        if (shouldIgnore(android.util.Log.ERROR)) return;
        android.util.Log.wtf(tag, msg, tr);
    }


}

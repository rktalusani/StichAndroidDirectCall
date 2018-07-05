/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.config;

/**
 * Defaults.
 *
 * @author emma tresanszki.
 */
public class Defaults {

    public static final int CONNECTION_TIMEOUT = 15 * 1000;
    public static final int CONNECTION_READ_TIMEOUT = 10 * 1000;
    public static final int MAX_ALLOWABLE_TIME_DELTA = 5 * 60 * 1000;
    public static final int RECONNECT_DELAY = 10 * 1000;
    public static final double RECONNECT_RANDOMIZATION_FACTOR = 0.2;
    public static final int RECONNECT_DELAY_THRESHOLD = 120 * 1000;
    public static final int BITMAP_COMPRESS_QUALITY = 90;
    public static final long TYPING_TIMER_LENGTH = 1000l;
    public static final long MAX_CONVERSATION_LIST_SIZE = 150;
}

/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.networking;

/**
 * Networking constants.
 *
 * @author emma tresanszki.
 */
public class Constants {
    public static final String CUSTOM_HEADER_AUTHORIZATION = "Authorization";
    public static final String CUSTOM_HEADER_VALUE = "Bearer ";

    public static final String FORM_KEY_FILE = "file";
    public static final String FORM_KEY_QUALITY_RATIO = "quality_ratio";
    public static final String FORM_KEY_MEDIUM_RATIO = "medium_size_ratio";
    public static final String FORM_KEY_THUMBNAIL_RATIO = "thumbnail_size_ratio";

    public static final long MAX_ALLOWED_FILESIZE = 15 * 1024 * 1024;
    public static final String FORM_VALUE_QUALITY = "100";
    public static final String FORM_VALUE_MEDIUM_SIZE = "50";
    public static final String FORM_VALUE_THUMBNAIL_SIZE = "30";

    public static final String CALL_PREFIX_NAME = "CALL_";
    public static final String CALL_NAME_SEPARATOR = "_";
}

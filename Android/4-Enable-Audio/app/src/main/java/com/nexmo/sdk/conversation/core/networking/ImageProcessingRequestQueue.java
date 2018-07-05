/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.networking;


import okhttp3.OkHttpClient;

/**
 * Image upload queue for Image Processing service.
 *
 * @author emma tresanszki.
 *
 * @hide
 */
public class ImageProcessingRequestQueue {
    private static final String TAG = ImageProcessingRequestQueue.class.getSimpleName();
    private static ImageProcessingRequestQueue sInstance;
    private OkHttpClient client ;

    private ImageProcessingRequestQueue(){
        this.client = getClient();
    }

    public static synchronized ImageProcessingRequestQueue getInstance() {
        if (sInstance == null)
            sInstance = new ImageProcessingRequestQueue();

        return sInstance;
    }

    public OkHttpClient getClient() {
        if (this.client == null)
            this.client = new OkHttpClient();

        return this.client;
    }

}

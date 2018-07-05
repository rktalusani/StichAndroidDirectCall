/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.networking;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.nexmo.sdk.conversation.core.util.Log;

import com.nexmo.sdk.conversation.client.ImageRepresentation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Image downloader.
 *
 * @hide
 */
public class ImageDownloader {
    private static final String TAG = ImageDownloader.class.getSimpleName();
    //todo provide a cancel method

    public static void downloadImage(final ImageRepresentation imageRepresentation, Callback callback, String token) {
        Log.d(TAG, "downloadImage ");

        final Request request = new Request.Builder()
                .url(imageRepresentation.getUrl())
                .addHeader(Constants.CUSTOM_HEADER_AUTHORIZATION, "Bearer " + token)
                .build();

        ImageProcessingRequestQueue.getInstance().getClient().newCall(request).enqueue(callback);
    }

    public static Bitmap decodeImage(Response response) throws IOException {

        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

        InputStream inputStream = response.body().byteStream();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n = 0;
        while (-1!=(n=inputStream.read(buf)))
        {
            out.write(buf, 0, n);
        }
        out.close();
        inputStream.close();

        byte[] responseByteArray = out.toByteArray();
        return BitmapFactory.decodeByteArray(responseByteArray, 0, responseByteArray.length);
    }

}

/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.networking;

import com.nexmo.sdk.conversation.core.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Image uploader.
 *
 * @hide
 */
public class ImageUploader {
    private static final String TAG = ImageUploader.class.getSimpleName();

    /**
     * Helper method for image uploading
     *
     * @param imagePath file to be uploaded
     * @param callback listener for notifications
     * @return Call from okhttp
     */
    public static CancelableCall uploadImage(final String imagePath, final Callback callback, String urlIPS, String token) {
        Log.d(TAG, "uploadImage " + imagePath);
        final File file = new File(imagePath);
        String fileName = file.getName();
        final AtomicBoolean inProgress = new AtomicBoolean(true);

        final InterruptableFileRequestBody fileRequestBody = new InterruptableFileRequestBody(file, "image/jpeg");
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(Constants.FORM_KEY_FILE, fileName, fileRequestBody)
                .addFormDataPart(Constants.FORM_KEY_QUALITY_RATIO, Constants.FORM_VALUE_QUALITY)
                .addFormDataPart(Constants.FORM_KEY_MEDIUM_RATIO, Constants.FORM_VALUE_MEDIUM_SIZE)
                .addFormDataPart(Constants.FORM_KEY_THUMBNAIL_RATIO, Constants.FORM_VALUE_THUMBNAIL_SIZE)
                .build();

        final Request request = new Request.Builder()
                .url(urlIPS)
                .addHeader(Constants.CUSTOM_HEADER_AUTHORIZATION, Constants.CUSTOM_HEADER_VALUE + token)
                .post(requestBody)
                .build();

        final OkHttpClient okHttpClient = new OkHttpClient();
        final Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                inProgress.set(false);
                callback.onFailure(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                inProgress.set(false);
                callback.onResponse(call, response);
            }
        });
        return new CancelableCall() {
            @Override
            public void cancel() {
                call.cancel();
                fileRequestBody.cancel();
            }

            @Override
            public float progress() {
                return fileRequestBody.getProgress() * 1F / fileRequestBody.contentLength();
            }

            @Override
            public boolean inProgress() {
                return inProgress.get();
            }
        };
    }

    public interface CancelableCall {
        /**
         * Cancel current request which is pending or in progress.
         * SDK try to it's best to cancel but can't guarantee successful result
         */
        void cancel();

        /**
         * Current progress. During upload process this value might be ahead of actual progress due buffer sizes
         * @return picture upload progress, value in the range [0.0, 1.0]
         */
        float progress();

        /**
         * Current status
         * @return true if operation is scheduled or in progress, false if call has been
         * finished by any reason including termination
         */
        boolean inProgress();
    }

}
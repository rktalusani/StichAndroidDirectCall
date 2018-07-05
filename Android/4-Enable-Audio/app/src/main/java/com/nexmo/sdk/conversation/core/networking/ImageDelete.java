package com.nexmo.sdk.conversation.core.networking;

import com.nexmo.sdk.conversation.core.util.Log;

import okhttp3.Callback;
import okhttp3.Request;

/**
 * Image delete helper.
 * @author emma tresanszki.
 *
 * @hide
 */
public class ImageDelete {
    private static final String TAG = ImageDelete.class.getSimpleName();

    public static void deleteImage(final String imagePath, final Callback callback, String token) {
        Log.d(TAG, "deleteImage: " + imagePath);

        final Request request = new Request.Builder()
                .url(imagePath)
                //.url(imagePath.contains(BuildConfig.IPS_ENDPOINT_DEFAULT) ? deleteEndpoint(imagePath) : imagePath)
                .addHeader(Constants.CUSTOM_HEADER_AUTHORIZATION, Constants.CUSTOM_HEADER_VALUE + token)
                .delete()
                .build();

        ImageProcessingRequestQueue.getInstance().getClient().newCall(request).enqueue(callback);
    }

    // might need to twitch PROD endpoint for DELETE operation: suffix with 'v3/media' instead of 'vi/files'
    private static String deleteEndpoint(String mediaUrl) {
       return  mediaUrl.replace("v1/files", "v3/media");
    }
}

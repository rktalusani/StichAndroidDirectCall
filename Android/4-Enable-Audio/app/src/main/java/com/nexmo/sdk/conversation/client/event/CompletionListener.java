package com.nexmo.sdk.conversation.client.event;

import android.os.Handler;

/**
 * Completion listener to notify upon onSuccess or onError on the main thread or background thread.
 * @author emma tresanszki.
 * @hide
 */
public class CompletionListener<T> {
    private final RequestHandler<T> listener;
    private Handler handler;


    CompletionListener(RequestHandler listener, Handler handler) {
        this.listener = listener;
        this.handler = handler;
    }

    
}

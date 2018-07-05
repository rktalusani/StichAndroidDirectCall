package com.nexmo.sdk.conversation.client;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.nexmo.sdk.conversation.client.event.ResultListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author rux
 * @hide
 */

public class EventSource<T> {
    private List<ResultListener<T>> subscriptions = Collections.synchronizedList(new ArrayList<ResultListener<T>>());
    private Handler handler;

    /**
     * If handler is null then all callbacks will be executed immediately on current calee's thread
     * @param handler
     */
    public EventSource(Handler handler) {
        this.handler = handler;
    }

    /**
     * Add listener for this event source
     */
    public Subscription<T> add(@NonNull ResultListener<T> listener) {
        this.subscriptions.add(listener);
        return new Subscription<>(listener, this);
    }

    /**
     * Remove subscription from this event source
     */
    public void remove(@NonNull Subscription<T> subscription) {
        this.subscriptions.remove(subscription.getListener());
    }

    /**
     * Remove listener from this event source
     */
    public void remove(@NonNull ResultListener<T> listener) {
        this.subscriptions.remove(listener);
    }


    /**
     * Does actual notification
     * If looper is used then each listener will be notified as separated looper message
     */
    void notifySubscriptions(final T value) {
        for (final ResultListener<T> subscription : subscriptions) {
            if (this.handler == null) {
                subscription.onSuccess(value);
                continue;
            }
            this.handler.post(new Runnable() {
                @Override
                public void run() {
                    subscription.onSuccess(value);
                }
            });
        }
    }


}

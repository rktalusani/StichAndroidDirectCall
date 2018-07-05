package com.nexmo.sdk.conversation.client;

import com.nexmo.sdk.conversation.client.event.ResultListener;

import java.util.Collection;

/**
 * @author rux
 * @hide
 */
public class Subscription<T> {
    private final ResultListener<T> listener;
    private final EventSource<T> eventSource;

    Subscription(ResultListener<T> listener, EventSource<T> eventSource) {
        this.listener = listener;
        this.eventSource = eventSource;
    }

    public ResultListener<T> getListener() {
        return listener;
    }

    /**
     * Remove given subscription from EventSource
     */
    public void unsubscribe() {
        this.eventSource.remove(this.listener);
    }

    /**
     * Add current subscription to the given list of subscriptions. It makes it easier to manage lifecycle
     * For ex:
     * <pre>
     * {@code
     *  object.someEvent()
     *     .add(new ResultListener<User>() {
     *       // . . .
     *     })
     *     .addTo(subscriptions);
     * }
     * </pre>
     *
     * @param collection to put current subscription to
     */
    public void addTo(Collection<Subscription<?>> collection) {
        collection.add(this);
    }
}

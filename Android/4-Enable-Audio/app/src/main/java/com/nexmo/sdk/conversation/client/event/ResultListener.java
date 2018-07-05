package com.nexmo.sdk.conversation.client.event;

/**
 * Common interface for dispatching events.
 *
 * @author chatitze moumin.
 */

public interface ResultListener<T> {

    /**
     * A Conversation related request has completed successfully.
     *
     * @param result    The result.
     */
    void onSuccess(T result);
}

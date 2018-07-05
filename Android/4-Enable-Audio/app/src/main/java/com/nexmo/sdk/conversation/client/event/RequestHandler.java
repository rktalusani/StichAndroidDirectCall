package com.nexmo.sdk.conversation.client.event;

/**
 * Generic event handler, responsible for handling
 * results of events created by user self.
 *
 * @author chatitze moumin.
 */

public interface RequestHandler<T> extends ResultListener<T> {

    /**
     * A Conversation related request has encountered an error.
     *
     * @param apiError    The exception describing the error.
     */
    void onError(final NexmoAPIError apiError);
}

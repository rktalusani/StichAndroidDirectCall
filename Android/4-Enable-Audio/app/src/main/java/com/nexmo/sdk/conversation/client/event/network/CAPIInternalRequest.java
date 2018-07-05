package com.nexmo.sdk.conversation.client.event.network;

import com.nexmo.sdk.conversation.core.client.request.Request;

/**
 *
 * @hide
 */
public class CAPIInternalRequest {
    private final Request request;
    private final CAPIAwareListener listener;

    public CAPIInternalRequest(Request request, CAPIAwareListener listener) {
        this.listener = listener;
        this.request = request;
    }

    public CAPIAwareListener getListener() {
        return listener;
    }

    public Request getRequest() {
        return request;
    }
}

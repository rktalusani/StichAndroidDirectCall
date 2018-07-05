package com.nexmo.sdk.conversation.client.event.misc;


/**
 * Created by rux on 24/02/17.
 */
public enum SessionError {
    INVALID_TOKEN("system:error:invalid-token"),
    EXPIRED_TOKEN("system:error:expired-token"),
    SESSION_INVALID("session:invalid"),
    SESSION_TERMINATED("session:terminated")
    ;

    private String eventName;

    SessionError(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }

    public static SessionError getByEventName(String eventName) {
        for (SessionError sessionError : values())
            if (sessionError.getEventName().equals(eventName))
                return sessionError;

        return null;
    }
}

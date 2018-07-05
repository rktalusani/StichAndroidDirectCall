/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.client.event;

import com.nexmo.sdk.conversation.client.ConversationClient;

/**
 * ConversationClientException indicates that an instance of {@link ConversationClient} cannot be acquired.
 * In most cases this means some mandatory params are not being set, and building a ConversationClient fails.
 * When exceptions are thrown, they should be caught by the application code.
 *
 * @author emma tresanszki.
 */
public class ConversationClientException extends Error {

    /**
     * Constructs a new ClientBuilderException that includes the current stack trace.
     */
    public ConversationClientException() {}

    /**
     * Constructs a new ClientBuilderException with the current stack trace and the specified detail message.
     *
     * @param message The detail message for this exception. Accepts null.
     */
    public ConversationClientException(String message) {
        super(message);
    }

    /**
     * Constructs a new ClientBuilderException with the current stack trace, the specified detail message and the specified cause.
     *
     * @param message     The detail message for this exception. Accepts null.
     * @param throwable   The cause of this exception.
     */
    public ConversationClientException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Constructs a new ClientBuilderException with the current stack trace and the specified cause.
     *
     * @param throwable The cause of this exception.
     */
    public ConversationClientException(Throwable throwable) {
        super(throwable);
    }

    public static void appendExceptionCause(StringBuilder stringBuilder, String cause) {
        if (stringBuilder.length() > 0 )
            stringBuilder.append(" , ");
        stringBuilder.append(cause);
    }

}

/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Base request class.
 *
 * @author emma tresanszki.
 *
 * @hide
 */
public abstract class Request<T extends RequestHandler, R> implements ResultParser<R> {
    private static final String TAG = Request.class.getSimpleName();

    public final TYPE type;
    public final String tid;
    public T listener;

    protected Request(TYPE type, String tid) {
        this.type = type;
        this.tid = tid;
    }

    protected Request(TYPE type) {
        this(type, UUID.randomUUID().toString());
    }

    protected Request(TYPE type, T listener) {
        this(type);
        this.listener = listener;
    }

    /**
     * Return unique request id
     */
    public String getTid() {
        return tid;
    }


    public T getListener() {
        return listener;
    }

    /**
     * Serialize request to json
     * @return JSON representation of current request
     */
    public JSONObject toJson() {
        try {
            return makeJson();
        } catch (JSONException shouldNeverHappen) {
            shouldNeverHappen.printStackTrace();
        }
        return null;
    }

    protected abstract JSONObject makeJson() throws JSONException;

    /**
     * Helper method for children of Request
     * creates new JSON object with populated "tid" field
     */
    protected JSONObject newTaggedResponse() throws JSONException {
        return jsonObject("tid", getTid());
    }


    /**
     * Constructs simple object with one pair {key: value}
     * @param key key of the object
     * @param value value of the object
     * @return JSONObject of one pair
     */
    protected static JSONObject jsonObject(String key, Object value) throws JSONException {
        return new JSONObject().put(key, value);
    }

    /**
     * Returns name of request to be sent to execute this query like 'push:subscribe'.
     * Children must implement it
     */
    public abstract String getRequestName();

    public String getRequestId() {
        return this.tid;
    }

    /**
     * Returns name of request to be sent to execute this query like 'push:subscribe:success'.
     * Children must implement it
     */
    public abstract String getSuccessEventName();

    @Override
    public String toString() {
        return TAG + " .type: " + this.type + " .tid: " + this.tid;
    }

    /**
     * Request wants to be persisted for retry.
     * Default behaviour - no
     */
    public boolean isPersistable() {
        return false;
    }

    public enum TYPE {
        CREATE,
        JOIN,
        INVITE,
        LEAVE,
        GET,
        GET_EVENTS,
        SEND_TEXT,
        SEND_IMAGE,
        DELETE_EVENT,
        MARK_TEXT_SEEN,
        MARK_IMAGE_SEEN,//initial image, no thumbnail
        MARK_TEXT_DELIVERED,
        MARK_IMAGE_DELIVERED,
        TYPING,
        PUSH_SUBSCRIBE,
        PUSH_UNSUBSCRIBE,
        GET_USER,
        RTC_NEW,
        RTC_TERMINATE,
        RTC_MUTE,
        RTC_EARMUFF,
        AUDIO_RINGING,

        OTHER // for all other types
    }

}

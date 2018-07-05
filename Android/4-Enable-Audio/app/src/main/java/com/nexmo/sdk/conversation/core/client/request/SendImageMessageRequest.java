/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.Image;
import com.nexmo.sdk.conversation.client.ImageRepresentation;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.util.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import static com.nexmo.sdk.conversation.core.client.request.Request.TYPE.SEND_IMAGE;

/**
 * Send text request.
 *
 * @author emma tresanszki.
 * @hide
 */
public class SendImageMessageRequest extends ConversationRequestBase<RequestHandler<Image>, SendTextMessageRequest.Container> {
    public static final String IMAGE_MESSAGE = "image";
    public static final String IMAGE_MESSAGE_SUCCESS = "image:success";
    public String file;
    public String memberId;
    JSONObject imageRepresentationsObject;

    public ImageRepresentation original,medium,thumbnail;


    public SendImageMessageRequest(String cid, String memberId, String file, RequestHandler<Image> listener) {
        super(SEND_IMAGE, cid, listener);
        this.memberId = memberId;
        this.file = file;
    }

    public void updateImages(ImageRepresentation original,
                             ImageRepresentation medium,
                             ImageRepresentation thumbnail,
                             @Deprecated JSONObject jsonObject) {
        this.original = original;
        this.medium = medium;
        this.thumbnail = thumbnail;
        this.imageRepresentationsObject = jsonObject;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newConversationTaggedResponse()
            .put("from", this.memberId)
            .put("body", jsonObject("representations", this.imageRepresentationsObject));
    }

    @Override
    public String getRequestName() {
        return IMAGE_MESSAGE;
    }

    @Override
    public String getSuccessEventName() {
        return IMAGE_MESSAGE_SUCCESS;
    }

    @Override
    public boolean isPersistable() {
        return true;
    }

    @Override
    public SendTextMessageRequest.Container parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        String imageId = body.getString("id");
        Date timestamp = DateUtil.parseDateFromJson(body, "timestamp");
        return new SendTextMessageRequest.Container(imageId, timestamp);
    }
}

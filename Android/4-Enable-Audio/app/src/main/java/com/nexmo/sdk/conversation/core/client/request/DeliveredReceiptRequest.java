package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.Event;
import com.nexmo.sdk.conversation.client.event.EventType;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * request for sending delivery receipts.
 * @author emma tresanszki.
 * @hide
 */
public class DeliveredReceiptRequest extends ConversationRequestBase<RequestHandler<Void>, Void> {
    public static final String TEXT_DELIVERED = "text:delivered";
    public static final String TEXT_DELIVERED_SUCCESS = "text:delivered:success";
    public static final String IMAGE_DELIVERED = "image:delivered";
    public static final String IMAGE_DELIVERED_SUCCESS = "image:delivered:success";
    public String memberId;
    public String eventId;
    public EventType type;

    public DeliveredReceiptRequest(Event event, RequestHandler<Void> listener) {
        super(event.getType() == EventType.TEXT ? TYPE.MARK_TEXT_DELIVERED : TYPE.MARK_IMAGE_DELIVERED, event.getConversation().getConversationId(), listener);
        this.memberId = event.getConversation().getMemberId();
        this.eventId = event.getId();
        this.type = event.getType();
    }

    @Override
    public Void parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        return null;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newConversationTaggedResponse()
                .put("from", memberId)
                .put("body", jsonObject("event_id", eventId));
    }

    @Override
    public String getRequestName() {
        return (this.type == EventType.TEXT ? TEXT_DELIVERED : IMAGE_DELIVERED);
    }

    @Override
    public String getSuccessEventName() {
        return (this.type == EventType.TEXT ? TEXT_DELIVERED_SUCCESS : IMAGE_DELIVERED_SUCCESS);
    }
}

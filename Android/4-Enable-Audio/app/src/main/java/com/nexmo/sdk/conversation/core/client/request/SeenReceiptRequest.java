package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.Event;
import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.SeenReceipt;
import com.nexmo.sdk.conversation.client.event.EventType;
import com.nexmo.sdk.conversation.client.event.RequestHandler;
import com.nexmo.sdk.conversation.core.util.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Request for sending seen receipts.
 * @author emma tresanszki.
 * @hide
 */
public class SeenReceiptRequest extends ConversationRequestBase<RequestHandler<SeenReceipt>, SeenReceiptRequest.Container> {
    public static final String TEXT_SEEN = "text:seen";
    public static final String TEXT_SEEN_SUCCESS = "text:seen:success";
    public static final String IMAGE_SEEN = "image:seen";
    public static final String IMAGE_SEEN_SUCCESS = "image:seen:success";
    public Member member;
    public Event event;
    public EventType type;

    public SeenReceiptRequest(Event event, Member member, RequestHandler<SeenReceipt> listener) {
        super(event.getType() == EventType.TEXT ? TYPE.MARK_TEXT_SEEN : TYPE.MARK_IMAGE_SEEN, event.getConversation().getConversationId(), listener);
        this.event = event;
        this.member = member;
        this.type = event.getType();
    }

    @Override
    public Container parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        Date timestamp = DateUtil.parseDateFromJson(body, "timestamp");
        return new Container(timestamp);
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newConversationTaggedResponse()
                .put("from", this.member.getMemberId())
                .put("body", jsonObject("event_id", this.event.getId()));
    }

    @Override
    public String getRequestName() {
        return (this.type == EventType.TEXT ? TEXT_SEEN : IMAGE_SEEN);
    }

    @Override
    public String getSuccessEventName() {
        return (this.type == EventType.TEXT ? TEXT_SEEN_SUCCESS : IMAGE_SEEN_SUCCESS);
    }

    public static class Container {
        public Date timestamp;

        public Container(Date timestamp) {
            this.timestamp = timestamp;
        }
    }
}

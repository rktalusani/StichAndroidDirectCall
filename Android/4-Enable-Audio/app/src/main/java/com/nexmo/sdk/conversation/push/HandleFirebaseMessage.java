package com.nexmo.sdk.conversation.push;

import android.content.Intent;
import android.os.Bundle;

import com.nexmo.sdk.conversation.client.Image;
import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.Text;
import com.nexmo.sdk.conversation.core.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import static com.nexmo.sdk.conversation.push.PushNotification.ACTION_TYPE_IMAGE;
import static com.nexmo.sdk.conversation.push.PushNotification.ACTION_TYPE_INVITE;
import static com.nexmo.sdk.conversation.push.PushNotification.ACTION_TYPE_INVITED_BY_MEMBER_ID;
import static com.nexmo.sdk.conversation.push.PushNotification.ACTION_TYPE_INVITED_BY_USERNAME;
import static com.nexmo.sdk.conversation.push.PushNotification.ACTION_TYPE_INVITE_CONVERSATION_ID;
import static com.nexmo.sdk.conversation.push.PushNotification.ACTION_TYPE_INVITE_CONVERSATION_NAME;
import static com.nexmo.sdk.conversation.push.PushNotification.ACTION_TYPE_TEXT;
import static com.nexmo.sdk.conversation.push.PushNotification.CONVERSATION_PUSH_ACTION;
import static com.nexmo.sdk.conversation.push.PushNotification.CONVERSATION_PUSH_CONVERSATION_ID;
import static com.nexmo.sdk.conversation.push.PushNotification.CONVERSATION_PUSH_TYPE;

class HandleFirebaseMessage {
    private static final String MESSAGE_TYPE_TEXT = "text";
    private static final String MESSAGE_TYPE_IMAGE = "image";
    private static final String MESSAGE_TYPE_INVITE = "member:invited";

    public Intent invoke(JSONObject eventJson) {
        Intent intent = new Intent(CONVERSATION_PUSH_ACTION);

        try {
            EventExtractor eventExtractor = new EventExtractor(eventJson).fromJson();
            switch (eventExtractor.getType()) {
                case MESSAGE_TYPE_TEXT: {
                    intent = textEventBundleIntent(intent, eventExtractor);
                    break;
                }
                case MESSAGE_TYPE_IMAGE: {
                    intent = imageEventBundleIntent(intent, eventExtractor);
                    break;
                }
                case MESSAGE_TYPE_INVITE: {
                    intent = inviteEventBundleEvent(intent, eventExtractor);
                    break;
                }
                default: {
                    Log.d(ConversationMessagingService.TAG, "incoming event of unrecognized type - " + eventExtractor.getType());
                    return null;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return intent;
    }

    Intent textEventBundleIntent(Intent intent, EventExtractor eventExtractor) throws JSONException {
        Log.d(ConversationMessagingService.TAG, "incoming TEXT");
        TextEvent textEvent = TextEvent.createTextEvent(eventExtractor.getSenderId(), eventExtractor.getEventId(), eventExtractor.getJsonBody(),eventExtractor.getCid());
        Bundle bundle = new Bundle();

        //broadcast with ACTION
        bundle.putString(CONVERSATION_PUSH_TYPE, ACTION_TYPE_TEXT);
        bundle.putParcelable(Text.class.getSimpleName(), textEvent.text);
        bundle.putString(CONVERSATION_PUSH_CONVERSATION_ID, textEvent.getConversationId());
        intent = intent.putExtras(bundle);
        return intent;
    }

    Intent imageEventBundleIntent(Intent intent, EventExtractor eventExtractor) throws JSONException {
        Log.d(ConversationMessagingService.TAG, "incoming IMAGE");
        ImageEvent imageEvent = ImageEvent.createImageEvent(eventExtractor.getSenderId(), eventExtractor.getEventId(), eventExtractor.getJsonBody(), eventExtractor.getCid());

        Bundle bundle = new Bundle();

        //broadcast with ACTION
        bundle.putString(CONVERSATION_PUSH_TYPE, ACTION_TYPE_IMAGE);
        bundle.putParcelable(Image.class.getSimpleName(), imageEvent.incomingImage);
        bundle.putString(CONVERSATION_PUSH_CONVERSATION_ID, imageEvent.getConversationId());
        intent = intent.putExtras(bundle);
        return intent;
    }

    Intent inviteEventBundleEvent(Intent intent, EventExtractor eventExtractor) throws JSONException {
        Log.d(ConversationMessagingService.TAG, "incoming Invite");
        InviteEvent inviteEvent = InviteEvent.createInviteEvent(eventExtractor.getBody(), eventExtractor.getSenderId(), eventExtractor.getCid());
        Bundle bundle = new Bundle();

        Log.d(ConversationMessagingService.TAG, "invited " + "cid: " + inviteEvent.conversationId + " .cname: " + inviteEvent.cName + " .senderMemberId: " + inviteEvent.senderMemberId + " .senderUsername: " + inviteEvent.senderUsername);
        //broadcast with ACTION
        bundle.putString(CONVERSATION_PUSH_TYPE, ACTION_TYPE_INVITE);
        bundle.putParcelable(Member.class.getSimpleName(), inviteEvent.invitedMember);
        bundle.putString(ACTION_TYPE_INVITED_BY_MEMBER_ID, inviteEvent.senderMemberId);
        bundle.putString(ACTION_TYPE_INVITED_BY_USERNAME, inviteEvent.senderUsername);
        bundle.putString(ACTION_TYPE_INVITE_CONVERSATION_ID, inviteEvent.conversationId);
        bundle.putString(ACTION_TYPE_INVITE_CONVERSATION_NAME, inviteEvent.cName);

        intent = intent.putExtras(bundle);
        return intent;
    }
}

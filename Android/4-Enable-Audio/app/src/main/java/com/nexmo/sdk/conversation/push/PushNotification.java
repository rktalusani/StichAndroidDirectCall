package com.nexmo.sdk.conversation.push;

/**
 * @author emma tresanszki
 */
public class PushNotification {
    public static final String CONVERSATION_PUSH_TYPE = "conversation.sdk.type";
    public static final String ACTION_TYPE_TEXT = "conversation.sdk.push.text";
    public static final String ACTION_TYPE_IMAGE = "conversation.sdk.push.image";
    public static final String ACTION_TYPE_INVITE = "conversation.sdk.push.member:invited";
    public static final String ACTION_TYPE_INVITED_BY_MEMBER_ID = "conversation.sdk.push.member:invitedBy:memberId";
    public static final String ACTION_TYPE_INVITED_BY_USERNAME = "conversation.sdk.push.member:invitedBy:username";
    public static final String CONVERSATION_PUSH_ACTION = "com.nexmo.sdk.conversation.PUSH";
    public static final String ACTION_TYPE_INVITE_CONVERSATION_ID  = "conversation.sdk.push.member:invited:conversation:id";
    public static final String ACTION_TYPE_INVITE_CONVERSATION_NAME  = "conversation.sdk.push.member:invited:conversation:name";
    public static final String CONVERSATION_PUSH_CONVERSATION_ID = "conversation.sdk.conversation:id";
}

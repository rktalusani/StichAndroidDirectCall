package com.nexmo.sdk.conversation.core.persistence.dao;

import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.User;

import java.util.List;
import java.util.Map;

/**
 * @author emma tresanszki.
 * @hide
 */
public interface ConversationDAO extends DAO<Conversation> {

    void insertAll(List<Conversation> items);

    Conversation read(String cid);

    List<Conversation> read(User user);

    Map<String, Conversation> getConversationsListAsMap(User user);
}

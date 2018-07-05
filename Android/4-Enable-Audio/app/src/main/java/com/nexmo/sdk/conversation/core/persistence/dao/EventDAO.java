package com.nexmo.sdk.conversation.core.persistence.dao;

import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.Event;

import java.util.List;

/**
 * @author emma tresanszki.
 * @hide
 */
public interface EventDAO extends DAO<Event> {

    void insertAll(String cid, List<Event> eventList);

    void update(Event event, String cid);

    List<Event> read(String cid, Conversation conversation);

    String getLastEventId(String cid);

    List<String> getEventIds(Conversation conversation);

    boolean isAny(String cid);
}

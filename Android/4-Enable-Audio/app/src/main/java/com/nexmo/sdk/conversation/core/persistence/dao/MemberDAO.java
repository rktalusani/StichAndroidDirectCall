package com.nexmo.sdk.conversation.core.persistence.dao;

import com.nexmo.sdk.conversation.client.Member;

import java.util.List;

/**
 * @author emma tresanszki.
 * @hide
 */
public interface MemberDAO extends DAO<Member> {

    void insertAll(String cid, List<Member> memberList);

    List<Member> read(String cid);
}

package com.nexmo.sdk.conversation.client.event.container;

import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.Member;

/**
 * Invitation details for any incoming invite.
 *
 * @author rux
 */

public final class Invitation {
    private Conversation conversation;
    private Member invitedMember;
    private String invitedBy;
    private boolean isAudioEnabled = false;

    public Invitation(Conversation conversation, Member invitedMember, String invitedBy) {
        this.conversation = conversation;
        this.invitedMember = invitedMember;
        this.invitedBy = invitedBy;
    }

    public Invitation(Conversation conversation, Member invitedMember, String invitedBy, boolean isAudioEnabled) {
        this(conversation, invitedMember, invitedBy);
        this.isAudioEnabled = isAudioEnabled;
    }
    public Conversation getConversation() {
        return conversation;
    }

    public Member getInvitedMember() {
        return invitedMember;
    }

    public String getInvitedBy() {
        return invitedBy;
    }

    public boolean isAudioEnabled() {
        return this.isAudioEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Invitation that = (Invitation) o;

        if (!conversation.equals(that.conversation)) return false;
        if (!invitedMember.equals(that.invitedMember)) return false;
        return invitedBy != null ? invitedBy.equals(that.invitedBy) : that.invitedBy == null;

    }

    @Override
    public int hashCode() {
        int result = conversation.hashCode();
        result = 31 * result + invitedMember.hashCode();
        result = 31 * result + (invitedBy != null ? invitedBy.hashCode() : 0);
        return result;
    }
}
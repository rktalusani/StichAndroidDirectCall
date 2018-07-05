package com.nexmo.sdk.conversation.client.event.container;

import com.nexmo.sdk.conversation.client.DeliveredReceipt;
import com.nexmo.sdk.conversation.client.Event;
import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.ReceiptRecord;
import com.nexmo.sdk.conversation.client.SeenReceipt;

/**
 * Receipt details for an event.
 *
 * @author rux
 */

public class Receipt<R extends ReceiptRecord> {
    private final Event message;
    private final Member member;
    private final R receipt;

    public Receipt(Event message, Member member, R receipt) {
        this.message = message;
        this.member = member;
        this.receipt = receipt;
    }

    public Event getMessage() {
        return message;
    }

    public Member getMember() {
        return member;
    }

    public R getReceipt() {
        return receipt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Receipt<?> receipt1 = (Receipt<?>) o;

        if (!message.equals(receipt1.message)) return false;
        if (!member.equals(receipt1.member)) return false;
        return receipt.equals(receipt1.receipt);

    }

    @Override
    public int hashCode() {
        int result = message.hashCode();
        result = 31 * result + member.hashCode();
        result = 31 * result + receipt.hashCode();
        return result;
    }

    public static class Delivery extends Receipt<DeliveredReceipt> {
        public Delivery(Event message, Member member, DeliveredReceipt receipt) {
            super(message, member, receipt);
        }
    }

    public static class Seen extends Receipt<SeenReceipt> {
        public Seen(Event message, Member member, SeenReceipt receipt) {
            super(message, member, receipt);
        }
    }
}
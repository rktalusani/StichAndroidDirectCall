package com.nexmo.sdk.conversation.client;

import android.os.Parcel;
import android.support.annotation.VisibleForTesting;

import java.util.Date;

/**
 *
 * A delivery receipt confirms delivery of your message, but not that the members involved in the conversation
 * have seen it or read it.
 * <p>Marking an event/message as Delivered is done implicitly.</p>
 *
 * @author emma tresanszki.
 * @date 17/10/16.
 */
public class DeliveredReceipt extends ReceiptRecord {

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected DeliveredReceipt(Event event, Member member, Date timestamp) {
        super(event, member, timestamp);
    }

    protected DeliveredReceipt(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public static final Creator<DeliveredReceipt> CREATOR = new Creator<DeliveredReceipt>() {
        @Override
        public DeliveredReceipt createFromParcel(Parcel in) {
            return new DeliveredReceipt(in);
        }

        @Override
        public DeliveredReceipt[] newArray(int size) {
            return new DeliveredReceipt[size];
        }
    };

}

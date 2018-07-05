/*
 * Copyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */
package com.nexmo.sdk.conversation.client;

import android.os.Parcel;
import android.support.annotation.VisibleForTesting;

import java.util.Date;

/**
 * Seen receipts for events: of type Text and Image.
 * <p>A seen receipt confirms that your message was opened/read.</p>
 * <p>Marking an event/message as Seen has to be done explicitly.</p>
 *
 *
 * @author emma tresanszki.
 */
public class SeenReceipt extends ReceiptRecord {

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    protected SeenReceipt(Event event, Member member, Date timestamp) {
        super(event, member, timestamp);
    }

    protected SeenReceipt(Parcel in) {
        super(in);
    }

    public static final Creator<SeenReceipt> CREATOR = new Creator<SeenReceipt>() {
        @Override
        public SeenReceipt createFromParcel(Parcel in) {
            return new SeenReceipt(in);
        }

        @Override
        public SeenReceipt[] newArray(int size) {
            return new SeenReceipt[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

}

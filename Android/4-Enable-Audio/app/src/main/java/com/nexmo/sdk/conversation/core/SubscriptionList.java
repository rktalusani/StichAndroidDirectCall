package com.nexmo.sdk.conversation.core;

import com.nexmo.sdk.conversation.client.Subscription;

import java.util.ArrayList;

/**
 * Utility list which makes it easier to manage subscriptions and app lifecycle
 * @author rux
 */

public class SubscriptionList extends ArrayList<Subscription<?>> {
    /**
     * Unsubscribe all subscribers
     */
    public void unsubscribeAll() {
        for (Subscription<?> subscription : this) {
            subscription.unsubscribe();
        }
    }
}

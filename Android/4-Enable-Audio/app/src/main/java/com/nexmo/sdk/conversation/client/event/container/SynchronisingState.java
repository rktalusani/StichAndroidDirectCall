package com.nexmo.sdk.conversation.client.event.container;

/**
 * Synchronisation information.
 *
 * <p>The order of sync events is important, depending on which STATE has been synced,
 * UI will be ready for update with fresh info.</p>
 * <ul>
 *     <li>OUT_OF_SYNC: Nothing has been synced, this event happens before sync has even started.</li>
 *     <li>CONVERSATIONS: Conversations preview has been synced.
 *     At this point all members can be displayed in the UI.
 *     Wait for the next state for updating members information in the UI.</li>
 *     <li>MEMBERS: all information about the members has been synced. At this point all members can be displayed in the UI.</li>
 * </ul>
 *
 * @author emma tresanszki.
 */
public class SynchronisingState {

    public enum STATE {
        /**
         * Nothing has been synced yet.
         */
        OUT_OF_SYNC,
        /**
         * Conversations preview has been synced.
         * At this point only basic conversation list can be displayed in the UI.
         * Wait for the next state for updating members information in the UI.
         */
        CONVERSATIONS,
        /**
         * All information about the members has been synced.
         * At this point all members can be displayed in the UI.
         */
        MEMBERS
    }

    public STATE state;//step
}

package org.veyra.client;

import org.telegram.messenger.MessagesStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory cache for ignore-list flags per (groupDialogId, peerId).
 * Populated lazily when a ChatActivity opens a group dialog.
 * Cleared and reloaded when the ignore list changes.
 *
 * All access on UI thread.
 */
public class VeyraIgnoreCache {

    // groupDialogId -> (peerId -> flags)
    private static final HashMap<Long, HashMap<Long, Integer>> cache = new HashMap<>();
    private static final HashMap<Long, Boolean> loaded = new HashMap<>();

    /** Returns ignore flags for a peer in a group, or 0 if not ignored / not loaded yet. */
    public static int getFlags(long groupDialogId, long peerId) {
        HashMap<Long, Integer> sub = cache.get(groupDialogId);
        if (sub == null) return 0;
        Integer f = sub.get(peerId);
        return f != null ? f : 0;
    }

    /** Returns true if any ignore flags are set for this peer. */
    public static boolean isIgnored(long groupDialogId, long peerId) {
        return getFlags(groupDialogId, peerId) != 0;
    }

    /** Returns true if specific flag is set for this peer. */
    public static boolean hasFlag(long groupDialogId, long peerId, int flag) {
        return (getFlags(groupDialogId, peerId) & flag) != 0;
    }

    /**
     * Load (or reload) ignore list for a group from DB.
     * Call once when ChatActivity starts for a group dialog, and after any ignore-list change.
     */
    public static void load(int currentAccount, long groupDialogId) {
        loaded.put(groupDialogId, false);
        MessagesStorage.getInstance(currentAccount).getIgnoreList(groupDialogId, (peerIds, flagsList) -> {
            HashMap<Long, Integer> sub = new HashMap<>();
            for (int i = 0; i < peerIds.length; i++) {
                if (flagsList[i] != 0) sub.put(peerIds[i], flagsList[i]);
            }
            cache.put(groupDialogId, sub);
            loaded.put(groupDialogId, true);
        });
    }

    /** Remove cached data for a dialog (e.g. when leaving the chat). */
    public static void evict(long groupDialogId) {
        cache.remove(groupDialogId);
        loaded.remove(groupDialogId);
    }

    /** Check if the list for this dialog has been loaded from DB. */
    public static boolean isLoaded(long groupDialogId) {
        Boolean b = loaded.get(groupDialogId);
        return b != null && b;
    }
}

/* Veyra Project */

package org.veyra.client;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class HiddenContentManager {

    private static final String PREFS_NAME = "veyra_hidden_content";
    private static final String KEY_HIDDEN_MSGS = "hidden_message_keys";

    private static final Set<String> hiddenKeys = Collections.synchronizedSet(new HashSet<>());
    private static boolean loaded = false;

    private static void ensureLoaded() {
        if (!loaded) {
            synchronized (hiddenKeys) {
                if (!loaded) {
                    try {
                        if (ApplicationLoader.applicationContext != null) {
                            SharedPreferences sp = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                            Set<String> set = sp.getStringSet(KEY_HIDDEN_MSGS, null);
                            if (set != null) {
                                hiddenKeys.addAll(set);
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    loaded = true;
                }
            }
        }
    }

    private static void save() {
        try {
            if (ApplicationLoader.applicationContext != null) {
                SharedPreferences sp = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                synchronized (hiddenKeys) {
                    sp.edit().putStringSet(KEY_HIDDEN_MSGS, new HashSet<>(hiddenKeys)).apply();
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static String buildKey(long dialogId, int messageId) {
        return dialogId + "_" + messageId;
    }

    public static void hideMessage(long dialogId, int messageId) {
        ensureLoaded();
        hiddenKeys.add(buildKey(dialogId, messageId));
        save();
    }

    public static boolean isMessageHidden(long dialogId, int messageId) {
        ensureLoaded();
        return hiddenKeys.contains(buildKey(dialogId, messageId));
    }

    public static boolean isEmpty() {
        ensureLoaded();
        return hiddenKeys.isEmpty();
    }

    public static int getHiddenCount() {
        ensureLoaded();
        return hiddenKeys.size();
    }

    public static int unhideAll(int currentAccount) {
        ensureLoaded();
        int count;
        synchronized (hiddenKeys) {
            count = hiddenKeys.size();
            hiddenKeys.clear();
        }
        save();
        if (count > 0) {
            try {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return count;
    }
}

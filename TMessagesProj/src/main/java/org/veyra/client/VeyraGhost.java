package org.veyra.client;

import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;

public class VeyraGhost {
    private static SharedPreferences prefs;

    public static boolean ghostMode = true;
    public static boolean hideOnline = true;
    public static boolean hideTyping = true;
    public static boolean readOnReply = true;
    public static boolean hideStories = true;

    private static final java.util.concurrent.ConcurrentHashMap<Long, Long> allowedReadDialogs = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        load();
    }

    private static void ensurePrefs() {
        if (prefs == null && ApplicationLoader.applicationContext != null) {
            prefs = ApplicationLoader.applicationContext.getSharedPreferences("veyra_ghost", 0);
        }
    }

    public static void load() {
        ensurePrefs();
        if (prefs != null) {
            ghostMode = prefs.getBoolean("ghostMode", true);
            hideOnline = prefs.getBoolean("hideOnline", true);
            hideTyping = prefs.getBoolean("hideTyping", true);
            readOnReply = prefs.getBoolean("readOnReply", true);
            hideStories = prefs.getBoolean("hideStories", true);
        }
    }

    public static void toggleGhostMode() {
        ghostMode = !ghostMode;
        save("ghostMode", ghostMode);
    }

    public static void toggleHideOnline() {
        hideOnline = !hideOnline;
        save("hideOnline", hideOnline);
    }

    public static void toggleHideTyping() {
        hideTyping = !hideTyping;
        save("hideTyping", hideTyping);
    }

    public static void toggleReadOnReply() {
        readOnReply = !readOnReply;
        save("readOnReply", readOnReply);
    }

    public static void toggleHideStories() {
        hideStories = !hideStories;
        save("hideStories", hideStories);
    }

    private static void save(String key, boolean val) {
        ensurePrefs();
        if (prefs != null) {
            prefs.edit().putBoolean(key, val).apply();
        }
    }

    public static void allowSendReadOnce(long dialogId) {
        allowedReadDialogs.put(dialogId, System.currentTimeMillis() + 8000L);
    }

    public static boolean consumeSendReadAllowed(long dialogId) {
        Long expire = allowedReadDialogs.get(dialogId);
        if (expire != null) {
            if (System.currentTimeMillis() <= expire) {
                return true;
            } else {
                allowedReadDialogs.remove(dialogId);
            }
        }
        return false;
    }

    public static boolean isGhostActive() {
        return ghostMode;
    }

    public static boolean shouldHideOnline() {
        return ghostMode && hideOnline;
    }

    public static boolean shouldHideTyping() {
        return ghostMode && hideTyping;
    }

    public static boolean shouldHideRead(long dialogId) {
        if (!ghostMode || !readOnReply) return false;
        if (consumeSendReadAllowed(dialogId)) return false;
        return true;
    }

    public static boolean shouldHideStories() {
        return ghostMode && hideStories;
    }
}

package org.telegram.messenger;

import android.content.SharedPreferences;

public class VeyraConfig {
    private static SharedPreferences preferences;
    private static boolean configLoaded;

    public static boolean antiDelete = true;
    public static boolean ghostMode = false;
    public static boolean hideTyping = false;
    public static boolean readOnReply = true;
    public static boolean confirmCall = true;
    public static boolean confirmLink = true;
    public static boolean cleanUrls = true;
    public static boolean persianCalendar = true;
    public static boolean showProfileId = true;
    public static boolean disableVibration = false;
    public static boolean disableUndo = false;
    public static boolean disableLinkPreviewByDefault = false;
    public static boolean ignoreContentRestrictions = true;
    public static boolean blockSecretChat = false;
    // 0 = normal (show online), 1 = hide online (always appear offline), 2 = always appear online
    public static int onlineMode = 0;

    public static void loadConfig() {
        if (configLoaded) return;
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("veyraconfig", 0);
        antiDelete = preferences.getBoolean("antiDelete", true);
        ghostMode = preferences.getBoolean("ghostMode", false);
        hideTyping = preferences.getBoolean("hideTyping", false);
        readOnReply = preferences.getBoolean("readOnReply", true);
        confirmCall = preferences.getBoolean("confirmCall", true);
        confirmLink = preferences.getBoolean("confirmLink", true);
        cleanUrls = preferences.getBoolean("cleanUrls", true);
        persianCalendar = preferences.getBoolean("persianCalendar", true);
        showProfileId = preferences.getBoolean("showProfileId", true);
        disableVibration = preferences.getBoolean("disableVibration", false);
        disableUndo = preferences.getBoolean("disableUndo", false);
        disableLinkPreviewByDefault = preferences.getBoolean("disableLinkPreviewByDefault", false);
        ignoreContentRestrictions = preferences.getBoolean("ignoreContentRestrictions", true);
        blockSecretChat = preferences.getBoolean("blockSecretChat", false);
        onlineMode = preferences.getInt("onlineMode", 0);
        configLoaded = true;
    }

    public static void setAntiDelete(boolean val) {
        antiDelete = val;
        save("antiDelete", val);
    }
    public static void setGhostMode(boolean val) {
        ghostMode = val;
        save("ghostMode", val);
    }
    public static void setHideTyping(boolean val) {
        hideTyping = val;
        save("hideTyping", val);
    }
    public static void setReadOnReply(boolean val) {
        readOnReply = val;
        save("readOnReply", val);
    }
    public static void setConfirmCall(boolean val) {
        confirmCall = val;
        save("confirmCall", val);
    }
    public static void setConfirmLink(boolean val) {
        confirmLink = val;
        save("confirmLink", val);
    }
    public static void setCleanUrls(boolean val) {
        cleanUrls = val;
        save("cleanUrls", val);
    }
    public static void setPersianCalendar(boolean val) {
        persianCalendar = val;
        save("persianCalendar", val);
    }
    public static void setShowProfileId(boolean val) {
        showProfileId = val;
        save("showProfileId", val);
    }
    public static void setDisableVibration(boolean val) {
        disableVibration = val;
        save("disableVibration", val);
    }
    public static void setDisableUndo(boolean val) {
        disableUndo = val;
        save("disableUndo", val);
    }
    public static void setDisableLinkPreviewByDefault(boolean val) {
        disableLinkPreviewByDefault = val;
        save("disableLinkPreviewByDefault", val);
    }
    public static void setIgnoreContentRestrictions(boolean val) {
        ignoreContentRestrictions = val;
        save("ignoreContentRestrictions", val);
    }
    public static void setBlockSecretChat(boolean val) {
        blockSecretChat = val;
        save("blockSecretChat", val);
    }
    public static void setOnlineMode(int val) {
        onlineMode = val;
        if (preferences == null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("veyraconfig", 0);
        }
        preferences.edit().putInt("onlineMode", val).apply();
    }

    private static void save(String key, boolean val) {
        if (preferences == null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("veyraconfig", 0);
        }
        preferences.edit().putBoolean(key, val).apply();
    }
}

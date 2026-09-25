package org.telegram.messenger;

import android.content.SharedPreferences;

public class VeyraConfig {
    private static SharedPreferences preferences;
    private static boolean configLoaded;

    // ==================== Privacy & Stealth ====================
    public static boolean antiDelete = true;
    public static boolean ghostMode = false;
    public static boolean hideTyping = false;
    public static boolean readOnReply = true;
    public static boolean blockSecretChat = false;
    // 0 = normal (show online), 1 = hide online, 2 = hide online + offline after sending message, 3 = always appear online
    public static int onlineMode = 0;

    // ==================== Controls & Interaction ====================
    public static boolean confirmCall = true;
    public static boolean confirmLink = true;
    public static boolean cleanUrls = true;
    public static boolean disableUndo = true;
    public static boolean disableLinkPreviewByDefault = false;
    public static boolean disableVibration = false;

    // ==================== UI & Features ====================
    public static boolean persianCalendar = true;
    public static boolean showProfileId = true;
    public static boolean ignoreContentRestrictions = true;
    // Dialog sorting
    public static boolean sortByUnread = false;
    public static boolean sortByUnmuted = false;
    // Disable trending stickers/emoji
    public static boolean disableTrending = false;
    // Keep original filename on download
    public static boolean keepOriginalFilename = false;

    // ==================== Chat List (forkgram port) ====================
    public static boolean disableGlobalSearch = false;
    public static boolean disableThumbsInDialogList = false;
    public static boolean enableLastSeenDots = false;

    // ==================== Appearance (forkgram port) ====================
    public static boolean hideConnectingToProxy = false;
    public static boolean disableBigEmoji = false;

    // ==================== Composing & Messages (forkgram port) ====================
    public static boolean mentionByName = false;
    public static boolean hideSendAsButton = false;
    public static boolean disableQuickReaction = false;
    public static boolean formatTimeWithSeconds = false;
    public static boolean stripBotLinkParams = false;
    public static boolean anonymousForwardNoQuote = false;
    public static boolean jumpToFirstMessage = true;
    public static boolean copyDialogId = true;

    // ==================== Media & Camera (forkgram port) ====================
    public static boolean rearCameraVideoMessages = false;

    // ==================== Stickers & GIFs (forkgram port) ====================
    public static boolean sendCaptionWithGif = true;
    public static boolean sendCaptionWithSticker = true;

    // ==================== Settings Lock (forkgram port) ====================
    public static String settingsLockHash = "";
    public static String settingsLockSalt = "";

    public static void reloadConfig() {
        configLoaded = false;
        loadConfig();
    }

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
        disableUndo = preferences.getBoolean("disableUndo", true);
        disableLinkPreviewByDefault = preferences.getBoolean("disableLinkPreviewByDefault", false);
        ignoreContentRestrictions = preferences.getBoolean("ignoreContentRestrictions", true);
        blockSecretChat = preferences.getBoolean("blockSecretChat", false);
        onlineMode = preferences.getInt("onlineMode", 0);
        sortByUnread = preferences.getBoolean("sortByUnread", false);
        sortByUnmuted = preferences.getBoolean("sortByUnmuted", false);
        disableTrending = preferences.getBoolean("disableTrending", false);
        keepOriginalFilename = preferences.getBoolean("keepOriginalFilename", false);

        disableGlobalSearch = preferences.getBoolean("disableGlobalSearch", false);
        disableThumbsInDialogList = preferences.getBoolean("disableThumbsInDialogList", false);
        enableLastSeenDots = preferences.getBoolean("enableLastSeenDots", false);

        hideConnectingToProxy = preferences.getBoolean("hideConnectingToProxy", false);
        disableBigEmoji = preferences.getBoolean("disableBigEmoji", false);

        mentionByName = preferences.getBoolean("mentionByName", false);
        hideSendAsButton = preferences.getBoolean("hideSendAsButton", false);
        disableQuickReaction = preferences.getBoolean("disableQuickReaction", false);
        formatTimeWithSeconds = preferences.getBoolean("formatTimeWithSeconds", false);
        stripBotLinkParams = preferences.getBoolean("stripBotLinkParams", false);
        anonymousForwardNoQuote = preferences.getBoolean("anonymousForwardNoQuote", false);
        jumpToFirstMessage = preferences.getBoolean("jumpToFirstMessage", true);
        copyDialogId = preferences.getBoolean("copyDialogId", true);

        rearCameraVideoMessages = preferences.getBoolean("rearCameraVideoMessages", false);

        sendCaptionWithGif = preferences.getBoolean("sendCaptionWithGif", true);
        sendCaptionWithSticker = preferences.getBoolean("sendCaptionWithSticker", true);

        settingsLockHash = preferences.getString("settingsLockHash", "");
        settingsLockSalt = preferences.getString("settingsLockSalt", "");

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
    public static void setSortByUnread(boolean val) {
        sortByUnread = val;
        save("sortByUnread", val);
    }
    public static void setSortByUnmuted(boolean val) {
        sortByUnmuted = val;
        save("sortByUnmuted", val);
    }
    public static void setDisableTrending(boolean val) {
        disableTrending = val;
        save("disableTrending", val);
    }
    public static void setKeepOriginalFilename(boolean val) {
        keepOriginalFilename = val;
        save("keepOriginalFilename", val);
    }

    public static void setDisableGlobalSearch(boolean val) {
        disableGlobalSearch = val;
        save("disableGlobalSearch", val);
    }
    public static void setDisableThumbsInDialogList(boolean val) {
        disableThumbsInDialogList = val;
        save("disableThumbsInDialogList", val);
    }
    public static void setEnableLastSeenDots(boolean val) {
        enableLastSeenDots = val;
        save("enableLastSeenDots", val);
    }

    public static void setHideConnectingToProxy(boolean val) {
        hideConnectingToProxy = val;
        save("hideConnectingToProxy", val);
    }
    public static void setDisableBigEmoji(boolean val) {
        disableBigEmoji = val;
        save("disableBigEmoji", val);
    }

    public static void setMentionByName(boolean val) {
        mentionByName = val;
        save("mentionByName", val);
    }
    public static void setHideSendAsButton(boolean val) {
        hideSendAsButton = val;
        save("hideSendAsButton", val);
    }
    public static void setDisableQuickReaction(boolean val) {
        disableQuickReaction = val;
        save("disableQuickReaction", val);
    }
    public static void setFormatTimeWithSeconds(boolean val) {
        formatTimeWithSeconds = val;
        save("formatTimeWithSeconds", val);
    }
    public static void setStripBotLinkParams(boolean val) {
        stripBotLinkParams = val;
        save("stripBotLinkParams", val);
    }
    public static void setAnonymousForwardNoQuote(boolean val) {
        anonymousForwardNoQuote = val;
        save("anonymousForwardNoQuote", val);
    }
    public static void setJumpToFirstMessage(boolean val) {
        jumpToFirstMessage = val;
        save("jumpToFirstMessage", val);
    }
    public static void setCopyDialogId(boolean val) {
        copyDialogId = val;
        save("copyDialogId", val);
    }

    public static void setRearCameraVideoMessages(boolean val) {
        rearCameraVideoMessages = val;
        save("rearCameraVideoMessages", val);
    }

    public static void setSendCaptionWithGif(boolean val) {
        sendCaptionWithGif = val;
        save("sendCaptionWithGif", val);
    }
    public static void setSendCaptionWithSticker(boolean val) {
        sendCaptionWithSticker = val;
        save("sendCaptionWithSticker", val);
    }

    public static boolean hasSettingsLock() {
        return settingsLockHash != null && settingsLockHash.length() > 0;
    }
    public static void setSettingsLock(String hash, String salt) {
        settingsLockHash = hash == null ? "" : hash;
        settingsLockSalt = salt == null ? "" : salt;
        if (preferences == null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("veyraconfig", 0);
        }
        preferences.edit().putString("settingsLockHash", settingsLockHash).putString("settingsLockSalt", settingsLockSalt).apply();
    }
    public static void clearSettingsLock() {
        setSettingsLock("", "");
    }
    public static boolean checkSettingsLockCode(String code) {
        if (!hasSettingsLock()) return true;
        return settingsLockHash.equals(Utilities.MD5(settingsLockSalt + code));
    }

    private static void save(String key, boolean val) {
        if (preferences == null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("veyraconfig", 0);
        }
        preferences.edit().putBoolean(key, val).apply();
    }
}

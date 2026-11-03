package org.veyra.client;

import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;

public class VeyraUIConfig {
    private static SharedPreferences prefs;

    public static final int TITLE_TYPE_TEXT = 0;
    public static final int TITLE_TYPE_ICON = 1;
    public static final int TITLE_TYPE_MIX = 2;

    public static int tabsTitleType = TITLE_TYPE_MIX;
    public static boolean bottomFilterTabs = false;
    public static boolean hideAllTab = false;

    // Chat usability & tweaks
    public static boolean hideKeyboardOnChatScroll = false;
    public static boolean disableVoiceAutoPlay = false;
    public static boolean disableJumpToNextChannel = false;
    public static boolean ignoreBlocked = false;
    public static boolean askBeforeCall = true;
    public static int downloadSpeedBoost = 1; // 0 = off, 1 = average, 2 = extreme

    // Edit History & Privacy
    public static boolean editHistoryEnabled = true;
    public static boolean editHistoryDropOldest = true;
    public static int editHistoryLimit = 20;
    public static boolean reactionHistoryEnabled = true;
    public static boolean reactionHistoryIncludeSelf = true;
    public static int reactionHistoryLimit = 20;
    public static boolean reactionHistoryDropOldest = true;
    public static boolean hidePhoneNumber = false;

    static {
        load();
    }

    private static void ensurePrefs() {
        if (prefs == null && ApplicationLoader.applicationContext != null) {
            prefs = ApplicationLoader.applicationContext.getSharedPreferences("veyra_ui_config", 0);
        }
    }

    public static void load() {
        ensurePrefs();
        if (prefs != null) {
            tabsTitleType = prefs.getInt("tabsTitleType", TITLE_TYPE_MIX);
            bottomFilterTabs = prefs.getBoolean("bottomFilterTabs", false);
            hideAllTab = prefs.getBoolean("hideAllTab", false);
            hideKeyboardOnChatScroll = prefs.getBoolean("hideKeyboardOnChatScroll", false);
            disableVoiceAutoPlay = prefs.getBoolean("disableVoiceAutoPlay", false);
            disableJumpToNextChannel = prefs.getBoolean("disableJumpToNextChannel", false);
            ignoreBlocked = prefs.getBoolean("ignoreBlocked", false);
            askBeforeCall = prefs.getBoolean("askBeforeCall", true);
            downloadSpeedBoost = prefs.getInt("downloadSpeedBoost", 1);
            editHistoryEnabled = prefs.getBoolean("editHistoryEnabled", true);
            editHistoryLimit = prefs.getInt("editHistoryLimit", 20);
            hidePhoneNumber = prefs.getBoolean("hidePhoneNumber", false);
        }
    }

    public static boolean isChatTypeAllowedForEditHistory(long dialogId) {
        return editHistoryEnabled;
    }

    public static boolean isChatTypeAllowedForReactionHistory(long dialogId) {
        return reactionHistoryEnabled;
    }

    public static void toggleEditHistory() {
        editHistoryEnabled = !editHistoryEnabled;
        save("editHistoryEnabled", editHistoryEnabled);
    }

    public static void toggleHidePhoneNumber() {
        hidePhoneNumber = !hidePhoneNumber;
        save("hidePhoneNumber", hidePhoneNumber);
    }

    public static boolean isAntiDeleteInScope(int account, long dialogId) {
        return VeyraScope.ANTI_DELETE.isInScope(account, dialogId, true);
    }

    public static boolean isAntiDeleteInScope(int account, long dialogId, long fromId) {
        return VeyraScope.ANTI_DELETE.isMessageInScope(account, dialogId, fromId, true);
    }

    // Scope gate for edit and reaction history logging (same model as anti-delete).
    public static boolean isHistoryInScope(int account, long dialogId) {
        return VeyraScope.HISTORY.isInScope(account, dialogId, true);
    }

    public static void setTabsTitleType(int val) {
        tabsTitleType = val;
        save("tabsTitleType", val);
    }

    public static void toggleBottomFilterTabs() {
        bottomFilterTabs = !bottomFilterTabs;
        save("bottomFilterTabs", bottomFilterTabs);
    }

    public static void toggleHideAllTab() {
        hideAllTab = !hideAllTab;
        save("hideAllTab", hideAllTab);
    }

    public static void toggleHideKeyboardOnChatScroll() {
        hideKeyboardOnChatScroll = !hideKeyboardOnChatScroll;
        save("hideKeyboardOnChatScroll", hideKeyboardOnChatScroll);
    }

    public static void toggleDisableVoiceAutoPlay() {
        disableVoiceAutoPlay = !disableVoiceAutoPlay;
        save("disableVoiceAutoPlay", disableVoiceAutoPlay);
    }

    public static void toggleDisableJumpToNextChannel() {
        disableJumpToNextChannel = !disableJumpToNextChannel;
        save("disableJumpToNextChannel", disableJumpToNextChannel);
    }

    public static void toggleIgnoreBlocked() {
        ignoreBlocked = !ignoreBlocked;
        save("ignoreBlocked", ignoreBlocked);
    }

    public static void toggleAskBeforeCall() {
        askBeforeCall = !askBeforeCall;
        save("askBeforeCall", askBeforeCall);
    }

    public static void setDownloadSpeedBoost(int val) {
        downloadSpeedBoost = val;
        save("downloadSpeedBoost", val);
    }

    private static void save(String key, boolean val) {
        ensurePrefs();
        if (prefs != null) prefs.edit().putBoolean(key, val).apply();
    }

    private static void save(String key, int val) {
        ensurePrefs();
        if (prefs != null) prefs.edit().putInt(key, val).apply();
    }
}

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

    // Media send confirmation
    public static boolean confirmBeforeSending = true;
    public static boolean confirmSendPhotoVideo = true;
    public static boolean confirmSendVoice = true;
    public static boolean confirmSendRoundVideo = true;
    public static boolean confirmSendGif = true;
    public static boolean confirmSendSticker = true;
    public static boolean confirmSendDocument = true;
    public static boolean confirmSendAudio = true;
    public static boolean roundVideoWatermark = false;

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
            confirmBeforeSending = prefs.getBoolean("confirmBeforeSending", true);
            confirmSendPhotoVideo = prefs.getBoolean("confirmSendPhotoVideo", true);
            confirmSendVoice = prefs.getBoolean("confirmSendVoice", true);
            confirmSendRoundVideo = prefs.getBoolean("confirmSendRoundVideo", true);
            confirmSendGif = prefs.getBoolean("confirmSendGif", true);
            confirmSendSticker = prefs.getBoolean("confirmSendSticker", true);
            confirmSendDocument = prefs.getBoolean("confirmSendDocument", true);
            confirmSendAudio = prefs.getBoolean("confirmSendAudio", true);
            roundVideoWatermark = prefs.getBoolean("roundVideoWatermark", false);
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

    public static String maskFormattedPhone(String formatted) {
        if (formatted == null || formatted.isEmpty()) {
            return "";
        }
        int totalDigits = 0;
        for (int i = 0; i < formatted.length(); i++) {
            if (Character.isDigit(formatted.charAt(i))) {
                totalDigits++;
            }
        }
        if (totalDigits <= 4) {
            return formatted;
        }

        int countryCodeDigits = 2;
        try {
            String stripped = org.telegram.PhoneFormat.PhoneFormat.stripExceptNumbers(formatted);
            org.telegram.PhoneFormat.CallingCodeInfo info = org.telegram.PhoneFormat.PhoneFormat.getInstance().findCallingCodeInfo(stripped);
            if (info != null && info.callingCode != null && !info.callingCode.isEmpty()) {
                countryCodeDigits = info.callingCode.length();
            }
        } catch (Throwable ignored) {}

        StringBuilder sb = new StringBuilder(formatted.length());
        int digitIndex = 0;
        for (int i = 0; i < formatted.length(); i++) {
            char c = formatted.charAt(i);
            if (Character.isDigit(c)) {
                digitIndex++;
                if (digitIndex <= countryCodeDigits || digitIndex > totalDigits - 2) {
                    sb.append(c);
                } else {
                    sb.append('•');
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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

    public static boolean isHistoryInScope(int account, long dialogId, long fromId) {
        return VeyraScope.HISTORY.isMessageInScope(account, dialogId, fromId, true);
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

    public static void toggleConfirmBeforeSending() {
        confirmBeforeSending = !confirmBeforeSending;
        save("confirmBeforeSending", confirmBeforeSending);
    }

    public static void toggleConfirmSendPhotoVideo() {
        confirmSendPhotoVideo = !confirmSendPhotoVideo;
        save("confirmSendPhotoVideo", confirmSendPhotoVideo);
    }

    public static void toggleConfirmSendVoice() {
        confirmSendVoice = !confirmSendVoice;
        save("confirmSendVoice", confirmSendVoice);
    }

    public static void toggleConfirmSendRoundVideo() {
        confirmSendRoundVideo = !confirmSendRoundVideo;
        save("confirmSendRoundVideo", confirmSendRoundVideo);
    }

    public static void toggleConfirmSendGif() {
        confirmSendGif = !confirmSendGif;
        save("confirmSendGif", confirmSendGif);
    }

    public static void toggleConfirmSendSticker() {
        confirmSendSticker = !confirmSendSticker;
        save("confirmSendSticker", confirmSendSticker);
    }

    public static void toggleConfirmSendDocument() {
        confirmSendDocument = !confirmSendDocument;
        save("confirmSendDocument", confirmSendDocument);
    }

    public static void toggleConfirmSendAudio() {
        confirmSendAudio = !confirmSendAudio;
        save("confirmSendAudio", confirmSendAudio);
    }

    public static void toggleRoundVideoWatermark() {
        roundVideoWatermark = !roundVideoWatermark;
        save("roundVideoWatermark", roundVideoWatermark);
    }

    public static void setDownloadSpeedBoost(int val) {
        downloadSpeedBoost = val;
        save("downloadSpeedBoost", val);
    }

    public static int getApiId(int account) {
        if (ApplicationLoader.applicationContext == null) return org.telegram.messenger.BuildVars.APP_ID;
        SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences("veyra_credentials_" + account, 0);
        int customId = p.getInt("api_id", 0);
        return customId > 0 ? customId : org.telegram.messenger.BuildVars.APP_ID;
    }

    public static String getApiHash(int account) {
        if (ApplicationLoader.applicationContext == null) return org.telegram.messenger.BuildVars.APP_HASH;
        SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences("veyra_credentials_" + account, 0);
        String customHash = p.getString("api_hash", null);
        return (customHash != null && customHash.trim().length() == 32) ? customHash.trim() : org.telegram.messenger.BuildVars.APP_HASH;
    }

    public static boolean hasCustomCredentials(int account) {
        if (ApplicationLoader.applicationContext == null) return false;
        SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences("veyra_credentials_" + account, 0);
        return p.getInt("api_id", 0) > 0 && p.getString("api_hash", "").trim().length() == 32;
    }

    public static void setCustomCredentials(int account, int apiId, String apiHash) {
        if (ApplicationLoader.applicationContext == null) return;
        SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences("veyra_credentials_" + account, 0);
        p.edit().putInt("api_id", apiId).putString("api_hash", apiHash).apply();
    }

    public static void clearCustomCredentials(int account) {
        if (ApplicationLoader.applicationContext == null) return;
        SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences("veyra_credentials_" + account, 0);
        p.edit().clear().apply();
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

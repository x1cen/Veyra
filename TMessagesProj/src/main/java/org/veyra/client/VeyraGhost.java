package org.veyra.client;

import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

// Veyra: Ghost Mode state and decision rules.
// Safety rules enforced here (not in the UI), so a wrong combination of switches cannot hurt the account:
// - every sub-option is ignored unless Ghost Mode and its parent switch are on;
// - typing "cancel" actions are never blocked, so an indicator can never get stuck on the other side;
// - mentions and view-once / self-destruct / secret media receipts are never blocked (timers and counters depend on them);
// - blocked requests never feed fake pts values into update sync.
public class VeyraGhost {
    private static SharedPreferences prefs;

    public static boolean ghostMode = true;

    // Online status
    public static boolean hideOnline = true;
    public static boolean offlineAfterMessage = false;

    // Typing and chat actions
    public static boolean hideTyping = true;
    public static boolean hideTypingText = true;
    public static boolean hideTypingVoice = true;
    public static boolean hideTypingVideo = true;
    public static boolean hideTypingFiles = true;

    // Read receipts
    public static boolean hideRead = true;
    public static boolean readOnReply = true;
    public static boolean hideSecretRead = true;
    public static boolean hideReadReactions = false;
    public static boolean hideMediaReceipts = false;

    // Views
    public static boolean hideStories = true;
    public static boolean hideChannelViews = false;

    // Low-value chats where read receipts are always sent even while hiding reads.
    public static boolean alwaysReadChannels = true;
    public static boolean alwaysReadLargeGroups = true;
    public static boolean alwaysReadBots = true;
    public static final int LARGE_GROUP_MIN_MEMBERS = 100;

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
        if (prefs == null) {
            return;
        }
        ghostMode = prefs.getBoolean("ghostMode", true);
        hideOnline = prefs.getBoolean("hideOnline", true);
        offlineAfterMessage = prefs.getBoolean("offlineAfterMessage", false);
        hideTyping = prefs.getBoolean("hideTyping", true);
        hideTypingText = prefs.getBoolean("hideTypingText", true);
        hideTypingVoice = prefs.getBoolean("hideTypingVoice", true);
        hideTypingVideo = prefs.getBoolean("hideTypingVideo", true);
        hideTypingFiles = prefs.getBoolean("hideTypingFiles", true);
        readOnReply = prefs.getBoolean("readOnReply", true);
        // Migration: the old single "readOnReply" switch both hid reads and released them on reply.
        hideRead = prefs.contains("hideRead") ? prefs.getBoolean("hideRead", true) : readOnReply;
        hideSecretRead = prefs.getBoolean("hideSecretRead", true);
        hideReadReactions = prefs.getBoolean("hideReadReactions", false);
        hideMediaReceipts = prefs.getBoolean("hideMediaReceipts", false);
        hideStories = prefs.getBoolean("hideStories", true);
        hideChannelViews = prefs.getBoolean("hideChannelViews", false);
        alwaysReadChannels = prefs.getBoolean("alwaysReadChannels", true);
        alwaysReadLargeGroups = prefs.getBoolean("alwaysReadLargeGroups", true);
        alwaysReadBots = prefs.getBoolean("alwaysReadBots", true);
    }

    private static void save(String key, boolean val) {
        ensurePrefs();
        if (prefs != null) {
            prefs.edit().putBoolean(key, val).apply();
        }
    }

    public static void toggleGhostMode() {
        ghostMode = !ghostMode;
        save("ghostMode", ghostMode);
        if (shouldHideOnline()) {
            sendOfflineNow();
        }
    }

    public static void toggleHideOnline() {
        hideOnline = !hideOnline;
        save("hideOnline", hideOnline);
        if (shouldHideOnline()) {
            sendOfflineNow();
        }
    }

    public static void toggleOfflineAfterMessage() { offlineAfterMessage = !offlineAfterMessage; save("offlineAfterMessage", offlineAfterMessage); }
    public static void toggleHideTyping() { hideTyping = !hideTyping; save("hideTyping", hideTyping); }
    public static void toggleHideTypingText() { hideTypingText = !hideTypingText; save("hideTypingText", hideTypingText); }
    public static void toggleHideTypingVoice() { hideTypingVoice = !hideTypingVoice; save("hideTypingVoice", hideTypingVoice); }
    public static void toggleHideTypingVideo() { hideTypingVideo = !hideTypingVideo; save("hideTypingVideo", hideTypingVideo); }
    public static void toggleHideTypingFiles() { hideTypingFiles = !hideTypingFiles; save("hideTypingFiles", hideTypingFiles); }
    public static void toggleHideRead() { hideRead = !hideRead; save("hideRead", hideRead); }
    public static void toggleReadOnReply() { readOnReply = !readOnReply; save("readOnReply", readOnReply); }
    public static void toggleHideSecretRead() { hideSecretRead = !hideSecretRead; save("hideSecretRead", hideSecretRead); }
    public static void toggleHideReadReactions() { hideReadReactions = !hideReadReactions; save("hideReadReactions", hideReadReactions); }
    public static void toggleHideMediaReceipts() { hideMediaReceipts = !hideMediaReceipts; save("hideMediaReceipts", hideMediaReceipts); }
    public static void toggleHideStories() { hideStories = !hideStories; save("hideStories", hideStories); }
    public static void toggleHideChannelViews() { hideChannelViews = !hideChannelViews; save("hideChannelViews", hideChannelViews); }
    public static void toggleAlwaysReadChannels() { alwaysReadChannels = !alwaysReadChannels; save("alwaysReadChannels", alwaysReadChannels); }
    public static void toggleAlwaysReadLargeGroups() { alwaysReadLargeGroups = !alwaysReadLargeGroups; save("alwaysReadLargeGroups", alwaysReadLargeGroups); }
    public static void toggleAlwaysReadBots() { alwaysReadBots = !alwaysReadBots; save("alwaysReadBots", alwaysReadBots); }

    // True when the dialog is a broadcast channel, a group above LARGE_GROUP_MIN_MEMBERS, or a bot and its exemption is on.
    // Unknown chats or unknown member counts are not exempt, so privacy is kept when in doubt.
    public static boolean isReadExempt(int account, long dialogId) {
        if (dialogId == 0 || org.telegram.messenger.DialogObject.isEncryptedDialog(dialogId)) {
            return false;
        }
        try {
            org.telegram.messenger.MessagesController mc = org.telegram.messenger.MessagesController.getInstance(account);
            if (dialogId > 0) {
                if (!alwaysReadBots) {
                    return false;
                }
                TLRPC.User user = mc.getUser(dialogId);
                return user != null && user.bot;
            }
            TLRPC.Chat chat = mc.getChat(-dialogId);
            if (chat == null) {
                return false;
            }
            if (org.telegram.messenger.ChatObject.isChannelAndNotMegaGroup(chat)) {
                return alwaysReadChannels;
            }
            if (!alwaysReadLargeGroups) {
                return false;
            }
            int count = chat.participants_count;
            if (count <= 0) {
                TLRPC.ChatFull full = mc.getChatFull(-dialogId);
                if (full != null) {
                    count = full.participants_count;
                }
            }
            return count > LARGE_GROUP_MIN_MEMBERS;
        } catch (Throwable ignore) {
            return false;
        }
    }

    // The server keeps an account "online" for minutes after the last update; go offline right away when hiding starts.
    private static void sendOfflineNow() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!UserConfig.getInstance(a).isClientActivated()) {
                continue;
            }
            TL_account.updateStatus req = new TL_account.updateStatus();
            req.offline = true;
            ConnectionsManager.getInstance(a).sendRequest(req, null);
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

    public static boolean shouldGoOfflineAfterMessage() {
        return shouldHideOnline() && offlineAfterMessage;
    }

    // action == null means plain typing (also used for secret chats, which have no action types).
    public static boolean shouldHideTypingAction(TLRPC.SendMessageAction action) {
        if (!ghostMode || !hideTyping) {
            return false;
        }
        if (action instanceof TLRPC.TL_sendMessageCancelAction) {
            return false;
        }
        if (action instanceof TLRPC.TL_sendMessageRecordAudioAction || action instanceof TLRPC.TL_sendMessageUploadAudioAction) {
            return hideTypingVoice;
        }
        if (action instanceof TLRPC.TL_sendMessageRecordVideoAction || action instanceof TLRPC.TL_sendMessageUploadVideoAction
                || action instanceof TLRPC.TL_sendMessageRecordRoundAction || action instanceof TLRPC.TL_sendMessageUploadRoundAction) {
            return hideTypingVideo;
        }
        if (action instanceof TLRPC.TL_sendMessageUploadDocumentAction || action instanceof TLRPC.TL_sendMessageUploadPhotoAction) {
            return hideTypingFiles;
        }
        return hideTypingText;
    }

    public static boolean shouldHideRead(long dialogId) {
        if (!ghostMode || !hideRead) {
            return false;
        }
        return !(readOnReply && consumeSendReadAllowed(dialogId));
    }

    public static boolean shouldHideRead(int account, long dialogId) {
        if (isReadExempt(account, dialogId)) {
            return false;
        }
        return shouldHideRead(dialogId);
    }

    public static boolean shouldHideSecretRead(long encryptedDialogId) {
        return hideSecretRead && shouldHideRead(encryptedDialogId);
    }

    public static boolean shouldHideReadReactions() {
        return ghostMode && hideRead && hideReadReactions;
    }

    public static boolean shouldHideReadReactions(int account, long dialogId) {
        return shouldHideReadReactions() && !isReadExempt(account, dialogId);
    }

    public static boolean shouldHideMediaReceipt(MessageObject messageObject) {
        if (!ghostMode || !hideRead || !hideMediaReceipts || messageObject == null || messageObject.messageOwner == null) {
            return false;
        }
        if (messageObject.messageOwner.mentioned) {
            return false;
        }
        if (isReadExempt(messageObject.currentAccount, messageObject.getDialogId())) {
            return false;
        }
        if (messageObject.isVoiceOnce() || messageObject.isRoundOnce() || messageObject.needDrawBluredPreview() || messageObject.isSecretMedia()) {
            return false;
        }
        return true;
    }

    public static boolean shouldHideStories() {
        return ghostMode && hideStories;
    }

    public static boolean shouldHideChannelViews() {
        return ghostMode && hideChannelViews;
    }
}

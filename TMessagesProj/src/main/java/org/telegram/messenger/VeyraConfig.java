package org.telegram.messenger;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VeyraConfig {
    private static SharedPreferences preferences;
    private static boolean configLoaded;

    /** The developer's Telegram user ID (injected from secret via BuildVars). Anti-delete, edit-history logging, and report/block options
     *  are bypassed when the current chat is a DM with this user (to preserve their privacy). */
    public static long getDeveloperUserId() {
        return BuildVars.DEVELOPER_USER_ID;
    }

    /** Returns true if the given dialogId is a DM with the developer. */
    public static boolean isDeveloperChat(long dialogId) {
        long devId = getDeveloperUserId();
        return devId != 0 && dialogId == devId;
    }

    /** Returns true if the message was sent BY the developer (not just in a DM with them). */
    public static boolean isDeveloperMessage(MessageObject messageObject) {
        long devId = getDeveloperUserId();
        if (devId == 0 || messageObject == null) return false;
        // Only match messages actually sent by the developer, not messages in the chat
        if (messageObject.getFromChatId() == devId) return true;
        if (messageObject.messageOwner != null && MessageObject.getPeerId(messageObject.messageOwner.from_id) == devId) return true;
        // In a private DM (not a group), out=false means it came from the other side (the developer)
        if (isDeveloperChat(messageObject.getDialogId()) && messageObject.messageOwner != null && !messageObject.messageOwner.out) return true;
        return false;
    }

    /** Returns true if the *local* user is the developer (i.e. the developer's own installation). */
    public static boolean isSelfDeveloper(int currentAccount) {
        long devId = getDeveloperUserId();
        if (devId == 0) return false;
        long selfId = UserConfig.getInstance(currentAccount).getClientUserId();
        return selfId == devId;
    }

    public static final int CATEGORY_ANTI_DELETE = 1;
    public static final int CATEGORY_EDIT_HISTORY = 2;
    public static final int CATEGORY_REACTION_HISTORY = 3;

    public static final int PEER_PRIVATE = 1;
    public static final int PEER_GROUP = 2;
    public static final int PEER_CHANNEL = 3;
    public static final int PEER_BOT = 4;

    // ==================== Privacy & Stealth ====================
    public static boolean antiDelete = true;
    public static boolean antiDeletePrivate = true;
    public static boolean antiDeleteGroups = true;
    public static boolean antiDeleteGroupsOnlyPrivate = true;
    public static boolean antiDeleteChannels = true;
    public static boolean antiDeleteChannelsOnlyPrivate = true;
    public static boolean antiDeleteBots = false;
    public static boolean ghostMode = true;
    public static boolean hideTyping = true;
    public static boolean readOnReply = true;
    public static boolean blockSecretChat = false;
    // 0 = normal (show online), 1 = hide online, 2 = hide online + offline after sending message, 3 = always appear online
    public static int onlineMode = 1;

    // Ghost Mode (Granular)
    public static boolean ghostHideOnline = true;
    public static boolean ghostHideTyping = true;
    public static boolean ghostHideUpload = true;
    public static boolean ghostHideRead = true;
    public static boolean ghostHideReadContents = true;
    public static boolean ghostHideStories = true;
    public static boolean ghostReadOnReply = true;
    public static boolean ghostHideChannelViews = false;
    public static boolean ghostHideSecretRead = true;

    private static final java.util.Set<Long> allowedReadDialogs = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    // Per-chat ghost exceptions: dialogs in this set are EXCLUDED from ghost mode (ghost disabled for them)
    private static final java.util.Set<Long> ghostExceptionDialogs = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public static void allowSendReadOnce(long dialogId) {
        allowedReadDialogs.add(dialogId);
    }

    public static boolean consumeSendReadAllowed(long dialogId) {
        // If this dialog is a ghost exception, always allow reads
        if (ghostExceptionDialogs.contains(dialogId)) return true;
        return allowedReadDialogs.remove(dialogId);
    }

    public static boolean isGhostExceptionDialog(long dialogId) {
        return ghostExceptionDialogs.contains(dialogId);
    }

    public static void setGhostException(long dialogId, boolean excluded) {
        if (excluded) {
            ghostExceptionDialogs.add(dialogId);
        } else {
            ghostExceptionDialogs.remove(dialogId);
        }
        // Persist
        if (preferences != null) {
            java.util.StringBuilder sb = new java.util.StringBuilder();
            for (long id : ghostExceptionDialogs) {
                if (sb.length() > 0) sb.append(',');
                sb.append(id);
            }
            preferences.edit().putString("ghostExceptionDialogs", sb.toString()).apply();
        }
    }

    private static void loadGhostExceptions() {
        if (preferences == null) return;
        String raw = preferences.getString("ghostExceptionDialogs", "");
        ghostExceptionDialogs.clear();
        if (!TextUtils.isEmpty(raw)) {
            for (String s : raw.split(",")) {
                try { ghostExceptionDialogs.add(Long.parseLong(s.trim())); } catch (NumberFormatException ignored) {}
            }
        }
    }

    public static boolean isGhostModeActive() {
        return ghostMode;
    }

    public static boolean isGhostHideOnline() {
        return ghostMode && ghostHideOnline;
    }

    public static boolean isGhostHideTyping() {
        return ghostMode && ghostHideTyping;
    }

    public static boolean isGhostHideUpload() {
        return ghostMode && ghostHideUpload;
    }

    public static boolean isGhostHideRead() {
        return ghostMode && ghostHideRead;
    }

    public static boolean isGhostHideReadContents() {
        return ghostMode && ghostHideReadContents;
    }

    public static boolean isGhostHideStories() {
        return ghostMode && ghostHideStories;
    }

    public static boolean isGhostReadOnReply() {
        return ghostMode && ghostReadOnReply;
    }

    public static boolean isGhostHideChannelViews() {
        return ghostMode && ghostHideChannelViews;
    }

    public static boolean isGhostHideSecretRead() {
        return ghostMode && ghostHideSecretRead;
    }

    // Security & Window
    public static boolean blockScreenCapture = false;

    // Custom Header Title (overrides "Telegram" in DialogsActivity)
    public static String customHeaderTitle = "";

    // Animated Header Title (0 = Off, 1 = Lightning, 2 = Rain, 3 = Fireworks, 4 = Meteors, 5 = Matrix, 6 = Glitch, 7 = Snow, 8 = Fire, 9 = Aurora)
    // animatedTitleMode removed — animation disabled by design

    // Media & Camera
    public static boolean disableVideoNoteWatermark = true;
    public static boolean highQualityVideoMessages = true;

    // Message Edit History
    public static boolean editHistoryEnabled = true;
    public static boolean editHistoryPrivate = true;
    public static boolean editHistoryGroups = true;
    public static boolean editHistoryGroupsOnlyPrivate = true;
    public static boolean editHistoryChannels = true;
    public static boolean editHistoryChannelsOnlyPrivate = true;
    public static boolean editHistoryBots = false;
    public static int editHistoryLimit = 10;
    public static boolean editHistoryDropOldest = true;

    public static boolean reactionHistoryEnabled = true;
    public static int reactionHistoryLimit = 20;
    public static boolean reactionHistoryDropOldest = true;
    public static boolean reactionHistoryPrivate = true;
    public static boolean reactionHistoryGroups = true;
    public static boolean reactionHistoryGroupsOnlyPrivate = true;
    public static boolean reactionHistoryChannels = false;
    public static boolean reactionHistoryChannelsOnlyPrivate = true;
    public static boolean reactionHistoryBots = false;

    // Ignore Owner / Admin in Groups & Channels (Recent Actions already log them)
    public static boolean antiDeleteGroupsIgnoreOwner = true;
    public static boolean antiDeleteGroupsIgnoreAdmin = false;
    public static boolean antiDeleteChannelsIgnoreOwner = true;
    public static boolean antiDeleteChannelsIgnoreAdmin = false;

    public static boolean editHistoryGroupsIgnoreOwner = true;
    public static boolean editHistoryGroupsIgnoreAdmin = false;
    public static boolean editHistoryChannelsIgnoreOwner = true;
    public static boolean editHistoryChannelsIgnoreAdmin = false;

    public static boolean reactionHistoryGroupsIgnoreOwner = true;
    public static boolean reactionHistoryGroupsIgnoreAdmin = false;
    public static boolean reactionHistoryChannelsIgnoreOwner = true;
    public static boolean reactionHistoryChannelsIgnoreAdmin = false;

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
    public static boolean disableDoubleTapReaction = false;
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
        antiDeletePrivate = preferences.getBoolean("antiDeletePrivate", true);
        antiDeleteGroups = preferences.getBoolean("antiDeleteGroups", true);
        antiDeleteGroupsOnlyPrivate = preferences.getBoolean("antiDeleteGroupsOnlyPrivate", true);
        antiDeleteChannels = preferences.getBoolean("antiDeleteChannels", true);
        antiDeleteChannelsOnlyPrivate = preferences.getBoolean("antiDeleteChannelsOnlyPrivate", true);
        antiDeleteBots = preferences.getBoolean("antiDeleteBots", false);
        ghostMode = preferences.getBoolean("ghostMode", true);
        ghostHideOnline = preferences.getBoolean("ghostHideOnline", true);
        ghostHideTyping = preferences.getBoolean("ghostHideTyping", true);
        ghostHideUpload = preferences.getBoolean("ghostHideUpload", true);
        ghostHideRead = preferences.getBoolean("ghostHideRead", true);
        ghostHideReadContents = preferences.getBoolean("ghostHideReadContents", true);
        ghostHideStories = preferences.getBoolean("ghostHideStories", true);
        ghostReadOnReply = preferences.getBoolean("ghostReadOnReply", true);
        readOnReply = ghostReadOnReply;
        hideTyping = ghostHideTyping;
        ghostHideChannelViews = preferences.getBoolean("ghostHideChannelViews", false);
        ghostHideSecretRead = preferences.getBoolean("ghostHideSecretRead", true);
        blockScreenCapture = preferences.getBoolean("blockScreenCapture", false);
        customHeaderTitle = preferences.getString("customHeaderTitle", "");
        // animatedTitleMode intentionally not loaded — removed by design

        disableVideoNoteWatermark = preferences.getBoolean("disableVideoNoteWatermark", true);
        highQualityVideoMessages = preferences.getBoolean("highQualityVideoMessages", true);

        editHistoryEnabled = preferences.getBoolean("editHistoryEnabled", true);
        editHistoryPrivate = preferences.getBoolean("editHistoryPrivate", true);
        editHistoryGroups = preferences.getBoolean("editHistoryGroups", true);
        editHistoryGroupsOnlyPrivate = preferences.getBoolean("editHistoryGroupsOnlyPrivate", true);
        editHistoryChannels = preferences.getBoolean("editHistoryChannels", true);
        editHistoryChannelsOnlyPrivate = preferences.getBoolean("editHistoryChannelsOnlyPrivate", true);
        editHistoryBots = preferences.getBoolean("editHistoryBots", false);
        editHistoryLimit = preferences.getInt("editHistoryLimit", 10);
        editHistoryDropOldest = preferences.getBoolean("editHistoryDropOldest", true);

        reactionHistoryEnabled = preferences.getBoolean("reactionHistoryEnabled", true);
        reactionHistoryLimit = preferences.getInt("reactionHistoryLimit", 20);
        reactionHistoryDropOldest = preferences.getBoolean("reactionHistoryDropOldest", true);
        reactionHistoryPrivate = preferences.getBoolean("reactionHistoryPrivate", true);
        reactionHistoryGroups = preferences.getBoolean("reactionHistoryGroups", true);
        reactionHistoryGroupsOnlyPrivate = preferences.getBoolean("reactionHistoryGroupsOnlyPrivate", true);
        reactionHistoryChannels = preferences.getBoolean("reactionHistoryChannels", false);
        reactionHistoryChannelsOnlyPrivate = preferences.getBoolean("reactionHistoryChannelsOnlyPrivate", true);
        reactionHistoryBots = preferences.getBoolean("reactionHistoryBots", false);

        antiDeleteGroupsIgnoreOwner = preferences.getBoolean("antiDeleteGroupsIgnoreOwner", true);
        antiDeleteGroupsIgnoreAdmin = preferences.getBoolean("antiDeleteGroupsIgnoreAdmin", false);
        antiDeleteChannelsIgnoreOwner = preferences.getBoolean("antiDeleteChannelsIgnoreOwner", true);
        antiDeleteChannelsIgnoreAdmin = preferences.getBoolean("antiDeleteChannelsIgnoreAdmin", false);

        editHistoryGroupsIgnoreOwner = preferences.getBoolean("editHistoryGroupsIgnoreOwner", true);
        editHistoryGroupsIgnoreAdmin = preferences.getBoolean("editHistoryGroupsIgnoreAdmin", false);
        editHistoryChannelsIgnoreOwner = preferences.getBoolean("editHistoryChannelsIgnoreOwner", true);
        editHistoryChannelsIgnoreAdmin = preferences.getBoolean("editHistoryChannelsIgnoreAdmin", false);

        reactionHistoryGroupsIgnoreOwner = preferences.getBoolean("reactionHistoryGroupsIgnoreOwner", true);
        reactionHistoryGroupsIgnoreAdmin = preferences.getBoolean("reactionHistoryGroupsIgnoreAdmin", false);
        reactionHistoryChannelsIgnoreOwner = preferences.getBoolean("reactionHistoryChannelsIgnoreOwner", true);
        reactionHistoryChannelsIgnoreAdmin = preferences.getBoolean("reactionHistoryChannelsIgnoreAdmin", false);

        loadExceptions(CATEGORY_ANTI_DELETE, preferences.getString("antiDeleteExceptions", ""));
        loadExceptions(CATEGORY_EDIT_HISTORY, preferences.getString("editHistoryExceptions", ""));
        loadExceptions(CATEGORY_REACTION_HISTORY, preferences.getString("reactionHistoryExceptions", ""));
        loadGhostExceptions();
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
        disableDoubleTapReaction = disableQuickReaction;
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

    private static final HashMap<Long, Boolean> antiDeleteExceptions = new HashMap<>();
    private static final HashMap<Long, Boolean> editHistoryExceptions = new HashMap<>();
    private static final HashMap<Long, Boolean> reactionHistoryExceptions = new HashMap<>();

    public static HashMap<Long, Boolean> getExceptions(int category) {
        if (category == CATEGORY_ANTI_DELETE) return antiDeleteExceptions;
        if (category == CATEGORY_EDIT_HISTORY) return editHistoryExceptions;
        return reactionHistoryExceptions;
    }

    public static Boolean getException(int category, long dialogId) {
        HashMap<Long, Boolean> map = getExceptions(category);
        synchronized (map) {
            return map.get(dialogId);
        }
    }

    public static void addException(int category, long dialogId, boolean enabled) {
        HashMap<Long, Boolean> map = getExceptions(category);
        synchronized (map) {
            map.put(dialogId, enabled);
            saveExceptions(category);
        }
    }

    public static void removeException(int category, long dialogId) {
        HashMap<Long, Boolean> map = getExceptions(category);
        synchronized (map) {
            map.remove(dialogId);
            saveExceptions(category);
        }
    }

    public static void clearExceptions(int category, int peerType) {
        HashMap<Long, Boolean> map = getExceptions(category);
        synchronized (map) {
            int currentAccount = UserConfig.selectedAccount;
            ArrayList<Long> toRemove = new ArrayList<>();
            for (Long did : map.keySet()) {
                if (peerType == PEER_PRIVATE && did > 0) {
                    org.telegram.tgnet.TLRPC.User u = MessagesController.getInstance(currentAccount).getUser(did);
                    if (u == null || !u.bot) toRemove.add(did);
                } else if (peerType == PEER_BOT && did > 0) {
                    org.telegram.tgnet.TLRPC.User u = MessagesController.getInstance(currentAccount).getUser(did);
                    if (u != null && u.bot) toRemove.add(did);
                } else if (peerType == PEER_GROUP && did < 0) {
                    org.telegram.tgnet.TLRPC.Chat c = MessagesController.getInstance(currentAccount).getChat(-did);
                    if (c == null || !ChatObject.isChannel(c) || c.megagroup) toRemove.add(did);
                } else if (peerType == PEER_CHANNEL && did < 0) {
                    org.telegram.tgnet.TLRPC.Chat c = MessagesController.getInstance(currentAccount).getChat(-did);
                    if (c != null && ChatObject.isChannel(c) && !c.megagroup) toRemove.add(did);
                }
            }
            for (Long did : toRemove) {
                map.remove(did);
            }
            saveExceptions(category);
        }
    }

    private static void saveExceptions(int category) {
        HashMap<Long, Boolean> map = getExceptions(category);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Long, Boolean> entry : map.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        String key = category == CATEGORY_ANTI_DELETE ? "antiDeleteExceptions" : (category == CATEGORY_EDIT_HISTORY ? "editHistoryExceptions" : "reactionHistoryExceptions");
        save(key, sb.toString());
    }

    private static void loadExceptions(int category, String raw) {
        HashMap<Long, Boolean> map = getExceptions(category);
        map.clear();
        if (TextUtils.isEmpty(raw)) return;
        String[] parts = raw.split(";");
        for (String p : parts) {
            String[] kv = p.split(":");
            if (kv.length == 2) {
                try {
                    map.put(Long.parseLong(kv[0]), Boolean.parseBoolean(kv[1]));
                } catch (Exception ignored) {}
            }
        }
    }

    public static boolean isPeerEnabled(int category, int peerType) {
        if (category == CATEGORY_ANTI_DELETE) {
            if (peerType == PEER_PRIVATE) return antiDeletePrivate;
            if (peerType == PEER_GROUP) return antiDeleteGroups;
            if (peerType == PEER_CHANNEL) return antiDeleteChannels;
            if (peerType == PEER_BOT) return antiDeleteBots;
        } else if (category == CATEGORY_EDIT_HISTORY) {
            if (peerType == PEER_PRIVATE) return editHistoryPrivate;
            if (peerType == PEER_GROUP) return editHistoryGroups;
            if (peerType == PEER_CHANNEL) return editHistoryChannels;
            if (peerType == PEER_BOT) return editHistoryBots;
        } else if (category == CATEGORY_REACTION_HISTORY) {
            if (peerType == PEER_PRIVATE) return reactionHistoryPrivate;
            if (peerType == PEER_GROUP) return reactionHistoryGroups;
            if (peerType == PEER_CHANNEL) return reactionHistoryChannels;
            if (peerType == PEER_BOT) return reactionHistoryBots;
        }
        return false;
    }

    public static void setPeerEnabled(int category, int peerType, boolean val) {
        if (category == CATEGORY_ANTI_DELETE) {
            if (peerType == PEER_PRIVATE) { antiDeletePrivate = val; save("antiDeletePrivate", val); }
            else if (peerType == PEER_GROUP) { antiDeleteGroups = val; save("antiDeleteGroups", val); }
            else if (peerType == PEER_CHANNEL) { antiDeleteChannels = val; save("antiDeleteChannels", val); }
            else if (peerType == PEER_BOT) { antiDeleteBots = val; save("antiDeleteBots", val); }
        } else if (category == CATEGORY_EDIT_HISTORY) {
            if (peerType == PEER_PRIVATE) { editHistoryPrivate = val; save("editHistoryPrivate", val); }
            else if (peerType == PEER_GROUP) { editHistoryGroups = val; save("editHistoryGroups", val); }
            else if (peerType == PEER_CHANNEL) { editHistoryChannels = val; save("editHistoryChannels", val); }
            else if (peerType == PEER_BOT) { editHistoryBots = val; save("editHistoryBots", val); }
        } else if (category == CATEGORY_REACTION_HISTORY) {
            if (peerType == PEER_PRIVATE) { reactionHistoryPrivate = val; save("reactionHistoryPrivate", val); }
            else if (peerType == PEER_GROUP) { reactionHistoryGroups = val; save("reactionHistoryGroups", val); }
            else if (peerType == PEER_CHANNEL) { reactionHistoryChannels = val; save("reactionHistoryChannels", val); }
            else if (peerType == PEER_BOT) { reactionHistoryBots = val; save("reactionHistoryBots", val); }
        }
    }

    public static boolean isOnlyPrivate(int category, int peerType) {
        if (category == CATEGORY_ANTI_DELETE) {
            if (peerType == PEER_GROUP) return antiDeleteGroupsOnlyPrivate;
            if (peerType == PEER_CHANNEL) return antiDeleteChannelsOnlyPrivate;
        } else if (category == CATEGORY_EDIT_HISTORY) {
            if (peerType == PEER_GROUP) return editHistoryGroupsOnlyPrivate;
            if (peerType == PEER_CHANNEL) return editHistoryChannelsOnlyPrivate;
        } else if (category == CATEGORY_REACTION_HISTORY) {
            if (peerType == PEER_GROUP) return reactionHistoryGroupsOnlyPrivate;
            if (peerType == PEER_CHANNEL) return reactionHistoryChannelsOnlyPrivate;
        }
        return false;
    }

    public static void setOnlyPrivate(int category, int peerType, boolean val) {
        if (category == CATEGORY_ANTI_DELETE) {
            if (peerType == PEER_GROUP) { antiDeleteGroupsOnlyPrivate = val; save("antiDeleteGroupsOnlyPrivate", val); }
            else if (peerType == PEER_CHANNEL) { antiDeleteChannelsOnlyPrivate = val; save("antiDeleteChannelsOnlyPrivate", val); }
        } else if (category == CATEGORY_EDIT_HISTORY) {
            if (peerType == PEER_GROUP) { editHistoryGroupsOnlyPrivate = val; save("editHistoryGroupsOnlyPrivate", val); }
            else if (peerType == PEER_CHANNEL) { editHistoryChannelsOnlyPrivate = val; save("editHistoryChannelsOnlyPrivate", val); }
        } else if (category == CATEGORY_REACTION_HISTORY) {
            if (peerType == PEER_GROUP) { reactionHistoryGroupsOnlyPrivate = val; save("reactionHistoryGroupsOnlyPrivate", val); }
            else if (peerType == PEER_CHANNEL) { reactionHistoryChannelsOnlyPrivate = val; save("reactionHistoryChannelsOnlyPrivate", val); }
        }
    }

    public static boolean isIgnoreOwner(int category, int peerType) {
        if (category == CATEGORY_ANTI_DELETE) {
            if (peerType == PEER_GROUP) return antiDeleteGroupsIgnoreOwner;
            if (peerType == PEER_CHANNEL) return antiDeleteChannelsIgnoreOwner;
        } else if (category == CATEGORY_EDIT_HISTORY) {
            if (peerType == PEER_GROUP) return editHistoryGroupsIgnoreOwner;
            if (peerType == PEER_CHANNEL) return editHistoryChannelsIgnoreOwner;
        } else if (category == CATEGORY_REACTION_HISTORY) {
            if (peerType == PEER_GROUP) return reactionHistoryGroupsIgnoreOwner;
            if (peerType == PEER_CHANNEL) return reactionHistoryChannelsIgnoreOwner;
        }
        return false;
    }

    public static void setIgnoreOwner(int category, int peerType, boolean val) {
        if (category == CATEGORY_ANTI_DELETE) {
            if (peerType == PEER_GROUP) { antiDeleteGroupsIgnoreOwner = val; save("antiDeleteGroupsIgnoreOwner", val); }
            else if (peerType == PEER_CHANNEL) { antiDeleteChannelsIgnoreOwner = val; save("antiDeleteChannelsIgnoreOwner", val); }
        } else if (category == CATEGORY_EDIT_HISTORY) {
            if (peerType == PEER_GROUP) { editHistoryGroupsIgnoreOwner = val; save("editHistoryGroupsIgnoreOwner", val); }
            else if (peerType == PEER_CHANNEL) { editHistoryChannelsIgnoreOwner = val; save("editHistoryChannelsIgnoreOwner", val); }
        } else if (category == CATEGORY_REACTION_HISTORY) {
            if (peerType == PEER_GROUP) { reactionHistoryGroupsIgnoreOwner = val; save("reactionHistoryGroupsIgnoreOwner", val); }
            else if (peerType == PEER_CHANNEL) { reactionHistoryChannelsIgnoreOwner = val; save("reactionHistoryChannelsIgnoreOwner", val); }
        }
    }

    public static boolean isIgnoreAdmin(int category, int peerType) {
        if (category == CATEGORY_ANTI_DELETE) {
            if (peerType == PEER_GROUP) return antiDeleteGroupsIgnoreAdmin;
            if (peerType == PEER_CHANNEL) return antiDeleteChannelsIgnoreAdmin;
        } else if (category == CATEGORY_EDIT_HISTORY) {
            if (peerType == PEER_GROUP) return editHistoryGroupsIgnoreAdmin;
            if (peerType == PEER_CHANNEL) return editHistoryChannelsIgnoreAdmin;
        } else if (category == CATEGORY_REACTION_HISTORY) {
            if (peerType == PEER_GROUP) return reactionHistoryGroupsIgnoreAdmin;
            if (peerType == PEER_CHANNEL) return reactionHistoryChannelsIgnoreAdmin;
        }
        return false;
    }

    public static void setIgnoreAdmin(int category, int peerType, boolean val) {
        if (category == CATEGORY_ANTI_DELETE) {
            if (peerType == PEER_GROUP) { antiDeleteGroupsIgnoreAdmin = val; save("antiDeleteGroupsIgnoreAdmin", val); }
            else if (peerType == PEER_CHANNEL) { antiDeleteChannelsIgnoreAdmin = val; save("antiDeleteChannelsIgnoreAdmin", val); }
        } else if (category == CATEGORY_EDIT_HISTORY) {
            if (peerType == PEER_GROUP) { editHistoryGroupsIgnoreAdmin = val; save("editHistoryGroupsIgnoreAdmin", val); }
            else if (peerType == PEER_CHANNEL) { editHistoryChannelsIgnoreAdmin = val; save("editHistoryChannelsIgnoreAdmin", val); }
        } else if (category == CATEGORY_REACTION_HISTORY) {
            if (peerType == PEER_GROUP) { reactionHistoryGroupsIgnoreAdmin = val; save("reactionHistoryGroupsIgnoreAdmin", val); }
            else if (peerType == PEER_CHANNEL) { reactionHistoryChannelsIgnoreAdmin = val; save("reactionHistoryChannelsIgnoreAdmin", val); }
        }
    }

    public static void setAntiDelete(boolean val) {
        antiDelete = val;
        save("antiDelete", val);
    }
    public static void setAntiDeletePrivate(boolean val) {
        antiDeletePrivate = val;
        save("antiDeletePrivate", val);
    }
    public static void setAntiDeleteGroups(boolean val) {
        antiDeleteGroups = val;
        save("antiDeleteGroups", val);
    }
    public static void setAntiDeleteChannels(boolean val) {
        antiDeleteChannels = val;
        save("antiDeleteChannels", val);
    }
    public static void setAntiDeleteBots(boolean val) {
        antiDeleteBots = val;
        save("antiDeleteBots", val);
    }

    public static org.telegram.tgnet.TLRPC.Chat findChat(int currentAccount, long chatId) {
        org.telegram.tgnet.TLRPC.Chat chat = null;
        try {
            chat = MessagesController.getInstance(currentAccount).getChat(chatId);
        } catch (Throwable ignored) {}
        if (chat == null) {
            try {
                MessagesStorage storage = MessagesStorage.getInstance(currentAccount);
                if (storage != null) {
                    if (Thread.currentThread() == storage.getStorageQueue().getHandler().getLooper().getThread()) {
                        chat = storage.getChat(chatId);
                    } else {
                        chat = storage.getChatSync(chatId);
                    }
                }
            } catch (Throwable ignored) {}
        }
        return chat;
    }

    public static boolean isChatTypeAllowedForAntiDelete(long dialogId) {
        if (!antiDelete) return false;
        Boolean exc = getException(CATEGORY_ANTI_DELETE, dialogId);
        if (exc != null) return exc;
        try {
            int currentAccount = UserConfig.selectedAccount;
            if (dialogId > 0) {
                org.telegram.tgnet.TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
                if (user != null && user.bot) {
                    return antiDeleteBots;
                }
                return antiDeletePrivate;
            } else {
                long chatId = -dialogId;
                org.telegram.tgnet.TLRPC.Chat chat = findChat(currentAccount, chatId);
                if (chat == null) return false;
                boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
                boolean isPublic = ChatObject.isPublic(chat);
                boolean isPrivate = !isPublic;
                boolean isOwner = chat.creator;
                boolean isAdmin = ChatObject.hasAdminRights(chat);
                if (isChannel) {
                    if (!antiDeleteChannels) return false;
                    if (antiDeleteChannelsOnlyPrivate && !isPrivate) return false;
                    if (antiDeleteChannelsIgnoreOwner && isOwner) return false;
                    if (antiDeleteChannelsIgnoreAdmin && isAdmin) return false;
                    return true;
                } else {
                    if (!antiDeleteGroups) return false;
                    if (antiDeleteGroupsOnlyPrivate && !isPrivate) return false;
                    if (antiDeleteGroupsIgnoreOwner && isOwner) return false;
                    if (antiDeleteGroupsIgnoreAdmin && isAdmin) return false;
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isChatTypeAllowedForEditHistory(long dialogId) {
        if (!editHistoryEnabled) return false;
        Boolean exc = getException(CATEGORY_EDIT_HISTORY, dialogId);
        if (exc != null) return exc;
        try {
            int currentAccount = UserConfig.selectedAccount;
            if (dialogId > 0) {
                org.telegram.tgnet.TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
                if (user != null && user.bot) {
                    return editHistoryBots;
                }
                return editHistoryPrivate;
            } else {
                long chatId = -dialogId;
                org.telegram.tgnet.TLRPC.Chat chat = findChat(currentAccount, chatId);
                if (chat == null) return false;
                boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
                boolean isPublic = ChatObject.isPublic(chat);
                boolean isPrivate = !isPublic;
                boolean isOwner = chat.creator;
                boolean isAdmin = ChatObject.hasAdminRights(chat);
                if (isChannel) {
                    if (!editHistoryChannels) return false;
                    if (editHistoryChannelsOnlyPrivate && !isPrivate) return false;
                    if (editHistoryChannelsIgnoreOwner && isOwner) return false;
                    if (editHistoryChannelsIgnoreAdmin && isAdmin) return false;
                    return true;
                } else {
                    if (!editHistoryGroups) return false;
                    if (editHistoryGroupsOnlyPrivate && !isPrivate) return false;
                    if (editHistoryGroupsIgnoreOwner && isOwner) return false;
                    if (editHistoryGroupsIgnoreAdmin && isAdmin) return false;
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isChatTypeAllowedForReactionHistory(long dialogId) {
        if (!reactionHistoryEnabled) return false;
        Boolean exc = getException(CATEGORY_REACTION_HISTORY, dialogId);
        if (exc != null) return exc;
        try {
            int currentAccount = UserConfig.selectedAccount;
            if (dialogId > 0) {
                org.telegram.tgnet.TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
                if (user != null && user.bot) {
                    return reactionHistoryBots;
                }
                return reactionHistoryPrivate;
            } else {
                long chatId = -dialogId;
                org.telegram.tgnet.TLRPC.Chat chat = findChat(currentAccount, chatId);
                if (chat == null) return false;
                boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
                boolean isPublic = ChatObject.isPublic(chat);
                boolean isPrivate = !isPublic;
                boolean isOwner = chat.creator;
                boolean isAdmin = ChatObject.hasAdminRights(chat);
                if (isChannel) {
                    if (!reactionHistoryChannels) return false;
                    if (reactionHistoryChannelsOnlyPrivate && !isPrivate) return false;
                    if (reactionHistoryChannelsIgnoreOwner && isOwner) return false;
                    if (reactionHistoryChannelsIgnoreAdmin && isAdmin) return false;
                    return true;
                } else {
                    if (!reactionHistoryGroups) return false;
                    if (reactionHistoryGroupsOnlyPrivate && !isPrivate) return false;
                    if (reactionHistoryGroupsIgnoreOwner && isOwner) return false;
                    if (reactionHistoryGroupsIgnoreAdmin && isAdmin) return false;
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }
    public static void setGhostMode(boolean val) {
        ghostMode = val;
        save("ghostMode", val);
    }
    public static void setGhostHideOnline(boolean val) {
        ghostHideOnline = val;
        save("ghostHideOnline", val);
    }
    public static void setGhostHideTyping(boolean val) {
        ghostHideTyping = val;
        hideTyping = val;
        save("ghostHideTyping", val);
        save("hideTyping", val);
    }
    public static void setGhostHideUpload(boolean val) {
        ghostHideUpload = val;
        save("ghostHideUpload", val);
    }
    public static void setGhostHideRead(boolean val) {
        ghostHideRead = val;
        save("ghostHideRead", val);
    }
    public static void setGhostHideReadContents(boolean val) {
        ghostHideReadContents = val;
        save("ghostHideReadContents", val);
    }
    public static void setGhostHideStories(boolean val) {
        ghostHideStories = val;
        save("ghostHideStories", val);
    }
    public static void setGhostReadOnReply(boolean val) {
        ghostReadOnReply = val;
        readOnReply = val;
        save("ghostReadOnReply", val);
        save("readOnReply", val);
    }
    public static void setGhostHideChannelViews(boolean val) {
        ghostHideChannelViews = val;
        save("ghostHideChannelViews", val);
    }
    public static void setGhostHideSecretRead(boolean val) {
        ghostHideSecretRead = val;
        save("ghostHideSecretRead", val);
    }
    public static void setBlockScreenCapture(boolean val) {
        blockScreenCapture = val;
        save("blockScreenCapture", val);
    }
    public static void setCustomHeaderTitle(String title) {
        customHeaderTitle = title != null ? title.trim() : "";
        save("customHeaderTitle", customHeaderTitle);
        // Notify DialogsActivity to update header immediately
        for (int i = 0; i < org.telegram.messenger.UserConfig.MAX_ACCOUNT_COUNT; i++) {
            org.telegram.messenger.NotificationCenter.getInstance(i)
                    .postNotificationName(org.telegram.messenger.NotificationCenter.veyraHeaderTitleChanged);
        }
    }
    // setAnimatedTitleMode removed — animation disabled by design

    public static void setDisableVideoNoteWatermark(boolean val) {
        disableVideoNoteWatermark = val;
        save("disableVideoNoteWatermark", val);
    }
    public static void setHighQualityVideoMessages(boolean val) {
        highQualityVideoMessages = val;
        save("highQualityVideoMessages", val);
    }

    public static void setEditHistoryEnabled(boolean val) {
        editHistoryEnabled = val;
        save("editHistoryEnabled", val);
    }
    public static void setReactionHistoryEnabled(boolean val) {
        reactionHistoryEnabled = val;
        save("reactionHistoryEnabled", val);
    }
    public static void setReactionHistoryLimit(int val) {
        reactionHistoryLimit = val;
        save("reactionHistoryLimit", val);
    }
    public static void setReactionHistoryDropOldest(boolean val) {
        reactionHistoryDropOldest = val;
        save("reactionHistoryDropOldest", val);
    }
    public static void setReactionHistoryPrivate(boolean val) {
        reactionHistoryPrivate = val;
        save("reactionHistoryPrivate", val);
    }
    public static void setReactionHistoryGroups(boolean val) {
        reactionHistoryGroups = val;
        save("reactionHistoryGroups", val);
    }
    public static void setReactionHistoryChannels(boolean val) {
        reactionHistoryChannels = val;
        save("reactionHistoryChannels", val);
    }
    public static void setReactionHistoryBots(boolean val) {
        reactionHistoryBots = val;
        save("reactionHistoryBots", val);
    }
    public static void setEditHistoryPrivate(boolean val) {
        editHistoryPrivate = val;
        save("editHistoryPrivate", val);
    }
    public static void setEditHistoryGroups(boolean val) {
        editHistoryGroups = val;
        save("editHistoryGroups", val);
    }
    public static void setEditHistoryChannels(boolean val) {
        editHistoryChannels = val;
        save("editHistoryChannels", val);
    }
    public static void setEditHistoryBots(boolean val) {
        editHistoryBots = val;
        save("editHistoryBots", val);
    }
    public static void setEditHistoryLimit(int val) {
        editHistoryLimit = val;
        save("editHistoryLimit", val);
    }
    public static void setEditHistoryDropOldest(boolean val) {
        editHistoryDropOldest = val;
        save("editHistoryDropOldest", val);
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
        disableDoubleTapReaction = val;
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

    private static void save(String key, int val) {
        if (preferences == null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("veyraconfig", 0);
        }
        preferences.edit().putInt(key, val).apply();
    }

    private static void save(String key, String val) {
        if (preferences == null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("veyraconfig", 0);
        }
        preferences.edit().putString(key, val != null ? val : "").apply();
    }
}

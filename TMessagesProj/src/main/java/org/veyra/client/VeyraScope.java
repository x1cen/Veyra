package org.veyra.client;

import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Veyra: reusable per-feature scope model shared by Ghost read-hiding, anti-delete, and
// edit/reaction history. Each feature owns one instance keyed by a unique prefs prefix.
//
// Decision model (isInScope):
//   - master switch off (default) -> feature applies everywhere;
//   - chat has a manual override (ticked/unticked in Included Chats) -> the override wins;
//   - otherwise the Chat Types options decide (isDefaultIncluded).
//
// Chat Types:
//   Private Chats, Bots;
//   Private Groups / Public Groups (parents), each with Small / Large sub-options that only
//   count while the parent is on; Skip Groups I Own or Admin;
//   Private Channels / Public Channels; Skip Channels I Own or Admin.
public class VeyraScope {

    public static final String OPT_PRIVATE = "private";
    public static final String OPT_BOTS = "bots";
    public static final String OPT_PRIVATE_GROUPS = "privGroups";
    public static final String OPT_PRIVATE_GROUPS_SMALL = "privGroupsSmall";
    public static final String OPT_PRIVATE_GROUPS_LARGE = "privGroupsLarge";
    public static final String OPT_PUBLIC_GROUPS = "pubGroups";
    public static final String OPT_PUBLIC_GROUPS_SMALL = "pubGroupsSmall";
    public static final String OPT_PUBLIC_GROUPS_LARGE = "pubGroupsLarge";
    public static final String OPT_PRIVATE_CHANNELS = "privChannels";
    public static final String OPT_PUBLIC_CHANNELS = "pubChannels";
    public static final String OPT_SKIP_GROUPS_OWN_ADMIN = "skipGroupsOwnAdmin";
    public static final String OPT_SKIP_CHANNELS_OWN_ADMIN = "skipChannelsOwnAdmin";

    private static final String[] ALL_OPTS = {
            OPT_PRIVATE, OPT_BOTS,
            OPT_PRIVATE_GROUPS, OPT_PRIVATE_GROUPS_SMALL, OPT_PRIVATE_GROUPS_LARGE,
            OPT_PUBLIC_GROUPS, OPT_PUBLIC_GROUPS_SMALL, OPT_PUBLIC_GROUPS_LARGE,
            OPT_PRIVATE_CHANNELS, OPT_PUBLIC_CHANNELS,
            OPT_SKIP_GROUPS_OWN_ADMIN, OPT_SKIP_CHANNELS_OWN_ADMIN
    };

    // Buckets returned by classify(); group size is reported separately by groupSize().
    public static final int PEER_PRIVATE = 0;
    public static final int PEER_BOT = 1;
    public static final int PEER_PRIVATE_GROUP = 2;
    public static final int PEER_PUBLIC_GROUP = 3;
    public static final int PEER_PRIVATE_CHANNEL = 4;
    public static final int PEER_PUBLIC_CHANNEL = 5;
    public static final int PEER_COUNT = 6;

    public static final int SIZE_UNKNOWN = 0;
    public static final int SIZE_SMALL = 1;
    public static final int SIZE_LARGE = 2;

    // Matches Telegram's chatReadMarkSizeThreshold: the member count above which the
    // "Seen by" list is no longer shown.
    public static final int LARGE_GROUP_MIN_MEMBERS = 100;

    public static final int SCOPE_GHOST_READ = 0;
    public static final int SCOPE_ANTI_DELETE = 1;
    public static final int SCOPE_HISTORY = 2;

    private static final HashMap<Integer, VeyraScope> REGISTRY = new HashMap<>();

    public static VeyraScope byId(int id) {
        return REGISTRY.get(id);
    }

    private static SharedPreferences prefs;

    private static SharedPreferences prefs() {
        if (prefs == null && ApplicationLoader.applicationContext != null) {
            prefs = ApplicationLoader.applicationContext.getSharedPreferences("veyra_scope", 0);
        }
        return prefs;
    }

    // All options off except the listed ones.
    private static HashMap<String, Boolean> defs(String... onKeys) {
        HashMap<String, Boolean> m = new HashMap<>();
        for (int i = 0; i < ALL_OPTS.length; i++) {
            m.put(ALL_OPTS[i], false);
        }
        for (int i = 0; i < onKeys.length; i++) {
            m.put(onKeys[i], true);
        }
        return m;
    }

    public final int id;
    private final String key;
    private final HashMap<String, Boolean> defaults;

    public boolean limitEnabled;
    private final ConcurrentHashMap<String, Boolean> options = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> overrides = new ConcurrentHashMap<>();

    public VeyraScope(int id, String key, HashMap<String, Boolean> defaults) {
        this.id = id;
        this.key = key;
        this.defaults = defaults;
        options.putAll(defaults);
        load();
        REGISTRY.put(id, this);
    }

    private String optKey(String opt) {
        return key + "_o_" + opt;
    }

    public void load() {
        SharedPreferences p = prefs();
        if (p == null) {
            return;
        }
        limitEnabled = p.getBoolean(key + "_limit", false);
        SharedPreferences.Editor ed = p.edit();
        boolean migrated = false;
        for (int i = 0; i < ALL_OPTS.length; i++) {
            String opt = ALL_OPTS[i];
            Boolean def = defaults.get(opt);
            boolean value = def != null && def;
            if (p.contains(optKey(opt))) {
                value = p.getBoolean(optKey(opt), value);
            } else {
                Boolean legacy = legacyValue(p, opt);
                if (legacy != null) {
                    value = legacy;
                    ed.putBoolean(optKey(opt), value);
                    migrated = true;
                }
            }
            options.put(opt, value);
        }
        if (migrated) {
            for (int i = 0; i < 5; i++) {
                ed.remove(key + "_peer" + i);
            }
            ed.remove(key + "_skipOwnAdmin");
            ed.apply();
        }
        overrides.clear();
        String raw = p.getString(key + "_ovr", null);
        if (raw != null) {
            parseOverrides(raw);
        } else {
            // Migration from the earlier Include/Exclude lists.
            addLegacy(p.getString(key + "_incl", ""), true);
            addLegacy(p.getString(key + "_excl", ""), false);
            if (!overrides.isEmpty()) {
                saveOverrides();
            }
            p.edit().remove(key + "_incl").remove(key + "_excl").apply();
        }
    }

    // Maps the previous Private/SmallGroup/LargeGroup/Channel/Bot switches onto the new options.
    private Boolean legacyValue(SharedPreferences p, String opt) {
        String k0 = key + "_peer0", k1 = key + "_peer1", k2 = key + "_peer2", k3 = key + "_peer3", k4 = key + "_peer4";
        String ks = key + "_skipOwnAdmin";
        switch (opt) {
            case OPT_PRIVATE:
                return p.contains(k0) ? p.getBoolean(k0, true) : null;
            case OPT_BOTS:
                return p.contains(k4) ? p.getBoolean(k4, false) : null;
            case OPT_PRIVATE_GROUPS_SMALL:
            case OPT_PUBLIC_GROUPS_SMALL:
                return p.contains(k1) ? p.getBoolean(k1, true) : null;
            case OPT_PRIVATE_GROUPS_LARGE:
            case OPT_PUBLIC_GROUPS_LARGE:
                return p.contains(k2) ? p.getBoolean(k2, false) : null;
            case OPT_PRIVATE_GROUPS:
            case OPT_PUBLIC_GROUPS:
                if (!p.contains(k1) && !p.contains(k2)) return null;
                return p.getBoolean(k1, false) || p.getBoolean(k2, false);
            case OPT_PRIVATE_CHANNELS:
            case OPT_PUBLIC_CHANNELS:
                return p.contains(k3) ? p.getBoolean(k3, false) : null;
            case OPT_SKIP_GROUPS_OWN_ADMIN:
            case OPT_SKIP_CHANNELS_OWN_ADMIN:
                return p.contains(ks) ? p.getBoolean(ks, false) : null;
            default:
                return null;
        }
    }

    private void parseOverrides(String raw) {
        if (raw.length() == 0) {
            return;
        }
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            String[] kv = parts[i].split(":");
            if (kv.length != 2) {
                continue;
            }
            try {
                overrides.put(Long.parseLong(kv[0].trim()), "1".equals(kv[1].trim()));
            } catch (NumberFormatException ignore) {
            }
        }
    }

    private void addLegacy(String raw, boolean value) {
        if (raw == null || raw.length() == 0) {
            return;
        }
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            try {
                overrides.put(Long.parseLong(parts[i].trim()), value);
            } catch (NumberFormatException ignore) {
            }
        }
    }

    private void saveOverrides() {
        SharedPreferences p = prefs();
        if (p == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Long, Boolean> e : overrides.entrySet()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(e.getKey()).append(":").append(e.getValue() ? "1" : "0");
        }
        p.edit().putString(key + "_ovr", sb.toString()).apply();
    }

    public void setLimitEnabled(boolean val) {
        limitEnabled = val;
        SharedPreferences p = prefs();
        if (p != null) {
            p.edit().putBoolean(key + "_limit", val).apply();
        }
    }

    public boolean isOn(String opt) {
        Boolean v = options.get(opt);
        return v != null && v;
    }

    public void setOn(String opt, boolean val) {
        options.put(opt, val);
        SharedPreferences p = prefs();
        if (p != null) {
            p.edit().putBoolean(optKey(opt), val).apply();
        }
    }

    public Boolean getOverride(long dialogId) {
        return overrides.get(dialogId);
    }

    public int getOverrideCount() {
        return overrides.size();
    }

    public ArrayList<Long> getOverrideIds() {
        return new ArrayList<>(overrides.keySet());
    }

    // "Sync with Chat Types": drop every manual change so all chats follow Chat Types again.
    public void clearOverrides() {
        overrides.clear();
        saveOverrides();
    }

    public static TLRPC.User findUser(int account, long userId) {
        if (userId == 0) return null;
        TLRPC.User user = null;
        try {
            user = MessagesController.getInstance(account).getUser(userId);
        } catch (Throwable ignored) {}
        if (user == null) {
            try {
                org.telegram.messenger.MessagesStorage storage = org.telegram.messenger.MessagesStorage.getInstance(account);
                if (storage != null) {
                    if (Thread.currentThread() == storage.getStorageQueue()) {
                        user = storage.getUser(userId);
                    } else {
                        user = storage.getUserSync(userId);
                    }
                }
            } catch (Throwable ignored) {}
        }
        return user;
    }

    public static TLRPC.Chat findChat(int account, long chatId) {
        if (chatId == 0) return null;
        if (chatId < 0) chatId = -chatId;
        TLRPC.Chat chat = null;
        try {
            chat = MessagesController.getInstance(account).getChat(chatId);
        } catch (Throwable ignored) {}
        if (chat == null) {
            try {
                org.telegram.messenger.MessagesStorage storage = org.telegram.messenger.MessagesStorage.getInstance(account);
                if (storage != null) {
                    if (Thread.currentThread() == storage.getStorageQueue()) {
                        chat = storage.getChat(chatId);
                    } else {
                        chat = storage.getChatSync(chatId);
                    }
                }
            } catch (Throwable ignored) {}
        }
        return chat;
    }

    // What the Chat Types options say for this chat, ignoring manual overrides.
    public boolean isDefaultIncluded(int account, long dialogId, boolean unknownDefault) {
        if (dialogId == 0) {
            return unknownDefault;
        }
        if (DialogObject.isEncryptedDialog(dialogId)) {
            return isOn(OPT_PRIVATE);
        }
        try {
            if (dialogId > 0) {
                TLRPC.User user = findUser(account, dialogId);
                if (user != null) {
                    return user.bot ? isOn(OPT_BOTS) : isOn(OPT_PRIVATE);
                }
                return unknownDefault;
            }
            TLRPC.Chat chat = findChat(account, -dialogId);
            if (chat == null) {
                return unknownDefault;
            }
            boolean isPublic = ChatObject.isPublic(chat);
            boolean ownOrAdmin = ChatObject.hasAdminRights(chat);
            if (ChatObject.isChannelAndNotMegaGroup(chat)) {
                if (ownOrAdmin && isOn(OPT_SKIP_CHANNELS_OWN_ADMIN)) {
                    return false;
                }
                return isOn(isPublic ? OPT_PUBLIC_CHANNELS : OPT_PRIVATE_CHANNELS);
            }
            if (ownOrAdmin && isOn(OPT_SKIP_GROUPS_OWN_ADMIN)) {
                return false;
            }
            if (!isOn(isPublic ? OPT_PUBLIC_GROUPS : OPT_PRIVATE_GROUPS)) {
                return false;
            }
            int size = groupSize(account, dialogId);
            if (size == SIZE_UNKNOWN) {
                return unknownDefault;
            }
            if (isPublic) {
                return isOn(size == SIZE_LARGE ? OPT_PUBLIC_GROUPS_LARGE : OPT_PUBLIC_GROUPS_SMALL);
            }
            return isOn(size == SIZE_LARGE ? OPT_PRIVATE_GROUPS_LARGE : OPT_PRIVATE_GROUPS_SMALL);
        } catch (Throwable ignore) {
            return unknownDefault;
        }
    }

    public boolean isIncluded(int account, long dialogId, boolean unknownDefault) {
        Boolean o = overrides.get(dialogId);
        if (o != null) {
            return o;
        }
        return isDefaultIncluded(account, dialogId, unknownDefault);
    }

    // Tick/untick one chat. Matching the default removes the override, so the chat keeps
    // following Chat Types; anything else is stored as a manual override.
    public void setIncluded(int account, long dialogId, boolean included) {
        if (included == isDefaultIncluded(account, dialogId, true)) {
            overrides.remove(dialogId);
        } else {
            overrides.put(dialogId, included);
        }
        saveOverrides();
    }

    // Bucket of a dialog, or -1 when the chat isn't loaded yet.
    public static int classify(int account, long dialogId) {
        if (dialogId == 0) {
            return -1;
        }
        if (DialogObject.isEncryptedDialog(dialogId)) {
            return PEER_PRIVATE;
        }
        try {
            if (dialogId > 0) {
                TLRPC.User user = findUser(account, dialogId);
                if (user != null) {
                    return user.bot ? PEER_BOT : PEER_PRIVATE;
                }
                return -1;
            }
            TLRPC.Chat chat = findChat(account, -dialogId);
            if (chat == null) {
                return -1;
            }
            boolean isPublic = ChatObject.isPublic(chat);
            if (ChatObject.isChannelAndNotMegaGroup(chat)) {
                return isPublic ? PEER_PUBLIC_CHANNEL : PEER_PRIVATE_CHANNEL;
            }
            return isPublic ? PEER_PUBLIC_GROUP : PEER_PRIVATE_GROUP;
        } catch (Throwable ignore) {
            return -1;
        }
    }

    // SIZE_SMALL / SIZE_LARGE for groups, SIZE_UNKNOWN when the member count isn't loaded.
    public static int groupSize(int account, long dialogId) {
        if (dialogId >= 0) {
            return SIZE_UNKNOWN;
        }
        try {
            TLRPC.Chat chat = findChat(account, -dialogId);
            if (chat == null) {
                return SIZE_UNKNOWN;
            }
            int count = chat.participants_count;
            if (count <= 0) {
                MessagesController mc = MessagesController.getInstance(account);
                TLRPC.ChatFull full = mc.getChatFull(-dialogId);
                if (full != null) {
                    count = full.participants_count;
                }
            }
            if (count <= 0) {
                return SIZE_UNKNOWN;
            }
            return count > LARGE_GROUP_MIN_MEMBERS ? SIZE_LARGE : SIZE_SMALL;
        } catch (Throwable ignore) {
            return SIZE_UNKNOWN;
        }
    }

    // unknownDefault: returned when the chat can't be classified yet. All three features pass
    // true: Ghost keeps hiding (privacy-safe), anti-delete and history keep data (data-safe).
    public boolean isInScope(int account, long dialogId, boolean unknownDefault) {
        if (!limitEnabled) {
            return true;
        }
        return isIncluded(account, dialogId, unknownDefault);
    }

    public boolean isMessageInScope(int account, long dialogId, long fromId, boolean unknownDefault) {
        if (!limitEnabled) {
            return true;
        }
        if (!isIncluded(account, dialogId, unknownDefault)) {
            return false;
        }
        if (fromId > 0) {
            TLRPC.User sender = findUser(account, fromId);
            if (sender != null && sender.bot && !isOn(OPT_BOTS)) {
                return false;
            }
        }
        return true;
    }

    // Per-feature instances (declared last so ALL_OPTS and REGISTRY are initialized first).
    public static final VeyraScope GHOST_READ = new VeyraScope(SCOPE_GHOST_READ, "ghostRead", defs(
            OPT_PRIVATE,
            OPT_PRIVATE_GROUPS, OPT_PRIVATE_GROUPS_SMALL,
            OPT_PUBLIC_GROUPS, OPT_PUBLIC_GROUPS_SMALL));
    public static final VeyraScope ANTI_DELETE = new VeyraScope(SCOPE_ANTI_DELETE, "antiDelete", defs(
            OPT_PRIVATE, OPT_BOTS,
            OPT_PRIVATE_GROUPS, OPT_PRIVATE_GROUPS_SMALL,
            OPT_PUBLIC_GROUPS, OPT_PUBLIC_GROUPS_SMALL,
            OPT_SKIP_GROUPS_OWN_ADMIN, OPT_SKIP_CHANNELS_OWN_ADMIN));
    public static final VeyraScope HISTORY = new VeyraScope(SCOPE_HISTORY, "history", defs(
            OPT_PRIVATE, OPT_BOTS,
            OPT_PRIVATE_GROUPS, OPT_PRIVATE_GROUPS_SMALL,
            OPT_PUBLIC_GROUPS, OPT_PUBLIC_GROUPS_SMALL,
            OPT_SKIP_GROUPS_OWN_ADMIN, OPT_SKIP_CHANNELS_OWN_ADMIN));
}

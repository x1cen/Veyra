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
//   - otherwise the default from Chat Types and Skip Own/Admin decides.
//
// A chat holds at most one override (included or not included), so it can never be in
// two contradicting states. An override equal to the default is dropped, so chats keep
// following Chat Types. clearOverrides() re-syncs the whole list with Chat Types.
public class VeyraScope {

    public static final int PEER_PRIVATE = 0;
    public static final int PEER_SMALL_GROUP = 1;
    public static final int PEER_LARGE_GROUP = 2;
    public static final int PEER_CHANNEL = 3;
    public static final int PEER_BOT = 4;
    public static final int PEER_COUNT = 5;

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

    public final int id;
    private final String key;
    private final boolean[] peerDefaults;

    public boolean limitEnabled;
    public boolean skipOwnAdmin;
    private final boolean[] peerEnabled = new boolean[PEER_COUNT];
    private final ConcurrentHashMap<Long, Boolean> overrides = new ConcurrentHashMap<>();

    public VeyraScope(int id, String key, boolean[] peerDefaults, boolean skipOwnAdminDefault) {
        this.id = id;
        this.key = key;
        this.peerDefaults = peerDefaults;
        this.skipOwnAdmin = skipOwnAdminDefault;
        System.arraycopy(peerDefaults, 0, peerEnabled, 0, PEER_COUNT);
        load();
        REGISTRY.put(id, this);
    }

    public void load() {
        SharedPreferences p = prefs();
        if (p == null) {
            return;
        }
        limitEnabled = p.getBoolean(key + "_limit", false);
        skipOwnAdmin = p.getBoolean(key + "_skipOwnAdmin", skipOwnAdmin);
        for (int i = 0; i < PEER_COUNT; i++) {
            peerEnabled[i] = p.getBoolean(key + "_peer" + i, peerDefaults[i]);
        }
        overrides.clear();
        String raw = p.getString(key + "_ovr", null);
        if (raw != null) {
            parseOverrides(raw);
        } else {
            // Migration from the previous Include/Exclude lists.
            addLegacy(p.getString(key + "_incl", ""), true);
            addLegacy(p.getString(key + "_excl", ""), false);
            if (!overrides.isEmpty()) {
                saveOverrides();
            }
            p.edit().remove(key + "_incl").remove(key + "_excl").apply();
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

    public void setSkipOwnAdmin(boolean val) {
        skipOwnAdmin = val;
        SharedPreferences p = prefs();
        if (p != null) {
            p.edit().putBoolean(key + "_skipOwnAdmin", val).apply();
        }
    }

    public boolean isPeerEnabled(int peerType) {
        return peerType >= 0 && peerType < PEER_COUNT && peerEnabled[peerType];
    }

    public void setPeerEnabled(int peerType, boolean val) {
        if (peerType < 0 || peerType >= PEER_COUNT) {
            return;
        }
        peerEnabled[peerType] = val;
        SharedPreferences p = prefs();
        if (p != null) {
            p.edit().putBoolean(key + "_peer" + peerType, val).apply();
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

    // What Chat Types and Skip Own/Admin say for this chat, ignoring manual overrides.
    public boolean isDefaultIncluded(int account, long dialogId, boolean unknownDefault) {
        if (dialogId < 0 && skipOwnAdmin && !DialogObject.isEncryptedDialog(dialogId)) {
            try {
                TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-dialogId);
                if (chat != null && ChatObject.hasAdminRights(chat)) {
                    return false;
                }
            } catch (Throwable ignore) {
            }
        }
        int type = classify(account, dialogId);
        if (type < 0) {
            return unknownDefault;
        }
        return peerEnabled[type];
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

    // Classify a dialog into one of the PEER_* buckets. Returns -1 when it can't be
    // classified yet (chat or member count not loaded), so the caller picks the safe default.
    public static int classify(int account, long dialogId) {
        if (dialogId == 0) {
            return -1;
        }
        if (DialogObject.isEncryptedDialog(dialogId)) {
            return PEER_PRIVATE;
        }
        if (dialogId > 0) {
            try {
                TLRPC.User user = MessagesController.getInstance(account).getUser(dialogId);
                if (user != null && user.bot) {
                    return PEER_BOT;
                }
            } catch (Throwable ignore) {
            }
            return PEER_PRIVATE;
        }
        try {
            MessagesController mc = MessagesController.getInstance(account);
            TLRPC.Chat chat = mc.getChat(-dialogId);
            if (chat == null) {
                return -1;
            }
            if (ChatObject.isChannelAndNotMegaGroup(chat)) {
                return PEER_CHANNEL;
            }
            int count = chat.participants_count;
            if (count <= 0) {
                TLRPC.ChatFull full = mc.getChatFull(-dialogId);
                if (full != null) {
                    count = full.participants_count;
                }
            }
            if (count <= 0) {
                return -1;
            }
            return count > LARGE_GROUP_MIN_MEMBERS ? PEER_LARGE_GROUP : PEER_SMALL_GROUP;
        } catch (Throwable ignore) {
            return -1;
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

    // Per-feature instances. peerDefaults = {Private, SmallGroup, LargeGroup, Channel, Bot}.
    public static final VeyraScope GHOST_READ = new VeyraScope(
            SCOPE_GHOST_READ, "ghostRead",
            new boolean[]{true, true, false, false, false}, false);
    public static final VeyraScope ANTI_DELETE = new VeyraScope(
            SCOPE_ANTI_DELETE, "antiDelete",
            new boolean[]{true, true, false, false, true}, true);
    public static final VeyraScope HISTORY = new VeyraScope(
            SCOPE_HISTORY, "history",
            new boolean[]{true, true, false, false, true}, true);
}

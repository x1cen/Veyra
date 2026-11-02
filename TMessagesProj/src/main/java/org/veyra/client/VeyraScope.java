package org.veyra.client;

import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Veyra: reusable per-feature scope model shared by Ghost read-hiding, anti-delete, and
// edit/reaction history. Each feature owns one instance keyed by a unique prefs prefix.
//
// Decision model (isInScope):
//   - master switch off (default)   -> feature applies everywhere;
//   - chat in the Excluded list     -> never in scope;
//   - chat in the Included list     -> always in scope (wins over Skip Own/Admin and per-type);
//   - Skip Own/Admin on + you own or admin the group/channel -> out of scope;
//   - otherwise                     -> the per-type switch for the chat's bucket decides.
//
// Included and Excluded are mutually exclusive: putting a chat in one removes it from the
// other, so a chat can never be ticked in both lists.
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

    private static final java.util.HashMap<Integer, VeyraScope> REGISTRY = new java.util.HashMap<>();

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
    private final Set<Long> included = Collections.synchronizedSet(new LinkedHashSet<Long>());
    private final Set<Long> excluded = Collections.synchronizedSet(new LinkedHashSet<Long>());

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
        loadSet(p.getString(key + "_incl", ""), included);
        loadSet(p.getString(key + "_excl", ""), excluded);
    }

    private static void loadSet(String raw, Set<Long> set) {
        set.clear();
        if (raw == null || raw.length() == 0) {
            return;
        }
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            try {
                set.add(Long.parseLong(parts[i].trim()));
            } catch (NumberFormatException ignore) {
            }
        }
    }

    private void saveSet(String suffix, Set<Long> set) {
        SharedPreferences p = prefs();
        if (p == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        synchronized (set) {
            for (Long id : set) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(id);
            }
        }
        p.edit().putString(key + suffix, sb.toString()).apply();
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

    public ArrayList<Long> getIncluded() {
        synchronized (included) {
            return new ArrayList<>(included);
        }
    }

    public ArrayList<Long> getExcluded() {
        synchronized (excluded) {
            return new ArrayList<>(excluded);
        }
    }

    public boolean isIncluded(long dialogId) {
        return included.contains(dialogId);
    }

    public boolean isExcluded(long dialogId) {
        return excluded.contains(dialogId);
    }

    // Replaces the whole Included list, enforcing mutual exclusivity against Excluded.
    public void setIncluded(List<Long> ids) {
        synchronized (included) {
            included.clear();
            if (ids != null) {
                included.addAll(ids);
            }
        }
        if (ids != null) {
            synchronized (excluded) {
                excluded.removeAll(ids);
            }
        }
        saveSet("_incl", included);
        saveSet("_excl", excluded);
    }

    // Replaces the whole Excluded list, enforcing mutual exclusivity against Included.
    public void setExcluded(List<Long> ids) {
        synchronized (excluded) {
            excluded.clear();
            if (ids != null) {
                excluded.addAll(ids);
            }
        }
        if (ids != null) {
            synchronized (included) {
                included.removeAll(ids);
            }
        }
        saveSet("_excl", excluded);
        saveSet("_incl", included);
    }

    public void remove(long dialogId) {
        boolean changed = included.remove(dialogId);
        changed = excluded.remove(dialogId) || changed;
        if (changed) {
            saveSet("_incl", included);
            saveSet("_excl", excluded);
        }
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

    // unknownDefault: returned when the chat can't be classified yet. Ghost passes true to keep
    // hiding (privacy-safe); anti-delete and history pass true to keep data (data-safe).
    public boolean isInScope(int account, long dialogId, boolean unknownDefault) {
        if (!limitEnabled) {
            return true;
        }
        if (excluded.contains(dialogId)) {
            return false;
        }
        if (included.contains(dialogId)) {
            return true;
        }
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

    // Per-feature instances. peerDefaults = {Private, SmallGroup, LargeGroup, Channel, Bot}.
    // Ghost read-hiding: hide where "Seen by" is actually shown (private + small groups);
    // large groups, channels and bots never show a seen list, so they're off by default.
    public static final VeyraScope GHOST_READ = new VeyraScope(
            SCOPE_GHOST_READ, "ghostRead",
            new boolean[]{true, true, false, false, false}, false);
    // Anti-delete and history: keep/log where data is worth keeping (private + small groups +
    // bots), skip big groups/channels and chats you own or admin (the admin log covers those).
    public static final VeyraScope ANTI_DELETE = new VeyraScope(
            SCOPE_ANTI_DELETE, "antiDelete",
            new boolean[]{true, true, false, false, true}, true);
    public static final VeyraScope HISTORY = new VeyraScope(
            SCOPE_HISTORY, "history",
            new boolean[]{true, true, false, false, true}, true);
}

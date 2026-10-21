/* Veyra Project */
package org.veyra.client;

import android.content.Context;
import android.graphics.Typeface;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.telegram.messenger.*;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class VeyraAntiDelete {

    public static ArrayList<TLRPC.TL_dcOption> getTheDC(int dcId, ArrayList<TLRPC.TL_dcOption> options) {
        ArrayList<TLRPC.TL_dcOption> filtered = new ArrayList<>();
        for (TLRPC.TL_dcOption t : options) {
            if (t.id == dcId) filtered.add(t);
        }
        if (filtered.isEmpty()) return null;
        return filtered;
    }

    public static String getDCGeoDummy(int dcId) {
        switch (dcId) {
            case 1:
            case 3:
                return "USA, Miami";
            case 2:
            case 4:
                return "NLD, Amsterdam";
            case 5:
                return "SGP, Singapore";
            default:
                return "UNK, unknown";
        }
    }

    public static float getVideoRoundMult(int id) {
        switch (id) {
            case 0:
                return 0.5f;
            case 2:
                return 2f;
            case 3:
                return 4f;
            default:
                return 1f;
        }
    }

    public static int getVideoRoundSize(int id) {
        switch (id) {
            case 0:
                return 192;
            case 2:
                return 768;
            default:
                return 384;
        }
    }

    public static int getPhotoSizeMax(int id) {
        switch (id) {
            case 0:
                return 1280;
            case 1:
                return 2560;
            case 2:
                return 3840;
            default:
                return 1280;
        }
    }

    public static Typeface getFont(String font) {
        switch (font) {
            case "fonts/rmedium.ttf"://bold
            case "fonts/ritalic.ttf"://italic
            case "fonts/rmediumitalic.ttf"://bold italic
            case "fonts/rmono.ttf": //mono
            case "fonts/mw_bold.ttf"://normal bold
            case "fonts/rcondensedbold.ttf"://QR
                return AndroidUtilities.getTypeface(font);
            default:
                return Typeface.createFromFile(font);
        }
    }

//    public static int getNotificationIcon() {
//        switch (MessagesController.getGlobalVeyraSettings().getInt("UIAppNotificationIconSelector", 0)) {
//            case 1:
//                return R.drawable.veyra_eyez;
//            case 2:
//                return R.drawable.notification;
//            case 3:
//                return R.drawable.msg_report_xxx;
//            case 0:
//            default:
//                return R.drawable.veyra_notification;
//        }
//    }

    public static String toJson(Object o) {
        return new GsonBuilder().disableHtmlEscaping().create().toJson(o);
    }

    private static final java.util.Set<Long> peerDeletedChats = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static final java.util.Map<Long, java.util.Set<Integer>> deletedMessagesByDialog = new java.util.concurrent.ConcurrentHashMap<>();

    public static void addDeletedMessage(long dialogId, int messageId) {
        if (dialogId > 0) {
            peerDeletedChats.add(dialogId);
        }
        java.util.Set<Integer> set = deletedMessagesByDialog.get(dialogId);
        if (set == null) {
            set = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
            deletedMessagesByDialog.put(dialogId, set);
        }
        set.add(messageId);
        if (dialogId != 0) {
            long altId = -dialogId;
            java.util.Set<Integer> altSet = deletedMessagesByDialog.get(altId);
            if (altSet == null) {
                altSet = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
                deletedMessagesByDialog.put(altId, altSet);
            }
            altSet.add(messageId);
        }
    }

    public static void saveDeletedMessageToDb(int account, long dialogId, int messageId) {
        addDeletedMessage(dialogId, messageId);
        org.telegram.messenger.MessagesStorage.getInstance(account).getStorageQueue().postRunnable(() -> {
            try {
                org.telegram.SQLite.SQLiteDatabase db = org.telegram.messenger.MessagesStorage.getInstance(account).getDatabase();
                if (db != null) {
                    db.executeFast(String.format(java.util.Locale.US, "INSERT OR REPLACE INTO veyra_message_deletions (mid, uid, isdel) VALUES (%d, %d, 1);", messageId, dialogId)).stepThis().dispose();
                }
            } catch (Throwable ignored) {}
        });
    }

    public static boolean isMessageDeleted(long dialogId, int messageId) {
        java.util.Set<Integer> set = deletedMessagesByDialog.get(dialogId);
        if (set != null && set.contains(messageId)) {
            return true;
        }
        if (dialogId != 0) {
            java.util.Set<Integer> altSet = deletedMessagesByDialog.get(-dialogId);
            if (altSet != null && altSet.contains(messageId)) {
                return true;
            }
        }
        return false;
    }

    public static void removeDeletedMessage(long dialogId, int messageId) {
        java.util.Set<Integer> set = deletedMessagesByDialog.get(dialogId);
        if (set != null) {
            set.remove(messageId);
        }
        if (dialogId != 0) {
            java.util.Set<Integer> altSet = deletedMessagesByDialog.get(-dialogId);
            if (altSet != null) {
                altSet.remove(messageId);
            }
        }
    }

    public static void markChatDeleted(long dialogId) {
        if (dialogId > 0) {
            peerDeletedChats.add(dialogId);
        }
    }

    public static void removeChatDeleted(long dialogId) {
        peerDeletedChats.remove(dialogId);
        deletedMessagesByDialog.remove(dialogId);
        if (dialogId != 0) {
            deletedMessagesByDialog.remove(-dialogId);
        }
    }

    public static void initFromDb(org.telegram.SQLite.SQLiteDatabase db) {
        if (db == null) return;
        try {
            db.executeFast("CREATE TABLE IF NOT EXISTS veyra_message_deletions (mid INTEGER, uid INTEGER, isdel INTEGER, PRIMARY KEY (mid, uid));").stepThis().dispose();
            org.telegram.SQLite.SQLiteCursor cursor = db.queryFinalized("SELECT mid, uid FROM veyra_message_deletions");
            while (cursor.next()) {
                int mid = cursor.intValue(0);
                long uid = cursor.longValue(1);
                addDeletedMessage(uid, mid);
            }
            cursor.dispose();
        } catch (Throwable ignored) {}
    }

    public static boolean isChatDeleted(long dialogId, TLRPC.User user) {
        if (user != null && (user.deleted || UserObject.isDeleted(user))) {
            return true;
        }
        if (dialogId > 0) {
            return peerDeletedChats.contains(dialogId);
        }
        return false;
    }

    public static void clearAll() {
        peerDeletedChats.clear();
        deletedMessagesByDialog.clear();
        org.telegram.messenger.MessagesStorage.getInstance(org.telegram.messenger.UserConfig.selectedAccount)
            .getStorageQueue().postRunnable(() -> {
                try {
                    org.telegram.SQLite.SQLiteDatabase db =
                        org.telegram.messenger.MessagesStorage.getInstance(org.telegram.messenger.UserConfig.selectedAccount).getDatabase();
                    db.executeFast("DELETE FROM veyra_message_deletions").stepThis().dispose();
                } catch (Exception ignored) {}
            });
    }

    /**
     * Purge all Veyra anti-delete entries for a specific dialog from MessagesStorage.
     * Call this when the user manually clears a "Deleted Account" chat.
     */
    public static void clearDialog(long dialogId) {
        removeChatDeleted(dialogId);
        org.telegram.messenger.MessagesStorage.getInstance(org.telegram.messenger.UserConfig.selectedAccount)
            .getStorageQueue().postRunnable(() -> {
                try {
                    org.telegram.SQLite.SQLiteDatabase db =
                        org.telegram.messenger.MessagesStorage.getInstance(org.telegram.messenger.UserConfig.selectedAccount).getDatabase();
                    db.executeFast("DELETE FROM veyra_message_deletions WHERE uid = " + dialogId).stepThis().dispose();
                } catch (Exception ignored) {}
            });
    }

    public static String toJsonNestedMaps(Map<Integer, Map<String, String>> map) {
        final Gson gson = new Gson();
        final JsonObject jsonObject = new JsonObject();
        for (Integer i : map.keySet()) {
            jsonObject.add(i.toString(), gson.toJsonTree(map.get(i), Map.class));
        }
        return jsonObject.toString();
    }

//    public static int getMaxInternalAccountId(Map<Integer, Map<String, String>> map) {//SharedConfig.thAccounts
//        Integer[] ids;
//        Integer nextId = 0;
//        if (map == null || map.isEmpty()) {
//            if (SharedConfig.activeAccounts != null && !SharedConfig.activeAccounts.isEmpty())
//                ids = SharedConfig.activeAccounts.stream().toArray(Integer[]::new);
//            else return 0;
//        } else {
//            if (map.containsKey(-1)) nextId = Integer.valueOf(map.get(-1).get("nextAccountId"));
//            ids = map.keySet().stream().toArray(Integer[]::new);
//        }
//        Arrays.sort(ids);
//        if (map.containsKey(-1) && nextId.compareTo(ids[ids.length - 1]) <= 0) {
//            nextId = ids[ids.length - 1] + 1;
//            map.get(-1).put("nextAccountId", nextId.toString());
//            ApplicationLoader.applicationContext.getSharedPreferences("veyra", Context.MODE_PRIVATE).edit()
//                .putString("thAccounts", VeyraAntiDelete.toJsonNestedMaps(map))
//                .apply();
//        }
//        return nextId;
//    }

//    public static boolean starrMark(boolean bool) {
//        switch (MessagesController.getGlobalVeyraSettings().getInt("VeyraStarrMark", 0)) {
//            case 0:
//                return false;
//            case 1:
//                return true;
//            default:
//                return bool;
//        }
//    }

//    public static float stickerSizeMult() {
//        switch (MessagesController.getGlobalVeyraSettings().getInt("UIStickerSize", 2)) {
//            case 0:
//                return 4f;
//            case 1:
//                return 2f;
//            case 3:
//                return 0.5f;
//            case 2:
//            default:
//                return 1f;
//        }
//    }
}

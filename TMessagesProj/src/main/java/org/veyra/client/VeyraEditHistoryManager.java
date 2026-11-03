/* Veyra Project */

package org.veyra.client;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;

import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class VeyraEditHistoryManager {

    private static final String DB_NAME = "veyra_edit_history.db";
    private static final int DB_VERSION = 5;
    private static final String TABLE_NAME = "edit_history";
    private static final String REACTION_TABLE_NAME = "reaction_history";
    // Account-less read/clear overloads keep the pre-v5 behaviour and match rows of every account.
    private static final int ACCOUNT_ANY = -1;

    public static class EditEntry {
        public final long dialogId;
        public final int messageId;
        public final int date;
        public final String text;
        public final byte[] data;

        public EditEntry(long dialogId, int messageId, int date, String text) {
            this(dialogId, messageId, date, text, null);
        }

        public EditEntry(long dialogId, int messageId, int date, String text, byte[] data) {
            this.dialogId = dialogId;
            this.messageId = messageId;
            this.date = date;
            this.text = text;
            this.data = data;
        }

        public MessageObject toMessageObject(int currentAccount) {
            if (data != null && data.length > 0) {
                try {
                    NativeByteBuffer byteBuffer = new NativeByteBuffer(data.length);
                    byteBuffer.writeBytes(data);
                    byteBuffer.position(0);
                    int constructor = byteBuffer.readInt32(false);
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(byteBuffer, constructor, false);
                    byteBuffer.reuse();
                    if (message != null) {
                        return new MessageObject(currentAccount, message, false, false);
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
            TLRPC.TL_message fallback = new TLRPC.TL_message();
            fallback.id = messageId;
            fallback.dialog_id = dialogId;
            fallback.date = date;
            fallback.message = text != null ? text : "";
            return new MessageObject(currentAccount, fallback, false, false);
        }
    }

    public static class ReactionEntry {
        public final long dialogId;
        public final int messageId;
        public final int date;
        public final String reaction;
        public final int count;
        public final long userId;
        public final String action; // "add" or "remove"

        public ReactionEntry(long dialogId, int messageId, int date, String reaction, int count, long userId, String action) {
            this.dialogId = dialogId;
            this.messageId = messageId;
            this.date = date;
            this.reaction = reaction;
            this.count = count;
            this.userId = userId;
            this.action = action;
        }
    }

    private static class DBHelper extends SQLiteOpenHelper {
        DBHelper() {
            super(ApplicationLoader.applicationContext, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "account INTEGER NOT NULL DEFAULT 0, " +
                    "dialog_id INTEGER, " +
                    "message_id INTEGER, " +
                    "date INTEGER, " +
                    "text TEXT, " +
                    "data BLOB);");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_msg ON " + TABLE_NAME + " (dialog_id, message_id);");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + REACTION_TABLE_NAME + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "account INTEGER NOT NULL DEFAULT 0, " +
                    "dialog_id INTEGER, " +
                    "message_id INTEGER, " +
                    "date INTEGER, " +
                    "reaction TEXT, " +
                    "count INTEGER, " +
                    "user_id INTEGER, " +
                    "action TEXT DEFAULT 'add');");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_react_msg ON " + REACTION_TABLE_NAME + " (dialog_id, message_id);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + REACTION_TABLE_NAME + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "dialog_id INTEGER, " +
                        "message_id INTEGER, " +
                        "date INTEGER, " +
                        "reaction TEXT, " +
                        "count INTEGER, " +
                        "user_id INTEGER);");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_react_msg ON " + REACTION_TABLE_NAME + " (dialog_id, message_id);");
            }
            if (oldVersion < 3) {
                try {
                    db.execSQL("ALTER TABLE " + REACTION_TABLE_NAME + " ADD COLUMN action TEXT DEFAULT 'add';");
                } catch (Exception ignored) {}
            }
            if (oldVersion < 4) {
                try {
                    db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN data BLOB;");
                } catch (Exception ignored) {}
            }
            if (oldVersion < 5) {
                // Rows recorded before per-account separation are kept and assigned to account 0.
                try {
                    db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN account INTEGER NOT NULL DEFAULT 0;");
                } catch (Exception e) {
                    FileLog.e(e);
                }
                try {
                    db.execSQL("ALTER TABLE " + REACTION_TABLE_NAME + " ADD COLUMN account INTEGER NOT NULL DEFAULT 0;");
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        }
    }

    private static DBHelper dbHelper;

    private static synchronized DBHelper getHelper() {
        if (dbHelper == null) {
            dbHelper = new DBHelper();
        }
        return dbHelper;
    }

    // Writes through account-less overloads have no event account; use the selected one.
    private static int legacyAccount() {
        return org.telegram.messenger.UserConfig.selectedAccount;
    }

    private static String accountClause(int account) {
        return account == ACCOUNT_ANY ? "" : " AND account = ?";
    }

    private static String[] withAccount(int account, String... args) {
        if (account == ACCOUNT_ANY) {
            return args;
        }
        String[] out = new String[args.length + 1];
        System.arraycopy(args, 0, out, 0, args.length);
        out[args.length] = String.valueOf(account);
        return out;
    }

    private static String accountWhere(int account) {
        return account == ACCOUNT_ANY ? null : "account = ?";
    }

    private static String[] accountArgs(int account) {
        return account == ACCOUNT_ANY ? null : new String[]{String.valueOf(account)};
    }

    public static void logEdit(long dialogId, int messageId, int date, String text) {
        logEdit(legacyAccount(), dialogId, messageId, date, text, null);
    }

    public static void logEdit(long dialogId, int messageId, int date, String text, TLRPC.Message messageOwner) {
        logEdit(legacyAccount(), dialogId, messageId, date, text, messageOwner);
    }

    public static void logEdit(int account, long dialogId, int messageId, int date, String text, TLRPC.Message messageOwner) {
        long fromId = messageOwner != null && messageOwner.from_id != null ? org.telegram.messenger.MessageObject.getPeerId(messageOwner.from_id) : (dialogId > 0 ? dialogId : 0);
        if (!VeyraUIConfig.isChatTypeAllowedForEditHistory(dialogId) || !VeyraUIConfig.isHistoryInScope(account, dialogId, fromId) || (TextUtils.isEmpty(text) && messageOwner == null)) {
            return;
        }
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();

            // Store text
            String encryptedText = text != null ? text : "";

            Cursor checkCursor = db.rawQuery("SELECT text FROM " + TABLE_NAME + " WHERE dialog_id = ? AND message_id = ? AND account = ? ORDER BY id DESC LIMIT 1",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId), String.valueOf(account)});
            if (checkCursor != null) {
                if (checkCursor.moveToFirst()) {
                    String lastRaw = checkCursor.getString(0);
                    checkCursor.close();
                    if (text != null && text.equals(lastRaw)) {
                        return; // no change
                    }
                } else {
                    checkCursor.close();
                }
            }

            // Check edit limit per message
            boolean dropOldest = VeyraUIConfig.editHistoryDropOldest;
            int maxEdits = Math.max(1, Math.min(50, VeyraUIConfig.editHistoryLimit));
            Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE dialog_id = ? AND message_id = ? AND account = ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId), String.valueOf(account)});
            int total = 0;
            if (countCursor != null) {
                if (countCursor.moveToFirst()) total = countCursor.getInt(0);
                countCursor.close();
            }
            if (total >= maxEdits) {
                if (dropOldest) {
                    int toDelete = total - maxEdits + 1;
                    db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE id IN (SELECT id FROM " + TABLE_NAME +
                            " WHERE dialog_id = ? AND message_id = ? AND account = ? ORDER BY id ASC LIMIT ?)",
                            new Object[]{dialogId, messageId, account, toDelete});
                } else {
                    return; // limit reached and we don't drop oldest
                }
            }

            byte[] serializedData = null;
            if (messageOwner != null) {
                try {
                    NativeByteBuffer buf = new NativeByteBuffer(messageOwner.getObjectSize());
                    messageOwner.serializeToStream(buf);
                    serializedData = new byte[buf.length()];
                    buf.buffer.position(0);
                    buf.buffer.get(serializedData);
                    buf.reuse();
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }

            ContentValues values = new ContentValues();
            values.put("account", account);
            values.put("dialog_id", dialogId);
            values.put("message_id", messageId);
            values.put("date", date);
            values.put("text", encryptedText);
            if (serializedData != null) {
                values.put("data", serializedData);
            }
            db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static List<EditEntry> getHistory(long dialogId, int messageId) {
        return getHistory(ACCOUNT_ANY, dialogId, messageId);
    }

    public static List<EditEntry> getHistory(int account, long dialogId, int messageId) {
        List<EditEntry> list = new ArrayList<>();
        if (!VeyraUIConfig.editHistoryEnabled) {
            return list;
        }
        try {
            SQLiteDatabase db = getHelper().getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT date, text, data FROM " + TABLE_NAME + " WHERE dialog_id = ? AND message_id = ?" + accountClause(account) + " ORDER BY id ASC",
                    withAccount(account, String.valueOf(dialogId), String.valueOf(messageId)));
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int date = cursor.getInt(0);
                    String text = cursor.getString(1);
                    byte[] data = cursor.isNull(2) ? null : cursor.getBlob(2);
                    list.add(new EditEntry(dialogId, messageId, date, text, data));
                }
                cursor.close();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static boolean hasHistory(long dialogId, int messageId) {
        return hasHistory(ACCOUNT_ANY, dialogId, messageId);
    }

    public static boolean hasHistory(int account, long dialogId, int messageId) {
        if (!VeyraUIConfig.editHistoryEnabled) {
            return false;
        }
        try {
            SQLiteDatabase db = getHelper().getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_NAME + " WHERE dialog_id = ? AND message_id = ?" + accountClause(account) + " LIMIT 1",
                    withAccount(account, String.valueOf(dialogId), String.valueOf(messageId)));
            boolean exists = cursor != null && cursor.moveToFirst();
            if (cursor != null) cursor.close();
            return exists;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    public static void deleteHistory(long dialogId, int messageId) {
        deleteHistory(ACCOUNT_ANY, dialogId, messageId);
    }

    public static void deleteHistory(int account, long dialogId, int messageId) {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(TABLE_NAME, "dialog_id = ? AND message_id = ?" + accountClause(account), withAccount(account, String.valueOf(dialogId), String.valueOf(messageId)));
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void deleteHistoryBatch(long dialogId, List<Integer> messageIds) {
        deleteHistoryBatch(ACCOUNT_ANY, dialogId, messageIds);
    }

    public static void deleteHistoryBatch(int account, long dialogId, List<Integer> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return;
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.beginTransaction();
            try {
                for (int msgId : messageIds) {
                    String[] args = withAccount(account, String.valueOf(dialogId), String.valueOf(msgId));
                    db.delete(TABLE_NAME, "dialog_id = ? AND message_id = ?" + accountClause(account), args);
                    db.delete(REACTION_TABLE_NAME, "dialog_id = ? AND message_id = ?" + accountClause(account), args);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Clear all edit+reaction history for all dialogs. */
    public static void clearAll() {
        clearAll(ACCOUNT_ANY);
    }

    public static void clearAll(int account) {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(TABLE_NAME, accountWhere(account), accountArgs(account));
            db.delete(REACTION_TABLE_NAME, accountWhere(account), accountArgs(account));
            clearRemovedReactions(account);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Clear only edit history (message edits). */
    public static void clearAllEdits() {
        clearAllEdits(ACCOUNT_ANY);
    }

    public static void clearAllEdits(int account) {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(TABLE_NAME, accountWhere(account), accountArgs(account));
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Clear only reaction history. */
    public static void clearAllReactions() {
        clearAllReactions(ACCOUNT_ANY);
    }

    public static void clearAllReactions(int account) {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(REACTION_TABLE_NAME, accountWhere(account), accountArgs(account));
            clearRemovedReactions(account);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Clear all edit history entries for a single dialog. */
    public static void clearDialog(long dialogId) {
        clearDialog(ACCOUNT_ANY, dialogId);
    }

    public static void clearDialog(int account, long dialogId) {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(TABLE_NAME, "(dialog_id = ? OR dialog_id = ?)" + accountClause(account), withAccount(account, String.valueOf(dialogId), String.valueOf(-dialogId)));
            db.delete(REACTION_TABLE_NAME, "(dialog_id = ? OR dialog_id = ?)" + accountClause(account), withAccount(account, String.valueOf(dialogId), String.valueOf(-dialogId)));
            clearRemovedReactions(account, dialogId);
            if (dialogId != 0) {
                clearRemovedReactions(account, -dialogId);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void clearDialogEdits(long dialogId) {
        clearDialogEdits(ACCOUNT_ANY, dialogId);
    }

    public static void clearDialogEdits(int account, long dialogId) {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(TABLE_NAME, "dialog_id = ?" + accountClause(account), withAccount(account, String.valueOf(dialogId)));
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void clearDialogReactions(long dialogId) {
        clearDialogReactions(ACCOUNT_ANY, dialogId);
    }

    public static void clearDialogReactions(int account, long dialogId) {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(REACTION_TABLE_NAME, "dialog_id = ?" + accountClause(account), withAccount(account, String.valueOf(dialogId)));
            clearRemovedReactions(account, dialogId);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /**
     * Log a reaction change event. Only logs reactions from OTHER users (not self).
     * Determines if this is an "add" or "remove" by comparing to previous state.
     *
     * @param dialogId   the dialog
     * @param messageId  the message
     * @param date       timestamp
     * @param reaction   emoji string
     * @param newCount   current total count for this reaction
     * @param userId     peer who reacted (0 if unknown)
     * @param selfUserId current user id — reactions from self are ignored
     */
    public static void logReaction(long dialogId, int messageId, int date, String reaction,
                                   int newCount, long userId, long selfUserId) {
        logReaction(legacyAccount(), dialogId, messageId, date, reaction, newCount, userId, selfUserId);
    }

    public static void logReaction(int account, long dialogId, int messageId, int date, String reaction,
                                   int newCount, long userId, long selfUserId) {
        if (!VeyraUIConfig.isChatTypeAllowedForReactionHistory(dialogId) || !VeyraUIConfig.isHistoryInScope(account, dialogId, userId) || TextUtils.isEmpty(reaction)) {
            return;
        }
        // Ignore own reactions unless enabled in settings
        long currentSelfId = org.telegram.messenger.UserConfig.getInstance(account).getClientUserId();
        if (!VeyraUIConfig.reactionHistoryIncludeSelf) {
            if (selfUserId != 0 && userId == selfUserId) {
                return;
            }
            if (currentSelfId != 0 && userId == currentSelfId) {
                return;
            }
        }
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            String acc = String.valueOf(account);

            // Determine action: compare newCount to last logged count for this reaction
            String action = "add";
            Cursor lastCursor = db.rawQuery(
                    "SELECT count FROM " + REACTION_TABLE_NAME +
                            " WHERE dialog_id = ? AND message_id = ? AND reaction = ? AND account = ? ORDER BY id DESC LIMIT 1",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId), reaction, acc});
            if (lastCursor != null) {
                if (lastCursor.moveToFirst()) {
                    int lastCount = lastCursor.getInt(0);
                    if (newCount < lastCount) {
                        action = "remove";
                    }
                }
                lastCursor.close();
            }

            // Only log reactions when removed (user requirement)
            if (!"remove".equals(action)) {
                return;
            }

            // For user-specific reactions: avoid duplicate user+reaction entry for same action
            if (userId != 0) {
                Cursor c = db.rawQuery(
                        "SELECT COALESCE(action, 'add') FROM " + REACTION_TABLE_NAME +
                                " WHERE dialog_id = ? AND message_id = ? AND user_id = ? AND reaction = ? AND account = ? ORDER BY id DESC LIMIT 1",
                        new String[]{String.valueOf(dialogId), String.valueOf(messageId),
                                String.valueOf(userId), reaction, acc});
                if (c != null) {
                    if (c.moveToFirst()) {
                        String lastAction = c.getString(0);
                        c.close();
                        if (action.equals(lastAction)) {
                            return; // same action repeated by this user — skip
                        }
                    } else {
                        c.close();
                    }
                }
            }

            if (!trimReactions(db, account, dialogId, messageId)) {
                return;
            }

            ContentValues values = new ContentValues();
            values.put("account", account);
            values.put("dialog_id", dialogId);
            values.put("message_id", messageId);
            values.put("date", date);
            values.put("reaction", reaction);
            values.put("count", newCount);
            values.put("user_id", userId);
            values.put("action", action);
            db.insert(REACTION_TABLE_NAME, null, values);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void logReactionRemoved(long dialogId, int messageId, int date, String reaction,
                                          long userId, long selfUserId) {
        logReactionRemoved(legacyAccount(), dialogId, messageId, date, reaction, userId, selfUserId);
    }

    public static void logReactionRemoved(int account, long dialogId, int messageId, int date, String reaction,
                                          long userId, long selfUserId) {
        if (!VeyraUIConfig.isChatTypeAllowedForReactionHistory(dialogId) || !VeyraUIConfig.isHistoryInScope(account, dialogId, userId) || TextUtils.isEmpty(reaction)) {
            return;
        }
        long currentSelfId = org.telegram.messenger.UserConfig.getInstance(account).getClientUserId();
        if (!VeyraUIConfig.reactionHistoryIncludeSelf) {
            if (selfUserId != 0 && userId == selfUserId) {
                return;
            }
            if (currentSelfId != 0 && userId == currentSelfId) {
                return;
            }
        }
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            if (!trimReactions(db, account, dialogId, messageId)) {
                return;
            }

            ContentValues values = new ContentValues();
            values.put("account", account);
            values.put("dialog_id", dialogId);
            values.put("message_id", messageId);
            values.put("date", date);
            values.put("reaction", reaction);
            values.put("count", 0);
            values.put("user_id", userId);
            values.put("action", "remove");
            db.insert(REACTION_TABLE_NAME, null, values);
            markReactionRemoved(account, dialogId, messageId);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    // Applies the per-message reaction limit. Returns false when the limit is reached and oldest rows are kept.
    private static boolean trimReactions(SQLiteDatabase db, int account, long dialogId, int messageId) {
        Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM " + REACTION_TABLE_NAME +
                " WHERE dialog_id = ? AND message_id = ? AND account = ?",
                new String[]{String.valueOf(dialogId), String.valueOf(messageId), String.valueOf(account)});
        int total = 0;
        if (countCursor != null) {
            if (countCursor.moveToFirst()) total = countCursor.getInt(0);
            countCursor.close();
        }
        int limit = Math.max(5, Math.min(100, VeyraUIConfig.reactionHistoryLimit));
        boolean dropOldest = VeyraUIConfig.reactionHistoryDropOldest;
        if (total >= limit) {
            if (dropOldest) {
                db.execSQL("DELETE FROM " + REACTION_TABLE_NAME +
                        " WHERE id IN (SELECT id FROM " + REACTION_TABLE_NAME +
                        " WHERE dialog_id = ? AND message_id = ? AND account = ? ORDER BY id ASC LIMIT ?)",
                        new Object[]{dialogId, messageId, account, total - limit + 1});
            } else {
                return false;
            }
        }
        return true;
    }

    private static final ConcurrentHashMap<Integer, ConcurrentHashMap<Long, Set<Integer>>> removedReactionMessages = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<Long, Set<Integer>> getRemovedReactions(int account) {
        ConcurrentHashMap<Long, Set<Integer>> map = removedReactionMessages.get(account);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            ConcurrentHashMap<Long, Set<Integer>> old = removedReactionMessages.putIfAbsent(account, map);
            if (old != null) {
                map = old;
            }
        }
        return map;
    }

    private static void clearRemovedReactions(int account) {
        if (account == ACCOUNT_ANY) {
            removedReactionMessages.clear();
        } else {
            removedReactionMessages.remove(account);
        }
    }

    private static void clearRemovedReactions(int account, long dialogId) {
        if (account == ACCOUNT_ANY) {
            for (ConcurrentHashMap<Long, Set<Integer>> map : removedReactionMessages.values()) {
                map.remove(dialogId);
            }
        } else {
            getRemovedReactions(account).remove(dialogId);
        }
    }

    public static void markReactionRemoved(long dialogId, int messageId) {
        markReactionRemoved(legacyAccount(), dialogId, messageId);
    }

    public static void markReactionRemoved(int account, long dialogId, int messageId) {
        ConcurrentHashMap<Long, Set<Integer>> map = getRemovedReactions(account);
        Set<Integer> set = map.get(dialogId);
        if (set == null) {
            set = java.util.Collections.synchronizedSet(new java.util.HashSet<Integer>());
            Set<Integer> old = map.putIfAbsent(dialogId, set);
            if (old != null) {
                set = old;
            }
        }
        set.add(messageId);
    }

    private static boolean isReactionRemovedCached(int account, long dialogId, int messageId) {
        if (account == ACCOUNT_ANY) {
            for (ConcurrentHashMap<Long, Set<Integer>> map : removedReactionMessages.values()) {
                Set<Integer> set = map.get(dialogId);
                if (set != null && set.contains(messageId)) {
                    return true;
                }
            }
            return false;
        }
        ConcurrentHashMap<Long, Set<Integer>> map = removedReactionMessages.get(account);
        Set<Integer> set = map != null ? map.get(dialogId) : null;
        return set != null && set.contains(messageId);
    }

    public static List<ReactionEntry> getReactionHistory(long dialogId, int messageId) {
        return getReactionHistory(ACCOUNT_ANY, dialogId, messageId);
    }

    public static List<ReactionEntry> getReactionHistory(int account, long dialogId, int messageId) {
        List<ReactionEntry> list = new ArrayList<>();
        if (!VeyraUIConfig.reactionHistoryEnabled) {
            return list;
        }
        try {
            SQLiteDatabase db = getHelper().getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT date, reaction, count, user_id, COALESCE(action,'add') FROM " +
                    REACTION_TABLE_NAME + " WHERE dialog_id = ? AND message_id = ?" + accountClause(account) + " ORDER BY id ASC",
                    withAccount(account, String.valueOf(dialogId), String.valueOf(messageId)));
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int date = cursor.getInt(0);
                    String reaction = cursor.getString(1);
                    int count = cursor.getInt(2);
                    long userId = cursor.getLong(3);
                    String action = cursor.getString(4);
                    list.add(new ReactionEntry(dialogId, messageId, date, reaction, count, userId, action));
                }
                cursor.close();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static boolean hasReactionHistory(long dialogId, int messageId) {
        return hasReactionHistory(ACCOUNT_ANY, dialogId, messageId);
    }

    public static boolean hasReactionHistory(int account, long dialogId, int messageId) {
        if (!VeyraUIConfig.reactionHistoryEnabled) {
            return false;
        }
        if (isReactionRemovedCached(account, dialogId, messageId)) {
            return true;
        }
        try {
            SQLiteDatabase db = getHelper().getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT 1 FROM " + REACTION_TABLE_NAME +
                    " WHERE dialog_id = ? AND message_id = ? AND action = 'remove'" + accountClause(account) + " LIMIT 1",
                    withAccount(account, String.valueOf(dialogId), String.valueOf(messageId)));
            boolean exists = cursor != null && cursor.moveToFirst();
            if (cursor != null) cursor.close();
            if (exists && account != ACCOUNT_ANY) {
                markReactionRemoved(account, dialogId, messageId);
            }
            return exists;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
}

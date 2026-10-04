/* Veyra Project */

package org.veyra.client;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.VeyraConfig;

import java.util.ArrayList;
import java.util.List;

public final class VeyraEditHistoryManager {

    private static final String DB_NAME = "veyra_edit_history.db";
    private static final int DB_VERSION = 3;
    private static final String TABLE_NAME = "edit_history";
    private static final String REACTION_TABLE_NAME = "reaction_history";

    public static class EditEntry {
        public final long dialogId;
        public final int messageId;
        public final int date;
        public final String text;

        public EditEntry(long dialogId, int messageId, int date, String text) {
            this.dialogId = dialogId;
            this.messageId = messageId;
            this.date = date;
            this.text = text;
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
                    "dialog_id INTEGER, " +
                    "message_id INTEGER, " +
                    "date INTEGER, " +
                    "text TEXT);");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_msg ON " + TABLE_NAME + " (dialog_id, message_id);");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + REACTION_TABLE_NAME + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
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
        }
    }

    private static DBHelper dbHelper;

    private static synchronized DBHelper getHelper() {
        if (dbHelper == null) {
            dbHelper = new DBHelper();
        }
        return dbHelper;
    }

    public static void logEdit(long dialogId, int messageId, int date, String text) {
        if (!VeyraConfig.isChatTypeAllowedForEditHistory(dialogId) || TextUtils.isEmpty(text)) {
            return;
        }
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();

            // Encrypt text before storing
            String encryptedText = VeyraKeyStore.encryptString(text);
            if (encryptedText == null) encryptedText = text; // fallback plain if keystore fails

            Cursor checkCursor = db.rawQuery("SELECT text FROM " + TABLE_NAME + " WHERE dialog_id = ? AND message_id = ? ORDER BY id DESC LIMIT 1",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            if (checkCursor != null) {
                if (checkCursor.moveToFirst()) {
                    String lastRaw = checkCursor.getString(0);
                    checkCursor.close();
                    // Decrypt to compare
                    String lastDecrypted = VeyraKeyStore.decryptString(lastRaw);
                    if (lastDecrypted == null) lastDecrypted = lastRaw;
                    if (text.equals(lastDecrypted)) {
                        return; // no change
                    }
                } else {
                    checkCursor.close();
                }
            }

            // Check edit limit per message
            boolean dropOldest = VeyraConfig.editHistoryDropOldest;
            int maxEdits = Math.max(1, Math.min(50, VeyraConfig.editHistoryLimit));
            Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE dialog_id = ? AND message_id = ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            int total = 0;
            if (countCursor != null) {
                if (countCursor.moveToFirst()) total = countCursor.getInt(0);
                countCursor.close();
            }
            if (total >= maxEdits) {
                if (dropOldest) {
                    int toDelete = total - maxEdits + 1;
                    db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE id IN (SELECT id FROM " + TABLE_NAME +
                            " WHERE dialog_id = ? AND message_id = ? ORDER BY id ASC LIMIT ?)",
                            new Object[]{dialogId, messageId, toDelete});
                } else {
                    return; // limit reached and we don't drop oldest
                }
            }

            ContentValues values = new ContentValues();
            values.put("dialog_id", dialogId);
            values.put("message_id", messageId);
            values.put("date", date);
            values.put("text", encryptedText);
            db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static List<EditEntry> getHistory(long dialogId, int messageId) {
        List<EditEntry> list = new ArrayList<>();
        if (!VeyraConfig.editHistoryEnabled) {
            return list;
        }
        try {
            SQLiteDatabase db = getHelper().getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT date, text FROM " + TABLE_NAME + " WHERE dialog_id = ? AND message_id = ? ORDER BY id ASC",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int date = cursor.getInt(0);
                    String raw = cursor.getString(1);
                    String text = VeyraKeyStore.decryptString(raw);
                    if (text == null) text = raw;
                    list.add(new EditEntry(dialogId, messageId, date, text));
                }
                cursor.close();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static boolean hasHistory(long dialogId, int messageId) {
        if (!VeyraConfig.editHistoryEnabled) {
            return false;
        }
        try {
            SQLiteDatabase db = getHelper().getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_NAME + " WHERE dialog_id = ? AND message_id = ? LIMIT 1",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            boolean exists = cursor != null && cursor.moveToFirst();
            if (cursor != null) cursor.close();
            return exists;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    public static void deleteHistory(long dialogId, int messageId) {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(TABLE_NAME, "dialog_id = ? AND message_id = ?", new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void deleteHistoryBatch(long dialogId, List<Integer> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return;
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.beginTransaction();
            try {
                for (int msgId : messageIds) {
                    db.delete(TABLE_NAME, "dialog_id = ? AND message_id = ?", new String[]{String.valueOf(dialogId), String.valueOf(msgId)});
                    db.delete(REACTION_TABLE_NAME, "dialog_id = ? AND message_id = ?", new String[]{String.valueOf(dialogId), String.valueOf(msgId)});
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
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(TABLE_NAME, null, null);
            db.delete(REACTION_TABLE_NAME, null, null);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Clear only edit history (message edits). */
    public static void clearAllEdits() {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(TABLE_NAME, null, null);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Clear only reaction history. */
    public static void clearAllReactions() {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(REACTION_TABLE_NAME, null, null);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Clear all edit history entries for a single dialog. */
    public static void clearDialog(long dialogId) {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(TABLE_NAME, "dialog_id = ?", new String[]{String.valueOf(dialogId)});
            db.delete(REACTION_TABLE_NAME, "dialog_id = ?", new String[]{String.valueOf(dialogId)});
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
        if (!VeyraConfig.isChatTypeAllowedForReactionHistory(dialogId) || TextUtils.isEmpty(reaction)) {
            return;
        }
        // Ignore own reactions
        if (selfUserId != 0 && userId == selfUserId) {
            return;
        }
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();

            // Determine action: compare newCount to last logged count for this reaction
            String action = "add";
            Cursor lastCursor = db.rawQuery(
                    "SELECT count FROM " + REACTION_TABLE_NAME +
                            " WHERE dialog_id = ? AND message_id = ? AND reaction = ? ORDER BY id DESC LIMIT 1",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId), reaction});
            if (lastCursor != null) {
                if (lastCursor.moveToFirst()) {
                    int lastCount = lastCursor.getInt(0);
                    if (newCount < lastCount) {
                        action = "remove";
                    } else if (newCount == lastCount) {
                        // No change — skip
                        lastCursor.close();
                        return;
                    }
                }
                lastCursor.close();
            }

            // For user-specific reactions: avoid duplicate user+reaction entry for same action
            if (userId != 0) {
                Cursor c = db.rawQuery(
                        "SELECT action FROM " + REACTION_TABLE_NAME +
                                " WHERE dialog_id = ? AND message_id = ? AND user_id = ? AND reaction = ? ORDER BY id DESC LIMIT 1",
                        new String[]{String.valueOf(dialogId), String.valueOf(messageId),
                                String.valueOf(userId), reaction});
                if (c != null) {
                    if (c.moveToFirst()) {
                        String lastAction = c.getString(0);
                        c.close();
                        if (action.equals(lastAction)) {
                            return; // same action repeated — skip
                        }
                    } else {
                        c.close();
                    }
                }
            }

            // Check limit per message
            Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM " + REACTION_TABLE_NAME +
                    " WHERE dialog_id = ? AND message_id = ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            int total = 0;
            if (countCursor != null) {
                if (countCursor.moveToFirst()) total = countCursor.getInt(0);
                countCursor.close();
            }
            int limit = Math.max(5, Math.min(100, VeyraConfig.reactionHistoryLimit));
            boolean dropOldest = VeyraConfig.reactionHistoryDropOldest;
            if (total >= limit) {
                if (dropOldest) {
                    db.execSQL("DELETE FROM " + REACTION_TABLE_NAME +
                            " WHERE id IN (SELECT id FROM " + REACTION_TABLE_NAME +
                            " WHERE dialog_id = ? AND message_id = ? ORDER BY id ASC LIMIT ?)",
                            new Object[]{dialogId, messageId, total - limit + 1});
                } else {
                    return;
                }
            }

            ContentValues values = new ContentValues();
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

    public static List<ReactionEntry> getReactionHistory(long dialogId, int messageId) {
        List<ReactionEntry> list = new ArrayList<>();
        if (!VeyraConfig.reactionHistoryEnabled) {
            return list;
        }
        try {
            SQLiteDatabase db = getHelper().getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT date, reaction, count, user_id, COALESCE(action,'add') FROM " +
                    REACTION_TABLE_NAME + " WHERE dialog_id = ? AND message_id = ? ORDER BY id ASC",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
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
        if (!VeyraConfig.reactionHistoryEnabled) {
            return false;
        }
        try {
            SQLiteDatabase db = getHelper().getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT 1 FROM " + REACTION_TABLE_NAME +
                    " WHERE dialog_id = ? AND message_id = ? LIMIT 1",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            boolean exists = cursor != null && cursor.moveToFirst();
            if (cursor != null) cursor.close();
            return exists;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
}

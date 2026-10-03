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
    private static final int DB_VERSION = 2;
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

        public ReactionEntry(long dialogId, int messageId, int date, String reaction, int count, long userId) {
            this.dialogId = dialogId;
            this.messageId = messageId;
            this.date = date;
            this.reaction = reaction;
            this.count = count;
            this.userId = userId;
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
                    "user_id INTEGER);");
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
                    // Decrypt stored value to compare
                    String stored = checkCursor.getString(0);
                    String storedDecrypted = VeyraKeyStore.decryptString(stored);
                    if (storedDecrypted == null) storedDecrypted = stored;
                    if (TextUtils.equals(storedDecrypted, text)) {
                        checkCursor.close();
                        return;
                    }
                }
                checkCursor.close();
            }

            Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE dialog_id = ? AND message_id = ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            int count = 0;
            if (countCursor != null) {
                if (countCursor.moveToFirst()) {
                    count = countCursor.getInt(0);
                }
                countCursor.close();
            }

            int limit = Math.max(5, Math.min(100, VeyraConfig.editHistoryLimit));
            if (count >= limit) {
                if (VeyraConfig.editHistoryDropOldest) {
                    int deleteCount = count - limit + 1;
                    db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE id IN (" +
                            "SELECT id FROM " + TABLE_NAME + " WHERE dialog_id = ? AND message_id = ? ORDER BY id ASC LIMIT ?)",
                            new Object[]{dialogId, messageId, deleteCount});
                } else {
                    return;
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
        try {
            SQLiteDatabase db = getHelper().getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT date, text FROM " + TABLE_NAME + " WHERE dialog_id = ? AND message_id = ? ORDER BY id ASC",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int date = cursor.getInt(0);
                    String encryptedText = cursor.getString(1);
                    // Decrypt on read
                    String text = VeyraKeyStore.decryptString(encryptedText);
                    if (text == null) text = encryptedText; // fallback for legacy plain rows
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
            if (cursor != null) {
                cursor.close();
            }
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
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < messageIds.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(messageIds.get(i));
            }
            db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE dialog_id = " + dialogId + " AND message_id IN (" + sb.toString() + ")");
            db.execSQL("DELETE FROM " + REACTION_TABLE_NAME + " WHERE dialog_id = " + dialogId + " AND message_id IN (" + sb.toString() + ")");
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void clearAll() {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(TABLE_NAME, null, null);
            db.delete(REACTION_TABLE_NAME, null, null);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Clear all edit history entries for a single dialog (e.g. on "Deleted Chat" wipe). */
    public static void clearDialog(long dialogId) {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(TABLE_NAME, "dialog_id = ?", new String[]{String.valueOf(dialogId)});
            db.delete(REACTION_TABLE_NAME, "dialog_id = ?", new String[]{String.valueOf(dialogId)});
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void logReaction(long dialogId, int messageId, int date, String reaction, int count, long userId) {
        if (!VeyraConfig.reactionHistoryEnabled || TextUtils.isEmpty(reaction)) {
            return;
        }
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("dialog_id", dialogId);
            values.put("message_id", messageId);
            values.put("date", date);
            values.put("reaction", reaction);
            values.put("count", count);
            values.put("user_id", userId);
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
            Cursor cursor = db.rawQuery("SELECT date, reaction, count, user_id FROM " + REACTION_TABLE_NAME + " WHERE dialog_id = ? AND message_id = ? ORDER BY id ASC",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int date = cursor.getInt(0);
                    String reaction = cursor.getString(1);
                    int count = cursor.getInt(2);
                    long userId = cursor.getLong(3);
                    list.add(new ReactionEntry(dialogId, messageId, date, reaction, count, userId));
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
            Cursor cursor = db.rawQuery("SELECT 1 FROM " + REACTION_TABLE_NAME + " WHERE dialog_id = ? AND message_id = ? LIMIT 1",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            boolean exists = cursor != null && cursor.moveToFirst();
            if (cursor != null) {
                cursor.close();
            }
            return exists;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
}

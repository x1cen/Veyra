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
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "edit_history";

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
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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
        if (!VeyraConfig.editHistoryEnabled || TextUtils.isEmpty(text)) {
            return;
        }
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();

            Cursor checkCursor = db.rawQuery("SELECT text FROM " + TABLE_NAME + " WHERE dialog_id = ? AND message_id = ? ORDER BY id DESC LIMIT 1",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            if (checkCursor != null) {
                if (checkCursor.moveToFirst()) {
                    String lastText = checkCursor.getString(0);
                    if (TextUtils.equals(lastText, text)) {
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
            values.put("text", text);
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
                    String text = cursor.getString(1);
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
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void clearAll() {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(TABLE_NAME, null, null);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Clear all edit history entries for a single dialog (e.g. on "Deleted Chat" wipe). */
    public static void clearDialog(long dialogId) {
        try {
            SQLiteDatabase db = getHelper().getWritableDatabase();
            db.delete(TABLE_NAME, "dialog_id = ?", new String[]{String.valueOf(dialogId)});
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}

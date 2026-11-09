/* Veyra Project — Dedicated Anti-Delete Database
 * Completely separate from Telegram's cache4.db to ensure data is never lost
 * during Reset Chats (forceResetDialogs) or Telegram cache wipes.
 */
package org.veyra.client;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;
import androidx.collection.LongSparseArray;

import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class VeyraDatabase {

    private static final ConcurrentHashMap<Integer, VeyraDatabase> instances = new ConcurrentHashMap<>();

    private final int currentAccount;
    private SQLiteDatabase database;
    private final Object lock = new Object();

    public static VeyraDatabase getInstance(int account) {
        VeyraDatabase instance = instances.get(account);
        if (instance == null) {
            instance = new VeyraDatabase(account);
            VeyraDatabase old = instances.putIfAbsent(account, instance);
            if (old != null) {
                instance = old;
            }
        }
        return instance;
    }

    private VeyraDatabase(int account) {
        this.currentAccount = account;
    }

    public static File getDbFile(int account) {
        File filesDir = ApplicationLoader.getFilesDirFixed();
        if (account != 0) {
            filesDir = new File(filesDir, "account" + account + "/");
            filesDir.mkdirs();
        }
        return new File(filesDir, "veyra.db");
    }

    public void init(SQLiteDatabase telegramDb) {
        synchronized (lock) {
            if (database != null) {
                return;
            }
            try {
                File dbFile = getDbFile(currentAccount);
                database = new SQLiteDatabase(dbFile.getPath());
                database.executeFast("PRAGMA secure_delete = ON").stepThis().dispose();
                database.executeFast("PRAGMA temp_store = MEMORY").stepThis().dispose();
                database.executeFast("PRAGMA journal_mode = WAL").stepThis().dispose();
                database.executeFast("PRAGMA journal_size_limit = 10485760").stepThis().dispose();

                // Create independent tables
                database.executeFast("CREATE TABLE IF NOT EXISTS veyra_deleted_index (mid INTEGER, uid INTEGER, isdel INTEGER, PRIMARY KEY (mid, uid));").stepThis().dispose();
                database.executeFast("CREATE TABLE IF NOT EXISTS veyra_deleted_messages (mid INTEGER, uid INTEGER, date INTEGER, data BLOB, PRIMARY KEY (mid, uid));").stepThis().dispose();

                // Migrate legacy entries from Telegram's cache4.db if any exist
                if (telegramDb != null) {
                    SQLiteCursor cursor = null;
                    boolean inTx = false;
                    try {
                        telegramDb.executeFast("CREATE TABLE IF NOT EXISTS veyra_message_deletions (mid INTEGER, uid INTEGER, isdel INTEGER, PRIMARY KEY (mid, uid));").stepThis().dispose();
                        cursor = telegramDb.queryFinalized("SELECT mid, uid FROM veyra_message_deletions");
                        if (cursor.next()) {
                            database.beginTransaction();
                            inTx = true;
                            do {
                                int mid = cursor.intValue(0);
                                long uid = cursor.longValue(1);
                                saveDeletedIndex(uid, mid);
                            } while (cursor.next());
                            database.commitTransaction();
                            inTx = false;
                            cursor.dispose();
                            cursor = null;
                            // Legacy rows are only dropped after the copy committed.
                            telegramDb.executeFast("DELETE FROM veyra_message_deletions").stepThis().dispose();
                        }
                    } catch (Throwable t) {
                        FileLog.e(t);
                    } finally {
                        if (inTx) {
                            try {
                                database.executeFast("ROLLBACK").stepThis().dispose();
                            } catch (Throwable t) {
                                FileLog.e(t);
                            }
                            // Resets the wrapper's transaction flag; the native COMMIT is a no-op after ROLLBACK.
                            database.commitTransaction();
                        }
                        if (cursor != null) {
                            cursor.dispose();
                        }
                    }
                }

                loadAllIntoMemory();
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    public void loadAllIntoMemory() {
        synchronized (lock) {
            if (database == null) return;
            try {
                SQLiteCursor cursor = database.queryFinalized("SELECT mid, uid FROM veyra_deleted_index");
                while (cursor.next()) {
                    int mid = cursor.intValue(0);
                    long uid = cursor.longValue(1);
                    VeyraAntiDelete.addDeletedMessage(currentAccount, uid, mid);
                }
                cursor.dispose();
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    public void saveDeletedIndex(long uid, int mid) {
        synchronized (lock) {
            if (database == null) return;
            try {
                database.executeFast(String.format(Locale.US, "INSERT OR REPLACE INTO veyra_deleted_index (mid, uid, isdel) VALUES (%d, %d, 1);", mid, uid)).stepThis().dispose();
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
        VeyraAntiDelete.addDeletedMessage(currentAccount, uid, mid);
    }

    public void saveDeletedMessage(long uid, int mid, int date, byte[] dataBytes) {
        saveDeletedIndex(uid, mid);
        if (dataBytes == null || dataBytes.length == 0) return;
        synchronized (lock) {
            if (database == null) return;
            NativeByteBuffer buffer = null;
            SQLitePreparedStatement state = null;
            try {
                buffer = new NativeByteBuffer(dataBytes.length);
                buffer.writeBytes(dataBytes);
                buffer.position(0);
                state = database.executeFast("INSERT OR REPLACE INTO veyra_deleted_messages VALUES(?, ?, ?, ?)");
                state.bindInteger(1, mid);
                state.bindLong(2, uid);
                state.bindInteger(3, date);
                state.bindByteBuffer(4, buffer);
                state.step();
            } catch (Throwable t) {
                FileLog.e(t);
            } finally {
                if (state != null) {
                    state.dispose();
                }
                if (buffer != null) {
                    buffer.reuse();
                }
            }
        }
    }

    public void restoreDeletedMessagesToStorage(int account, SQLiteDatabase telegramDb) {
        if (telegramDb == null) return;
        synchronized (lock) {
            if (database == null) return;
            try {
                // 1. Ensure veyra_message_deletions in telegramDb has all entries
                SQLiteCursor cursor = database.queryFinalized("SELECT mid, uid FROM veyra_deleted_index");
                while (cursor.next()) {
                    int mid = cursor.intValue(0);
                    long uid = cursor.longValue(1);
                    try {
                        telegramDb.executeFast(String.format(Locale.US, "INSERT OR REPLACE INTO veyra_message_deletions (mid, uid, isdel) VALUES (%d, %d, 1);", mid, uid)).stepThis().dispose();
                    } catch (Throwable ignored) {}
                }
                cursor.dispose();

                // 2. Ensure all deleted_messages are restored into messages_v2 if missing
                SQLiteCursor msgCursor = database.queryFinalized("SELECT mid, uid, date, data FROM veyra_deleted_messages WHERE data IS NOT NULL");
                SQLitePreparedStatement insertState = null;
                try {
                    while (msgCursor.next()) {
                        int mid = msgCursor.intValue(0);
                        long uid = msgCursor.longValue(1);
                        int date = msgCursor.intValue(2);
                        NativeByteBuffer data = msgCursor.byteBufferValue(3);
                        if (data != null) {
                            try {
                                if (insertState == null) {
                                    insertState = telegramDb.executeFast("INSERT OR IGNORE INTO messages_v2 (mid, uid, read_state, send_state, date, data, out, ttl, media, replydata, imp, mention, forwards, replies_data, thread_reply_id, is_channel, reply_to_message_id, custom_params, group_id, reply_to_story_id) VALUES (?, ?, 0, 0, ?, ?, ?, 0, 0, NULL, 0, 0, 0, NULL, 0, ?, 0, NULL, NULL, 0)");
                                }
                                int outFlag = 0;
                                long channelId = 0;
                                try {
                                    org.telegram.tgnet.TLRPC.Message parsed = org.telegram.tgnet.TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                    if (parsed != null) {
                                        outFlag = (org.telegram.messenger.MessageObject.isOut(parsed) || parsed.from_scheduled) ? 1 : 0;
                                        channelId = org.telegram.messenger.MessageObject.getChannelId(parsed);
                                    }
                                } catch (Throwable ignored) {
                                } finally {
                                    data.position(0);
                                }
                                insertState.requery();
                                insertState.bindInteger(1, mid);
                                insertState.bindLong(2, uid);
                                insertState.bindInteger(3, date);
                                insertState.bindByteBuffer(4, data);
                                insertState.bindInteger(5, outFlag);
                                insertState.bindLong(6, channelId);
                                insertState.step();
                            } catch (Throwable ignored) {
                            } finally {
                                data.reuse();
                            }
                        }
                    }
                } finally {
                    if (insertState != null) {
                        insertState.dispose();
                    }
                    msgCursor.dispose();
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    public void deleteForDialog(long uid) {
        synchronized (lock) {
            if (database == null) return;
            try {
                database.executeFast(String.format(Locale.US, "DELETE FROM veyra_deleted_index WHERE uid = %d OR uid = %d", uid, -uid)).stepThis().dispose();
                database.executeFast(String.format(Locale.US, "DELETE FROM veyra_deleted_messages WHERE uid = %d OR uid = %d", uid, -uid)).stepThis().dispose();
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
        VeyraAntiDelete.removeChatDeleted(currentAccount, uid);
    }

    public void deleteMessage(long uid, int mid) {
        synchronized (lock) {
            if (database == null) return;
            try {
                database.executeFast(String.format(Locale.US, "DELETE FROM veyra_deleted_index WHERE mid = %d AND (uid = %d OR uid = %d)", mid, uid, -uid)).stepThis().dispose();
                database.executeFast(String.format(Locale.US, "DELETE FROM veyra_deleted_messages WHERE mid = %d AND (uid = %d OR uid = %d)", mid, uid, -uid)).stepThis().dispose();
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
        VeyraAntiDelete.removeDeletedMessage(currentAccount, uid, mid);
    }

    public void deleteMessages(long uid, List<Integer> mids) {
        if (mids == null || mids.isEmpty()) return;
        synchronized (lock) {
            if (database == null) return;
            try {
                String ids = TextUtils.join(",", mids);
                database.executeFast(String.format(Locale.US, "DELETE FROM veyra_deleted_index WHERE mid IN(%s) AND (uid = %d OR uid = %d)", ids, uid, -uid)).stepThis().dispose();
                database.executeFast(String.format(Locale.US, "DELETE FROM veyra_deleted_messages WHERE mid IN(%s) AND (uid = %d OR uid = %d)", ids, uid, -uid)).stepThis().dispose();
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
        for (int i = 0; i < mids.size(); i++) {
            VeyraAntiDelete.removeDeletedMessage(currentAccount, uid, mids.get(i));
        }
    }

    public boolean hasEntries() {
        synchronized (lock) {
            if (database == null) {
                File dbFile = getDbFile(currentAccount);
                if (!dbFile.exists()) {
                    return false;
                }
                try {
                    init(null);
                } catch (Throwable ignored) {}
            }
            if (database == null) return false;
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized("SELECT 1 FROM veyra_deleted_index LIMIT 1");
                return cursor.next();
            } catch (Throwable ignored) {
                return false;
            } finally {
                if (cursor != null) cursor.dispose();
            }
        }
    }

    public androidx.collection.LongSparseArray<ArrayList<Integer>> getAllDeletedMessages() {
        androidx.collection.LongSparseArray<ArrayList<Integer>> result = new androidx.collection.LongSparseArray<>();
        synchronized (lock) {
            if (database == null) {
                File dbFile = getDbFile(currentAccount);
                if (!dbFile.exists()) {
                    return result;
                }
                try {
                    init(null);
                } catch (Throwable ignored) {}
            }
            if (database == null) return result;
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized("SELECT uid, mid FROM veyra_deleted_index");
                while (cursor.next()) {
                    long uid = cursor.longValue(0);
                    int mid = cursor.intValue(1);
                    ArrayList<Integer> list = result.get(uid);
                    if (list == null) {
                        list = new ArrayList<>();
                        result.put(uid, list);
                    }
                    list.add(mid);
                }
            } catch (Throwable t) {
                FileLog.e(t);
            } finally {
                if (cursor != null) cursor.dispose();
            }
        }
        return result;
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    public ArrayList<Integer> getDeletedMessagesForDialog(long uid) {
        ArrayList<Integer> result = new ArrayList<>();
        synchronized (lock) {
            if (database == null) {
                File dbFile = getDbFile(currentAccount);
                if (!dbFile.exists()) {
                    return result;
                }
                try {
                    init(null);
                } catch (Throwable ignored) {}
            }
            if (database == null) return result;
            SQLiteCursor cursor = null;
            try {
                cursor = database.queryFinalized(String.format(Locale.US, "SELECT mid FROM veyra_deleted_index WHERE uid = %d OR uid = %d", uid, -uid));
                while (cursor.next()) {
                    result.add(cursor.intValue(0));
                }
            } catch (Throwable t) {
                FileLog.e(t);
            } finally {
                if (cursor != null) cursor.dispose();
            }
        }
        return result;
    }

    public static void purgeAntiDeletedMessages(final int account, final long specificDialogId, final Runnable onComplete) {
        MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            SQLiteDatabase db = storage.getDatabase();
            if (db == null) {
                if (onComplete != null) AndroidUtilities.runOnUIThread(onComplete);
                return;
            }

            VeyraDatabase vDb = VeyraDatabase.getInstance(account);
            LongSparseArray<ArrayList<Integer>> deletedByDialog = new LongSparseArray<>();
            if (specificDialogId != 0) {
                ArrayList<Integer> mids = vDb.getDeletedMessagesForDialog(specificDialogId);
                if (!mids.isEmpty()) {
                    deletedByDialog.put(specificDialogId, mids);
                }
            } else {
                deletedByDialog = vDb.getAllDeletedMessages();
            }

            ArrayList<File> filesToDelete = new ArrayList<>();
            long currentUser = UserConfig.getInstance(account).getClientUserId();

            if (deletedByDialog != null && deletedByDialog.size() > 0) {
                for (int a = 0; a < deletedByDialog.size(); a++) {
                    long did = deletedByDialog.keyAt(a);
                    ArrayList<Integer> mids = deletedByDialog.valueAt(a);
                    if (mids == null || mids.isEmpty()) continue;

                    String ids = TextUtils.join(",", mids);
                    SQLiteCursor cursor = null;
                    try {
                        cursor = db.queryFinalized(String.format(Locale.US, "SELECT data FROM messages_v2 WHERE mid IN(%s) AND (uid = %d OR uid = %d)", ids, did, -did));
                        while (cursor.next()) {
                            NativeByteBuffer data = cursor.byteBufferValue(0);
                            if (data != null) {
                                try {
                                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                    if (message != null) {
                                        message.readAttachPath(data, currentUser);
                                        if (message.media != null) {
                                            if (message.media.document != null) {
                                                File f = FileLoader.getInstance(account).getPathToAttach(message.media.document);
                                                if (f != null && f.exists()) filesToDelete.add(f);
                                            }
                                            if (message.media.photo != null && message.media.photo.sizes != null) {
                                                for (TLRPC.PhotoSize size : message.media.photo.sizes) {
                                                    File f = FileLoader.getInstance(account).getPathToAttach(size);
                                                    if (f != null && f.exists()) filesToDelete.add(f);
                                                }
                                            }
                                        }
                                        if (!TextUtils.isEmpty(message.attachPath)) {
                                            File f = new File(message.attachPath);
                                            if (f.exists()) filesToDelete.add(f);
                                        }
                                    }
                                } catch (Throwable ignored) {}
                                data.reuse();
                            }
                        }
                    } catch (Throwable t) {
                        FileLog.e(t);
                    } finally {
                        if (cursor != null) cursor.dispose();
                    }

                    try {
                        db.executeFast(String.format(Locale.US, "DELETE FROM messages_v2 WHERE mid IN(%s) AND (uid = %d OR uid = %d)", ids, did, -did)).stepThis().dispose();
                        db.executeFast(String.format(Locale.US, "DELETE FROM messages_topics WHERE mid IN(%s) AND (uid = %d OR uid = %d)", ids, did, -did)).stepThis().dispose();
                        db.executeFast(String.format(Locale.US, "DELETE FROM media_v4 WHERE mid IN(%s) AND (uid = %d OR uid = %d)", ids, did, -did)).stepThis().dispose();
                        db.executeFast(String.format(Locale.US, "DELETE FROM chat_pinned_v2 WHERE mid IN(%s) AND (uid = %d OR uid = %d)", ids, did, -did)).stepThis().dispose();
                        db.executeFast(String.format(Locale.US, "DELETE FROM widget_state WHERE mid IN(%s) AND (uid = %d OR uid = %d)", ids, did, -did)).stepThis().dispose();
                        db.executeFast(String.format(Locale.US, "DELETE FROM bot_keyboard WHERE mid IN(%s) AND (uid = %d OR uid = %d)", ids, did, -did)).stepThis().dispose();
                    } catch (Throwable t) {
                        FileLog.e(t);
                    }
                }
            }

            for (File f : filesToDelete) {
                try {
                    if (f != null && f.exists()) f.delete();
                } catch (Throwable ignored) {}
            }

            if (specificDialogId != 0) {
                vDb.deleteForDialog(specificDialogId);
                VeyraEditHistoryManager.clearDialog(account, specificDialogId);
                VeyraAntiDelete.removeChatDeleted(account, specificDialogId);
            } else {
                vDb.clearAll();
                VeyraEditHistoryManager.clearAll(account);
                VeyraAntiDelete.clearAll(account);
            }

            try {
                SQLiteDatabase vSqlite = vDb.getDatabase();
                if (vSqlite != null) {
                    try {
                        vSqlite.executeFast("PRAGMA wal_checkpoint(TRUNCATE);").stepThis().dispose();
                        vSqlite.executeFast("VACUUM;").stepThis().dispose();
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            AndroidUtilities.runOnUIThread(() -> {
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
                if (onComplete != null) onComplete.run();
            });
        });
    }

    public void clearAll() {
        synchronized (lock) {
            if (database != null) {
                try {
                    database.executeFast("DELETE FROM veyra_deleted_index").stepThis().dispose();
                    database.executeFast("DELETE FROM veyra_deleted_messages").stepThis().dispose();
                    database.executeFast("PRAGMA journal_size_limit = 0").stepThis().dispose();
                    database.executeFast("VACUUM").stepThis().dispose();
                    database.executeFast("PRAGMA journal_size_limit = 10485760").stepThis().dispose();
                } catch (Throwable t) {
                    FileLog.e(t);
                }
            } else {
                try {
                    File dbFile = getDbFile(currentAccount);
                    if (dbFile.exists()) dbFile.delete();
                    new File(dbFile.getPath() + "-wal").delete();
                    new File(dbFile.getPath() + "-shm").delete();
                } catch (Throwable t) {
                    FileLog.e(t);
                }
            }
        }
    }
}

/* Veyra Project — Dedicated Anti-Delete Database
 * Completely separate from Telegram's cache4.db to ensure data is never lost
 * during Reset Chats (forceResetDialogs) or Telegram cache wipes.
 */
package org.veyra.client;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesStorage;
import org.telegram.tgnet.NativeByteBuffer;

import java.io.File;
import java.util.ArrayList;
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

    public void init(SQLiteDatabase telegramDb) {
        synchronized (lock) {
            if (database != null) {
                return;
            }
            try {
                File filesDir = ApplicationLoader.getFilesDirFixed();
                if (currentAccount != 0) {
                    filesDir = new File(filesDir, "account" + currentAccount + "/");
                    filesDir.mkdirs();
                }
                File dbFile = new File(filesDir, "veyra.db");
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
                    try {
                        telegramDb.executeFast("CREATE TABLE IF NOT EXISTS veyra_message_deletions (mid INTEGER, uid INTEGER, isdel INTEGER, PRIMARY KEY (mid, uid));").stepThis().dispose();
                        SQLiteCursor cursor = telegramDb.queryFinalized("SELECT mid, uid FROM veyra_message_deletions");
                        while (cursor.next()) {
                            int mid = cursor.intValue(0);
                            long uid = cursor.longValue(1);
                            saveDeletedIndex(uid, mid);
                        }
                        cursor.dispose();
                    } catch (Throwable ignored) {}
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
                    VeyraAntiDelete.addDeletedMessage(uid, mid);
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
        VeyraAntiDelete.addDeletedMessage(uid, mid);
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
                                    insertState = telegramDb.executeFast("INSERT OR IGNORE INTO messages_v2 VALUES(?, ?, 0, 0, ?, ?, ?, 0, 0, 0, 0, 0, 0, 0, NULL)");
                                }
                                insertState.requery();
                                insertState.bindInteger(1, mid);
                                insertState.bindLong(2, uid);
                                insertState.bindInteger(3, date);
                                insertState.bindByteBuffer(4, data);
                                insertState.bindInteger(5, (uid < 0 && (uid < -2000000000 || uid < -1000000000)) ? 1 : 0);
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
        VeyraAntiDelete.removeChatDeleted(uid);
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
        VeyraAntiDelete.removeDeletedMessage(uid, mid);
    }

    public void clearAll() {
        synchronized (lock) {
            if (database == null) return;
            try {
                database.executeFast("DELETE FROM veyra_deleted_index").stepThis().dispose();
                database.executeFast("DELETE FROM veyra_deleted_messages").stepThis().dispose();
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }
}

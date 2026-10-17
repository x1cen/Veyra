/* Veyra Project — Emergency Handler */

package org.veyra.client;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.SystemClock;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;

import java.io.File;
import java.util.ArrayList;

public class VeyraEmergencyHandler {

    // ─── Public entry points called from DialogsActivity ───────────────────

    public static void confirmLocalWipePublic(final BaseFragment fragment, final int currentAccount) {
        if (fragment == null || fragment.getParentActivity() == null) return;
        Context ctx = fragment.getParentActivity();
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(LocaleController.getString("VeyraEmergencyLocalWipe", R.string.VeyraEmergencyLocalWipe));
        b.setMessage(LocaleController.getString("VeyraEmergencyConfirmLocal", R.string.VeyraEmergencyConfirmLocal));
        b.setPositiveButton(LocaleController.getString("OK", R.string.OK), (d, w) -> executeLocalWipe(ctx));
        b.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        b.show();
    }

    public static void confirmFullWipePublic(final BaseFragment fragment, final int currentAccount) {
        if (fragment == null || fragment.getParentActivity() == null) return;
        Context ctx = fragment.getParentActivity();
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(LocaleController.getString("VeyraEmergencyFullWipe", R.string.VeyraEmergencyFullWipe));
        b.setMessage(LocaleController.getString("VeyraEmergencyConfirmFull", R.string.VeyraEmergencyConfirmFull));
        b.setPositiveButton(LocaleController.getString("OK", R.string.OK), (d, w) ->
                checkAndExecuteFullWipe(fragment, currentAccount, ctx));
        b.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        b.show();
    }

    // ─── Legacy entry point (kept for compatibility) ────────────────────────

    public static void showEmergencyDialog(final BaseFragment fragment, final int currentAccount) {
        confirmLocalWipePublic(fragment, currentAccount);
    }

    // ─── Internal: Full Wipe ────────────────────────────────────────────────

    private static void checkAndExecuteFullWipe(final BaseFragment fragment,
                                                 final int currentAccount,
                                                 final Context context) {
        boolean isOnline = false;
        try {
            int state = ConnectionsManager.getInstance(currentAccount).getConnectionState();
            isOnline = (state == ConnectionsManager.ConnectionStateConnected);
            if (!isOnline && ApplicationLoader.applicationContext != null) {
                ConnectivityManager cm = (ConnectivityManager)
                        ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
                isOnline = (ni != null && ni.isConnected());
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }

        if (!isOnline) {
            // Only alert — no auto-fallback to local wipe
            AlertDialog.Builder b = new AlertDialog.Builder(context);
            b.setTitle(LocaleController.getString("VeyraEmergencyNoInternetTitle", R.string.VeyraEmergencyNoInternetTitle));
            b.setMessage(LocaleController.getString("VeyraEmergencyNoInternet", R.string.VeyraEmergencyNoInternet));
            b.setNegativeButton(LocaleController.getString("OK", R.string.OK), null);
            b.show();
            return;
        }

        final AlertDialog progress = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER);
        progress.setMessage(LocaleController.getString("VeyraEmergencyProgress", R.string.VeyraEmergencyProgress));
        progress.setCanceledOnTouchOutside(false);
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                executeFullWipeBlocking(currentAccount);
            } catch (Throwable t) {
                FileLog.e(t);
            }
            AndroidUtilities.runOnUIThread(() -> {
                try { progress.dismiss(); } catch (Throwable ignored) {}
                executeLocalWipe(context);
            });
        }).start();
    }

    public static void executeDuressAction(Context context, int action, boolean fallbackToLocal) {
        if (action == SharedConfig.DURESS_ACTION_LOCAL_WIPE) {
            executeLocalWipe(context);
            return;
        }

        boolean isOnline = false;
        try {
            isOnline = ApplicationLoader.isNetworkOnline();
            if (!isOnline && ApplicationLoader.applicationContext != null) {
                ConnectivityManager cm = (ConnectivityManager)
                        ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
                isOnline = (ni != null && ni.isConnected());
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }

        if (!isOnline) {
            if (fallbackToLocal) {
                FileLog.d("Duress: No internet, falling back to Local Wipe");
                executeLocalWipe(context);
            }
            return;
        }

        new Thread(() -> {
            try {
                executeFullWipeBlocking(UserConfig.selectedAccount);
            } catch (Throwable t) {
                FileLog.e(t);
            } finally {
                AndroidUtilities.runOnUIThread(() -> executeLocalWipe(context));
            }
        }, "DuressFullWipeThread").start();
    }

    /**
     * Full remote wipe — runs on a background thread.
     *
     * Order of operations:
     * 1. For every group the user is a member of:
     *    - If they own it → delete the group entirely.
     *    - If they are admin (and have premium) → delete all messages for everyone, then leave.
     *    - Otherwise → delete messages 100 at a time, then leave.
     * 2. For every channel:
     *    - If they own it → delete the channel entirely.
     *    - Otherwise → leave.
     * 3. For every private / bot chat → 2-way history delete.
     * 4. Delete the Telegram account.
     */
    private static void executeFullWipeBlocking(int currentAccount) {
        MessagesController mc = MessagesController.getInstance(currentAccount);
        long selfId = UserConfig.getInstance(currentAccount).getClientUserId();
        TLRPC.User self = UserConfig.getInstance(currentAccount).getCurrentUser();

        ArrayList<TLRPC.Dialog> allDialogs;
        try {
            allDialogs = new ArrayList<>(mc.getAllDialogs());
        } catch (Throwable t) {
            FileLog.e(t);
            return;
        }

        for (TLRPC.Dialog dialog : allDialogs) {
            if (dialog == null) continue;
            try {
                long did = dialog.id;

                if (did > 0) {
                    // ── Private / bot chat ──────────────────────────────────
                    TLRPC.TL_messages_deleteHistory req = new TLRPC.TL_messages_deleteHistory();
                    req.peer = mc.getInputPeer(did);
                    req.just_clear = false;
                    req.revoke = true;
                    req.max_id = Integer.MAX_VALUE;
                    if (req.peer != null) {
                        ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);
                        safeSleep(80);
                    }

                } else if (did < 0) {
                    long chatId = -did;
                    TLRPC.Chat chat = mc.getChat(chatId);
                    if (chat == null) continue;

                    boolean isChannel = ChatObject.isChannel(chat);
                    boolean isOwner   = (chat.creator);

                    if (isChannel) {
                        if (isOwner) {
                            // Delete channel entirely
                            TLRPC.TL_channels_deleteChannel delChan = new TLRPC.TL_channels_deleteChannel();
                            delChan.channel = mc.getInputChannel(chatId);
                            if (delChan.channel != null) {
                                ConnectionsManager.getInstance(currentAccount).sendRequest(delChan, null);
                                safeSleep(100);
                            }
                        } else {
                            // Leave channel
                            TLRPC.TL_channels_leaveChannel leave = new TLRPC.TL_channels_leaveChannel();
                            leave.channel = mc.getInputChannel(chatId);
                            if (leave.channel != null) {
                                ConnectionsManager.getInstance(currentAccount).sendRequest(leave, null);
                                safeSleep(80);
                            }
                        }
                    } else {
                        // ── Legacy group / supergroup ───────────────────────
                        if (isOwner) {
                            // Owner: delete the whole group
                            if (ChatObject.isMegagroup(chat)) {
                                TLRPC.TL_channels_deleteChannel delSg = new TLRPC.TL_channels_deleteChannel();
                                delSg.channel = mc.getInputChannel(chatId);
                                if (delSg.channel != null) {
                                    ConnectionsManager.getInstance(currentAccount).sendRequest(delSg, null);
                                    safeSleep(120);
                                }
                            } else {
                                TLRPC.TL_messages_deleteChat delChat = new TLRPC.TL_messages_deleteChat();
                                delChat.chat_id = chatId;
                                ConnectionsManager.getInstance(currentAccount).sendRequest(delChat, null);
                                safeSleep(120);
                            }
                        } else {
                            boolean isAdmin = ChatObject.hasAdminRights(chat) ||
                                    (chat.admin_rights != null);
                            if (isAdmin) {
                                // Admin: delete all messages for everyone then leave
                                if (ChatObject.isMegagroup(chat)) {
                                    TLRPC.TL_channels_deleteHistory dh = new TLRPC.TL_channels_deleteHistory();
                                    dh.channel = mc.getInputChannel(chatId);
                                    dh.max_id = Integer.MAX_VALUE;
                                    dh.for_everyone = true;
                                    if (dh.channel != null) {
                                        ConnectionsManager.getInstance(currentAccount).sendRequest(dh, null);
                                        safeSleep(150);
                                    }
                                }
                                // Leave
                                if (ChatObject.isMegagroup(chat)) {
                                    TLRPC.TL_channels_leaveChannel leave = new TLRPC.TL_channels_leaveChannel();
                                    leave.channel = mc.getInputChannel(chatId);
                                    if (leave.channel != null) {
                                        ConnectionsManager.getInstance(currentAccount).sendRequest(leave, null);
                                        safeSleep(80);
                                    }
                                } else {
                                    leaveBasicGroup(mc, currentAccount, chatId, self);
                                }
                            } else {
                                // Regular member: delete own messages 100 at a time, then leave
                                deleteOwnMessagesInBatches(mc, currentAccount, chatId, selfId);
                                safeSleep(120);
                                if (ChatObject.isMegagroup(chat)) {
                                    TLRPC.TL_channels_leaveChannel leave = new TLRPC.TL_channels_leaveChannel();
                                    leave.channel = mc.getInputChannel(chatId);
                                    if (leave.channel != null) {
                                        ConnectionsManager.getInstance(currentAccount).sendRequest(leave, null);
                                        safeSleep(80);
                                    }
                                } else {
                                    leaveBasicGroup(mc, currentAccount, chatId, self);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }

        // ── Delete Telegram account ─────────────────────────────────────────
        try {
            org.telegram.tgnet.tl.TL_account.deleteAccount delReq = new org.telegram.tgnet.tl.TL_account.deleteAccount();
            delReq.reason = "Emergency self-destruction";
            ConnectionsManager.getInstance(currentAccount).sendRequest(delReq, null);
            safeSleep(1500);
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    /** Leave a basic (non-channel) group and revoke history. */
    private static void leaveBasicGroup(MessagesController mc, int currentAccount,
                                         long chatId, TLRPC.User self) {
        if (self == null) return;
        try {
            TLRPC.TL_messages_deleteChatUser req = new TLRPC.TL_messages_deleteChatUser();
            req.chat_id = chatId;
            req.user_id = mc.getInputUser(self);
            req.revoke_history = true;
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);
            safeSleep(80);
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    /**
     * For non-admin members: search for our own messages by user ID and
     * delete them in batches of 100, respecting FloodWait with 60ms gaps.
     *
     * Note: TL_messages_search is async; we use a simple blocking latch.
     */
    private static void deleteOwnMessagesInBatches(MessagesController mc, int currentAccount,
                                                     long chatId, long selfId) {
        try {
            int offsetId = 0;
            while (true) {
                // Search our own messages in this chat
                TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
                req.peer = mc.getInputPeer(chatId);
                req.q = "";
                req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
                req.from_id = mc.getInputPeer(selfId);
                req.limit = 100;
                req.offset_id = offsetId;
                req.flags |= 64; // from_id

                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                final int[] lastId = {0};
                final ArrayList<Integer> ids = new ArrayList<>();

                ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                    if (response instanceof TLRPC.messages_Messages) {
                        TLRPC.messages_Messages msgs = (TLRPC.messages_Messages) response;
                        for (TLRPC.Message m : msgs.messages) {
                            ids.add(m.id);
                            if (lastId[0] == 0 || m.id < lastId[0]) lastId[0] = m.id;
                        }
                    }
                    latch.countDown();
                });

                try { latch.await(8, java.util.concurrent.TimeUnit.SECONDS); } catch (Throwable ignored) {}

                if (ids.isEmpty()) break;

                // Delete the batch
                if (ChatObject.isMegagroup(mc.getChat(chatId))) {
                    TLRPC.TL_channels_deleteMessages delMsgs = new TLRPC.TL_channels_deleteMessages();
                    delMsgs.channel = mc.getInputChannel(chatId);
                    delMsgs.id = ids;
                    ConnectionsManager.getInstance(currentAccount).sendRequest(delMsgs, null);
                } else {
                    TLRPC.TL_messages_deleteMessages delMsgs = new TLRPC.TL_messages_deleteMessages();
                    delMsgs.id = ids;
                    delMsgs.revoke = true;
                    ConnectionsManager.getInstance(currentAccount).sendRequest(delMsgs, null);
                }

                safeSleep(60);
                if (lastId[0] == 0 || ids.size() < 100) break;
                offsetId = lastId[0];
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    // ─── Local wipe ─────────────────────────────────────────────────────────

    public static void executeLocalWipe(Context context) {
        deleteTelegramDocumentsAndMedia();
        try { VeyraAntiDelete.clearAll(); } catch (Throwable ignored) {}
        try { VeyraEditHistoryManager.clearAll(); } catch (Throwable ignored) {}
        try { VeyraKeyStore.deleteKey(); } catch (Throwable ignored) {}
        VeyraSecurity.wipeAllDataAndReset(context);
    }

    private static void deleteTelegramDocumentsAndMedia() {
        try {
            deleteRecursive(new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Telegram"));
            deleteRecursive(new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Telegram"));
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        f.delete();
    }

    private static void safeSleep(long ms) {
        try { SystemClock.sleep(ms); } catch (Throwable ignored) {}
    }
}

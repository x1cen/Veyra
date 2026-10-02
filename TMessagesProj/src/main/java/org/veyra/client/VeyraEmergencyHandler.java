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
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;

import java.io.File;
import java.util.ArrayList;

public class VeyraEmergencyHandler {

    public static void showEmergencyDialog(final BaseFragment fragment, final int currentAccount) {
        if (fragment == null || fragment.getParentActivity() == null) return;
        Context context = fragment.getParentActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("VeyraEmergencyTitle", R.string.VeyraEmergencyTitle));

        CharSequence[] items = new CharSequence[]{
                LocaleController.getString("VeyraEmergencyLocalWipe", R.string.VeyraEmergencyLocalWipe),
                LocaleController.getString("VeyraEmergencyFullWipe", R.string.VeyraEmergencyFullWipe)
        };

        builder.setItems(items, (dialog, which) -> {
            if (which == 0) {
                confirmLocalWipe(fragment, context);
            } else if (which == 1) {
                confirmFullWipe(fragment, currentAccount, context);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    private static void confirmLocalWipe(final BaseFragment fragment, final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("VeyraEmergencyLocalWipe", R.string.VeyraEmergencyLocalWipe));
        builder.setMessage(LocaleController.getString("VeyraEmergencyConfirmLocal", R.string.VeyraEmergencyConfirmLocal));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            executeLocalWipe(context);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    private static void confirmFullWipe(final BaseFragment fragment, final int currentAccount, final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("VeyraEmergencyFullWipe", R.string.VeyraEmergencyFullWipe));
        builder.setMessage(LocaleController.getString("VeyraEmergencyConfirmFull", R.string.VeyraEmergencyConfirmFull));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            checkAndExecuteFullWipe(fragment, currentAccount, context);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    private static void checkAndExecuteFullWipe(final BaseFragment fragment, final int currentAccount, final Context context) {
        boolean isOnline = false;
        try {
            int state = ConnectionsManager.getInstance(currentAccount).getConnectionState();
            isOnline = (state == ConnectionsManager.ConnectionStateConnected);
            if (!isOnline && ApplicationLoader.applicationContext != null) {
                ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
                isOnline = (ni != null && ni.isConnected());
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }

        if (!isOnline) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(LocaleController.getString("VeyraEmergencyNoInternetTitle", R.string.VeyraEmergencyNoInternetTitle));
            builder.setMessage(LocaleController.getString("VeyraEmergencyNoInternet", R.string.VeyraEmergencyNoInternet));
            builder.setPositiveButton(LocaleController.getString("VeyraEmergencyRunLocal", R.string.VeyraEmergencyRunLocal), (d, w) -> {
                executeLocalWipe(context);
            });
            builder.setNegativeButton(LocaleController.getString("Retry", R.string.Retry), (d, w) -> {
                checkAndExecuteFullWipe(fragment, currentAccount, context);
            });
            builder.setNeutralButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            builder.show();
            return;
        }

        final AlertDialog progress = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER);
        progress.setMessage(LocaleController.getString("VeyraEmergencyProgress", R.string.VeyraEmergencyProgress));
        progress.setCanceledOnTouchOutside(false);
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            // Step 1: Wipe all local media / Documents / Telegram files
            deleteTelegramDocumentsAndMedia();

            // Step 2: Iterate dialogs and leave groups / 2-way delete
            try {
                MessagesController mc = MessagesController.getInstance(currentAccount);
                ArrayList<TLRPC.Dialog> allDialogs = new ArrayList<>(mc.getAllDialogs());
                for (TLRPC.Dialog dialog : allDialogs) {
                    if (dialog == null) continue;
                    try {
                        long did = dialog.id;
                        if (did > 0) {
                            // 1-on-1 private chat: delete 2-way
                            TLRPC.TL_messages_deleteHistory req = new TLRPC.TL_messages_deleteHistory();
                            req.peer = mc.getInputPeer(did);
                            req.just_clear = false;
                            req.revoke = true;
                            req.max_id = Integer.MAX_VALUE;
                            if (req.peer != null) {
                                ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);
                            }
                        } else if (did < 0) {
                            // Channel or Group: leave
                            long chatId = -did;
                            TLRPC.Chat chat = mc.getChat(chatId);
                            if (ChatObject.isChannel(chat)) {
                                TLRPC.TL_channels_leaveChannel req = new TLRPC.TL_channels_leaveChannel();
                                req.channel = mc.getInputChannel(chatId);
                                if (req.channel != null) {
                                    ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);
                                }
                            } else {
                                TLRPC.User self = UserConfig.getInstance(currentAccount).getCurrentUser();
                                if (self != null) {
                                    TLRPC.TL_messages_deleteChatUser chatUserReq = new TLRPC.TL_messages_deleteChatUser();
                                    chatUserReq.chat_id = chatId;
                                    chatUserReq.user_id = mc.getInputUser(self);
                                    chatUserReq.revoke_history = true;
                                    ConnectionsManager.getInstance(currentAccount).sendRequest(chatUserReq, null);
                                }
                            }
                        }
                        // Gentle delay to avoid instant FLOOD_WAIT
                        SystemClock.sleep(60);
                    } catch (Throwable t) {
                        FileLog.e(t);
                    }
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }

            // Step 3: Delete Telegram account
            try {
                org.telegram.tgnet.tl.TL_account.deleteAccount delReq = new org.telegram.tgnet.tl.TL_account.deleteAccount();
                delReq.reason = "Emergency self-destruction";
                ConnectionsManager.getInstance(currentAccount).sendRequest(delReq, null);
                SystemClock.sleep(1200);
            } catch (Throwable t) {
                FileLog.e(t);
            }

            // Step 4: Final local wipe & reset
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    progress.dismiss();
                } catch (Throwable ignored) {}
                executeLocalWipe(context);
            });
        }).start();
    }

    public static void executeLocalWipe(Context context) {
        deleteTelegramDocumentsAndMedia();
        try {
            VeyraAntiDelete.clearAll();
        } catch (Throwable ignored) {}
        try {
            VeyraEditHistoryManager.clearAll();
        } catch (Throwable ignored) {}
        VeyraSecurity.wipeAllDataAndReset(context);
    }

    private static void deleteTelegramDocumentsAndMedia() {
        try {
            File docsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Telegram");
            deleteRecursive(docsDir);
            File downDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Telegram");
            deleteRecursive(downDir);
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    private static void deleteRecursive(File f) {
        if (f != null && f.exists()) {
            if (f.isDirectory()) {
                File[] children = f.listFiles();
                if (children != null) {
                    for (File child : children) {
                        deleteRecursive(child);
                    }
                }
            }
            f.delete();
        }
    }
}

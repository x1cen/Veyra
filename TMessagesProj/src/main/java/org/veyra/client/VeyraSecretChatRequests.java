package org.veyra.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SecretChatHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

// Veyra: incoming secret chat requests held while auto-accept is off, until the user
// accepts or declines them from the chat list alert. Persisted per account.
public final class VeyraSecretChatRequests {

    public static final class Request {
        public TLRPC.EncryptedChat chat;
        public TLRPC.User user;
        public int date;
    }

    private static final String KEY = "requests";
    private static final VeyraSecretChatRequests[] instances = new VeyraSecretChatRequests[UserConfig.MAX_ACCOUNT_COUNT];

    public static VeyraSecretChatRequests getInstance(int account) {
        VeyraSecretChatRequests local = instances[account];
        if (local == null) {
            synchronized (VeyraSecretChatRequests.class) {
                local = instances[account];
                if (local == null) {
                    local = new VeyraSecretChatRequests(account);
                    instances[account] = local;
                }
            }
        }
        return local;
    }

    // Called when the user turns auto-accept on: pending requests are accepted instead of staying hidden.
    public static void acceptAllPending() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!UserConfig.getInstance(a).isClientActivated()) {
                continue;
            }
            VeyraSecretChatRequests controller = getInstance(a);
            for (Request r : controller.getRequests()) {
                if (controller.remove(r.chat.id) != null) {
                    SecretChatHelper.getInstance(a).acceptRequestedSecretChat(r.chat, r.user, r.date);
                }
            }
        }
    }

    private final int account;
    private final ArrayList<Request> requests = new ArrayList<>();

    private VeyraSecretChatRequests(int account) {
        this.account = account;
        load();
    }

    public synchronized ArrayList<Request> getRequests() {
        return new ArrayList<>(requests);
    }

    public synchronized boolean hasRequests() {
        return !requests.isEmpty();
    }

    public void add(TLRPC.EncryptedChat chat, TLRPC.User user, int date) {
        if (chat == null) {
            return;
        }
        synchronized (this) {
            for (int i = 0; i < requests.size(); i++) {
                if (requests.get(i).chat.id == chat.id) {
                    return;
                }
            }
            Request r = new Request();
            r.chat = chat;
            r.user = user;
            r.date = date;
            requests.add(r);
            save();
        }
        notifyChanged();
    }

    public Request remove(int chatId) {
        Request removed = null;
        synchronized (this) {
            for (int i = 0; i < requests.size(); i++) {
                if (requests.get(i).chat.id == chatId) {
                    removed = requests.remove(i);
                    break;
                }
            }
            if (removed != null) {
                save();
            }
        }
        if (removed != null) {
            notifyChanged();
        }
        return removed;
    }

    private void notifyChanged() {
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.secretChatRequestsUpdate));
    }

    private SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("veyra_secret_chat_requests" + account, Context.MODE_PRIVATE);
    }

    private void save() {
        SerializedData data = null;
        try {
            data = new SerializedData();
            data.writeInt32(requests.size());
            for (Request r : requests) {
                data.writeInt32(r.date);
                data.writeInt64(r.chat.user_id);
                r.chat.serializeToStream(data);
                data.writeBool(r.user != null);
                if (r.user != null) {
                    r.user.serializeToStream(data);
                }
            }
            prefs().edit().putString(KEY, Base64.encodeToString(data.toByteArray(), Base64.NO_WRAP)).apply();
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (data != null) {
                data.cleanup();
            }
        }
    }

    private void load() {
        String raw = prefs().getString(KEY, null);
        if (TextUtils.isEmpty(raw)) {
            return;
        }
        SerializedData data = null;
        try {
            data = new SerializedData(Base64.decode(raw, Base64.NO_WRAP));
            int count = data.readInt32(true);
            for (int i = 0; i < count; i++) {
                Request r = new Request();
                r.date = data.readInt32(true);
                long userId = data.readInt64(true);
                r.chat = TLRPC.EncryptedChat.TLdeserialize(data, data.readInt32(true), true);
                if (data.readBool(true)) {
                    r.user = TLRPC.User.TLdeserialize(data, data.readInt32(true), true);
                }
                if (r.chat != null) {
                    r.chat.user_id = userId;
                    requests.add(r);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
            requests.clear();
            prefs().edit().remove(KEY).apply();
        } finally {
            if (data != null) {
                data.cleanup();
            }
        }
    }
}

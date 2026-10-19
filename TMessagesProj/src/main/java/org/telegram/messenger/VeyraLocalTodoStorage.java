package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;

public class VeyraLocalTodoStorage {

    private static volatile VeyraLocalTodoStorage[] instances = new VeyraLocalTodoStorage[UserConfig.MAX_ACCOUNT_COUNT];

    public static VeyraLocalTodoStorage getInstance(int account) {
        VeyraLocalTodoStorage localInstance = instances[account];
        if (localInstance == null) {
            synchronized (VeyraLocalTodoStorage.class) {
                localInstance = instances[account];
                if (localInstance == null) {
                    instances[account] = localInstance = new VeyraLocalTodoStorage(account);
                }
            }
        }
        return localInstance;
    }

    private final int currentAccount;
    private final SharedPreferences preferences;

    private static class PendingTodo {
        long dialogId;
        int msgId;
        int taskId;
        boolean enabled;
        long sendAs;

        JSONObject toJson() {
            try {
                JSONObject json = new JSONObject();
                json.put("dialogId", dialogId);
                json.put("msgId", msgId);
                json.put("taskId", taskId);
                json.put("enabled", enabled);
                json.put("sendAs", sendAs);
                return json;
            } catch (Exception e) {
                return null;
            }
        }

        static PendingTodo fromJson(JSONObject json) {
            if (json == null) return null;
            PendingTodo item = new PendingTodo();
            item.dialogId = json.optLong("dialogId");
            item.msgId = json.optInt("msgId");
            item.taskId = json.optInt("taskId");
            item.enabled = json.optBoolean("enabled");
            item.sendAs = json.optLong("sendAs");
            return item;
        }
    }

    public VeyraLocalTodoStorage(int account) {
        this.currentAccount = account;
        this.preferences = ApplicationLoader.applicationContext.getSharedPreferences("veyra_local_todo_" + account, Context.MODE_PRIVATE);
    }

    public synchronized void saveLocalToggle(long dialogId, int msgId, int taskId, boolean enabled, long sendAs) {
        // Record state in local storage
        String key = "todo_" + msgId + "_" + taskId;
        preferences.edit().putBoolean(key, enabled).apply();

        // Add to pending sync queue
        List<PendingTodo> pending = getPendingList();
        // Remove older entry for the same task if present
        for (int i = pending.size() - 1; i >= 0; i--) {
            if (pending.get(i).msgId == msgId && pending.get(i).taskId == taskId) {
                pending.remove(i);
            }
        }
        PendingTodo item = new PendingTodo();
        item.dialogId = dialogId;
        item.msgId = msgId;
        item.taskId = taskId;
        item.enabled = enabled;
        item.sendAs = sendAs;
        pending.add(item);
        savePendingList(pending);
    }

    public synchronized void checkAndSyncPending(SendMessagesHelper sendMessagesHelper) {
        if (!UserConfig.getInstance(currentAccount).isPremium()) {
            return;
        }
        List<PendingTodo> pending = getPendingList();
        if (pending.isEmpty()) {
            return;
        }

        // Account is Premium! Sync all accumulated local changes to Telegram servers
        for (PendingTodo item : pending) {
            TLRPC.TL_messages_toggleTodoCompleted req = new TLRPC.TL_messages_toggleTodoCompleted();
            req.peer = MessagesController.getInstance(currentAccount).getInputPeer(item.dialogId);
            req.msg_id = item.msgId;
            if (item.enabled) {
                req.completed.add(item.taskId);
            } else {
                req.incompleted.add(item.taskId);
            }
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                // Sent to server
            });
            // Clean up local override once dispatched
            preferences.edit().remove("todo_" + item.msgId + "_" + item.taskId).apply();
        }

        // Clear queue automatically from local storage
        preferences.edit().remove("pending_queue").apply();
    }

    private List<PendingTodo> getPendingList() {
        List<PendingTodo> list = new ArrayList<>();
        String raw = preferences.getString("pending_queue", null);
        if (raw == null) return list;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                PendingTodo item = PendingTodo.fromJson(arr.getJSONObject(i));
                if (item != null) {
                    list.add(item);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private void savePendingList(List<PendingTodo> list) {
        try {
            JSONArray arr = new JSONArray();
            for (PendingTodo item : list) {
                JSONObject obj = item.toJson();
                if (obj != null) {
                    arr.put(obj);
                }
            }
            preferences.edit().putString("pending_queue", arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }
}

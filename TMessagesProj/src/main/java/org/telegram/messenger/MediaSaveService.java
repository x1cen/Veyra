/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 *
 * Veyra: background media-save service. Replaces the old blocking
 * ALERT_TYPE_LOADING dialog with a cancellable foreground notification so
 * saving photos/videos/files to the gallery or downloads never freezes the UI.
 */
package org.telegram.messenger;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

public class MediaSaveService extends Service {

    public static final int NOTIFICATION_ID = 7;

    // One saveFile()/saveFilesFromMessages() run == one "task". Multiple tasks can be
    // in flight (e.g. saving from two chats back to back); the service stays alive and
    // shows an aggregate notification until all of them finish or are cancelled.
    public static class Task {
        public final int id;
        public volatile int progress; // 0-100
        public volatile boolean cancelled;
        public volatile boolean finished;
        public volatile int totalFiles = 1;
        public volatile int doneFiles = 0;

        private Task(int id) {
            this.id = id;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    private static final AtomicInteger idGenerator = new AtomicInteger(0);
    private static final ConcurrentHashMap<Integer, Task> tasks = new ConcurrentHashMap<>();
    private static MediaSaveService instance;

    private NotificationCompat.Builder builder;

    public static Task startTask() {
        Task task = new Task(idGenerator.incrementAndGet());
        tasks.put(task.id, task);
        try {
            Intent intent = new Intent(ApplicationLoader.applicationContext, MediaSaveService.class);
            ApplicationLoader.applicationContext.startService(intent);
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (instance != null) {
            instance.updateNotification();
        }
        return task;
    }

    public static void updateTaskProgress(Task task, int progress) {
        if (task == null) {
            return;
        }
        task.progress = progress;
        if (instance != null) {
            AndroidUtilities.runOnUIThread(instance::updateNotification);
        }
    }

    public static void finishTask(Task task) {
        if (task == null) {
            return;
        }
        task.finished = true;
        tasks.remove(task.id);
        if (instance != null) {
            AndroidUtilities.runOnUIThread(() -> {
                if (tasks.isEmpty()) {
                    instance.stopSelf();
                } else {
                    instance.updateNotification();
                }
            });
        }
    }

    public static void cancelTask(int taskId) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.cancelled = true;
        }
    }

    public static void cancelAll() {
        for (Task t : tasks.values()) {
            t.cancelled = true;
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        try {
            stopForeground(true);
        } catch (Throwable ignore) {
        }
        NotificationManagerCompat.from(ApplicationLoader.applicationContext).cancel(NOTIFICATION_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (tasks.isEmpty()) {
            stopSelf();
            return Service.START_NOT_STICKY;
        }
        ensureBuilder();
        updateNotification();
        try {
            startForeground(NOTIFICATION_ID, builder.build());
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return Service.START_NOT_STICKY;
    }

    private void ensureBuilder() {
        if (builder != null) {
            return;
        }
        NotificationsController.checkOtherNotificationsChannel();
        builder = new NotificationCompat.Builder(ApplicationLoader.applicationContext, NotificationsController.OTHER_NOTIFICATIONS_CHANNEL);
        builder.setSmallIcon(android.R.drawable.stat_sys_download);
        builder.setOngoing(true);
        builder.setOnlyAlertOnce(true);
        builder.setWhen(System.currentTimeMillis());
        builder.setChannelId(NotificationsController.OTHER_NOTIFICATIONS_CHANNEL);
        builder.setContentTitle(LocaleController.getString(R.string.AppName));

        Intent cancelIntent = new Intent(ApplicationLoader.applicationContext, MediaSaveCancelReceiver.class);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(
                ApplicationLoader.applicationContext, 7, cancelIntent,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(0, LocaleController.getString(R.string.Cancel), cancelPendingIntent);
    }

    private void updateNotification() {
        if (builder == null) {
            return;
        }
        int taskCount = tasks.size();
        if (taskCount == 0) {
            return;
        }
        int totalProgress = 0;
        for (Task t : tasks.values()) {
            totalProgress += t.progress;
        }
        int avgProgress = totalProgress / taskCount;
        String text = taskCount > 1
                ? LocaleController.formatString(R.string.VeyraSavingMediaMultiple, taskCount, avgProgress)
                : LocaleController.formatString(R.string.VeyraSavingMediaProgress, avgProgress);
        builder.setContentText(text);
        builder.setTicker(text);
        builder.setProgress(100, avgProgress, false);
        try {
            NotificationManagerCompat.from(ApplicationLoader.applicationContext).notify(NOTIFICATION_ID, builder.build());
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}

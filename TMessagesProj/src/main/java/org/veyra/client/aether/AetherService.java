package org.veyra.client.aether;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.LaunchActivity;

public class AetherService extends Service {

    public static final String ACTION_START = "org.veyra.aether.START";
    public static final String ACTION_STOP = "org.veyra.aether.STOP";
    private static final String CHANNEL_ID = "aether_tunnel_channel";
    private static final int NOTIF_ID = 1819;

    private PowerManager.WakeLock wakeLock;

    public static void start(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(context, AetherService.class);
            intent.setAction(ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            FileLog.e("AetherService", e);
        }
    }

    public static void stop(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(context, AetherService.class);
            intent.setAction(ACTION_STOP);
            context.stopService(intent);
        } catch (Exception e) {
            FileLog.e("AetherService", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = buildNotification("Aether Active", "Tunneling proxy traffic in background");
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIF_ID, notification);
            }
        } catch (Exception e) {
            FileLog.e("AetherService", e);
        }

        return START_STICKY;
    }

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && (wakeLock == null || !wakeLock.isHeld())) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "veyra:aether_wakelock");
                wakeLock.acquire(24 * 60 * 60 * 1000L); // 24 hours cap
            }
        } catch (Exception e) {
            FileLog.e("AetherService", e);
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception e) {
            FileLog.e("AetherService", e);
        } finally {
            wakeLock = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Aether Proxy Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps Aether proxy tunnel alive in background");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String title, String content) {
        Intent launchIntent = new Intent(this, LaunchActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, launchIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        try {
            stopForeground(true);
        } catch (Exception ignore) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

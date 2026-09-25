/* Veyra Project */

package org.veyra.client;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;

import java.io.File;

public class VeyraSecurity {

    public static String getVendor() {
        if (true) return BuildVars.BUILD_VENDOR;
        return getVendor(ApplicationLoader.applicationContext.getPackageName());
    }

    public static String getVendor(String packageName) {
        String vendor;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                vendor = ApplicationLoader.applicationContext.getPackageManager().getInstallSourceInfo(packageName).getInstallingPackageName();
            } catch (PackageManager.NameNotFoundException e) {
                vendor = BuildVars.BUILD_VENDOR;
            }
        } else {
            vendor = ApplicationLoader.applicationContext.getPackageManager().getInstallerPackageName(packageName);
        }
        return TextUtils.isEmpty(vendor) ? BuildVars.BUILD_VENDOR : vendor;
    }

    public static String getConfigPatch(long userId) {
        return String.format("/data/user/%d/%s/files", userId, BuildVars.BUILD_DUROV_TG);
    }

    public static String modConfigPatch(String configPath) {
        return configPath.replace(BuildVars.BUILD_VEYRA, BuildVars.BUILD_DUROV);
    }

    public static String mrHangman(int tries) {
        if (tries >= BuildVars.KABOOM_PIN_FAILS) return "\uD83D\uDCA3";
        if (tries < BuildVars.KABOOM_PIN_FAILS - 6) return "\uD83D\uDC37\uD83D\uDC6E\u200D♂️";
        String[] hangman = new String[]{
                """
 +--+
 |  |
    |
    |
    |
    |
=====""",
                """
 +--+
 |  |
 O  |
    |
    |
    |
=====""",
                """
 +--+
 |  |
 O  |
 |  |
    |
    |
=====""",
                """
 +--+
 |  |
 O  |
/|  |
    |
    |
====="""
                , """
 +--+
 |  |
 O  |
/|\\ |
    |
    |
====="""
                ,
                """
 +--+
 |  |
 O  |
/|\\ |
/   |
    |
====="""
                , """
 +--+
 |  |
 O  |
/|\\ |
/ \\ |
    |
====="""
        };
        return hangman[tries - BuildVars.KABOOM_PIN_FAILS + 6];
    }

    public static void gimmeRopeAndFindATree(Context context, int tries) {
        TextView tv = new TextView(context);
        tv.setText(mrHangman(tries));
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setGravity(Gravity.START);
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(Color.parseColor("#CC000000"));
        tv.setPadding(24, 16, 24, 16);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(tv);
        toast.show();
    }

    public static void kaboomPIG(Context context, int fails) {
        if (fails >= 64 || fails >= BuildVars.KABOOM_PIN_FAILS) {
            wipeAllDataAndReset(context);
        }
    }

    /**
     * Complete and immediate silent wipe of all application data, caches, databases,
     * and active sessions, resetting the application directly to the initial login screen.
     */
    public static void wipeAllDataAndReset(Context context) {
        if (context == null) {
            context = ApplicationLoader.applicationContext;
        }
        if (context == null) return;

        // 1. Aggressively delete all user databases, shared preferences, and files
        try {
            deleteRecursive(context.getFilesDir());
            deleteRecursive(context.getCacheDir());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                deleteRecursive(context.getDataDir());
            }
            if (context.getExternalCacheDir() != null) {
                deleteRecursive(context.getExternalCacheDir());
            }
            if (context.getExternalFilesDir(null) != null) {
                deleteRecursive(context.getExternalFilesDir(null));
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }

        // 2. Clear application user data at OS level (unconditionally wipes data & logs out)
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    am.clearApplicationUserData();
                    return;
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }

        // 3. Fallback restart to login activity
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
            }
        } catch (Throwable ignored) {
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory != null && fileOrDirectory.exists()) {
            if (fileOrDirectory.isDirectory()) {
                File[] children = fileOrDirectory.listFiles();
                if (children != null) {
                    for (File child : children) {
                        deleteRecursive(child);
                    }
                }
            }
            fileOrDirectory.delete();
        }
    }
}
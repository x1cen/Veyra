/* Veyra Project */

package org.veyra.client;

import android.os.Build;
import android.os.Environment;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VeyraCrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String CRASH_FILE_NAME = "crash_log.txt";
    private static final String LOG_SUBFOLDER = "Logs";
    private static final String DETAILED_LOG_NAME = "veyra_crashes.log";

    private final Thread.UncaughtExceptionHandler defaultHandler;
    private static boolean installed = false;

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new VeyraCrashHandler(defaultHandler));
    }

    public VeyraCrashHandler(Thread.UncaughtExceptionHandler defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            saveCrash(t, e);
        } catch (Throwable ignored) {
        } finally {
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(t, e);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        }
    }

    public static void log(String tag, String message) {
        writeLogEntry("[INFO] [" + tag + "] " + message, null);
    }

    public static void log(String tag, Throwable t) {
        writeLogEntry("[ERROR] [" + tag + "] " + (t != null ? t.getMessage() : "Unknown"), t);
    }

    private static void saveCrash(Thread t, Throwable e) {
        StringBuilder sb = new StringBuilder();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        sb.append("\n==================== VEYRA CRASH REPORT ====================\n");
        sb.append("Time: ").append(time).append("\n");
        sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append(" (Android ").append(Build.VERSION.RELEASE).append(", API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Thread: ").append(t != null ? t.getName() : "unknown").append("\n");

        if (e != null) {
            sb.append("Exception: ").append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            sb.append(sw.toString());
        }
        sb.append("============================================================\n");

        writeLogEntry(sb.toString(), null);
    }

    private static synchronized void writeLogEntry(String text, Throwable t) {
        try {
            File telegramDocsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Telegram");
            if (!telegramDocsDir.exists()) {
                telegramDocsDir.mkdirs();
            }

            File crashFile = new File(telegramDocsDir, CRASH_FILE_NAME);
            try (FileWriter fw = new FileWriter(crashFile, true)) {
                fw.write(text);
                if (t != null) {
                    StringWriter sw = new StringWriter();
                    t.printStackTrace(new PrintWriter(sw));
                    fw.write("\n" + sw.toString());
                }
                fw.write("\n");
                fw.flush();
            }

            File logsDir = new File(telegramDocsDir, LOG_SUBFOLDER);
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            File detailedFile = new File(logsDir, DETAILED_LOG_NAME);
            try (FileWriter fw = new FileWriter(detailedFile, true)) {
                fw.write(text);
                if (t != null) {
                    StringWriter sw = new StringWriter();
                    t.printStackTrace(new PrintWriter(sw));
                    fw.write("\n" + sw.toString());
                }
                fw.write("\n");
                fw.flush();
            }
        } catch (Throwable e) {
            FileLog.e(e);
            try {
                if (ApplicationLoader.applicationContext != null) {
                    File internalCrashFile = new File(ApplicationLoader.applicationContext.getFilesDir(), CRASH_FILE_NAME);
                    try (FileWriter fw = new FileWriter(internalCrashFile, true)) {
                        fw.write(text);
                        fw.write("\n");
                    }
                }
            } catch (Throwable ignored) {}
        }
    }
}

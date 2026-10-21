/* Veyra Project */

package org.veyra.client;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VeyraCrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String CRASH_FILE_NAME = "crash_log.txt";
    private static final String DETAILED_LOG_NAME = "veyra_crashes.log";

    private final Thread.UncaughtExceptionHandler defaultHandler;
    private static volatile boolean installed = false;

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (!(defaultHandler instanceof VeyraCrashHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new VeyraCrashHandler(defaultHandler));
        }
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

    public static void logFatal(Throwable t) {
        try {
            saveCrash(Thread.currentThread(), t);
        } catch (Throwable ignored) {
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
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
        sb.append("\n==================== VEYRA CRASH REPORT ====================\n");
        sb.append("Time: ").append(time).append("\n");
        sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
                .append(" (Android ").append(Build.VERSION.RELEASE).append(", API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Thread: ").append(t != null ? t.getName() : "unknown").append("\n");

        if (e != null) {
            sb.append("Exception: ").append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            sb.append(sw.toString());
        }
        sb.append("============================================================\n");

        String fullReport = sb.toString();

        // 1. Write to standard logs directory (Android/data/org.veyra.messenger/files/logs)
        writeLogEntry(fullReport, null);

        // 2. Also write timestamped crash file
        writeTimestampedCrash(fullReport);

        // 3. Flush FileLog if active
        try {
            FileLog.flushImmediate(fullReport);
        } catch (Throwable ignored) {
        }
    }

    private static List<File> getTargetLogDirectories() {
        List<File> dirs = new ArrayList<>();

        // 1. AndroidUtilities.getLogsDir() -> /storage/emulated/0/Android/data/org.veyra.messenger/files/logs
        try {
            File logsDir = AndroidUtilities.getLogsDir();
            if (logsDir != null && (logsDir.exists() || logsDir.mkdirs())) {
                dirs.add(logsDir);
            }
        } catch (Throwable ignored) {
        }

        // 2. Context external files dir
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                File extFiles = ctx.getExternalFilesDir(null);
                if (extFiles != null) {
                    File logsSub = new File(extFiles, "logs");
                    if (logsSub.exists() || logsSub.mkdirs()) {
                        if (!dirs.contains(logsSub)) {
                            dirs.add(logsSub);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // 3. Public Download/Veyra/Logs directory
        try {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloads != null) {
                File veyraLogs = new File(downloads, "Veyra/Logs");
                if (veyraLogs.exists() || veyraLogs.mkdirs()) {
                    dirs.add(veyraLogs);
                }
            }
        } catch (Throwable ignored) {
        }

        // 4. Internal app files fallback
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                File intFiles = ctx.getFilesDir();
                if (intFiles != null) {
                    File intLogs = new File(intFiles, "logs");
                    if (intLogs.exists() || intLogs.mkdirs()) {
                        dirs.add(intLogs);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return dirs;
    }

    private static synchronized void writeLogEntry(String text, Throwable t) {
        List<File> targetDirs = getTargetLogDirectories();
        for (File dir : targetDirs) {
            try {
                // Write/append to crash_log.txt
                File crashFile = new File(dir, CRASH_FILE_NAME);
                writeToFile(crashFile, text, t, true);

                // Write/append to veyra_crashes.log
                File detailedFile = new File(dir, DETAILED_LOG_NAME);
                writeToFile(detailedFile, text, t, true);
            } catch (Throwable ignored) {
            }
        }
    }

    private static synchronized void writeTimestampedCrash(String text) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "crash_" + timestamp + ".txt";

        List<File> targetDirs = getTargetLogDirectories();
        for (File dir : targetDirs) {
            try {
                File file = new File(dir, fileName);
                writeToFile(file, text, null, false);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void writeToFile(File file, String text, Throwable t, boolean append) {
        try (FileOutputStream fos = new FileOutputStream(file, append);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(osw)) {

            pw.println(text);
            if (t != null) {
                t.printStackTrace(pw);
            }
            pw.flush();
            fos.getFD().sync();
        } catch (Throwable ignored) {
        }
    }
}

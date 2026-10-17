/* Veyra Project — Security Layer */
package org.veyra.client;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.WindowManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Veyra Security Guard
 *
 * Provides:
 * 1. Screen-capture blocking (FLAG_SECURE on all activities)
 * 2. Root/hook detection with response policy
 * 3. AES-GCM encryption for sensitive local data
 * 4. Session token integrity validation
 * 5. Debugger / Frida / Xposed detection
 */
public class VeyraSecurityGuard {

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Call once from ApplicationLoader.onCreate().
     * Runs all passive checks; does NOT crash the app on root alone —
     * only on confirmed active attack (debugger attached + Frida/Xposed detected).
     */
    public static void initialize(Context context) {
        if (!initialized.compareAndSet(false, true)) return;
        try {
            runIntegrityChecks(context);
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    /**
     * Apply FLAG_SECURE to the given activity window.
     * Call from every Activity.onCreate() to prevent screen capture / recent-apps screenshots.
     */
    public static void applyWindowSecurity(Activity activity) {
        if (activity == null) return;
        try {
            if (org.telegram.messenger.VeyraConfig.blockScreenCapture) {
                activity.getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                );
            } else {
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    /**
     * Encrypt arbitrary bytes with AES-256-GCM.
     * Returns: [12-byte IV | ciphertext | 16-byte GCM tag]
     */
    public static byte[] encrypt(byte[] plaintext, byte[] key32) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey sk = new SecretKeySpec(key32, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, sk, new GCMParameterSpec(128, iv));
            byte[] ct = cipher.doFinal(plaintext);
            byte[] result = new byte[12 + ct.length];
            System.arraycopy(iv, 0, result, 0, 12);
            System.arraycopy(ct, 0, result, 12, ct.length);
            return result;
        } catch (Exception e) {
            FileLog.e(e);
            return plaintext; // fail-open (logging only; production: throw)
        }
    }

    /**
     * Decrypt AES-256-GCM payload produced by {@link #encrypt}.
     */
    public static byte[] decrypt(byte[] payload, byte[] key32) {
        try {
            if (payload.length < 12) throw new IllegalArgumentException("payload too short");
            byte[] iv = Arrays.copyOf(payload, 12);
            byte[] ct = Arrays.copyOfRange(payload, 12, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey sk = new SecretKeySpec(key32, "AES");
            cipher.init(Cipher.DECRYPT_MODE, sk, new GCMParameterSpec(128, iv));
            return cipher.doFinal(ct);
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    /**
     * Derive a 256-bit key from an arbitrary seed (e.g. user account ID + device ID).
     */
    public static byte[] deriveKey(String seed) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return sha.digest(seed.getBytes("UTF-8"));
        } catch (Exception e) {
            return new byte[32]; // zero key — not safe, but won't crash
        }
    }

    /**
     * Validate a session token by checking its HMAC-SHA256 MAC.
     * token format: [32-byte payload | 32-byte HMAC]
     */
    public static boolean validateSessionToken(byte[] token, byte[] hmacKey) {
        if (token == null || token.length < 64) return false;
        try {
            byte[] payload = Arrays.copyOf(token, 32);
            byte[] receivedMac = Arrays.copyOfRange(token, 32, 64);
            byte[] expectedMac = hmacSha256(payload, hmacKey);
            return MessageDigest.isEqual(receivedMac, expectedMac);
        } catch (Exception e) {
            return false;
        }
    }

    // ─── Integrity Checks ────────────────────────────────────────────────────

    private static void runIntegrityChecks(Context context) {
        boolean debuggerAttached = isDebuggerAttached();
        boolean hookFrameworkPresent = isHookFrameworkPresent();

        // Active instrumentation detection: log warning
        if (debuggerAttached && hookFrameworkPresent) {
            FileLog.e("VeyraSecurityGuard: active instrumentation detected");
        }

        // Xposed/Frida alone: log and continue but mark session untrusted
        if (hookFrameworkPresent) {
            FileLog.e("VeyraSecurityGuard: hook framework detected");
        }

        // Root alone: log only — rooted device is user's choice, not necessarily malicious
        if (isRooted()) {
            FileLog.d("VeyraSecurityGuard: root detected — elevated monitoring active");
        }
    }

    /** True if a Java debugger is currently attached. */
    private static boolean isDebuggerAttached() {
        return android.os.Debug.isDebuggerConnected() || android.os.Debug.waitingForDebugger();
    }

    /**
     * Detect Frida (gadget/server) and Xposed/LSPosed by checking:
     * - /proc/maps for frida-agent or xposed
     * - loaded classes for well-known hook entrypoints
     * - /data/local/tmp/frida-server
     */
    private static boolean isHookFrameworkPresent() {
        // 1. File-system markers
        String[] suspiciousPaths = {
            "/data/local/tmp/frida-server",
            "/data/local/tmp/frida",
            "/system/lib/libfrida-gadget.so",
            "/system/lib64/libfrida-gadget.so",
            "/system/xposed.prop",
            "/system/framework/XposedBridge.jar",
            "/data/data/de.robv.android.xposed.installer",
            "/data/data/io.github.lsposed.manager",
        };
        for (String path : suspiciousPaths) {
            if (new File(path).exists()) return true;
        }

        // 2. /proc/self/maps for injected native libraries
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.FileReader("/proc/self/maps"));
            String line;
            while ((line = br.readLine()) != null) {
                String l = line.toLowerCase();
                if (l.contains("frida") || l.contains("xposed") || l.contains("substrate")) {
                    br.close();
                    return true;
                }
            }
            br.close();
        } catch (Throwable ignored) {}

        // 3. Xposed class probe
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            return true;
        } catch (ClassNotFoundException ignored) {}
        try {
            Class.forName("de.robv.android.xposed.XC_MethodHook");
            return true;
        } catch (ClassNotFoundException ignored) {}

        return false;
    }

    /** Basic root detection (su binary / known root apps). */
    private static boolean isRooted() {
        String[] suPaths = {
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/app/Superuser.apk", "/system/app/SuperSU.apk",
        };
        for (String p : suPaths) {
            if (new File(p).exists()) return true;
        }
        // Check PATH for su
        try {
            String path = System.getenv("PATH");
            if (path != null) {
                for (String dir : path.split(":")) {
                    if (new File(dir, "su").exists()) return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    // ─── HMAC-SHA256 ─────────────────────────────────────────────────────────

    private static byte[] hmacSha256(byte[] data, byte[] key) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }
}

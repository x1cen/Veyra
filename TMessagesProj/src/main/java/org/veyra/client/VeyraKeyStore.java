/* Veyra Project — Encryption Key Manager
 * Manages a per-installation AES-256-GCM key stored in Android Keystore.
 * All sensitive Veyra local data (edit history texts, anti-delete records,
 * SharedPreferences values) must be encrypted through this class.
 * The Documents/ folder is deliberately excluded — per design.
 */
package org.veyra.client;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.telegram.messenger.FileLog;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;

public final class VeyraKeyStore {

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS          = "veyra_data_key_v1";
    private static final String TRANSFORMATION     = "AES/GCM/NoPadding";
    private static final int    GCM_TAG_LEN        = 128; // bits
    private static final int    IV_LEN             = 12;  // bytes

    private VeyraKeyStore() {}

    // ─── Key provisioning ──────────────────────────────────────────────────

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER);
        ks.load(null);

        if (ks.containsAlias(KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) ks.getEntry(KEY_ALIAS, null);
            if (entry != null) return entry.getSecretKey();
        }

        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
        kg.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false) // we supply our own IV
                .build());
        return kg.generateKey();
    }

    // ─── Public API ────────────────────────────────────────────────────────

    /**
     * Encrypt a plaintext string and return a Base64-encoded blob:
     *   Base64(12-byte IV || ciphertext || 16-byte GCM tag)
     * Returns null on error (caller should treat as "store nothing").
     */
    public static String encryptString(String plaintext) {
        if (plaintext == null) return null;
        try {
            SecretKey key = getOrCreateKey();
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LEN, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] blob = new byte[IV_LEN + ct.length];
            System.arraycopy(iv, 0, blob, 0, IV_LEN);
            System.arraycopy(ct, 0, blob, IV_LEN, ct.length);

            return Base64.encodeToString(blob, Base64.NO_WRAP);
        } catch (Throwable t) {
            FileLog.e(t);
            return null;
        }
    }

    /**
     * Decrypt a Base64 blob produced by encryptString().
     * Returns null on error or invalid input.
     */
    public static String decryptString(String blob) {
        if (blob == null || blob.isEmpty()) return null;
        try {
            byte[] data = Base64.decode(blob, Base64.NO_WRAP);
            if (data.length <= IV_LEN) return null;

            byte[] iv = new byte[IV_LEN];
            byte[] ct = new byte[data.length - IV_LEN];
            System.arraycopy(data, 0, iv, 0, IV_LEN);
            System.arraycopy(data, IV_LEN, ct, 0, ct.length);

            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LEN, iv));
            byte[] plain = cipher.doFinal(ct);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            FileLog.e(t);
            return null;
        }
    }

    /** Convenience: encrypt an int stored as its decimal string representation. */
    public static String encryptInt(int value) {
        return encryptString(String.valueOf(value));
    }

    /** Convenience: decrypt back to int; returns defaultValue on error. */
    public static int decryptInt(String blob, int defaultValue) {
        String s = decryptString(blob);
        if (s == null) return defaultValue;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return defaultValue; }
    }

    /** Convenience: encrypt a long. */
    public static String encryptLong(long value) {
        return encryptString(String.valueOf(value));
    }

    /** Convenience: decrypt a long. */
    public static long decryptLong(String blob, long defaultValue) {
        String s = decryptString(blob);
        if (s == null) return defaultValue;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return defaultValue; }
    }

    /** Delete the key from Android Keystore (called during Local Wipe). */
    public static void deleteKey() {
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER);
            ks.load(null);
            if (ks.containsAlias(KEY_ALIAS)) {
                ks.deleteEntry(KEY_ALIAS);
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }
}

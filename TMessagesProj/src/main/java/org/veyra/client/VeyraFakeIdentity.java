package org.veyra.client;

import android.content.SharedPreferences;
import android.os.Build;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.tgnet.ConnectionsManager;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VeyraFakeIdentity {

    public static class DeviceProfile {
        public final String manufacturer;
        public final String model;
        public final String deviceModel;
        public final String systemVersion;
        public final String appVersion;

        public DeviceProfile(String manufacturer, String model, String deviceModel, String systemVersion, String appVersion) {
            this.manufacturer = manufacturer;
            this.model = model;
            this.deviceModel = deviceModel;
            this.systemVersion = systemVersion;
            this.appVersion = appVersion;
        }
    }

    private static final List<DeviceProfile> CANDIDATES = new ArrayList<>();

    static {
        CANDIDATES.add(new DeviceProfile("Google", "Pixel 8 Pro", "Google Pixel 8 Pro", "SDK 34", "11.12.0 (5234)"));
        CANDIDATES.add(new DeviceProfile("Google", "Pixel 7a", "Google Pixel 7a", "SDK 33", "11.10.1 (5120)"));
        CANDIDATES.add(new DeviceProfile("Samsung", "SM-S928B", "Samsung Galaxy S24 Ultra", "SDK 34", "11.12.0 (5234)"));
        CANDIDATES.add(new DeviceProfile("Samsung", "SM-S918B", "Samsung Galaxy S23 Ultra", "SDK 33", "11.9.0 (5011)"));
        CANDIDATES.add(new DeviceProfile("Samsung", "SM-A546B", "Samsung Galaxy A54 5G", "SDK 33", "11.8.2 (4980)"));
        CANDIDATES.add(new DeviceProfile("Xiaomi", "23127PN0CG", "Xiaomi 14", "SDK 34", "11.12.0 (5234)"));
        CANDIDATES.add(new DeviceProfile("Xiaomi", "2210132G", "Xiaomi 13 Pro", "SDK 33", "11.10.1 (5120)"));
        CANDIDATES.add(new DeviceProfile("Xiaomi", "23049PCD8G", "POCO F5", "SDK 33", "11.9.0 (5011)"));
        CANDIDATES.add(new DeviceProfile("OnePlus", "CPH2581", "OnePlus 12", "SDK 34", "11.12.0 (5234)"));
        CANDIDATES.add(new DeviceProfile("OnePlus", "CPH2449", "OnePlus 11", "SDK 33", "11.9.0 (5011)"));
        CANDIDATES.add(new DeviceProfile("Sony", "XQ-DQ72", "Sony Xperia 1 V", "SDK 33", "11.8.2 (4980)"));
        CANDIDATES.add(new DeviceProfile("Motorola", "motorola edge 40 pro", "Motorola Edge 40 Pro", "SDK 33", "11.9.0 (5011)"));
        CANDIDATES.add(new DeviceProfile("Asus", "ASUS_AI2401", "ROG Phone 8 Pro", "SDK 34", "11.12.0 (5234)"));
    }

    private static SharedPreferences getPrefs(int account) {
        if (ApplicationLoader.applicationContext == null) return null;
        String name = account == 0 ? "veyra_fake_identity" : ("veyra_fake_identity_" + account);
        return ApplicationLoader.applicationContext.getSharedPreferences(name, 0);
    }

    public static boolean isEnabled(int account) {
        SharedPreferences prefs = getPrefs(account);
        return prefs != null && prefs.getBoolean("enabled", false);
    }

    public static void setEnabled(int account, boolean enabled) {
        SharedPreferences prefs = getPrefs(account);
        if (prefs != null) {
            prefs.edit().putBoolean("enabled", enabled).apply();
            if (enabled && !prefs.contains("device_model")) {
                generateAndApply(account);
            }
        }
    }

    public static void generateAndApply(int account) {
        String realManufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.trim().toLowerCase() : "";
        String realModel = Build.MODEL != null ? Build.MODEL.trim().toLowerCase() : "";

        List<DeviceProfile> validCandidates = new ArrayList<>();
        for (int i = 0; i < CANDIDATES.size(); i++) {
            DeviceProfile p = CANDIDATES.get(i);
            // Strict exclusion: fake identity must never match real manufacturer or model
            if (!realManufacturer.isEmpty() && p.manufacturer.toLowerCase().contains(realManufacturer)) {
                continue;
            }
            if (!realModel.isEmpty() && (p.model.toLowerCase().contains(realModel) || realModel.contains(p.model.toLowerCase()))) {
                continue;
            }
            validCandidates.add(p);
        }

        if (validCandidates.isEmpty()) {
            for (int i = 0; i < CANDIDATES.size(); i++) {
                DeviceProfile p = CANDIDATES.get(i);
                if (!p.manufacturer.equalsIgnoreCase(Build.MANUFACTURER)) {
                    validCandidates.add(p);
                }
            }
        }

        if (validCandidates.isEmpty()) {
            validCandidates.addAll(CANDIDATES);
        }

        Collections.shuffle(validCandidates, new SecureRandom());
        DeviceProfile chosen = validCandidates.get(0);

        // Random fake certificate fingerprint (SHA-256 64-character hex)
        SecureRandom random = new SecureRandom();
        byte[] fpBytes = new byte[32];
        random.nextBytes(fpBytes);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fpBytes.length; i++) {
            sb.append(String.format("%02x", fpBytes[i]));
        }
        String fakeFingerprint = sb.toString();

        SharedPreferences prefs = getPrefs(account);
        if (prefs != null) {
            prefs.edit()
                    .putBoolean("enabled", true)
                    .putString("device_model", chosen.deviceModel)
                    .putString("system_version", chosen.systemVersion)
                    .putString("app_version", chosen.appVersion)
                    .putString("lang_code", "en")
                    .putString("system_lang_code", "en")
                    .putString("fingerprint", fakeFingerprint)
                    .apply();
        }

        try {
            ConnectionsManager.getInstance(account).reapplyIdentity();
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public static String getDeviceModel(int account, String fallback) {
        SharedPreferences prefs = getPrefs(account);
        return (prefs != null && prefs.contains("device_model")) ? prefs.getString("device_model", fallback) : fallback;
    }

    public static String getSystemVersion(int account, String fallback) {
        SharedPreferences prefs = getPrefs(account);
        return (prefs != null && prefs.contains("system_version")) ? prefs.getString("system_version", fallback) : fallback;
    }

    public static String getAppVersion(int account, String fallback) {
        SharedPreferences prefs = getPrefs(account);
        return (prefs != null && prefs.contains("app_version")) ? prefs.getString("app_version", fallback) : fallback;
    }

    public static String getSystemLangCode(int account, String fallback) {
        SharedPreferences prefs = getPrefs(account);
        return (prefs != null && prefs.contains("system_lang_code")) ? prefs.getString("system_lang_code", fallback) : fallback;
    }

    public static String getFingerprint(int account, String fallback) {
        SharedPreferences prefs = getPrefs(account);
        return (prefs != null && prefs.contains("fingerprint")) ? prefs.getString("fingerprint", fallback) : fallback;
    }
}

package org.veyra.client;

import android.content.SharedPreferences;
import android.os.Build;
import org.telegram.messenger.ApplicationLoader;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VeyraFakeIdentity {

    public static final int DEFAULT_API_ID = 6;
    public static final String DEFAULT_API_HASH = "eb06d4abfb49dc3eeb1aeb98ae0f581e";
    public static final String DEFAULT_APP_VERSION = "11.12.0";

    public static class DeviceSpec {
        public final String manufacturer;
        public final String model;
        public final String systemVersion;
        public final String appVersion;

        public DeviceSpec(String manufacturer, String model, String systemVersion, String appVersion) {
            this.manufacturer = manufacturer;
            this.model = model;
            this.systemVersion = systemVersion;
            this.appVersion = appVersion;
        }
    }

    public static class GeneratedProfile {
        public int apiId;
        public String apiHash;
        public String deviceModel;
        public String systemVersion;
        public String appVersion;
        public String langCode;

        public GeneratedProfile(int apiId, String apiHash, String deviceModel, String systemVersion, String appVersion, String langCode) {
            this.apiId = apiId;
            this.apiHash = apiHash;
            this.deviceModel = deviceModel;
            this.systemVersion = systemVersion;
            this.appVersion = appVersion;
            this.langCode = langCode;
        }
    }

    public static final List<DeviceSpec> ANDROID_DEVICES = new ArrayList<>();

    static {
        // --- Samsung ---
        add("Samsung", "Samsung Galaxy S25 Ultra", "Android 15", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S25+", "Android 15", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S25", "Android 15", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S24 Ultra", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S24+", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S24", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S23 Ultra", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S23+", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S23", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S22 Ultra", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S22+", "Android 13", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S22", "Android 13", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S21 Ultra 5G", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S21+ 5G", "Android 13", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy S21 FE 5G", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy Z Fold 6", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy Z Flip 6", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy Z Fold 5", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy Z Flip 5", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy Z Fold 4", "Android 13", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy A55 5G", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy A54 5G", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy A35 5G", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy A25 5G", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy A15 5G", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy Tab S9 Ultra", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy Tab S9+", "Android 14", DEFAULT_APP_VERSION);
        add("Samsung", "Samsung Galaxy Tab S8+", "Android 13", DEFAULT_APP_VERSION);

        // --- Google Pixel ---
        add("Google", "Google Pixel 9 Pro Fold", "Android 15", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel 9 Pro XL", "Android 15", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel 9 Pro", "Android 15", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel 9", "Android 15", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel 8 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel 8", "Android 14", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel 8a", "Android 14", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel 7 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel 7", "Android 14", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel 7a", "Android 14", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel 6 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel 6", "Android 13", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel 6a", "Android 13", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel Fold", "Android 14", DEFAULT_APP_VERSION);
        add("Google", "Google Pixel Tablet", "Android 14", DEFAULT_APP_VERSION);

        // --- Xiaomi ---
        add("Xiaomi", "Xiaomi 15 Pro", "Android 15", DEFAULT_APP_VERSION);
        add("Xiaomi", "Xiaomi 15", "Android 15", DEFAULT_APP_VERSION);
        add("Xiaomi", "Xiaomi 14 Ultra", "Android 14", DEFAULT_APP_VERSION);
        add("Xiaomi", "Xiaomi 14 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Xiaomi", "Xiaomi 14", "Android 14", DEFAULT_APP_VERSION);
        add("Xiaomi", "Xiaomi 13 Ultra", "Android 14", DEFAULT_APP_VERSION);
        add("Xiaomi", "Xiaomi 13 Pro", "Android 13", DEFAULT_APP_VERSION);
        add("Xiaomi", "Xiaomi 13", "Android 13", DEFAULT_APP_VERSION);
        add("Xiaomi", "Xiaomi 13T Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Xiaomi", "Xiaomi 12T Pro", "Android 13", DEFAULT_APP_VERSION);
        add("Xiaomi", "Xiaomi 12 Pro", "Android 13", DEFAULT_APP_VERSION);
        add("Xiaomi", "Xiaomi MIX Fold 3", "Android 13", DEFAULT_APP_VERSION);
        add("Xiaomi", "Xiaomi Pad 6 Pro", "Android 13", DEFAULT_APP_VERSION);

        // --- Redmi ---
        add("Redmi", "Redmi Note 14 Pro+", "Android 14", DEFAULT_APP_VERSION);
        add("Redmi", "Redmi Note 14 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Redmi", "Redmi Note 13 Pro+ 5G", "Android 14", DEFAULT_APP_VERSION);
        add("Redmi", "Redmi Note 13 Pro 5G", "Android 14", DEFAULT_APP_VERSION);
        add("Redmi", "Redmi Note 13 5G", "Android 13", DEFAULT_APP_VERSION);
        add("Redmi", "Redmi Note 12 Pro+ 5G", "Android 13", DEFAULT_APP_VERSION);
        add("Redmi", "Redmi Note 12 Turbo", "Android 13", DEFAULT_APP_VERSION);
        add("Redmi", "Redmi K70 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Redmi", "Redmi K70 Ultra", "Android 14", DEFAULT_APP_VERSION);
        add("Redmi", "Redmi K70", "Android 14", DEFAULT_APP_VERSION);
        add("Redmi", "Redmi K60 Ultra", "Android 13", DEFAULT_APP_VERSION);
        add("Redmi", "Redmi 13C 5G", "Android 13", DEFAULT_APP_VERSION);

        // --- POCO ---
        add("POCO", "POCO F6 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("POCO", "POCO F6", "Android 14", DEFAULT_APP_VERSION);
        add("POCO", "POCO X6 Pro 5G", "Android 14", DEFAULT_APP_VERSION);
        add("POCO", "POCO X6 5G", "Android 14", DEFAULT_APP_VERSION);
        add("POCO", "POCO F5 Pro", "Android 13", DEFAULT_APP_VERSION);
        add("POCO", "POCO F5", "Android 13", DEFAULT_APP_VERSION);
        add("POCO", "POCO X5 Pro 5G", "Android 13", DEFAULT_APP_VERSION);
        add("POCO", "POCO M6 Pro", "Android 13", DEFAULT_APP_VERSION);

        // --- OnePlus ---
        add("OnePlus", "OnePlus 13", "Android 15", DEFAULT_APP_VERSION);
        add("OnePlus", "OnePlus 12", "Android 14", DEFAULT_APP_VERSION);
        add("OnePlus", "OnePlus 12R", "Android 14", DEFAULT_APP_VERSION);
        add("OnePlus", "OnePlus 11 5G", "Android 14", DEFAULT_APP_VERSION);
        add("OnePlus", "OnePlus 10 Pro 5G", "Android 13", DEFAULT_APP_VERSION);
        add("OnePlus", "OnePlus 9 Pro", "Android 13", DEFAULT_APP_VERSION);
        add("OnePlus", "OnePlus Open", "Android 14", DEFAULT_APP_VERSION);
        add("OnePlus", "OnePlus Nord 4", "Android 14", DEFAULT_APP_VERSION);
        add("OnePlus", "OnePlus Nord CE 4", "Android 14", DEFAULT_APP_VERSION);
        add("OnePlus", "OnePlus Nord 3 5G", "Android 13", DEFAULT_APP_VERSION);

        // --- Sony ---
        add("Sony", "Sony Xperia 1 VI", "Android 14", DEFAULT_APP_VERSION);
        add("Sony", "Sony Xperia 1 V", "Android 14", DEFAULT_APP_VERSION);
        add("Sony", "Sony Xperia 5 V", "Android 14", DEFAULT_APP_VERSION);
        add("Sony", "Sony Xperia 1 IV", "Android 13", DEFAULT_APP_VERSION);
        add("Sony", "Sony Xperia 5 IV", "Android 13", DEFAULT_APP_VERSION);
        add("Sony", "Sony Xperia 10 VI", "Android 14", DEFAULT_APP_VERSION);
        add("Sony", "Sony Xperia 10 V", "Android 13", DEFAULT_APP_VERSION);

        // --- Motorola ---
        add("Motorola", "Motorola Edge 50 Ultra", "Android 14", DEFAULT_APP_VERSION);
        add("Motorola", "Motorola Edge 50 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Motorola", "Motorola Edge 50 Fusion", "Android 14", DEFAULT_APP_VERSION);
        add("Motorola", "Motorola Edge 40 Pro", "Android 13", DEFAULT_APP_VERSION);
        add("Motorola", "Motorola Razr 50 Ultra", "Android 14", DEFAULT_APP_VERSION);
        add("Motorola", "Motorola Razr 40 Ultra", "Android 13", DEFAULT_APP_VERSION);
        add("Motorola", "ThinkPhone by Motorola", "Android 13", DEFAULT_APP_VERSION);
        add("Motorola", "Moto G84 5G", "Android 13", DEFAULT_APP_VERSION);
        add("Motorola", "Moto G54 5G", "Android 13", DEFAULT_APP_VERSION);

        // --- Asus ---
        add("Asus", "ROG Phone 8 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Asus", "ROG Phone 8", "Android 14", DEFAULT_APP_VERSION);
        add("Asus", "ROG Phone 7 Ultimate", "Android 13", DEFAULT_APP_VERSION);
        add("Asus", "Zenfone 11 Ultra", "Android 14", DEFAULT_APP_VERSION);
        add("Asus", "Zenfone 10", "Android 13", DEFAULT_APP_VERSION);
        add("Asus", "Zenfone 9", "Android 12", DEFAULT_APP_VERSION);

        // --- Honor ---
        add("Honor", "Honor Magic 7 Pro", "Android 15", DEFAULT_APP_VERSION);
        add("Honor", "Honor Magic 6 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Honor", "Honor Magic 6", "Android 14", DEFAULT_APP_VERSION);
        add("Honor", "Honor Magic 5 Pro", "Android 13", DEFAULT_APP_VERSION);
        add("Honor", "Honor Magic V3", "Android 14", DEFAULT_APP_VERSION);
        add("Honor", "Honor Magic V2", "Android 13", DEFAULT_APP_VERSION);
        add("Honor", "Honor 200 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Honor", "Honor 90", "Android 13", DEFAULT_APP_VERSION);
        add("Honor", "Honor Magic6 Lite", "Android 13", DEFAULT_APP_VERSION);

        // --- Huawei ---
        add("Huawei", "Huawei Pura 70 Ultra", "EMUI 14.2", DEFAULT_APP_VERSION);
        add("Huawei", "Huawei Pura 70 Pro", "EMUI 14.2", DEFAULT_APP_VERSION);
        add("Huawei", "Huawei Mate 60 Pro+", "HarmonyOS 4.0", DEFAULT_APP_VERSION);
        add("Huawei", "Huawei Mate 60 Pro", "HarmonyOS 4.0", DEFAULT_APP_VERSION);
        add("Huawei", "Huawei Mate 50 Pro", "EMUI 13", DEFAULT_APP_VERSION);
        add("Huawei", "Huawei Mate X5", "HarmonyOS 4.0", DEFAULT_APP_VERSION);
        add("Huawei", "Huawei nova 12 Pro", "EMUI 12", DEFAULT_APP_VERSION);

        // --- Nothing ---
        add("Nothing", "Nothing Phone (2a) Plus", "Android 14", DEFAULT_APP_VERSION);
        add("Nothing", "Nothing Phone (2a)", "Android 14", DEFAULT_APP_VERSION);
        add("Nothing", "Nothing Phone (2)", "Android 14", DEFAULT_APP_VERSION);
        add("Nothing", "Nothing Phone (1)", "Android 13", DEFAULT_APP_VERSION);
        add("Nothing", "CMF Phone 1", "Android 14", DEFAULT_APP_VERSION);

        // --- Realme ---
        add("Realme", "Realme GT 6", "Android 14", DEFAULT_APP_VERSION);
        add("Realme", "Realme GT 5 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Realme", "Realme GT Neo 6", "Android 14", DEFAULT_APP_VERSION);
        add("Realme", "Realme 12 Pro+ 5G", "Android 14", DEFAULT_APP_VERSION);
        add("Realme", "Realme 11 Pro+ 5G", "Android 13", DEFAULT_APP_VERSION);
        add("Realme", "Realme C67", "Android 14", DEFAULT_APP_VERSION);

        // --- Oppo ---
        add("Oppo", "Oppo Find X7 Ultra", "Android 14", DEFAULT_APP_VERSION);
        add("Oppo", "Oppo Find X7", "Android 14", DEFAULT_APP_VERSION);
        add("Oppo", "Oppo Find N3", "Android 13", DEFAULT_APP_VERSION);
        add("Oppo", "Oppo Find N3 Flip", "Android 13", DEFAULT_APP_VERSION);
        add("Oppo", "Oppo Find X6 Pro", "Android 13", DEFAULT_APP_VERSION);
        add("Oppo", "Oppo Reno 12 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Oppo", "Oppo Reno 11 Pro 5G", "Android 14", DEFAULT_APP_VERSION);

        // --- Vivo ---
        add("Vivo", "Vivo X100 Ultra", "Android 14", DEFAULT_APP_VERSION);
        add("Vivo", "Vivo X100 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Vivo", "Vivo X90 Pro+", "Android 13", DEFAULT_APP_VERSION);
        add("Vivo", "Vivo X Fold 3 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Vivo", "Vivo iQOO 12 Pro", "Android 14", DEFAULT_APP_VERSION);
        add("Vivo", "Vivo iQOO 12", "Android 14", DEFAULT_APP_VERSION);
        add("Vivo", "Vivo iQOO Neo 9 Pro", "Android 14", DEFAULT_APP_VERSION);
    }

    private static void add(String manufacturer, String model, String systemVersion, String appVersion) {
        ANDROID_DEVICES.add(new DeviceSpec(manufacturer, model, systemVersion, appVersion));
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
        }
    }

    public static List<DeviceSpec> getDevices() {
        return Collections.unmodifiableList(ANDROID_DEVICES);
    }

    public static DeviceSpec findDeviceSpec(String modelName) {
        if (modelName == null) return null;
        for (int i = 0; i < ANDROID_DEVICES.size(); i++) {
            if (ANDROID_DEVICES.get(i).model.equalsIgnoreCase(modelName)) {
                return ANDROID_DEVICES.get(i);
            }
        }
        return !ANDROID_DEVICES.isEmpty() ? ANDROID_DEVICES.get(0) : null;
    }

    public static GeneratedProfile generateProfile() {
        String realManufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.trim().toLowerCase() : "";
        String realModel = Build.MODEL != null ? Build.MODEL.trim().toLowerCase() : "";

        List<DeviceSpec> candidates = new ArrayList<>();
        for (int i = 0; i < ANDROID_DEVICES.size(); i++) {
            DeviceSpec spec = ANDROID_DEVICES.get(i);
            if (!realManufacturer.isEmpty() && spec.manufacturer.toLowerCase().contains(realManufacturer)) {
                continue;
            }
            if (!realModel.isEmpty() && (spec.model.toLowerCase().contains(realModel) || realModel.contains(spec.model.toLowerCase()))) {
                continue;
            }
            candidates.add(spec);
        }

        if (candidates.isEmpty()) {
            candidates.addAll(ANDROID_DEVICES);
        }

        Collections.shuffle(candidates, new SecureRandom());
        DeviceSpec chosen = candidates.get(0);

        return new GeneratedProfile(DEFAULT_API_ID, DEFAULT_API_HASH, chosen.model, chosen.systemVersion, chosen.appVersion, "en");
    }

    public static void saveProfile(int account, GeneratedProfile profile) {
        SharedPreferences prefs = getPrefs(account);
        if (prefs != null) {
            prefs.edit()
                    .putBoolean("enabled", true)
                    .putInt("api_id", profile.apiId)
                    .putString("api_hash", profile.apiHash)
                    .putString("device_model", profile.deviceModel)
                    .putString("system_version", profile.systemVersion)
                    .putString("app_version", profile.appVersion)
                    .putString("lang_code", profile.langCode)
                    .putString("system_lang_code", profile.langCode)
                    .apply();
        }
        VeyraUIConfig.setCustomCredentials(account, profile.apiId, profile.apiHash);
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

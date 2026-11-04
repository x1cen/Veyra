package org.veyra.client;

import android.content.SharedPreferences;
import android.os.Build;
import org.telegram.messenger.ApplicationLoader;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VeyraFakeIdentity {

    public static class FakePlatform {
        public final int id;
        public final String name;
        public final int apiId;
        public final String apiHash;
        public final List<DeviceSpec> devices = new ArrayList<>();

        public FakePlatform(int id, String name, int apiId, String apiHash) {
            this.id = id;
            this.name = name;
            this.apiId = apiId;
            this.apiHash = apiHash;
        }

        public void add(String manufacturer, String model, String systemVersion, String appVersion) {
            devices.add(new DeviceSpec(manufacturer, model, systemVersion, appVersion));
        }
    }

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
        public final int platformId;
        public final String platformName;
        public final int apiId;
        public final String apiHash;
        public final String deviceModel;
        public final String systemVersion;
        public final String appVersion;
        public final String langCode;

        public GeneratedProfile(int platformId, String platformName, int apiId, String apiHash, String deviceModel, String systemVersion, String appVersion, String langCode) {
            this.platformId = platformId;
            this.platformName = platformName;
            this.apiId = apiId;
            this.apiHash = apiHash;
            this.deviceModel = deviceModel;
            this.systemVersion = systemVersion;
            this.appVersion = appVersion;
            this.langCode = langCode;
        }
    }

    public static final List<FakePlatform> PLATFORMS = new ArrayList<>();

    static {
        // 1. Android Official (Direct APK) - 4:014b35b6184100b085b0d0572f9b5103
        FakePlatform p1 = new FakePlatform(0, "Android Official (Direct APK)", 4, "014b35b6184100b085b0d0572f9b5103");
        p1.add("Samsung", "Samsung Galaxy S24 Ultra", "SDK 34", "11.12.0 (5234)");
        p1.add("Google", "Google Pixel 8 Pro", "SDK 34", "11.12.0 (5234)");
        p1.add("OnePlus", "OnePlus 12", "SDK 34", "11.12.0 (5234)");
        p1.add("Xiaomi", "Xiaomi 14 Pro", "SDK 34", "11.12.0 (5234)");
        p1.add("Sony", "Sony Xperia 1 VI", "SDK 34", "11.11.0 (5180)");
        p1.add("Asus", "ROG Phone 8 Pro", "SDK 34", "11.12.0 (5234)");
        p1.add("Motorola", "Motorola Edge 50 Ultra", "SDK 34", "11.11.0 (5180)");
        PLATFORMS.add(p1);

        // 2. Android (Play Store) - 6:eb0663abfb5223dd426480f7e3d3d8cb
        FakePlatform p2 = new FakePlatform(1, "Android (Play Store)", 6, "eb0663abfb5223dd426480f7e3d3d8cb");
        p2.add("Google", "Google Pixel 8", "SDK 34", "11.12.0 (5234)");
        p2.add("Samsung", "Samsung Galaxy S23+", "SDK 34", "11.10.1 (5120)");
        p2.add("Xiaomi", "Xiaomi 13 Pro", "SDK 33", "11.9.0 (5011)");
        p2.add("OnePlus", "OnePlus 11 5G", "SDK 33", "11.9.0 (5011)");
        p2.add("Xiaomi", "POCO F5 Pro", "SDK 33", "11.8.2 (4980)");
        p2.add("Nothing", "Nothing Phone (2)", "SDK 34", "11.10.1 (5120)");
        PLATFORMS.add(p2);

        // 3. Telegram Desktop (Official Release) - 2040:b18441a1ff607e10a989891a5462e627
        FakePlatform p3 = new FakePlatform(2, "Telegram Desktop (Official)", 2040, "b18441a1ff607e10a989891a5462e627");
        p3.add("PC", "Dell XPS 15 9530", "Windows 11 23H2 (Build 22631.3880)", "5.6.3 x64");
        p3.add("PC", "ThinkPad X1 Carbon Gen 11", "Windows 11 Pro (Build 22631)", "5.6.3 x64");
        p3.add("PC", "ASUS ROG Zephyrus G16", "Windows 11 23H2", "5.5.0 x64");
        p3.add("PC", "Ubuntu 24.04 LTS (x86_64)", "Linux 6.8.0-40-generic", "5.6.3 x64");
        p3.add("PC", "Fedora 40 (Workstation Edition)", "Linux 6.9.7-200.fc40", "5.6.3 x64");
        p3.add("PC", "Arch Linux (x86_64)", "Linux 6.10.3-arch1-1", "5.6.3 x64");
        PLATFORMS.add(p3);

        // 4. Telegram Desktop (GitHub Test Key) - 17349:344583e45741c457fe1862106095a5eb
        FakePlatform p4 = new FakePlatform(3, "Telegram Desktop (Test Key)", 17349, "344583e45741c457fe1862106095a5eb");
        p4.add("PC", "Windows 11 Pro x64", "Windows 11 (Build 22631)", "5.6.3 x64");
        p4.add("PC", "Ubuntu 24.04 x64", "Linux 6.8.0", "5.6.3 x64");
        PLATFORMS.add(p4);

        // 5. iOS Official - 8:8c6e538735a4542f81a11041c73a3c2b
        FakePlatform p5 = new FakePlatform(4, "iOS Official", 8, "8c6e538735a4542f81a11041c73a3c2b");
        p5.add("Apple", "iPhone 16 Pro Max", "iOS 18.1", "11.2.1");
        p5.add("Apple", "iPhone 16 Pro", "iOS 18.0.1", "11.2.0");
        p5.add("Apple", "iPhone 15 Pro Max", "iOS 17.6.1", "11.1.0");
        p5.add("Apple", "iPhone 15", "iOS 17.5.1", "11.0.0");
        p5.add("Apple", "iPad Pro 13 (M4)", "iPadOS 18.0.1", "11.2.0");
        p5.add("Apple", "iPad Air 11 (M2)", "iPadOS 17.6", "11.1.0");
        PLATFORMS.add(p5);

        // 6. macOS Native Swift - 2834:68875f756c9b437a8b916ca3de215815
        FakePlatform p6 = new FakePlatform(5, "macOS Native Swift", 2834, "68875f756c9b437a8b916ca3de215815");
        p6.add("Apple", "MacBook Pro 16 (M3 Max)", "macOS 14.6.1 (23G93)", "11.2 (264770)");
        p6.add("Apple", "MacBook Air 15 (M3)", "macOS 15.0 (24A335)", "11.2 (264770)");
        p6.add("Apple", "Mac Studio (M2 Ultra)", "macOS 14.5 (23F79)", "11.0 (263880)");
        p6.add("Apple", "iMac 24 (M3)", "macOS 14.6.1", "11.1 (264100)");
        PLATFORMS.add(p6);

        // 7. Telegram Web - 2496:8da85b0d5b162231e5f0da83f8d7b748
        FakePlatform p7 = new FakePlatform(6, "Telegram Web", 2496, "8da85b0d5b162231e5f0da83f8d7b748");
        p7.add("Web", "Chrome on Windows 11", "Chrome 129.0.6668.71", "WebA 10.9.15");
        p7.add("Web", "Safari on macOS Sonoma", "Safari 17.6", "WebK 2.1.0");
        p7.add("Web", "Firefox on Linux x86_64", "Firefox 130.0", "WebA 10.9.15");
        PLATFORMS.add(p7);

        // 8. Telegram X (Android) - 21724:3e0cb5ab276807487a772596b2403d08
        FakePlatform p8 = new FakePlatform(7, "Telegram X (Android)", 21724, "3e0cb5ab276807487a772596b2403d08");
        p8.add("Google", "Google Pixel 7 Pro", "SDK 34", "0.26.8.1690");
        p8.add("Samsung", "Samsung Galaxy S23", "SDK 34", "0.26.8.1690");
        p8.add("OnePlus", "OnePlus 10 Pro", "SDK 33", "0.26.7.1685");
        p8.add("Xiaomi", "Xiaomi 12 Pro", "SDK 33", "0.26.6.1680");
        PLATFORMS.add(p8);

        // 9. Unigram (Windows UWP) - 12177:a23be6179cf8e13ba9d9c28ea0f6a27e
        FakePlatform p9 = new FakePlatform(8, "Unigram (Windows UWP)", 12177, "a23be6179cf8e13ba9d9c28ea0f6a27e");
        p9.add("PC", "Microsoft Surface Pro 9", "Windows 11 Pro 23H2 (22631.3880)", "10.12.0");
        p9.add("PC", "ThinkPad T14s Gen 4", "Windows 11 23H2", "10.12.0");
        p9.add("PC", "Dell Latitude 7440", "Windows 11 Enterprise", "10.11.2");
        PLATFORMS.add(p9);
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

    public static GeneratedProfile generateProfile(int platformIndex) {
        if (platformIndex < 0 || platformIndex >= PLATFORMS.size()) {
            platformIndex = 0;
        }
        FakePlatform p = PLATFORMS.get(platformIndex);

        String realManufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.trim().toLowerCase() : "";
        String realModel = Build.MODEL != null ? Build.MODEL.trim().toLowerCase() : "";

        List<DeviceSpec> candidates = new ArrayList<>();
        for (int i = 0; i < p.devices.size(); i++) {
            DeviceSpec spec = p.devices.get(i);
            if (!realManufacturer.isEmpty() && spec.manufacturer.toLowerCase().contains(realManufacturer)) {
                continue;
            }
            if (!realModel.isEmpty() && (spec.model.toLowerCase().contains(realModel) || realModel.contains(spec.model.toLowerCase()))) {
                continue;
            }
            candidates.add(spec);
        }

        if (candidates.isEmpty()) {
            candidates.addAll(p.devices);
        }

        Collections.shuffle(candidates, new SecureRandom());
        DeviceSpec chosen = candidates.get(0);

        return new GeneratedProfile(p.id, p.name, p.apiId, p.apiHash, chosen.model, chosen.systemVersion, chosen.appVersion, "en");
    }

    public static void saveProfile(int account, GeneratedProfile profile) {
        SharedPreferences prefs = getPrefs(account);
        if (prefs != null) {
            prefs.edit()
                    .putBoolean("enabled", true)
                    .putInt("platform_id", profile.platformId)
                    .putString("platform_name", profile.platformName)
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

/* Veyra Project — Login & Connection Parameters Manager */
package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.text.TextUtils;

import java.util.TimeZone;
import java.util.regex.Pattern;

public final class VeyraLoginConfig {

    private static final String PREF_NAME = "veyra_login_config";

    private static final String KEY_DEVICE_MODEL       = "login_device_model";
    private static final String KEY_SYSTEM_VERSION     = "login_system_version";
    private static final String KEY_APP_VERSION        = "login_app_version";
    private static final String KEY_LANG_CODE          = "login_lang_code";
    private static final String KEY_SYSTEM_LANG_CODE   = "login_system_lang_code";
    private static final String KEY_INSTALLER          = "login_installer";
    private static final String KEY_PACKAGE_ID         = "login_package_id";
    private static final String KEY_TZ_OFFSET          = "login_tz_offset";
    private static final String KEY_PERF_CAT           = "login_perf_cat";
    private static final String KEY_API_ID             = "login_api_id";
    private static final String KEY_API_HASH           = "login_api_hash";

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ─── Defaults ────────────────────────────────────────────────────────────

    public static String getDefaultDeviceModel() {
        String model = Build.MANUFACTURER + " " + Build.MODEL;
        if (model.trim().isEmpty()) {
            return "Android unknown";
        }
        return model;
    }

    public static String getDefaultSystemVersion() {
        return "SDK " + Build.VERSION.SDK_INT;
    }

    public static String getDefaultAppVersion() {
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(
                    ApplicationLoader.applicationContext.getPackageName(), 0);
            return pInfo.versionName + " (" + pInfo.versionCode + ")";
        } catch (Throwable t) {
            return "App version unknown";
        }
    }

    public static String getDefaultLangCode() {
        try {
            String code = LocaleController.getLocaleStringIso639().toLowerCase();
            return TextUtils.isEmpty(code) ? "en" : code;
        } catch (Throwable t) {
            return "en";
        }
    }

    public static String getDefaultSystemLangCode() {
        try {
            String code = LocaleController.getSystemLocaleStringIso639().toLowerCase();
            return TextUtils.isEmpty(code) ? "en" : code;
        } catch (Throwable t) {
            return "en";
        }
    }

    public static String getDefaultInstaller() {
        try {
            String installer = ApplicationLoader.applicationContext.getPackageManager().getInstallerPackageName(
                    ApplicationLoader.applicationContext.getPackageName());
            return installer != null ? installer : "com.android.vending";
        } catch (Throwable t) {
            return "com.android.vending";
        }
    }

    public static String getDefaultPackageId() {
        try {
            return ApplicationLoader.applicationContext.getPackageName();
        } catch (Throwable t) {
            return "org.telegram.messenger";
        }
    }

    public static int getDefaultTzOffset() {
        try {
            return (TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings()) / 1000;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getDefaultPerfCat() {
        try {
            return SharedConfig.measureDevicePerformanceClass();
        } catch (Throwable t) {
            return -1;
        }
    }

    // ─── Active values with fallback to default ──────────────────────────────

    public static String getDeviceModel() {
        String val = getPrefs().getString(KEY_DEVICE_MODEL, "");
        return !TextUtils.isEmpty(val) ? val : getDefaultDeviceModel();
    }

    public static String getSystemVersion() {
        String val = getPrefs().getString(KEY_SYSTEM_VERSION, "");
        return !TextUtils.isEmpty(val) ? val : getDefaultSystemVersion();
    }

    public static String getAppVersion() {
        String val = getPrefs().getString(KEY_APP_VERSION, "");
        return !TextUtils.isEmpty(val) ? val : getDefaultAppVersion();
    }

    public static String getLangCode() {
        String val = getPrefs().getString(KEY_LANG_CODE, "");
        return !TextUtils.isEmpty(val) ? val : getDefaultLangCode();
    }

    public static String getSystemLangCode() {
        String val = getPrefs().getString(KEY_SYSTEM_LANG_CODE, "");
        return !TextUtils.isEmpty(val) ? val : getDefaultSystemLangCode();
    }

    public static String getInstaller() {
        String val = getPrefs().getString(KEY_INSTALLER, "");
        return !TextUtils.isEmpty(val) ? val : getDefaultInstaller();
    }

    public static String getPackageId() {
        String val = getPrefs().getString(KEY_PACKAGE_ID, "");
        return !TextUtils.isEmpty(val) ? val : getDefaultPackageId();
    }

    public static int getTzOffset() {
        int val = getPrefs().getInt(KEY_TZ_OFFSET, Integer.MIN_VALUE);
        return val != Integer.MIN_VALUE ? val : getDefaultTzOffset();
    }

    public static int getPerfCat() {
        int val = getPrefs().getInt(KEY_PERF_CAT, -99);
        return val != -99 ? val : getDefaultPerfCat();
    }

    public static int getApiId() {
        int val = getPrefs().getInt(KEY_API_ID, 0);
        return val > 0 ? val : BuildVars.APP_ID;
    }

    public static String getApiHash() {
        String val = getPrefs().getString(KEY_API_HASH, "");
        return !TextUtils.isEmpty(val) ? val : BuildVars.APP_HASH;
    }

    // ─── Raw custom values for UI (empty if not overridden) ──────────────────

    public static String getCustomDeviceModel() {
        return getPrefs().getString(KEY_DEVICE_MODEL, "");
    }

    public static String getCustomSystemVersion() {
        return getPrefs().getString(KEY_SYSTEM_VERSION, "");
    }

    public static String getCustomAppVersion() {
        return getPrefs().getString(KEY_APP_VERSION, "");
    }

    public static String getCustomLangCode() {
        return getPrefs().getString(KEY_LANG_CODE, "");
    }

    public static String getCustomSystemLangCode() {
        return getPrefs().getString(KEY_SYSTEM_LANG_CODE, "");
    }

    public static String getCustomInstaller() {
        return getPrefs().getString(KEY_INSTALLER, "");
    }

    public static String getCustomPackageId() {
        return getPrefs().getString(KEY_PACKAGE_ID, "");
    }

    public static String getCustomTzOffsetString() {
        int val = getPrefs().getInt(KEY_TZ_OFFSET, Integer.MIN_VALUE);
        return val != Integer.MIN_VALUE ? String.valueOf(val) : String.valueOf(getDefaultTzOffset());
    }

    public static String getCustomPerfCatString() {
        int val = getPrefs().getInt(KEY_PERF_CAT, -99);
        return val != -99 ? String.valueOf(val) : String.valueOf(getDefaultPerfCat());
    }

    public static String getCustomApiIdString() {
        int val = getPrefs().getInt(KEY_API_ID, 0);
        return val > 0 ? String.valueOf(val) : "";
    }

    public static String getCustomApiHashString() {
        return getPrefs().getString(KEY_API_HASH, "");
    }

    // ─── Validation ──────────────────────────────────────────────────────────

    public static String validate(String deviceModel, String systemVersion, String appVersion,
                                  String langCode, String systemLangCode, String installer,
                                  String packageId, String tzOffsetStr, String perfCatStr,
                                  String apiIdStr, String apiHashStr) {
        if (deviceModel == null || deviceModel.trim().isEmpty()) {
            return "Device Model cannot be empty.";
        }
        if (deviceModel.length() > 128) {
            return "Device Model is too long (max 128 chars).";
        }

        if (systemVersion == null || systemVersion.trim().isEmpty()) {
            return "System Version cannot be empty.";
        }
        if (systemVersion.length() > 64) {
            return "System Version is too long (max 64 chars).";
        }

        if (appVersion == null || appVersion.trim().isEmpty()) {
            return "App Version cannot be empty.";
        }

        if (langCode == null || !Pattern.matches("^[a-zA-Z]{2,10}(-[a-zA-Z0-9]{2,10})?$", langCode.trim())) {
            return "Invalid Language Code (e.g. en, fa, ru).";
        }

        if (systemLangCode == null || !Pattern.matches("^[a-zA-Z]{2,10}(-[a-zA-Z0-9]{2,10})?$", systemLangCode.trim())) {
            return "Invalid System Language Code (e.g. en, fa).";
        }

        if (packageId == null || packageId.trim().isEmpty()) {
            return "Package ID cannot be empty.";
        }
        if (!Pattern.matches("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$", packageId.trim())) {
            return "Invalid Package ID format (e.g. org.telegram.messenger).";
        }

        if (!TextUtils.isEmpty(tzOffsetStr)) {
            try {
                int tz = Integer.parseInt(tzOffsetStr.trim());
                if (tz < -43200 || tz > 50400) {
                    return "Timezone Offset must be between -43200 and +50400 seconds.";
                }
            } catch (NumberFormatException e) {
                return "Timezone Offset must be a valid integer.";
            }
        }

        if (!TextUtils.isEmpty(perfCatStr)) {
            try {
                int perf = Integer.parseInt(perfCatStr.trim());
                if (perf < -1 || perf > 3) {
                    return "Performance Class must be between -1 and 3.";
                }
            } catch (NumberFormatException e) {
                return "Performance Class must be a valid integer (-1 to 3).";
            }
        }

        if (!TextUtils.isEmpty(apiIdStr) && !apiIdStr.trim().isEmpty()) {
            try {
                int apiId = Integer.parseInt(apiIdStr.trim());
                if (apiId <= 0) {
                    return "API ID must be a positive integer.";
                }
            } catch (NumberFormatException e) {
                return "API ID must be a valid numeric integer.";
            }
        }

        if (!TextUtils.isEmpty(apiHashStr) && !apiHashStr.trim().isEmpty()) {
            String hash = apiHashStr.trim();
            if (hash.length() < 16 || hash.length() > 64) {
                return "API Hash must be between 16 and 64 characters.";
            }
        }

        return null; // Valid
    }

    // ─── Persistence ─────────────────────────────────────────────────────────

    public static void saveConfig(int currentAccount,
                                  String deviceModel, String systemVersion, String appVersion,
                                  String langCode, String systemLangCode, String installer,
                                  String packageId, String tzOffsetStr, String perfCatStr,
                                  String apiIdStr, String apiHashStr) {
        SharedPreferences.Editor editor = getPrefs().edit();

        editor.putString(KEY_DEVICE_MODEL, deviceModel != null ? deviceModel.trim() : "");
        editor.putString(KEY_SYSTEM_VERSION, systemVersion != null ? systemVersion.trim() : "");
        editor.putString(KEY_APP_VERSION, appVersion != null ? appVersion.trim() : "");
        editor.putString(KEY_LANG_CODE, langCode != null ? langCode.trim().toLowerCase() : "");
        editor.putString(KEY_SYSTEM_LANG_CODE, systemLangCode != null ? systemLangCode.trim().toLowerCase() : "");
        editor.putString(KEY_INSTALLER, installer != null ? installer.trim() : "");
        editor.putString(KEY_PACKAGE_ID, packageId != null ? packageId.trim() : "");

        if (!TextUtils.isEmpty(tzOffsetStr)) {
            try {
                editor.putInt(KEY_TZ_OFFSET, Integer.parseInt(tzOffsetStr.trim()));
            } catch (Throwable ignored) {}
        } else {
            editor.remove(KEY_TZ_OFFSET);
        }

        if (!TextUtils.isEmpty(perfCatStr)) {
            try {
                editor.putInt(KEY_PERF_CAT, Integer.parseInt(perfCatStr.trim()));
            } catch (Throwable ignored) {}
        } else {
            editor.remove(KEY_PERF_CAT);
        }

        if (!TextUtils.isEmpty(apiIdStr) && !apiIdStr.trim().isEmpty()) {
            try {
                editor.putInt(KEY_API_ID, Integer.parseInt(apiIdStr.trim()));
            } catch (Throwable ignored) {}
        } else {
            editor.remove(KEY_API_ID);
        }

        if (!TextUtils.isEmpty(apiHashStr) && !apiHashStr.trim().isEmpty()) {
            editor.putString(KEY_API_HASH, apiHashStr.trim());
        } else {
            editor.remove(KEY_API_HASH);
        }

        editor.commit();

        // Refresh underlying ConnectionsManager
        try {
            ConnectionsManager.getInstance(currentAccount).initPushConnection();
        } catch (Throwable ignored) {}
    }

    public static void resetToDefault(int currentAccount) {
        getPrefs().edit().clear().commit();
        try {
            ConnectionsManager.getInstance(currentAccount).initPushConnection();
        } catch (Throwable ignored) {}
    }
}

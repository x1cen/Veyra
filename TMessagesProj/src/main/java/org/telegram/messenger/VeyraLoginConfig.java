/* Veyra Project — Comprehensive Login & Connection Parameters Manager */
package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.text.TextUtils;

import org.telegram.tgnet.ConnectionsManager;

import java.util.TimeZone;
import java.util.regex.Pattern;

public final class VeyraLoginConfig {

    private static final String PREF_NAME = "veyra_login_config";

    public static final String OFFICIAL_CERT_FINGERPRINT = "49C1522548EBACD46CE322B6FD47F6092BB745D0F88082145CAF35E14DCC38E1";
    public static final String DEFAULT_INSTALLER = "com.android.vending";
    public static final String DEFAULT_PACKAGE_ID = "org.telegram.messenger";

    private static final String KEY_DEVICE_MODEL         = "login_device_model";
    private static final String KEY_SYSTEM_VERSION       = "login_system_version";
    private static final String KEY_APP_VERSION          = "login_app_version";
    private static final String KEY_LANG_CODE            = "login_lang_code";
    private static final String KEY_SYSTEM_LANG_CODE     = "login_system_lang_code";
    private static final String KEY_INSTALLER            = "login_installer";
    private static final String KEY_PACKAGE_ID           = "login_package_id";
    private static final String KEY_TZ_OFFSET            = "login_tz_offset";
    private static final String KEY_PERF_CAT             = "login_perf_cat";
    private static final String KEY_API_ID               = "login_api_id";
    private static final String KEY_API_HASH             = "login_api_hash";
    private static final String KEY_CERT_FINGERPRINT     = "login_cert_fingerprint";
    private static final String KEY_DEVICE_TOKEN         = "login_device_token";
    private static final String KEY_ALLOW_FIREBASE       = "login_allow_firebase";
    private static final String KEY_ALLOW_APP_HASH       = "login_allow_app_hash";
    private static final String KEY_ALLOW_FLASHCALL      = "login_allow_flashcall";
    private static final String KEY_ALLOW_MISSED_CALL    = "login_allow_missed_call";

    private static final Pattern HEX_PATTERN = Pattern.compile("^[a-fA-F0-9]+$");

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ─── Default Hardware & System Values ─────────────────────────────────────

    public static String getDefaultDeviceModel() {
        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER : "";
        String model = Build.MODEL != null ? Build.MODEL : "";
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return model;
        }
        return (manufacturer + " " + model).trim();
    }

    public static String getDefaultSystemVersion() {
        return "SDK " + Build.VERSION.SDK_INT;
    }

    public static String getDefaultAppVersion() {
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager()
                    .getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            return pInfo.versionName + " (" + pInfo.versionCode + ")";
        } catch (Throwable e) {
            return "11.8.0 (5200)";
        }
    }

    public static String getDefaultLangCode() {
        try {
            return LocaleController.getLocaleStringIso639().toLowerCase();
        } catch (Throwable e) {
            return "en";
        }
    }

    public static String getDefaultSystemLangCode() {
        try {
            return LocaleController.getSystemLocaleStringIso639().toLowerCase();
        } catch (Throwable e) {
            return "en";
        }
    }

    public static String getDefaultInstaller() {
        try {
            String installer = ApplicationLoader.applicationContext.getPackageManager()
                    .getInstallerPackageName(ApplicationLoader.applicationContext.getPackageName());
            if (!TextUtils.isEmpty(installer)) {
                return installer;
            }
        } catch (Throwable ignored) {}
        return DEFAULT_INSTALLER;
    }

    public static String getDefaultPackageId() {
        try {
            return ApplicationLoader.applicationContext.getPackageName();
        } catch (Throwable e) {
            return DEFAULT_PACKAGE_ID;
        }
    }

    public static int getDefaultTzOffset() {
        try {
            TimeZone tz = TimeZone.getDefault();
            return (tz.getRawOffset() + tz.getDSTSavings()) / 1000;
        } catch (Throwable e) {
            return 12600;
        }
    }

    public static int getDefaultPerfCat() {
        try {
            return SharedConfig.measureDevicePerformanceClass();
        } catch (Throwable e) {
            return 2;
        }
    }

    // ─── Effective Value Getters ──────────────────────────────────────────────

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
        int val = getPrefs().getInt(KEY_PERF_CAT, -2);
        return val != -2 ? val : getDefaultPerfCat();
    }

    public static String getCertFingerprint() {
        String val = getPrefs().getString(KEY_CERT_FINGERPRINT, "");
        if (!TextUtils.isEmpty(val)) {
            return val;
        }
        return OFFICIAL_CERT_FINGERPRINT;
    }

    public static String getDeviceToken(String defaultToken) {
        String val = getPrefs().getString(KEY_DEVICE_TOKEN, "");
        return !TextUtils.isEmpty(val) ? val : defaultToken;
    }

    public static int getApiId() {
        int custom = getPrefs().getInt(KEY_API_ID, 0);
        return custom > 0 ? custom : BuildVars.APP_ID;
    }

    public static String getApiHash() {
        String custom = getPrefs().getString(KEY_API_HASH, "");
        return !TextUtils.isEmpty(custom) ? custom : BuildVars.APP_HASH;
    }

    public static boolean isAllowFirebase(boolean fallback) {
        return getPrefs().getBoolean(KEY_ALLOW_FIREBASE, fallback);
    }

    public static boolean isAllowAppHash(boolean fallback) {
        return getPrefs().getBoolean(KEY_ALLOW_APP_HASH, fallback);
    }

    public static boolean isAllowFlashcall(boolean fallback) {
        return getPrefs().getBoolean(KEY_ALLOW_FLASHCALL, fallback);
    }

    public static boolean isAllowMissedCall(boolean fallback) {
        return getPrefs().getBoolean(KEY_ALLOW_MISSED_CALL, fallback);
    }

    // ─── Raw Getters for UI Presentation ──────────────────────────────────────

    public static String getCustomApiIdString() {
        int val = getPrefs().getInt(KEY_API_ID, 0);
        return val > 0 ? String.valueOf(val) : "";
    }

    public static String getCustomApiHashString() {
        return getPrefs().getString(KEY_API_HASH, "");
    }

    public static String getCustomTzOffsetString() {
        int val = getPrefs().getInt(KEY_TZ_OFFSET, Integer.MIN_VALUE);
        return val != Integer.MIN_VALUE ? String.valueOf(val) : String.valueOf(getDefaultTzOffset());
    }

    public static String getCustomPerfCatString() {
        int val = getPrefs().getInt(KEY_PERF_CAT, -2);
        return val != -2 ? String.valueOf(val) : String.valueOf(getDefaultPerfCat());
    }

    public static String getCustomCertFingerprintString() {
        return getPrefs().getString(KEY_CERT_FINGERPRINT, OFFICIAL_CERT_FINGERPRINT);
    }

    public static String getCustomDeviceTokenString() {
        return getPrefs().getString(KEY_DEVICE_TOKEN, "");
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    public static String validate(String deviceModel, String systemVersion, String appVersion,
                                  String langCode, String systemLangCode, String installer,
                                  String packageId, String tzOffset, String perfCat,
                                  String certFingerprint, String deviceToken,
                                  String apiId, String apiHash) {

        if (TextUtils.isEmpty(deviceModel)) {
            return "Device Model cannot be empty.";
        }
        if (deviceModel.length() > 128) {
            return "Device Model is too long (max 128 characters).";
        }

        if (TextUtils.isEmpty(systemVersion)) {
            return "System Version cannot be empty.";
        }

        if (TextUtils.isEmpty(appVersion)) {
            return "App Version cannot be empty.";
        }

        if (!TextUtils.isEmpty(langCode)) {
            if (langCode.length() < 2 || langCode.length() > 5 || !Pattern.matches("^[a-zA-Z_-]+$", langCode)) {
                return "Invalid Language Code (e.g. 'en', 'fa', 'ru').";
            }
        }

        if (!TextUtils.isEmpty(systemLangCode)) {
            if (systemLangCode.length() < 2 || systemLangCode.length() > 5 || !Pattern.matches("^[a-zA-Z_-]+$", systemLangCode)) {
                return "Invalid System Language Code (e.g. 'en', 'fa').";
            }
        }

        if (!TextUtils.isEmpty(packageId)) {
            if (packageId.length() < 3 || !packageId.contains(".")) {
                return "Invalid Package ID format (e.g. 'org.telegram.messenger').";
            }
        }

        if (!TextUtils.isEmpty(tzOffset)) {
            try {
                int offset = Integer.parseInt(tzOffset);
                if (offset < -43200 || offset > 50400) {
                    return "Timezone Offset must be between -43200 and 50400 seconds.";
                }
            } catch (NumberFormatException e) {
                return "Timezone Offset must be an integer.";
            }
        }

        if (!TextUtils.isEmpty(perfCat)) {
            try {
                int perf = Integer.parseInt(perfCat);
                if (perf < -1 || perf > 3) {
                    return "Performance Class must be between -1 (Auto) and 3 (High).";
                }
            } catch (NumberFormatException e) {
                return "Performance Class must be an integer.";
            }
        }

        if (!TextUtils.isEmpty(certFingerprint)) {
            String clean = certFingerprint.replace(":", "").replace(" ", "").trim();
            if (clean.length() != 64 || !HEX_PATTERN.matcher(clean).matches()) {
                return "Certificate Fingerprint must be a 64-character SHA-256 Hex string.";
            }
        }

        if (!TextUtils.isEmpty(apiId)) {
            try {
                int id = Integer.parseInt(apiId);
                if (id <= 0) {
                    return "API ID must be a positive number.";
                }
            } catch (NumberFormatException e) {
                return "API ID must contain digits only.";
            }
        }

        if (!TextUtils.isEmpty(apiHash)) {
            String cleanHash = apiHash.trim();
            if (cleanHash.length() != 32 || !HEX_PATTERN.matcher(cleanHash).matches()) {
                return "API Hash must be a 32-character hexadecimal string.";
            }
        }

        return null;
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    public static void saveConfig(int currentAccount,
                                  String deviceModel, String systemVersion, String appVersion,
                                  String langCode, String systemLangCode, String installer,
                                  String packageId, String tzOffset, String perfCat,
                                  String certFingerprint, String deviceToken,
                                  String apiIdStr, String apiHashStr,
                                  boolean allowFirebase, boolean allowAppHash,
                                  boolean allowFlashcall, boolean allowMissedCall) {

        SharedPreferences.Editor editor = getPrefs().edit();

        putOrRemove(editor, KEY_DEVICE_MODEL, deviceModel, getDefaultDeviceModel());
        putOrRemove(editor, KEY_SYSTEM_VERSION, systemVersion, getDefaultSystemVersion());
        putOrRemove(editor, KEY_APP_VERSION, appVersion, getDefaultAppVersion());
        putOrRemove(editor, KEY_LANG_CODE, langCode, getDefaultLangCode());
        putOrRemove(editor, KEY_SYSTEM_LANG_CODE, systemLangCode, getDefaultSystemLangCode());
        putOrRemove(editor, KEY_INSTALLER, installer, getDefaultInstaller());
        putOrRemove(editor, KEY_PACKAGE_ID, packageId, getDefaultPackageId());

        if (!TextUtils.isEmpty(tzOffset)) {
            try {
                editor.putInt(KEY_TZ_OFFSET, Integer.parseInt(tzOffset));
            } catch (Throwable e) {
                editor.remove(KEY_TZ_OFFSET);
            }
        } else {
            editor.remove(KEY_TZ_OFFSET);
        }

        if (!TextUtils.isEmpty(perfCat)) {
            try {
                editor.putInt(KEY_PERF_CAT, Integer.parseInt(perfCat));
            } catch (Throwable e) {
                editor.remove(KEY_PERF_CAT);
            }
        } else {
            editor.remove(KEY_PERF_CAT);
        }

        if (!TextUtils.isEmpty(certFingerprint)) {
            editor.putString(KEY_CERT_FINGERPRINT, certFingerprint.replace(":", "").replace(" ", "").trim().toUpperCase());
        } else {
            editor.remove(KEY_CERT_FINGERPRINT);
        }

        if (!TextUtils.isEmpty(deviceToken)) {
            editor.putString(KEY_DEVICE_TOKEN, deviceToken.trim());
        } else {
            editor.remove(KEY_DEVICE_TOKEN);
        }

        if (!TextUtils.isEmpty(apiIdStr) && !apiIdStr.trim().isEmpty()) {
            try {
                editor.putInt(KEY_API_ID, Integer.parseInt(apiIdStr.trim()));
            } catch (Throwable e) {
                editor.remove(KEY_API_ID);
            }
        } else {
            editor.remove(KEY_API_ID);
        }

        if (!TextUtils.isEmpty(apiHashStr) && !apiHashStr.trim().isEmpty()) {
            editor.putString(KEY_API_HASH, apiHashStr.trim());
        } else {
            editor.remove(KEY_API_HASH);
        }

        editor.putBoolean(KEY_ALLOW_FIREBASE, allowFirebase);
        editor.putBoolean(KEY_ALLOW_APP_HASH, allowAppHash);
        editor.putBoolean(KEY_ALLOW_FLASHCALL, allowFlashcall);
        editor.putBoolean(KEY_ALLOW_MISSED_CALL, allowMissedCall);

        editor.commit();

        try {
            ConnectionsManager.getInstance(currentAccount).checkConnection();
        } catch (Throwable ignored) {}
    }

    private static void putOrRemove(SharedPreferences.Editor editor, String key, String value, String defaultValue) {
        if (!TextUtils.isEmpty(value) && !value.trim().equalsIgnoreCase(defaultValue)) {
            editor.putString(key, value.trim());
        } else {
            editor.remove(key);
        }
    }

    public static void resetToDefault(int currentAccount) {
        getPrefs().edit().clear().commit();
        try {
            ConnectionsManager.getInstance(currentAccount).checkConnection();
        } catch (Throwable ignored) {}
    }
}

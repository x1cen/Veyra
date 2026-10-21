package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VeyraConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.IconBackgroundColors;
import org.telegram.ui.Components.LayoutHelper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class VeyraSettingsActivity extends VeyraSettingsBaseActivity {

    @Override
    protected String getScreenTitle() {
        return LocaleController.getString("VeyraSettings", R.string.VeyraSettings);
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();

        r.add(VeyraSettingsRow.header("Anti-Censorship & Proxy"));
        r.add(VeyraSettingsRow.category(
                IconBackgroundColors.PURPLE.top, IconBackgroundColors.PURPLE.bottom,
                R.drawable.msg_channel,
                "Aether Anti-Censorship",
                () -> presentFragment(new AetherActivity())
        ));
        r.add(VeyraSettingsRow.category(
                IconBackgroundColors.BLUE.top, IconBackgroundColors.BLUE.bottom,
                R.drawable.msg_policy,
                "Auto Discovery",
                () -> presentFragment(new ProxyDiscoveryActivity())
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 1: Features & Privacy
        r.add(VeyraSettingsRow.header("Features & Privacy"));
        r.add(VeyraSettingsRow.category(
                IconBackgroundColors.PURPLE.top, IconBackgroundColors.PURPLE.bottom,
                R.drawable.msg_secret,
                LocaleController.getString("VeyraGhostMode", R.string.VeyraGhostMode),
                () -> presentFragment(new VeyraGhostModeSettingsActivity())
        ));
        r.add(VeyraSettingsRow.category(
                IconBackgroundColors.RED.top, IconBackgroundColors.RED.bottom,
                R.drawable.msg_delete,
                LocaleController.getString("VeyraAntiDelete", R.string.VeyraAntiDelete),
                () -> presentFragment(new VeyraAntiDeleteSettingsActivity())
        ));
        r.add(VeyraSettingsRow.category(
                IconBackgroundColors.BLUE.top, IconBackgroundColors.BLUE.bottom,
                R.drawable.msg_edit,
                LocaleController.getString("VeyraEditHistory", R.string.VeyraEditHistory),
                () -> presentFragment(new VeyraEditHistorySettingsActivity())
        ));
        r.add(VeyraSettingsRow.category(
                IconBackgroundColors.ORANGE.top, IconBackgroundColors.ORANGE.bottom,
                R.drawable.msg_reactions,
                "Reaction History",
                () -> presentFragment(new VeyraReactionHistorySettingsActivity())
        ));
        r.add(VeyraSettingsRow.category(
                IconBackgroundColors.GREEN.top, IconBackgroundColors.GREEN.bottom,
                R.drawable.msg_permissions,
                LocaleController.getString("VeyraPrivacySecurity", R.string.VeyraPrivacySecurity),
                () -> presentFragment(new VeyraPrivacySettingsActivity())
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 2: Chats & Appearance
        r.add(VeyraSettingsRow.header("Chats & Appearance"));
        r.add(VeyraSettingsRow.category(
                IconBackgroundColors.CYAN.top, IconBackgroundColors.CYAN.bottom,
                R.drawable.msg_discussion,
                LocaleController.getString("VeyraChatList", R.string.VeyraChatList),
                () -> presentFragment(new VeyraChatListSettingsActivity())
        ));
        r.add(VeyraSettingsRow.category(
                IconBackgroundColors.BLUE_DEEP.top, IconBackgroundColors.BLUE_DEEP.bottom,
                R.drawable.msg_message,
                LocaleController.getString("VeyraComposingMessages", R.string.VeyraComposingMessages),
                () -> presentFragment(new VeyraComposingSettingsActivity())
        ));
        r.add(VeyraSettingsRow.category(
                IconBackgroundColors.ORANGE_DEEP.top, IconBackgroundColors.ORANGE_DEEP.bottom,
                R.drawable.msg_media,
                LocaleController.getString("VeyraMediaCamera", R.string.VeyraMediaCamera),
                () -> presentFragment(new VeyraMediaSettingsActivity())
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 3: Network & Tools (Aether moved to the top container)
        r.add(VeyraSettingsRow.header("Network & Tools"));
        r.add(VeyraSettingsRow.category(
                IconBackgroundColors.GRAY.top, IconBackgroundColors.GRAY.bottom,
                R.drawable.msg_settings_old,
                LocaleController.getString("VeyraControlsGeneral", R.string.VeyraControlsGeneral),
                () -> presentFragment(new VeyraControlsSettingsActivity())
        ));
        r.add(VeyraSettingsRow.category(
                IconBackgroundColors.BLUE_LIGHT.top, IconBackgroundColors.BLUE_LIGHT.bottom,
                R.drawable.msg_fave,
                LocaleController.getString("VeyraBackupRestore", R.string.VeyraBackupRestore),
                () -> presentFragment(new VeyraBackupSettingsActivity())
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 4: Info
        r.add(VeyraSettingsRow.header("Info"));
        r.add(VeyraSettingsRow.category(
                IconBackgroundColors.BLUE_DEEP.top, IconBackgroundColors.BLUE_DEEP.bottom,
                R.drawable.msg_mask,
                "Device & Identity",
                () -> presentFragment(new VeyraLoginConfigActivity())
        ));
        r.add(VeyraSettingsRow.shadow());

        return r;
    }

    private static boolean isCheckingForUpdates = false;

    private void checkForUpdates() {
        checkForUpdates(getParentActivity(), this);
    }

    public static void checkForUpdates(android.app.Activity parentActivity, BaseFragment fragment) {
        if (isCheckingForUpdates) {
            return;
        }
        if (parentActivity == null || fragment == null) return;
        isCheckingForUpdates = true;
        final boolean isFarsi = "fa".equals(LocaleController.getInstance().getCurrentLocale().getLanguage());
        final AlertDialog progressDialog = new AlertDialog(parentActivity, AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.setMessage(isFarsi ? "در حال بررسی به روزرسانی..." : "Checking for updates...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.showDelayed(150);

        Utilities.externalNetworkQueue.postRunnable(() -> {
            String latestTag = null;
            String changelog = null;
            String releaseUrl = "https://github.com/x1cen/Veyra/releases/latest";
            String downloadUrl = null;
            HttpURLConnection connection = null;
            boolean success = false;
            String deviceAbi = getDeviceAbi();
            try {
                // Query /releases (not /releases/latest) — latest excludes prereleases when only prereleases exist
                URL url = new URL("https://api.github.com/repos/x1cen/Veyra/releases");
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setRequestProperty("User-Agent", "Veyra-Android");
                int code = connection.getResponseCode();
                if (code == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    org.json.JSONArray releases = new org.json.JSONArray(sb.toString());
                    // Prefer latest stable (non-prerelease, non-draft); fallback to latest at all
                    JSONObject chosen = null;
                    for (int i = 0; i < releases.length(); i++) {
                        JSONObject r = releases.getJSONObject(i);
                        if (r.optBoolean("draft", false)) continue;
                        if (!r.optBoolean("prerelease", false)) { chosen = r; break; }
                    }
                    if (chosen == null) {
                        for (int i = 0; i < releases.length(); i++) {
                            JSONObject r = releases.getJSONObject(i);
                            if (r.optBoolean("draft", false)) continue;
                            chosen = r; break;
                        }
                    }
                    if (chosen != null) {
                        latestTag = chosen.optString("tag_name", null);
                        if (latestTag != null && latestTag.startsWith("v")) {
                            latestTag = latestTag.substring(1);
                        }
                        changelog = chosen.optString("body", "");
                        String htmlUrl = chosen.optString("html_url", null);
                        if (htmlUrl != null && !htmlUrl.isEmpty()) {
                            releaseUrl = htmlUrl;
                        }
                        // Pick APK matching device ABI (arm64-v8a, armeabi-v7a, x86_64, universal)
                        org.json.JSONArray assets = chosen.optJSONArray("assets");
                        if (assets != null) {
                            String bestUrl = null;
                            String fallbackUrl = null;
                            for (int j = 0; j < assets.length(); j++) {
                                JSONObject asset = assets.getJSONObject(j);
                                String name = asset.optString("name", "");
                                String url2 = asset.optString("browser_download_url", "");
                                if (!name.endsWith(".apk")) continue;
                                if (fallbackUrl == null) fallbackUrl = url2;
                                if (deviceAbi != null && name.contains(deviceAbi)) {
                                    bestUrl = url2;
                                    break;
                                }
                                if (name.contains("universal")) bestUrl = url2;
                            }
                            downloadUrl = bestUrl != null ? bestUrl : fallbackUrl;
                            if (downloadUrl == null || downloadUrl.isEmpty()) {
                                downloadUrl = releaseUrl;
                            }
                        } else {
                            downloadUrl = releaseUrl;
                        }
                        success = (latestTag != null && !latestTag.isEmpty());
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (connection != null) {
                    try {
                        connection.disconnect();
                    } catch (Exception ignore) {
                    }
                }
            }

            final boolean fetchOk = success;
            final String tag = latestTag;
            final String notes = changelog;
            final String targetUrl = downloadUrl != null ? downloadUrl : releaseUrl;
            final String pageUrl = releaseUrl;

            AndroidUtilities.runOnUIThread(() -> {
                isCheckingForUpdates = false;
                try {
                    progressDialog.dismiss();
                } catch (Exception ignore) {
                }
                if (fragment.getParentActivity() == null) {
                    return;
                }
                if (!fetchOk) {
                    AlertDialog.Builder errBuilder = new AlertDialog.Builder(fragment.getParentActivity());
                    errBuilder.setTitle(isFarsi ? "خطا" : "Error");
                    errBuilder.setMessage(isFarsi ? "خطا در بررسی به روزرسانی. اتصال اینترنت را بررسی کنید."
                            : "Failed to check for updates. Check your connection.");
                    errBuilder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                    fragment.showDialog(errBuilder.create());
                    return;
                }

                String currentVersion = getAppVersionName();
                int currentCode = parseSemver(currentVersion);
                int latestCode = parseSemver(tag);

                if (latestCode > currentCode) {
                    String title = isFarsi ? ("نسخه جدید ویرا (" + tag + ") موجود است") : ("Veyra " + tag + " available");
                    String rawNotes = (notes != null && !notes.trim().isEmpty()) ? notes.trim()
                            : (isFarsi ? "نسخه جدید ویرا منتشر شده است." : "A new version of Veyra is available.");
                    // Strip markdown formatting so AlertDialog shows plain text
                    String msg = rawNotes
                            .replaceAll("(?m)^#{1,6}\\s*", "")       // ## headers
                            .replaceAll("\\*\\*(.+?)\\*\\*", "$1")   // **bold**
                            .replaceAll("__(.+?)__", "$1")            // __bold__
                            .replaceAll("\\*(.+?)\\*", "$1")          // *italic*
                            .replaceAll("_(.+?)_", "$1")              // _italic_
                            .replaceAll("`{1,3}[^`]*`{1,3}", "")      // `code` / ```blocks```
                            .replaceAll("(?m)^[-*+]\\s+", "• ")       // bullet lists
                            .replaceAll("\\[(.+?)\\]\\(.*?\\)", "$1") // [text](url)
                            .replaceAll("\\n{3,}", "\n\n")            // collapse triple newlines
                            .trim();
                    AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity())
                            .setTitle(title)
                            .setMessage(msg)
                            .setPositiveButton(isFarsi ? "به روزرسانی" : "Update", (dialog, which) -> {
                                try {
                                    String dl = targetUrl;
                                    boolean isApk = dl != null && dl.endsWith(".apk");
                                    if (isApk) {
                                        Browser.openUrl(fragment.getParentActivity(), dl);
                                    } else {
                                        Browser.openUrl(fragment.getParentActivity(), pageUrl);
                                    }
                                } catch (Exception e) {
                                    FileLog.e(e);
                                    try { Browser.openUrl(fragment.getParentActivity(), pageUrl); } catch (Exception ignore) {}
                                }
                            })
                            .setNeutralButton(isFarsi ? "صفحه انتشار" : "View Release", (dialog, which) -> {
                                try { Browser.openUrl(fragment.getParentActivity(), pageUrl); } catch (Exception e) { FileLog.e(e); }
                            })
                            .setNegativeButton(isFarsi ? "بعدا" : "Later", null);
                    fragment.showDialog(builder.create());
                } else {
                    AlertDialog.Builder upToDateBuilder = new AlertDialog.Builder(fragment.getParentActivity());
                    upToDateBuilder.setTitle(isFarsi ? "به روزرسانی ویرا" : "Veyra Update");
                    upToDateBuilder.setMessage(isFarsi ? ("شما از آخرین نسخه ویرا (" + currentVersion + ") استفاده می کنید.")
                            : ("You have the latest version of Veyra (" + currentVersion + ")."));
                    upToDateBuilder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                    fragment.showDialog(upToDateBuilder.create());
                }
            });
        });
    }

    private static int parseSemver(String v) {
        if (v == null) return 0;
        v = v.trim();
        if (v.startsWith("12.")) {
            return 0;
        }
        String[] parts = v.split("[.\\-+]");
        int major = parts.length > 0 ? parseNumber(parts[0]) : 0;
        int minor = parts.length > 1 ? parseNumber(parts[1]) : 0;
        int patch = parts.length > 2 ? parseNumber(parts[2]) : 0;
        major = Math.max(0, Math.min(999, major));
        minor = Math.max(0, Math.min(999, minor));
        patch = Math.max(0, Math.min(999, patch));
        return major * 1_000_000 + minor * 1_000 + patch;
    }

    private static int parseNumber(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String getAppVersionName() {
        try {
            android.content.pm.PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager()
                    .getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            return pInfo.versionName;
        } catch (Exception e) {
            return "1.2.0";
        }
    }

    private static String getDeviceAbi() {
        try {
            String abi = android.os.Build.SUPPORTED_ABIS != null && android.os.Build.SUPPORTED_ABIS.length > 0
                    ? android.os.Build.SUPPORTED_ABIS[0] : android.os.Build.CPU_ABI;
            if (abi == null) return null;
            abi = abi.toLowerCase();
            if (abi.contains("arm64")) return "arm64-v8a";
            if (abi.contains("armeabi") || abi.contains("armv7")) return "armeabi-v7a";
            if (abi.contains("x86_64")) return "x86_64";
            if (abi.contains("x86")) return "x86";
            return abi;
        } catch (Exception e) {
            return null;
        }
    }
}

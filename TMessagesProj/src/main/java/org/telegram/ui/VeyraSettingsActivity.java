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

    private boolean unlocked = false;
    private FrameLayout lockContainer;
    private EditTextBoldCursor lockEditText;

    @Override
    protected String getScreenTitle() {
        return LocaleController.getString("VeyraSettings", R.string.VeyraSettings);
    }

    @Override
    public View createView(Context context) {
        View view = super.createView(context);

        if (VeyraConfig.hasSettingsLock() && !unlocked) {
            setupLockOverlay(context);
        }

        return view;
    }

    private void setupLockOverlay(Context context) {
        if (!(fragmentView instanceof FrameLayout)) return;
        FrameLayout root = (FrameLayout) fragmentView;

        boolean isFarsi = "fa".equals(LocaleController.getInstance().getCurrentLocale().getLanguage());

        lockContainer = new FrameLayout(context);
        lockContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        lockContainer.setClickable(true);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        lockContainer.addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 32, 0, 32, 0));

        TextView titleView = new TextView(context);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        titleView.setText(isFarsi ? "تنظیمات ویرا قفل است" : "Veyra Settings Locked");
        content.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 8));

        TextView descView = new TextView(context);
        descView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        descView.setGravity(Gravity.CENTER_HORIZONTAL);
        descView.setText(isFarsi ? "برای ورود، رمز عبور را وارد کنید" : "Enter your passcode to continue");
        content.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 24));

        lockEditText = new EditTextBoldCursor(context);
        lockEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        lockEditText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        lockEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        lockEditText.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        lockEditText.setSingleLine(true);
        lockEditText.setMaxLines(1);
        lockEditText.setGravity(Gravity.CENTER);
        lockEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        lockEditText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        lockEditText.setCursorSize(AndroidUtilities.dp(22));
        lockEditText.setCursorWidth(1.5f);
        lockEditText.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});
        content.addView(lockEditText, LayoutHelper.createLinear(240, 44, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        lockEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                checkUnlock();
                return true;
            }
            return false;
        });

        root.addView(lockContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        lockEditText.postDelayed(() -> {
            if (lockEditText != null) {
                lockEditText.requestFocus();
                AndroidUtilities.showKeyboard(lockEditText);
            }
        }, 200);
    }

    private void checkUnlock() {
        if (lockEditText == null) return;
        String code = lockEditText.getText().toString();
        if (VeyraConfig.checkSettingsLockCode(code)) {
            unlocked = true;
            AndroidUtilities.hideKeyboard(lockEditText);
            if (lockContainer != null && fragmentView instanceof FrameLayout) {
                ((FrameLayout) fragmentView).removeView(lockContainer);
                lockContainer = null;
            }
        } else {
            AndroidUtilities.shakeView(lockEditText);
            boolean isFarsi = "fa".equals(LocaleController.getInstance().getCurrentLocale().getLanguage());
            BulletinFactory.of(VeyraSettingsActivity.this).createErrorBulletin(isFarsi ? "رمز نادرست است" : "Wrong passcode").show();
        }
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();

        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraSettingsCategories", R.string.VeyraSettingsCategories)));

        // Category 1: Ghost Mode
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraGhostMode", R.string.VeyraGhostMode),
                LocaleController.getString("VeyraGhostModeDesc", R.string.VeyraGhostModeDesc),
                () -> presentFragment(new VeyraGhostModeSettingsActivity())
        ));

        // Category 2: Message Edit History
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraEditHistory", R.string.VeyraEditHistory),
                LocaleController.getString("VeyraEditHistoryDesc", R.string.VeyraEditHistoryDesc),
                () -> presentFragment(new VeyraEditHistorySettingsActivity())
        ));

        // Category 3: Anti-Delete Messages
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraAntiDelete", R.string.VeyraAntiDelete),
                LocaleController.getString("VeyraAntiDeleteDesc", R.string.VeyraAntiDeleteDesc),
                () -> presentFragment(new VeyraAntiDeleteSettingsActivity())
        ));

        // Category 4: Privacy & Security
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraPrivacySecurity", R.string.VeyraPrivacySecurity),
                LocaleController.getString("VeyraPrivacySecurityDesc", R.string.VeyraPrivacySecurityDesc),
                () -> presentFragment(new VeyraPrivacySettingsActivity())
        ));

        // Category 4: Chat List
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraChatList", R.string.VeyraChatList),
                LocaleController.getString("VeyraChatListDesc", R.string.VeyraChatListDesc),
                () -> presentFragment(new VeyraChatListSettingsActivity())
        ));

        // Category 5: Composing & Messages
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraComposingMessages", R.string.VeyraComposingMessages),
                LocaleController.getString("VeyraComposingMessagesDesc", R.string.VeyraComposingMessagesDesc),
                () -> presentFragment(new VeyraComposingSettingsActivity())
        ));

        // Category 6: Media & Camera
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraMediaCamera", R.string.VeyraMediaCamera),
                LocaleController.getString("VeyraMediaCameraDesc", R.string.VeyraMediaCameraDesc),
                () -> presentFragment(new VeyraMediaSettingsActivity())
        ));

        // Category 7: Controls & General
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraControlsGeneral", R.string.VeyraControlsGeneral),
                LocaleController.getString("VeyraControlsGeneralDesc", R.string.VeyraControlsGeneralDesc),
                () -> presentFragment(new VeyraControlsSettingsActivity())
        ));

        // Category 8: Backup & Restore
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraBackupRestore", R.string.VeyraBackupRestore),
                LocaleController.getString("VeyraBackupRestoreDesc", R.string.VeyraBackupRestoreDesc),
                () -> presentFragment(new VeyraBackupSettingsActivity())
        ));

        r.add(VeyraSettingsRow.shadow());

        // About section
        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraAbout", R.string.VeyraAbout)));

        r.add(VeyraSettingsRow.detail(
                LocaleController.getString("VeyraUnlockedLimits", R.string.VeyraUnlockedLimits),
                LocaleController.getString("VeyraUnlockedLimitsDesc", R.string.VeyraUnlockedLimitsDesc),
                true
        ));

        r.add(VeyraSettingsRow.detail(
                LocaleController.getString("VeyraAdFree", R.string.VeyraAdFree),
                LocaleController.getString("VeyraAdFreeDesc", R.string.VeyraAdFreeDesc),
                true
        ));

        r.add(VeyraSettingsRow.detail(
                LocaleController.getString("VeyraVersion", R.string.VeyraVersion),
                getAppVersionName(),
                true
        ));

        r.add(VeyraSettingsRow.button(
                LocaleController.getString("VeyraCheckForUpdates", R.string.VeyraCheckForUpdates),
                false, false,
                this::checkForUpdates
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
        BulletinFactory.of(fragment).createSimpleBulletin(
                R.raw.chats_infotip,
                isFarsi ? "در حال بررسی به‌روزرسانی..." : "Checking for updates…"
        ).show();

        Utilities.externalNetworkQueue.postRunnable(() -> {
            String latestTag = null;
            String changelog = null;
            String releaseUrl = "https://github.com/x1cen/Veyra/releases/latest";
            HttpURLConnection connection = null;
            boolean success = false;
            try {
                URL url = new URL("https://api.github.com/repos/x1cen/Veyra/releases/latest");
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
                    JSONObject release = new JSONObject(sb.toString());
                    latestTag = release.optString("tag_name", null);
                    if (latestTag != null && latestTag.startsWith("v")) {
                        latestTag = latestTag.substring(1);
                    }
                    changelog = release.optString("body", "");
                    String htmlUrl = release.optString("html_url", null);
                    if (htmlUrl != null && !htmlUrl.isEmpty()) {
                        releaseUrl = htmlUrl;
                    }
                    success = (latestTag != null && !latestTag.isEmpty());
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
            final String targetUrl = releaseUrl;

            AndroidUtilities.runOnUIThread(() -> {
                isCheckingForUpdates = false;
                if (fragment.getParentActivity() == null) {
                    return;
                }
                if (!fetchOk) {
                    BulletinFactory.of(fragment).createErrorBulletin(
                            isFarsi ? "خطا در بررسی به‌روزرسانی. اتصال اینترنت را بررسی کنید."
                                    : "Failed to check for updates. Check your connection."
                    ).show();
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
                            .setPositiveButton(isFarsi ? "به‌روزرسانی" : "Update", (dialog, which) -> {
                                try {
                                    Browser.openUrl(fragment.getParentActivity(), targetUrl);
                                } catch (Exception e) {
                                    FileLog.e(e);
                                }
                            })
                            .setNegativeButton(isFarsi ? "بعداً" : "Later", null);
                    fragment.showDialog(builder.create());
                } else {
                    BulletinFactory.of(fragment).createSimpleBulletin(
                            R.raw.chats_infotip,
                            isFarsi ? ("شما از آخرین نسخه ویرا (" + currentVersion + ") استفاده می‌کنید")
                                    : ("You have the latest version of Veyra (" + currentVersion + ")")
                    ).show();
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
            return "1.1.2";
        }
    }
}

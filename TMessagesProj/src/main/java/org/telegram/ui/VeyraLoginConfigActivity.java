/* Veyra Project — Advanced Login & Connection Parameters Customizer */
package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraLoginConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class VeyraLoginConfigActivity extends VeyraSettingsBaseActivity {

    private static final int RESET_BUTTON = 1;
    private static final int RANDOM_BUTTON = 2;
    private static final int DONE_BUTTON = 3;

    private static final String[][] RANDOM_DEVICE_PROFILES = {
            {"samsung SM-S928B", "SDK 34", "3"},   // Galaxy S24 Ultra
            {"samsung SM-S926B", "SDK 34", "3"},   // Galaxy S24+
            {"samsung SM-S918B", "SDK 34", "3"},   // Galaxy S23 Ultra
            {"samsung SM-F946B", "SDK 34", "3"},   // Galaxy Z Fold 5
            {"samsung SM-A556B", "SDK 34", "2"},   // Galaxy A55 5G
            {"samsung SM-A546B", "SDK 33", "2"},   // Galaxy A54 5G
            {"Google Pixel 9 Pro XL", "SDK 35", "3"},
            {"Google Pixel 9 Pro", "SDK 35", "3"},
            {"Google Pixel 8 Pro", "SDK 34", "3"},
            {"Google Pixel 8a", "SDK 34", "2"},
            {"Google Pixel 7 Pro", "SDK 34", "3"},
            {"Xiaomi 24030PN60G", "SDK 34", "3"},  // Xiaomi 14 Ultra
            {"Xiaomi 23116PN5BC", "SDK 34", "3"},  // Xiaomi 14 Pro
            {"Xiaomi 2210132G", "SDK 33", "3"},    // Xiaomi 13 Pro
            {"OnePlus CPH2581", "SDK 34", "3"},    // OnePlus 12
            {"OnePlus CPH2449", "SDK 33", "3"},    // OnePlus 11
            {"Sony XQ-EC54", "SDK 34", "3"},       // Xperia 1 VI
            {"Asus ASUS_AI2401_D", "SDK 34", "3"}  // ROG Phone 8
    };

    private final boolean isReadOnly;

    public VeyraLoginConfigActivity() {
        this(false);
    }

    public VeyraLoginConfigActivity(boolean readOnly) {
        super();
        this.isReadOnly = readOnly;
    }

    @Override
    protected String getScreenTitle() {
        return "Device & Identity";
    }

    @Override
    public View createView(Context context) {
        View v = super.createView(context);
        if (actionBar != null) {
            if (!isReadOnly) {
                ActionBarMenu menu = actionBar.createMenu();
                menu.addItemWithWidth(RESET_BUTTON, R.drawable.msg_retry, AndroidUtilities.dp(48), "Restore Defaults");
                menu.addItemWithWidth(RANDOM_BUTTON, R.drawable.menu_random, AndroidUtilities.dp(48), "Randomize");
                menu.addItemWithWidth(DONE_BUTTON, R.drawable.ic_ab_done, AndroidUtilities.dp(56), "Save");
            }

            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    } else if (id == DONE_BUTTON) {
                        saveAndFinish();
                    } else if (id == RANDOM_BUTTON) {
                        randomizeIdentity();
                    } else if (id == RESET_BUTTON) {
                        confirmReset();
                    }
                }
            });
        }
        return v;
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();

        if (isReadOnly) {
            // Hardware Emulation (Read-only)
            r.add(VeyraSettingsRow.header("Hardware Emulation"));
            r.add(VeyraSettingsRow.detail(
                    "Device Model",
                    () -> VeyraLoginConfig.getDeviceModel(),
                    true,
                    () -> copyValue("Device Model", VeyraLoginConfig.getDeviceModel())
            ));
            r.add(VeyraSettingsRow.detail(
                    "System Version",
                    () -> VeyraLoginConfig.getSystemVersion(),
                    true,
                    () -> copyValue("System Version", VeyraLoginConfig.getSystemVersion())
            ));
            r.add(VeyraSettingsRow.detail(
                    "App Version",
                    () -> VeyraLoginConfig.getAppVersion(),
                    true,
                    () -> copyValue("App Version", VeyraLoginConfig.getAppVersion())
            ));
            r.add(VeyraSettingsRow.detail(
                    "Performance Class",
                    this::getPerfCatDisplay,
                    false,
                    () -> copyValue("Performance Class", getPerfCatDisplay())
            ));
            r.add(VeyraSettingsRow.shadow());

            // Security & Verification (Read-only)
            r.add(VeyraSettingsRow.header("Security & Verification"));
            r.add(VeyraSettingsRow.detail(
                    "Certificate Fingerprint",
                    this::getShortCertFingerprint,
                    true,
                    () -> showReadOnlyValueDialog("Certificate Fingerprint", VeyraLoginConfig.getCertFingerprint())
            ));
            r.add(VeyraSettingsRow.detail(
                    "Push / FCM Device Token",
                    this::getShortDeviceToken,
                    true,
                    () -> showReadOnlyValueDialog("Push / FCM Device Token", VeyraLoginConfig.getDeviceToken(""))
            ));
            r.add(VeyraSettingsRow.detail(
                    "Play Integrity & SafetyNet",
                    () -> VeyraLoginConfig.isAllowFirebase(true) ? "Enabled" : "Disabled",
                    true,
                    null
            ));
            r.add(VeyraSettingsRow.detail(
                    "SMS Retriever App Hash",
                    () -> VeyraLoginConfig.isAllowAppHash(true) ? "Enabled" : "Disabled",
                    true,
                    null
            ));
            r.add(VeyraSettingsRow.detail(
                    "Flash Call Verification",
                    () -> VeyraLoginConfig.isAllowFlashcall(false) ? "Enabled" : "Disabled",
                    true,
                    null
            ));
            r.add(VeyraSettingsRow.detail(
                    "Missed Call Verification",
                    () -> VeyraLoginConfig.isAllowMissedCall(false) ? "Enabled" : "Disabled",
                    false,
                    null
            ));
            r.add(VeyraSettingsRow.shadow());

            // Localization & Environment (Read-only)
            r.add(VeyraSettingsRow.header("Localization & Environment"));
            r.add(VeyraSettingsRow.detail(
                    "App Language",
                    () -> VeyraLoginConfig.getLangCode(),
                    true,
                    () -> copyValue("App Language", VeyraLoginConfig.getLangCode())
            ));
            r.add(VeyraSettingsRow.detail(
                    "System Language",
                    () -> VeyraLoginConfig.getSystemLangCode(),
                    true,
                    () -> copyValue("System Language", VeyraLoginConfig.getSystemLangCode())
            ));
            r.add(VeyraSettingsRow.detail(
                    "Installer Package",
                    () -> {
                        String inst = VeyraLoginConfig.getInstaller();
                        return TextUtils.isEmpty(inst) ? "Direct APK (None)" : inst;
                    },
                    true,
                    () -> copyValue("Installer Package", VeyraLoginConfig.getInstaller())
            ));
            r.add(VeyraSettingsRow.detail(
                    "Package Identifier",
                    () -> VeyraLoginConfig.getPackageId(),
                    false,
                    () -> copyValue("Package Identifier", VeyraLoginConfig.getPackageId())
            ));
            r.add(VeyraSettingsRow.shadow());

            return r;
        }

        // Editable Mode (Before initial login)
        // Section 1: Presets
        r.add(VeyraSettingsRow.header("Device Presets"));
        r.add(VeyraSettingsRow.detail(
                "Select Device Preset",
                () -> VeyraLoginConfig.getDeviceModel(),
                false,
                this::showPresetPicker
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 2: Hardware Emulation
        r.add(VeyraSettingsRow.header("Hardware Emulation"));
        r.add(VeyraSettingsRow.detail(
                "Device Model",
                () -> VeyraLoginConfig.getDeviceModel(),
                true,
                () -> showEditDialog("Device Model", VeyraLoginConfig.getDeviceModel(), val -> {
                    VeyraLoginConfig.setDeviceModel(val);
                    applyAndNotify("Updated Device Model");
                })
        ));
        r.add(VeyraSettingsRow.detail(
                "System Version",
                () -> VeyraLoginConfig.getSystemVersion(),
                true,
                () -> showEditDialog("System Version", VeyraLoginConfig.getSystemVersion(), val -> {
                    VeyraLoginConfig.setSystemVersion(val);
                    applyAndNotify("Updated System Version");
                })
        ));
        r.add(VeyraSettingsRow.detail(
                "App Version",
                () -> VeyraLoginConfig.getAppVersion(),
                true,
                () -> showEditDialog("App Version", VeyraLoginConfig.getAppVersion(), val -> {
                    VeyraLoginConfig.setAppVersion(val);
                    applyAndNotify("Updated App Version");
                })
        ));
        r.add(VeyraSettingsRow.detail(
                "Performance Class",
                this::getPerfCatDisplay,
                false,
                this::showPerfPicker
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 3: Security & Verification
        r.add(VeyraSettingsRow.header("Security & Verification"));
        r.add(VeyraSettingsRow.detail(
                "Certificate Fingerprint",
                this::getShortCertFingerprint,
                true,
                this::showCertDialog
        ));
        r.add(VeyraSettingsRow.detail(
                "Push / FCM Device Token",
                this::getShortDeviceToken,
                true,
                this::showTokenDialog
        ));
        r.add(VeyraSettingsRow.toggle(
                "Play Integrity & SafetyNet",
                "Verify app authenticity via Google Play Services",
                () -> VeyraLoginConfig.isAllowFirebase(true),
                val -> {
                    VeyraLoginConfig.setAllowFirebase(val);
                    applyAndNotify(val ? "Play Integrity enabled" : "Play Integrity disabled");
                },
                true
        ));
        r.add(VeyraSettingsRow.toggle(
                "SMS Retriever App Hash",
                "Automatic detection of SMS verification codes",
                () -> VeyraLoginConfig.isAllowAppHash(true),
                val -> {
                    VeyraLoginConfig.setAllowAppHash(val);
                    applyAndNotify(val ? "App Hash enabled" : "App Hash disabled");
                },
                true
        ));
        r.add(VeyraSettingsRow.toggle(
                "Flash Call Verification",
                "Allow verification via incoming phone call",
                () -> VeyraLoginConfig.isAllowFlashcall(false),
                val -> {
                    VeyraLoginConfig.setAllowFlashcall(val);
                    applyAndNotify(val ? "Flash Call enabled" : "Flash Call disabled");
                },
                true
        ));
        r.add(VeyraSettingsRow.toggle(
                "Missed Call Verification",
                "Allow verification via missed phone call",
                () -> VeyraLoginConfig.isAllowMissedCall(false),
                val -> {
                    VeyraLoginConfig.setAllowMissedCall(val);
                    applyAndNotify(val ? "Missed Call enabled" : "Missed Call disabled");
                },
                false
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 4: Localization & Environment
        r.add(VeyraSettingsRow.header("Localization & Environment"));
        r.add(VeyraSettingsRow.detail(
                "App Language",
                () -> VeyraLoginConfig.getLangCode(),
                true,
                () -> showEditDialog("App Language", VeyraLoginConfig.getLangCode(), val -> {
                    VeyraLoginConfig.setLangCode(val);
                    applyAndNotify("Updated App Language");
                })
        ));
        r.add(VeyraSettingsRow.detail(
                "System Language",
                () -> VeyraLoginConfig.getSystemLangCode(),
                true,
                () -> showEditDialog("System Language", VeyraLoginConfig.getSystemLangCode(), val -> {
                    VeyraLoginConfig.setSystemLangCode(val);
                    applyAndNotify("Updated System Language");
                })
        ));
        r.add(VeyraSettingsRow.detail(
                "Installer Package",
                () -> {
                    String inst = VeyraLoginConfig.getInstaller();
                    return TextUtils.isEmpty(inst) ? "Direct APK (None)" : inst;
                },
                true,
                () -> showEditDialog("Installer Package", VeyraLoginConfig.getInstaller(), val -> {
                    VeyraLoginConfig.setInstaller(val);
                    applyAndNotify("Updated Installer");
                })
        ));
        r.add(VeyraSettingsRow.detail(
                "Package Identifier",
                () -> VeyraLoginConfig.getPackageId(),
                false,
                () -> showEditDialog("Package Identifier", VeyraLoginConfig.getPackageId(), val -> {
                    VeyraLoginConfig.setPackageId(val);
                    applyAndNotify("Updated Package Identifier");
                })
        ));
        r.add(VeyraSettingsRow.shadow());

        return r;
    }

    private void saveAndFinish() {
        try {
            ConnectionsManager.getInstance(currentAccount).applyDeviceSettings();
        } catch (Throwable ignored) {}
        try {
            if (canShowBulletin()) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Device parameters saved").show();
            }
        } catch (Throwable ignored) {}
        finishFragment();
    }

    private void copyValue(String label, String value) {
        if (TextUtils.isEmpty(value)) return;
        AndroidUtilities.addToClipboard(value);
        try {
            if (canShowBulletin()) {
                BulletinFactory.of(this).createCopyBulletin(label + " copied").show();
            }
        } catch (Throwable ignored) {}
    }

    private void showReadOnlyValueDialog(String title, String value) {
        if (getParentActivity() == null || TextUtils.isEmpty(value)) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);
        builder.setMessage(value);
        builder.setPositiveButton(LocaleController.getString("Copy", R.string.Copy), (dialog, which) -> {
            copyValue(title, value);
        });
        builder.setNegativeButton(LocaleController.getString("Close", R.string.Close), null);
        showDialog(builder.create());
    }

    private String getPerfCatDisplay() {
        int perf = VeyraLoginConfig.getPerfCat();
        switch (perf) {
            case 0: return "Low (0)";
            case 1: return "Average (1)";
            case 2: return "High (2)";
            case 3: return "Flagship (3)";
            default: return "Custom (" + perf + ")";
        }
    }

    private String getShortCertFingerprint() {
        String fp = VeyraLoginConfig.getCertFingerprint();
        if (TextUtils.isEmpty(fp)) {
            return "Official Default";
        }
        if (fp.equalsIgnoreCase(VeyraLoginConfig.OFFICIAL_CERT_FINGERPRINT)) {
            return "Official Telegram SHA-256";
        }
        if (fp.length() > 16) {
            return fp.substring(0, 8) + "..." + fp.substring(fp.length() - 8);
        }
        return fp;
    }

    private String getShortDeviceToken() {
        String token = VeyraLoginConfig.getDeviceToken(null);
        if (TextUtils.isEmpty(token)) {
            return "System FCM Default";
        }
        if (token.length() > 20) {
            return token.substring(0, 10) + "..." + token.substring(token.length() - 8);
        }
        return token;
    }

    private EditTextBoldCursor createStyledInputBox(String text, String hint) {
        Theme.ResourcesProvider resourcesProvider = getResourceProvider();
        EditTextBoldCursor editText = new EditTextBoldCursor(getParentActivity());
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextGray2, resourcesProvider));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourcesProvider));
        editText.setCursorWidth(1.5f);
        if (text != null) {
            editText.setText(text);
        }
        if (hint != null) {
            editText.setHint(hint);
        }
        editText.setSelectAllOnFocus(true);
        editText.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

        final GradientDrawable fieldBackground = new GradientDrawable();
        fieldBackground.setCornerRadius(AndroidUtilities.dp(10));
        fieldBackground.setColor(Theme.multAlpha(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider), 0.06f));
        final int normalStroke = Theme.multAlpha(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider), 0.16f);
        final int activeStroke = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourcesProvider);
        fieldBackground.setStroke(AndroidUtilities.dp(1), normalStroke);
        editText.setBackground(fieldBackground);

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            fieldBackground.setStroke(AndroidUtilities.dp(hasFocus ? 1.5f : 1), hasFocus ? activeStroke : normalStroke);
        });
        return editText;
    }

    private void showEditDialog(String title, String currentValue, Consumer<String> onSave) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);

        final EditTextBoldCursor editText = createStyledInputBox(currentValue != null ? currentValue : "", null);

        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 14, 24, 14));
        builder.setView(container);

        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            String newVal = editText.getText().toString().trim();
            onSave.accept(newVal);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
        editText.requestFocus();
        AndroidUtilities.showKeyboard(editText);
    }

    private void showPresetPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Choose Device Preset");

        String[] presetNames = new String[]{
                "Samsung Galaxy S24 Ultra (SDK 34)",
                "Samsung Galaxy S23 Ultra (SDK 34)",
                "Google Pixel 9 Pro XL (SDK 35)",
                "Google Pixel 8 Pro (SDK 34)",
                "Xiaomi 14 Pro (SDK 34)",
                "OnePlus 12 (SDK 34)",
                "Sony Xperia 1 VI (SDK 34)",
                "Official Telegram Direct (Web APK)",
                "Current Device Real Hardware"
        };

        builder.setItems(presetNames, (dialog, which) -> {
            switch (which) {
                case 0:
                    applyPreset("samsung SM-S928B", "SDK 34", "com.android.vending", "org.telegram.messenger", 3);
                    break;
                case 1:
                    applyPreset("samsung SM-S918B", "SDK 34", "com.android.vending", "org.telegram.messenger", 3);
                    break;
                case 2:
                    applyPreset("Google Pixel 9 Pro XL", "SDK 35", "com.android.vending", "org.telegram.messenger", 3);
                    break;
                case 3:
                    applyPreset("Google Pixel 8 Pro", "SDK 34", "com.android.vending", "org.telegram.messenger", 3);
                    break;
                case 4:
                    applyPreset("Xiaomi 23116PN5BC", "SDK 34", "com.android.vending", "org.telegram.messenger", 3);
                    break;
                case 5:
                    applyPreset("OnePlus CPH2581", "SDK 34", "com.android.vending", "org.telegram.messenger", 3);
                    break;
                case 6:
                    applyPreset("Sony XQ-EC54", "SDK 34", "com.android.vending", "org.telegram.messenger", 3);
                    break;
                case 7:
                    applyPreset("Google Pixel 9 Pro", "SDK 35", "", "org.telegram.messenger.web", 3);
                    break;
                case 8:
                    VeyraLoginConfig.resetToDefault(currentAccount);
                    break;
            }
            applyAndNotify("Applied: " + presetNames[which]);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void applyPreset(String model, String sysVer, String installer, String pkgId, int perf) {
        VeyraLoginConfig.setDeviceModel(model);
        VeyraLoginConfig.setSystemVersion(sysVer);
        VeyraLoginConfig.setInstaller(installer);
        VeyraLoginConfig.setPackageId(pkgId);
        VeyraLoginConfig.setPerformanceClass(perf);
        VeyraLoginConfig.setCertFingerprint(VeyraLoginConfig.OFFICIAL_CERT_FINGERPRINT);
    }

    private void showPerfPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Performance Class");

        String[] items = new String[]{"Low (0)", "Average (1)", "High (2)", "Flagship (3)"};
        builder.setItems(items, (dialog, which) -> {
            VeyraLoginConfig.setPerformanceClass(which);
            applyAndNotify("Performance Class: " + items[which]);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showCertDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Certificate Fingerprint");

        final EditTextBoldCursor editText = createStyledInputBox(VeyraLoginConfig.getCertFingerprint(), "SHA-256 Hex");

        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 14, 24, 14));
        builder.setView(container);

        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            String newVal = editText.getText().toString().trim();
            VeyraLoginConfig.setCertFingerprint(newVal);
            applyAndNotify("Updated Certificate Fingerprint");
        });
        builder.setNeutralButton("Official Default", (dialog, which) -> {
            VeyraLoginConfig.setCertFingerprint(VeyraLoginConfig.OFFICIAL_CERT_FINGERPRINT);
            applyAndNotify("Restored Official Certificate Fingerprint");
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
        editText.requestFocus();
        AndroidUtilities.showKeyboard(editText);
    }

    private void showTokenDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Push / FCM Device Token");

        final EditTextBoldCursor editText = createStyledInputBox(VeyraLoginConfig.getDeviceToken(""), "FCM Token (f...:APA91b...)");

        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 14, 24, 14));
        builder.setView(container);

        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            String newVal = editText.getText().toString().trim();
            VeyraLoginConfig.setDeviceToken(newVal);
            applyAndNotify("Updated FCM Device Token");
        });
        builder.setNeutralButton("Clear (Default)", (dialog, which) -> {
            VeyraLoginConfig.setDeviceToken("");
            applyAndNotify("Cleared Custom FCM Token");
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
        editText.requestFocus();
        AndroidUtilities.showKeyboard(editText);
    }

    private void randomizeIdentity() {
        Random rnd = new Random();
        String[] profile = RANDOM_DEVICE_PROFILES[rnd.nextInt(RANDOM_DEVICE_PROFILES.length)];
        VeyraLoginConfig.setDeviceModel(profile[0]);
        VeyraLoginConfig.setSystemVersion(profile[1]);
        try {
            VeyraLoginConfig.setPerformanceClass(Integer.parseInt(profile[2]));
        } catch (Throwable ignored) {}

        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-";
        StringBuilder sb = new StringBuilder(140);
        sb.append("f");
        for (int i = 0; i < 21; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        sb.append(":APA91b");
        for (int i = 0; i < 112; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        VeyraLoginConfig.setDeviceToken(sb.toString());
        applyAndNotify("Randomized: " + profile[0]);
    }

    private void confirmReset() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Restore Defaults");
        builder.setMessage("Reset all device identity, hardware emulation, and spoofed signatures back to original defaults?");
        builder.setPositiveButton(LocaleController.getString("Reset", R.string.Reset), (dialog, which) -> {
            VeyraLoginConfig.resetToDefault(currentAccount);
            try {
                ConnectionsManager.getInstance(currentAccount).applyDeviceSettings();
            } catch (Throwable ignored) {}
            reloadRows();
            try {
                if (canShowBulletin()) {
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Restored to default hardware").show();
                }
            } catch (Throwable ignored) {}
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void applyAndNotify(String message) {
        try {
            ConnectionsManager.getInstance(currentAccount).applyDeviceSettings();
        } catch (Throwable ignored) {}
        reloadRows();
        try {
            if (canShowBulletin()) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, message).show();
            }
        } catch (Throwable ignored) {}
    }

    private boolean canShowBulletin() {
        return getParentActivity() != null && fragmentView != null && !isFinishing();
    }
}

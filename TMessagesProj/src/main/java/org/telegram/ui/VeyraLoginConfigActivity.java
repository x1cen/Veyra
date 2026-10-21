/* Veyra Project — Advanced Login & Connection Parameters Customizer */
package org.telegram.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraLoginConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

public class VeyraLoginConfigActivity extends BaseFragment {

    private static final int DONE_BUTTON = 1;

    private EditTextBoldCursor deviceModelField;
    private EditTextBoldCursor systemVersionField;
    private EditTextBoldCursor appVersionField;
    private EditTextBoldCursor perfCatField;

    private EditTextBoldCursor certFingerprintField;
    private EditTextBoldCursor deviceTokenField;

    private EditTextBoldCursor langCodeField;
    private EditTextBoldCursor systemLangCodeField;
    private EditTextBoldCursor tzOffsetField;
    private EditTextBoldCursor installerField;
    private EditTextBoldCursor packageIdField;

    private EditTextBoldCursor apiIdField;
    private EditTextBoldCursor apiHashField;

    private TextCheckCell firebaseCheckCell;
    private TextCheckCell appHashCheckCell;
    private TextCheckCell flashcallCheckCell;
    private TextCheckCell missedCallCheckCell;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Login & Device Parameters");
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(DONE_BUTTON, R.drawable.ic_ab_done);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == DONE_BUTTON) {
                    save();
                }
            }
        });

        fragmentView = new ScrollView(context);
        ScrollView scrollView = (ScrollView) fragmentView;
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(36));
        scrollView.addView(rootLayout, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT));

        // ─── Hero Header Card ─────────────────────────────────────────────────
        LinearLayout heroCard = new LinearLayout(context);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setGravity(Gravity.CENTER_HORIZONTAL);
        heroCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        ImageView heroIcon = new ImageView(context);
        heroIcon.setImageResource(R.drawable.settings_devices);
        heroIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton), PorterDuff.Mode.SRC_IN));
        heroIcon.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(64), 0x1A2CA5E0));
        heroIcon.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        heroCard.addView(heroIcon, LayoutHelper.createLinear(64, 64, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));

        TextView heroTitle = new TextView(context);
        heroTitle.setText("Connection & Device Fingerprint");
        heroTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        heroTitle.setTypeface(AndroidUtilities.bold());
        heroTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        heroTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        heroCard.addView(heroTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 4));

        TextView heroSubtitle = new TextView(context);
        heroSubtitle.setText("Customize MTProto handshake headers, hardware emulation signatures, and verification tokens sent to Telegram servers.");
        heroSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        heroSubtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        heroSubtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        heroCard.addView(heroSubtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 12, 0, 12, 12));

        rootLayout.addView(heroCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // ─── Preset Chips Bar ─────────────────────────────────────────────────
        addPresetChips(context, rootLayout);

        // ─── Card 1: Hardware & System Emulation ──────────────────────────────
        LinearLayout cardHardware = createCard(context, rootLayout, "Hardware & System Emulation", R.drawable.settings_devices);
        deviceModelField = addField(context, cardHardware, "Device Model", VeyraLoginConfig.getDeviceModel(), "Sent as initConnection.device_model (e.g. samsung SM-S928B)", InputType.TYPE_CLASS_TEXT);
        systemVersionField = addField(context, cardHardware, "System Version", VeyraLoginConfig.getSystemVersion(), "Android SDK level / OS version (e.g. SDK 34)", InputType.TYPE_CLASS_TEXT);
        appVersionField = addField(context, cardHardware, "App Version", VeyraLoginConfig.getAppVersion(), "Telegram client version (e.g. 11.8.0 (5200))", InputType.TYPE_CLASS_TEXT);
        perfCatField = addField(context, cardHardware, "Performance Class", VeyraLoginConfig.getCustomPerfCatString(), "Device power tier: -1 (Auto), 1 (Low), 2 (Med), 3 (High)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);

        // ─── Card 2: Security & Certificate Fingerprint ───────────────────────
        LinearLayout cardSecurity = createCard(context, rootLayout, "Security & App Verification", R.drawable.settings_devices);
        certFingerprintField = addField(context, cardSecurity, "Certificate SHA-256 Fingerprint", VeyraLoginConfig.getCustomCertFingerprintString(), "Official Telegram signature: 49C15225...38E1 (avoids 3rd-party flags)", InputType.TYPE_CLASS_TEXT);
        deviceTokenField = addField(context, cardSecurity, "Push / FCM Device Token", VeyraLoginConfig.getCustomDeviceTokenString(), "Leave empty to dynamically generate real FCM push token", InputType.TYPE_CLASS_TEXT);

        // ─── Card 3: Localization & Network ───────────────────────────────────
        LinearLayout cardLocale = createCard(context, rootLayout, "Localization & Distribution", R.drawable.settings_devices);
        langCodeField = addField(context, cardLocale, "App Language Code", VeyraLoginConfig.getLangCode(), "ISO-639 language code for app UI (e.g. en, fa)", InputType.TYPE_CLASS_TEXT);
        systemLangCodeField = addField(context, cardLocale, "System Language Code", VeyraLoginConfig.getSystemLangCode(), "Device OS primary language (e.g. en, fa)", InputType.TYPE_CLASS_TEXT);
        tzOffsetField = addField(context, cardLocale, "Timezone Offset (Seconds)", VeyraLoginConfig.getCustomTzOffsetString(), "Seconds from UTC (e.g. 12600 for Iran UTC+3:30)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        installerField = addField(context, cardLocale, "Installer Package", VeyraLoginConfig.getInstaller(), "com.android.vending (Google Play) or empty for Direct APK", InputType.TYPE_CLASS_TEXT);
        packageIdField = addField(context, cardLocale, "Package Identifier", VeyraLoginConfig.getPackageId(), "Application package name (org.telegram.messenger)", InputType.TYPE_CLASS_TEXT);

        // ─── Card 4: MTProto API Credentials ──────────────────────────────────
        LinearLayout cardApi = createCard(context, rootLayout, "Custom API Credentials (Hidden by Default)", R.drawable.settings_devices);

        TextView apiNote = new TextView(context);
        apiNote.setText("Zero-Knowledge Security: Official built-in API credentials are protected and never shown. Leave blank to use built-in Telegram API keys safely.");
        apiNote.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        apiNote.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        apiNote.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        cardApi.addView(apiNote, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        apiIdField = addField(context, cardApi, "API ID", VeyraLoginConfig.getCustomApiIdString(), "Optional custom integer API ID (leave empty for default)", InputType.TYPE_CLASS_NUMBER);
        apiHashField = addField(context, cardApi, "API Hash", VeyraLoginConfig.getCustomApiHashString(), "Optional 32-character Hex API Hash (leave empty for default)", InputType.TYPE_CLASS_TEXT);

        // ─── Card 5: Authentication Security Flags ────────────────────────────
        LinearLayout cardFlags = createCard(context, rootLayout, "Auth Verification Flags", R.drawable.settings_devices);

        firebaseCheckCell = new TextCheckCell(context);
        firebaseCheckCell.setTextAndCheck("Play Integrity & SafetyNet Token", VeyraLoginConfig.isAllowFirebase(true), true);
        firebaseCheckCell.setOnClickListener(v -> firebaseCheckCell.setChecked(!firebaseCheckCell.isChecked()));
        cardFlags.addView(firebaseCheckCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        appHashCheckCell = new TextCheckCell(context);
        appHashCheckCell.setTextAndCheck("SMS Retriever App Hash", VeyraLoginConfig.isAllowAppHash(true), true);
        appHashCheckCell.setOnClickListener(v -> appHashCheckCell.setChecked(!appHashCheckCell.isChecked()));
        cardFlags.addView(appHashCheckCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        flashcallCheckCell = new TextCheckCell(context);
        flashcallCheckCell.setTextAndCheck("Allow Flash Call Verification", VeyraLoginConfig.isAllowFlashcall(false), true);
        flashcallCheckCell.setOnClickListener(v -> flashcallCheckCell.setChecked(!flashcallCheckCell.isChecked()));
        cardFlags.addView(flashcallCheckCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        missedCallCheckCell = new TextCheckCell(context);
        missedCallCheckCell.setTextAndCheck("Allow Missed Call Verification", VeyraLoginConfig.isAllowMissedCall(false), false);
        missedCallCheckCell.setOnClickListener(v -> missedCallCheckCell.setChecked(!missedCallCheckCell.isChecked()));
        cardFlags.addView(missedCallCheckCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // ─── Action Buttons ───────────────────────────────────────────────────
        TextView saveButton = new TextView(context);
        saveButton.setText("Save & Apply Changes");
        saveButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        saveButton.setTypeface(AndroidUtilities.bold());
        saveButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        saveButton.setGravity(Gravity.CENTER);
        saveButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(14), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        saveButton.setOnClickListener(v -> save());
        rootLayout.addView(saveButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50, 16, 20, 16, 10));

        TextView resetButton = new TextView(context);
        resetButton.setText("Reset All to Device Defaults");
        resetButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        resetButton.setTypeface(AndroidUtilities.bold());
        resetButton.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
        resetButton.setGravity(Gravity.CENTER);
        resetButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(14), 0x1AE53935, 0x33E53935));
        resetButton.setOnClickListener(v -> showResetConfirmation());
        rootLayout.addView(resetButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, 16, 0, 16, 16));

        return fragmentView;
    }

    private void addPresetChips(Context context, LinearLayout parent) {
        HorizontalScrollView hScroll = new HorizontalScrollView(context);
        hScroll.setHorizontalScrollBarEnabled(false);
        hScroll.setClipToPadding(false);
        hScroll.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14), AndroidUtilities.dp(14));

        LinearLayout chipsContainer = new LinearLayout(context);
        chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
        hScroll.addView(chipsContainer);

        addChip(context, chipsContainer, "👑 Official Telegram", () -> {
            applyPreset("Google Pixel 9 Pro", "SDK 35", "11.8.0 (5200)", "com.android.vending", "org.telegram.messenger", 3, VeyraLoginConfig.OFFICIAL_CERT_FINGERPRINT);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Preset: Official Telegram").show();
        });

        addChip(context, chipsContainer, "📱 Samsung S24 Ultra", () -> {
            applyPreset("samsung SM-S928B", "SDK 34", "11.8.0 (5200)", "com.android.vending", "org.telegram.messenger", 3, VeyraLoginConfig.OFFICIAL_CERT_FINGERPRINT);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Preset: Samsung Galaxy S24 Ultra").show();
        });

        addChip(context, chipsContainer, "⚡ Pixel 9 Pro", () -> {
            applyPreset("Google Pixel 9 Pro", "SDK 35", "11.8.0 (5200)", "com.android.vending", "org.telegram.messenger", 3, VeyraLoginConfig.OFFICIAL_CERT_FINGERPRINT);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Preset: Google Pixel 9 Pro").show();
        });

        addChip(context, chipsContainer, "🚀 Xiaomi 14 Pro", () -> {
            applyPreset("Xiaomi 23116PN5BC", "SDK 34", "11.8.0 (5200)", "com.android.vending", "org.telegram.messenger", 3, VeyraLoginConfig.OFFICIAL_CERT_FINGERPRINT);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Preset: Xiaomi 14 Pro").show();
        });

        addChip(context, chipsContainer, "🔄 My Real Device", () -> {
            populateFieldsWithDefaults();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Preset: Actual Hardware").show();
        });

        parent.addView(hScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addChip(Context context, LinearLayout parent, String label, Runnable onClick) {
        TextView chip = new TextView(context);
        chip.setText(label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        chip.setTypeface(AndroidUtilities.bold());
        chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        chip.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_windowBackgroundWhite), Theme.getColor(Theme.key_listSelector)));
        chip.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));
        chip.setOnClickListener(v -> onClick.run());
        parent.addView(chip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
    }

    private void applyPreset(String model, String sysVer, String appVer, String installer, String pkgId, int perf, String cert) {
        if (deviceModelField != null) deviceModelField.setText(model);
        if (systemVersionField != null) systemVersionField.setText(sysVer);
        if (appVersionField != null) appVersionField.setText(appVer);
        if (installerField != null) installerField.setText(installer);
        if (packageIdField != null) packageIdField.setText(pkgId);
        if (perfCatField != null) perfCatField.setText(String.valueOf(perf));
        if (certFingerprintField != null) certFingerprintField.setText(cert);
    }

    private LinearLayout createCard(Context context, LinearLayout parent, String title, int iconRes) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_windowBackgroundWhite)));
        card.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));

        HeaderCell headerCell = new HeaderCell(context);
        headerCell.setText(title);
        card.addView(headerCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        parent.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 14, 0, 14, 14));
        return card;
    }

    private EditTextBoldCursor addField(Context context, LinearLayout parent, String hint, String initialValue, String subtitle, int inputType) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), AndroidUtilities.dp(10));

        EditTextBoldCursor field = new EditTextBoldCursor(context);
        field.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        field.setHint(hint);
        // Explicitly set Telegram's authentic hint color (never black!)
        field.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        field.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        field.setBackground(null);
        field.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        field.setCursorSize(AndroidUtilities.dp(20));
        field.setCursorWidth(1.5f);
        field.setSingleLine(true);
        field.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        field.setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        field.setTransformHintToHeader(true);
        field.setLineColors(
                Theme.getColor(Theme.key_windowBackgroundWhiteInputField),
                Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated),
                Theme.getColor(Theme.key_text_RedRegular)
        );
        field.setInputType(inputType);

        if (initialValue != null) {
            field.setText(initialValue);
        }

        container.addView(field, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 54));

        if (!TextUtils.isEmpty(subtitle)) {
            TextView subView = new TextView(context);
            subView.setText(subtitle);
            subView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            subView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            subView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
            container.addView(subView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        parent.addView(container, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return field;
    }

    private void save() {
        String deviceModel = deviceModelField.getText().toString().trim();
        String systemVersion = systemVersionField.getText().toString().trim();
        String appVersion = appVersionField.getText().toString().trim();
        String perfCat = perfCatField.getText().toString().trim();

        String certFingerprint = certFingerprintField.getText().toString().trim();
        String deviceToken = deviceTokenField.getText().toString().trim();

        String langCode = langCodeField.getText().toString().trim();
        String systemLangCode = systemLangCodeField.getText().toString().trim();
        String tzOffset = tzOffsetField.getText().toString().trim();
        String installer = installerField.getText().toString().trim();
        String packageId = packageIdField.getText().toString().trim();

        String apiId = apiIdField.getText().toString().trim();
        String apiHash = apiHashField.getText().toString().trim();

        boolean allowFirebase = firebaseCheckCell != null && firebaseCheckCell.isChecked();
        boolean allowAppHash = appHashCheckCell != null && appHashCheckCell.isChecked();
        boolean allowFlashcall = flashcallCheckCell != null && flashcallCheckCell.isChecked();
        boolean allowMissedCall = missedCallCheckCell != null && missedCallCheckCell.isChecked();

        String error = VeyraLoginConfig.validate(
                deviceModel, systemVersion, appVersion,
                langCode, systemLangCode, installer,
                packageId, tzOffset, perfCat,
                certFingerprint, deviceToken,
                apiId, apiHash
        );

        if (error != null) {
            BulletinFactory.of(this).createErrorBulletin(error).show();
            return;
        }

        VeyraLoginConfig.saveConfig(
                currentAccount,
                deviceModel, systemVersion, appVersion,
                langCode, systemLangCode, installer,
                packageId, tzOffset, perfCat,
                certFingerprint, deviceToken,
                apiId, apiHash,
                allowFirebase, allowAppHash,
                allowFlashcall, allowMissedCall
        );

        BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Connection parameters applied!").show();
        finishFragment();
    }

    private void showResetConfirmation() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Reset to Default");
        builder.setMessage("Are you sure you want to revert all connection and device parameters back to their defaults?");
        builder.setPositiveButton("Reset", (dialog, which) -> {
            VeyraLoginConfig.resetToDefault(currentAccount);
            populateFieldsWithDefaults();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Reverted to physical hardware defaults").show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void populateFieldsWithDefaults() {
        if (deviceModelField != null) deviceModelField.setText(VeyraLoginConfig.getDefaultDeviceModel());
        if (systemVersionField != null) systemVersionField.setText(VeyraLoginConfig.getDefaultSystemVersion());
        if (appVersionField != null) appVersionField.setText(VeyraLoginConfig.getDefaultAppVersion());
        if (perfCatField != null) perfCatField.setText(String.valueOf(VeyraLoginConfig.getDefaultPerfCat()));

        if (certFingerprintField != null) certFingerprintField.setText(VeyraLoginConfig.OFFICIAL_CERT_FINGERPRINT);
        if (deviceTokenField != null) deviceTokenField.setText("");

        if (langCodeField != null) langCodeField.setText(VeyraLoginConfig.getDefaultLangCode());
        if (systemLangCodeField != null) systemLangCodeField.setText(VeyraLoginConfig.getDefaultSystemLangCode());
        if (tzOffsetField != null) tzOffsetField.setText(String.valueOf(VeyraLoginConfig.getDefaultTzOffset()));
        if (installerField != null) installerField.setText(VeyraLoginConfig.getDefaultInstaller());
        if (packageIdField != null) packageIdField.setText(VeyraLoginConfig.getDefaultPackageId());

        if (apiIdField != null) apiIdField.setText("");
        if (apiHashField != null) apiHashField.setText("");

        if (firebaseCheckCell != null) firebaseCheckCell.setChecked(true);
        if (appHashCheckCell != null) appHashCheckCell.setChecked(true);
        if (flashcallCheckCell != null) flashcallCheckCell.setChecked(false);
        if (missedCallCheckCell != null) missedCallCheckCell.setChecked(false);
    }
}

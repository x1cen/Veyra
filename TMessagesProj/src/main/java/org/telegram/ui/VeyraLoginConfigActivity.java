/* Veyra Project — Login & Connection Parameters Configuration Screen */
package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
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
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

public class VeyraLoginConfigActivity extends BaseFragment {

    private static final int DONE_BUTTON = 1;

    private EditTextBoldCursor deviceModelField;
    private EditTextBoldCursor systemVersionField;
    private EditTextBoldCursor appVersionField;
    private EditTextBoldCursor langCodeField;
    private EditTextBoldCursor systemLangCodeField;
    private EditTextBoldCursor installerField;
    private EditTextBoldCursor packageIdField;
    private EditTextBoldCursor tzOffsetField;
    private EditTextBoldCursor perfCatField;
    private EditTextBoldCursor apiIdField;
    private EditTextBoldCursor apiHashField;

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

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT));

        // Info Banner
        TextInfoPrivacyCell topInfo = new TextInfoPrivacyCell(context);
        topInfo.setText("These parameters are sent to Telegram servers during initial connection and authentication (initConnection & sendCode). Sensitive credentials like API ID and Hash remain hidden by default.");
        linearLayout.addView(topInfo, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // ─── Section 1: Device & System ───────────────────────────────────────
        HeaderCell headerDevice = new HeaderCell(context);
        headerDevice.setText("Device & System Parameters");
        headerDevice.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        linearLayout.addView(headerDevice, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout container1 = createFieldsContainer(context);
        deviceModelField = addField(context, container1, "Device Model (e.g. samsung SM-S918B)", VeyraLoginConfig.getDeviceModel(), InputType.TYPE_CLASS_TEXT);
        systemVersionField = addField(context, container1, "System Version (e.g. SDK 34)", VeyraLoginConfig.getSystemVersion(), InputType.TYPE_CLASS_TEXT);
        appVersionField = addField(context, container1, "App Version (e.g. 11.8.0 (5200))", VeyraLoginConfig.getAppVersion(), InputType.TYPE_CLASS_TEXT);
        langCodeField = addField(context, container1, "App Language Code (e.g. en, fa)", VeyraLoginConfig.getLangCode(), InputType.TYPE_CLASS_TEXT);
        systemLangCodeField = addField(context, container1, "System Language Code (e.g. en)", VeyraLoginConfig.getSystemLangCode(), InputType.TYPE_CLASS_TEXT);
        linearLayout.addView(container1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // ─── Section 2: Package & Network ─────────────────────────────────────
        HeaderCell headerPackage = new HeaderCell(context);
        headerPackage.setText("Package & Network Parameters");
        headerPackage.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        linearLayout.addView(headerPackage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 0));

        LinearLayout container2 = createFieldsContainer(context);
        installerField = addField(context, container2, "Installer (e.g. com.android.vending)", VeyraLoginConfig.getInstaller(), InputType.TYPE_CLASS_TEXT);
        packageIdField = addField(context, container2, "Package ID (e.g. org.telegram.messenger)", VeyraLoginConfig.getPackageId(), InputType.TYPE_CLASS_TEXT);
        tzOffsetField = addField(context, container2, "Timezone Offset in Seconds (e.g. 12600)", String.valueOf(VeyraLoginConfig.getTzOffset()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        perfCatField = addField(context, container2, "Performance Class (1 to 3, or -1 for Auto)", String.valueOf(VeyraLoginConfig.getPerfCat()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        linearLayout.addView(container2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // ─── Section 3: Custom API Credentials ────────────────────────────────
        HeaderCell headerApi = new HeaderCell(context);
        headerApi.setText("Custom API Credentials (Optional)");
        headerApi.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        linearLayout.addView(headerApi, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 0));

        LinearLayout container3 = createFieldsContainer(context);
        apiIdField = addField(context, container3, "API ID (Leave empty for default)", VeyraLoginConfig.getCustomApiIdString(), InputType.TYPE_CLASS_NUMBER);
        apiHashField = addField(context, container3, "API Hash (Leave empty for default)", VeyraLoginConfig.getCustomApiHashString(), InputType.TYPE_CLASS_TEXT);
        linearLayout.addView(container3, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextInfoPrivacyCell apiInfo = new TextInfoPrivacyCell(context);
        apiInfo.setText("If left blank, official built-in Telegram API ID & Hash will be used securely without exposing them in this interface.");
        linearLayout.addView(apiInfo, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // ─── Buttons ──────────────────────────────────────────────────────────
        TextView saveButton = new TextView(context);
        saveButton.setText("Save Changes");
        saveButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        saveButton.setTypeface(AndroidUtilities.bold());
        saveButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        saveButton.setGravity(Gravity.CENTER);
        saveButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        saveButton.setOnClickListener(v -> save());
        linearLayout.addView(saveButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 16, 12, 16, 8));

        TextView resetButton = new TextView(context);
        resetButton.setText("Reset to Default");
        resetButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        resetButton.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
        resetButton.setGravity(Gravity.CENTER);
        resetButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), 0x1AE53935, 0x33E53935));
        resetButton.setOnClickListener(v -> showResetConfirmation());
        linearLayout.addView(resetButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 16, 4, 16, 32));

        return fragmentView;
    }

    private LinearLayout createFieldsContainer(Context context) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        return container;
    }

    private EditTextBoldCursor addField(Context context, LinearLayout parent, String hint, String initialValue, int inputType) {
        FrameLayout frameLayout = new FrameLayout(context);
        parent.addView(frameLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64, 16, 0, 16, 0));

        EditTextBoldCursor field = new EditTextBoldCursor(context);
        field.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        field.setHint(hint);
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
        field.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
        field.setInputType(inputType);
        if (initialValue != null) {
            field.setText(initialValue);
        }

        frameLayout.addView(field, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return field;
    }

    private void save() {
        String deviceModel = deviceModelField.getText().toString().trim();
        String systemVersion = systemVersionField.getText().toString().trim();
        String appVersion = appVersionField.getText().toString().trim();
        String langCode = langCodeField.getText().toString().trim();
        String systemLangCode = systemLangCodeField.getText().toString().trim();
        String installer = installerField.getText().toString().trim();
        String packageId = packageIdField.getText().toString().trim();
        String tzOffset = tzOffsetField.getText().toString().trim();
        String perfCat = perfCatField.getText().toString().trim();
        String apiId = apiIdField.getText().toString().trim();
        String apiHash = apiHashField.getText().toString().trim();

        String error = VeyraLoginConfig.validate(deviceModel, systemVersion, appVersion, langCode, systemLangCode, installer, packageId, tzOffset, perfCat, apiId, apiHash);
        if (error != null) {
            BulletinFactory.of(this).createErrorBulletin(error).show();
            return;
        }

        VeyraLoginConfig.saveConfig(currentAccount, deviceModel, systemVersion, appVersion, langCode, systemLangCode, installer, packageId, tzOffset, perfCat, apiId, apiHash);
        BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Connection parameters saved!").show();
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
            BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Reverted to defaults").show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void populateFieldsWithDefaults() {
        if (deviceModelField != null) deviceModelField.setText(VeyraLoginConfig.getDefaultDeviceModel());
        if (systemVersionField != null) systemVersionField.setText(VeyraLoginConfig.getDefaultSystemVersion());
        if (appVersionField != null) appVersionField.setText(VeyraLoginConfig.getDefaultAppVersion());
        if (langCodeField != null) langCodeField.setText(VeyraLoginConfig.getDefaultLangCode());
        if (systemLangCodeField != null) systemLangCodeField.setText(VeyraLoginConfig.getDefaultSystemLangCode());
        if (installerField != null) installerField.setText(VeyraLoginConfig.getDefaultInstaller());
        if (packageIdField != null) packageIdField.setText(VeyraLoginConfig.getDefaultPackageId());
        if (tzOffsetField != null) tzOffsetField.setText(String.valueOf(VeyraLoginConfig.getDefaultTzOffset()));
        if (perfCatField != null) perfCatField.setText(String.valueOf(VeyraLoginConfig.getDefaultPerfCat()));
        if (apiIdField != null) apiIdField.setText("");
        if (apiHashField != null) apiHashField.setText("");
    }
}

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
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

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
        lockEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        lockEditText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        lockEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        lockEditText.setGravity(Gravity.CENTER);
        lockEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        lockEditText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        lockEditText.setCursorSize(AndroidUtilities.dp(20));
        lockEditText.setCursorWidth(1.5f);
        content.addView(lockEditText, LayoutHelper.createLinear(200, 44, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

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

        // Category 1: Privacy & Security
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraPrivacySecurity", R.string.VeyraPrivacySecurity),
                LocaleController.getString("VeyraPrivacySecurityDesc", R.string.VeyraPrivacySecurityDesc),
                () -> presentFragment(new VeyraPrivacySettingsActivity())
        ));

        // Category 2: Chat List
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraChatList", R.string.VeyraChatList),
                LocaleController.getString("VeyraChatListDesc", R.string.VeyraChatListDesc),
                () -> presentFragment(new VeyraChatListSettingsActivity())
        ));

        // Category 3: Composing & Messages
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraComposingMessages", R.string.VeyraComposingMessages),
                LocaleController.getString("VeyraComposingMessagesDesc", R.string.VeyraComposingMessagesDesc),
                () -> presentFragment(new VeyraComposingSettingsActivity())
        ));

        // Category 4: Media & Camera
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraMediaCamera", R.string.VeyraMediaCamera),
                LocaleController.getString("VeyraMediaCameraDesc", R.string.VeyraMediaCameraDesc),
                () -> presentFragment(new VeyraMediaSettingsActivity())
        ));

        // Category 5: Controls & General
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraControlsGeneral", R.string.VeyraControlsGeneral),
                LocaleController.getString("VeyraControlsGeneralDesc", R.string.VeyraControlsGeneralDesc),
                () -> presentFragment(new VeyraControlsSettingsActivity())
        ));

        // Category 6: Backup & Restore
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
                "1.0.4-beta3",
                true
        ));

        r.add(VeyraSettingsRow.button(
                "GitHub: x1cen/Veyra",
                false, false,
                () -> Browser.openUrl(getParentActivity(), "https://github.com/x1cen/Veyra")
        ));

        r.add(VeyraSettingsRow.shadow());

        return r;
    }
}

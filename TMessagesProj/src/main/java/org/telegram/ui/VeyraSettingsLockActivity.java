package org.telegram.ui;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VeyraConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

public class VeyraSettingsLockActivity extends BaseFragment {

    private static final int done_button = 1;

    private EditTextBoldCursor passwordEditText;
    private TextView titleTextView;
    private TextView descriptionTextView;
    private ActionBarMenuItem doneItem;

    private int state = 0; // 0 = enter current (if locked), 1 = enter new, 2 = confirm new
    private String firstEnteredCode = "";

    public interface UnlockCallback {
        void onUnlocked();
    }

    private UnlockCallback unlockCallback;

    public VeyraSettingsLockActivity() {
        super();
    }

    public VeyraSettingsLockActivity(UnlockCallback callback) {
        super();
        this.unlockCallback = callback;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        if (unlockCallback != null) {
            state = 0; // enter current code to unlock
        } else if (VeyraConfig.hasSettingsLock()) {
            state = 0; // enter current code before changing
        } else {
            state = 1; // enter new code
        }
        return true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(false);
        actionBar.setTitle(LocaleController.getString("VeyraSettingsLock", R.string.VeyraSettingsLock));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    processDone();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        doneItem = menu.addItem(done_button, R.drawable.ic_ab_done);

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        fragmentView = frameLayout;

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        frameLayout.addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 24, 40, 24, 0));

        titleTextView = new TextView(context);
        titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        linearLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 10));

        descriptionTextView = new TextView(context);
        descriptionTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        descriptionTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        descriptionTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        linearLayout.addView(descriptionTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 24));

        passwordEditText = new EditTextBoldCursor(context);
        passwordEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        passwordEditText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        passwordEditText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        passwordEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        passwordEditText.setGravity(Gravity.CENTER);
        passwordEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        passwordEditText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        passwordEditText.setCursorSize(AndroidUtilities.dp(20));
        passwordEditText.setCursorWidth(1.5f);
        linearLayout.addView(passwordEditText, LayoutHelper.createLinear(200, 44, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 0));

        passwordEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                processDone();
                return true;
            }
            return false;
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });

        updateUI();

        return fragmentView;
    }

    private void updateUI() {
        boolean isFarsi = "fa".equals(LocaleController.getInstance().getCurrentLocale().getLanguage());
        if (state == 0) {
            titleTextView.setText(isFarsi ? "رمز فعلی را وارد کنید" : "Enter Current Code");
            descriptionTextView.setText(isFarsi ? "برای دسترسی به تنظیمات ویرا، رمز خود را وارد کنید" : "Enter your code to access Veyra Settings");
        } else if (state == 1) {
            titleTextView.setText(isFarsi ? "رمز جدید را وارد کنید" : "Enter New Code");
            descriptionTextView.setText(isFarsi ? "یک پین عددی برای قفل کردن تنظیمات ویرا تعیین کنید" : "Set a numeric PIN to protect Veyra Settings");
        } else if (state == 2) {
            titleTextView.setText(isFarsi ? "تأیید رمز جدید" : "Confirm New Code");
            descriptionTextView.setText(isFarsi ? "رمز جدید را دوباره وارد نمایید" : "Re-enter the new code to confirm");
        }
        if (passwordEditText != null) {
            passwordEditText.setText("");
            passwordEditText.requestFocus();
            AndroidUtilities.showKeyboard(passwordEditText);
        }
    }

    private void processDone() {
        if (passwordEditText == null) return;
        String code = passwordEditText.getText().toString();
        if (code.length() < 4) {
            AndroidUtilities.shakeView(passwordEditText);
            return;
        }

        boolean isFarsi = "fa".equals(LocaleController.getInstance().getCurrentLocale().getLanguage());

        if (state == 0) {
            if (VeyraConfig.checkSettingsLockCode(code)) {
                if (unlockCallback != null) {
                    finishFragment();
                    unlockCallback.onUnlocked();
                } else {
                    state = 1;
                    updateUI();
                }
            } else {
                AndroidUtilities.shakeView(passwordEditText);
                BulletinFactory.of(VeyraSettingsLockActivity.this).createErrorBulletin(isFarsi ? "رمز نادرست است" : "Wrong code").show();
            }
        } else if (state == 1) {
            firstEnteredCode = code;
            state = 2;
            updateUI();
        } else if (state == 2) {
            if (code.equals(firstEnteredCode)) {
                try {
                    byte[] salt = new byte[16];
                    Utilities.random.nextBytes(salt);
                    String saltStr = Base64.encodeToString(salt, Base64.DEFAULT);
                    String hash = Utilities.MD5(saltStr + code);
                    VeyraConfig.setSettingsLock(hash, saltStr);
                    finishFragment();
                    BulletinFactory.of(VeyraSettingsLockActivity.this).createSimpleBulletin(R.raw.chats_infotip, isFarsi ? "قفل تنظیمات ویرا با موفقیت ذخیره شد" : "Settings lock saved successfully").show();
                } catch (Exception e) {
                    FileLog.e(e);
                    finishFragment();
                }
            } else {
                AndroidUtilities.shakeView(passwordEditText);
                BulletinFactory.of(VeyraSettingsLockActivity.this).createErrorBulletin(isFarsi ? "رمزها مطابقت ندارند" : "Codes do not match").show();
                state = 1;
                firstEnteredCode = "";
                updateUI();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (passwordEditText != null) {
            passwordEditText.postDelayed(() -> {
                if (passwordEditText != null) {
                    passwordEditText.requestFocus();
                    AndroidUtilities.showKeyboard(passwordEditText);
                }
            }, 200);
        }
    }
}

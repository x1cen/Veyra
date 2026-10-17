package org.telegram.ui;

import java.util.ArrayList;
import java.util.List;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.OutlineTextContainerView;
import android.util.TypedValue;
import android.view.Gravity;

public class VeyraChatListSettingsActivity extends VeyraSettingsBaseActivity {

    @Override
    protected String getScreenTitle() {
        return LocaleController.getString("VeyraChatList", R.string.VeyraChatList);
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();
        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraChatList", R.string.VeyraChatList)));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraDisableGlobalSearch", R.string.VeyraDisableGlobalSearch),
                LocaleController.getString("VeyraDisableGlobalSearchDesc", R.string.VeyraDisableGlobalSearchDesc),
                () -> VeyraConfig.disableGlobalSearch,
                v -> VeyraConfig.setDisableGlobalSearch(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraDisableThumbsInDialogList", R.string.VeyraDisableThumbsInDialogList),
                LocaleController.getString("VeyraDisableThumbsInDialogListDesc", R.string.VeyraDisableThumbsInDialogListDesc),
                () -> VeyraConfig.disableThumbsInDialogList,
                v -> {
                    VeyraConfig.setDisableThumbsInDialogList(v);
                    if (listView != null) {
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
                    }
                },
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraEnableLastSeenDots", R.string.VeyraEnableLastSeenDots),
                LocaleController.getString("VeyraEnableLastSeenDotsDesc", R.string.VeyraEnableLastSeenDotsDesc),
                () -> VeyraConfig.enableLastSeenDots,
                v -> {
                    VeyraConfig.setEnableLastSeenDots(v);
                    if (listView != null) {
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
                    }
                },
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraSortByUnread", R.string.VeyraSortByUnread),
                LocaleController.getString("VeyraSortByUnreadDesc", R.string.VeyraSortByUnreadDesc),
                () -> VeyraConfig.sortByUnread,
                v -> {
                    VeyraConfig.setSortByUnread(v);
                    MessagesController.getInstance(currentAccount).sortDialogs(null);
                },
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraSortByUnmuted", R.string.VeyraSortByUnmuted),
                LocaleController.getString("VeyraSortByUnmutedDesc", R.string.VeyraSortByUnmutedDesc),
                () -> VeyraConfig.sortByUnmuted,
                v -> {
                    VeyraConfig.setSortByUnmuted(v);
                    MessagesController.getInstance(currentAccount).sortDialogs(null);
                },
                false
        ));

        // Veyra: custom header title (static text)
        r.add(VeyraSettingsRow.detail(
                LocaleController.getString("VeyraCustomHeaderTitle", R.string.VeyraCustomHeaderTitle),
                () -> android.text.TextUtils.isEmpty(VeyraConfig.customHeaderTitle)
                        ? LocaleController.getString("Default", R.string.Default)
                        : VeyraConfig.customHeaderTitle,
                false,
                this::showCustomHeaderTitleDialog
        ));

        r.add(VeyraSettingsRow.shadow());
        return r;
    }

    private void showCustomHeaderTitleDialog() {
        if (getParentActivity() == null) return;
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("VeyraCustomHeaderTitle", R.string.VeyraCustomHeaderTitle));

        Theme.ResourcesProvider resourcesProvider = new Theme.ResourcesProvider() {
            @Override
            public int getColor(int key) {
                if (key == Theme.key_windowBackgroundWhiteInputField || key == Theme.key_windowBackgroundWhiteInputFieldActivated) {
                    int c = Theme.getColor(Theme.key_windowBackgroundWhiteInputField);
                    return c != 0 ? c : 0x55808080;
                }
                return Theme.getColor(key);
            }
        };

        OutlineTextContainerView outlineView = new OutlineTextContainerView(getParentActivity(), resourcesProvider);
        outlineView.setText(LocaleController.getString("VeyraCustomHeaderTitle", R.string.VeyraCustomHeaderTitle));

        final EditTextBoldCursor editText = new EditTextBoldCursor(getParentActivity());
        editText.setText(VeyraConfig.customHeaderTitle);
        editText.setSelection(editText.getText().length());
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setCursorWidth(1.5f);
        editText.setBackground(null);
        editText.setSingleLine(true);
        editText.setMaxLines(1);
        editText.setLines(1);
        editText.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        editText.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
        outlineView.attachEditText(editText);
        outlineView.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        boolean hasInitialText = !android.text.TextUtils.isEmpty(VeyraConfig.customHeaderTitle);
        outlineView.animateSelection(false, hasInitialText, false);

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            outlineView.animateSelection(hasFocus, hasFocus || editText.length() > 0);
        });
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                outlineView.animateSelection(editText.hasFocus(), editText.hasFocus() || s.length() > 0);
            }
        });

        android.widget.FrameLayout container = new android.widget.FrameLayout(getParentActivity());
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(12));
        container.addView(outlineView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 58));
        builder.setView(container);

        // Save custom text
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            VeyraConfig.setCustomHeaderTitle(editText.getText().toString().trim());
            reloadRows();
        });

        // Use profile name
        builder.setNeutralButton(LocaleController.getString("VeyraUseProfileName", R.string.VeyraUseProfileName), (dialog, which) -> {
            org.telegram.tgnet.TLRPC.User me = org.telegram.messenger.UserConfig.getInstance(currentAccount).getCurrentUser();
            if (me != null && !android.text.TextUtils.isEmpty(me.first_name)) {
                String name = me.first_name + (android.text.TextUtils.isEmpty(me.last_name) ? "" : " " + me.last_name);
                VeyraConfig.setCustomHeaderTitle(name.trim());
            } else {
                VeyraConfig.setCustomHeaderTitle("");
            }
            reloadRows();
        });

        // Cancel / reset to default (empty = use Telegram logo)
        builder.setNegativeButton(LocaleController.getString(R.string.Reset), (dialog, which) -> {
            VeyraConfig.setCustomHeaderTitle("");
            reloadRows();
        });

        builder.show();
    }
}

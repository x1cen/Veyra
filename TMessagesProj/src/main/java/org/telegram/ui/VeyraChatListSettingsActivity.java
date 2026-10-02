package org.telegram.ui;

import java.util.ArrayList;
import java.util.List;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;
import org.telegram.ui.Components.LayoutHelper;

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

        r.add(VeyraSettingsRow.detail(
                LocaleController.getString("VeyraCustomHeaderTitle", R.string.VeyraCustomHeaderTitle),
                () -> android.text.TextUtils.isEmpty(VeyraConfig.customHeaderTitle) ? LocaleController.getString("Default", R.string.Default) : VeyraConfig.customHeaderTitle,
                false,
                this::showCustomHeaderTitleDialog
        ));

        r.add(VeyraSettingsRow.detail(
                LocaleController.getString("VeyraAnimatedTitle", R.string.VeyraAnimatedTitle),
                () -> animatedTitleLabel(VeyraConfig.animatedTitleMode),
                false,
                this::showAnimatedTitleDialog
        ));

        r.add(VeyraSettingsRow.shadow());
        return r;
    }

    private void showCustomHeaderTitleDialog() {
        if (getParentActivity() == null) return;
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("VeyraCustomHeaderTitle", R.string.VeyraCustomHeaderTitle));
        builder.setMessage(LocaleController.getString("VeyraCustomHeaderTitleDialog", R.string.VeyraCustomHeaderTitleDialog));

        final android.widget.EditText editText = new android.widget.EditText(getParentActivity());
        editText.setText(VeyraConfig.customHeaderTitle);
        editText.setSelection(editText.getText().length());
        editText.setSingleLine(true);
        android.widget.FrameLayout container = new android.widget.FrameLayout(getParentActivity());
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8), AndroidUtilities.dp(20), AndroidUtilities.dp(8));
        container.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(container);

        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            VeyraConfig.setCustomHeaderTitle(editText.getText().toString());
            reloadRows();
        });
        builder.setNeutralButton(LocaleController.getString(R.string.Reset), (dialog, which) -> {
            VeyraConfig.setCustomHeaderTitle("");
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private String animatedTitleLabel(int mode) {
        switch (mode) {
            case 1:
                return LocaleController.getString("VeyraAnimatedTitleLightning", R.string.VeyraAnimatedTitleLightning);
            case 2:
                return LocaleController.getString("VeyraAnimatedTitleRain", R.string.VeyraAnimatedTitleRain);
            case 3:
                return LocaleController.getString("VeyraAnimatedTitleFireworks", R.string.VeyraAnimatedTitleFireworks);
            case 4:
                return LocaleController.getString("VeyraAnimatedTitleMeteors", R.string.VeyraAnimatedTitleMeteors);
            case 5:
                return LocaleController.getString("VeyraAnimatedTitleMatrix", R.string.VeyraAnimatedTitleMatrix);
            case 6:
                return LocaleController.getString("VeyraAnimatedTitleGlitch", R.string.VeyraAnimatedTitleGlitch);
            case 7:
                return LocaleController.getString("VeyraAnimatedTitleSnow", R.string.VeyraAnimatedTitleSnow);
            case 8:
                return LocaleController.getString("VeyraAnimatedTitleFire", R.string.VeyraAnimatedTitleFire);
            case 9:
                return LocaleController.getString("VeyraAnimatedTitleAurora", R.string.VeyraAnimatedTitleAurora);
            default:
                return LocaleController.getString("VeyraAnimatedTitleOff", R.string.VeyraAnimatedTitleOff);
        }
    }

    private void showAnimatedTitleDialog() {
        if (getParentActivity() == null) return;
        CharSequence[] options = new CharSequence[]{
                animatedTitleLabel(0),
                animatedTitleLabel(1),
                animatedTitleLabel(2),
                animatedTitleLabel(3),
                animatedTitleLabel(4),
                animatedTitleLabel(5),
                animatedTitleLabel(6),
                animatedTitleLabel(7),
                animatedTitleLabel(8),
                animatedTitleLabel(9)
        };
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("VeyraAnimatedTitle", R.string.VeyraAnimatedTitle));
        builder.setItems(options, (dialog, which) -> {
            VeyraConfig.setAnimatedTitleMode(which);
            reloadRows();
        });
        builder.show();
    }
}

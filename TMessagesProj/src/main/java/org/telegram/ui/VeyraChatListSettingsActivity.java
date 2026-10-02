package org.telegram.ui;

import java.util.ArrayList;
import java.util.List;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;

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
                LocaleController.getString("VeyraAnimatedTitle", R.string.VeyraAnimatedTitle),
                () -> animatedTitleLabel(VeyraConfig.animatedTitleMode),
                false,
                this::showAnimatedTitleDialog
        ));

        r.add(VeyraSettingsRow.shadow());
        return r;
    }

    private String animatedTitleLabel(int mode) {
        switch (mode) {
            case 1:
                return LocaleController.getString("VeyraAnimatedTitlePulse", R.string.VeyraAnimatedTitlePulse);
            case 2:
                return LocaleController.getString("VeyraAnimatedTitleWave", R.string.VeyraAnimatedTitleWave);
            case 3:
                return LocaleController.getString("VeyraAnimatedTitleGlow", R.string.VeyraAnimatedTitleGlow);
            default:
                return LocaleController.getString("VeyraAnimatedTitleOff", R.string.VeyraAnimatedTitleOff);
        }
    }

    private void showAnimatedTitleDialog() {
        if (getParentActivity() == null) return;
        CharSequence[] options = new CharSequence[]{
                animatedTitleLabel(0), animatedTitleLabel(1), animatedTitleLabel(2), animatedTitleLabel(3)
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

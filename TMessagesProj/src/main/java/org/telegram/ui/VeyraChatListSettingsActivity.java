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

        r.add(VeyraSettingsRow.shadow());
        return r;
    }
}

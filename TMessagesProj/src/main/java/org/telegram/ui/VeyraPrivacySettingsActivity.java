package org.telegram.ui;

import java.util.ArrayList;
import java.util.List;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;
import org.telegram.ui.Components.BulletinFactory;

public class VeyraPrivacySettingsActivity extends VeyraSettingsBaseActivity {

    @Override
    protected String getScreenTitle() {
        return LocaleController.getString("VeyraPrivacySecurity", R.string.VeyraPrivacySecurity);
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();
        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraPrivacySecurity", R.string.VeyraPrivacySecurity)));
        r.add(VeyraSettingsRow.detail(
                LocaleController.getString("VeyraOnlineStatus", R.string.VeyraOnlineStatus),
                () -> onlineModeLabel(VeyraConfig.onlineMode),
                true,
                this::showOnlineModeDialog
        ));
        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraReadOnReply", R.string.VeyraReadOnReply),
                LocaleController.getString("VeyraReadOnReplyDesc", R.string.VeyraReadOnReplyDesc),
                () -> VeyraConfig.readOnReply, v -> VeyraConfig.setReadOnReply(v), true
        ));
        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraAntiDelete", R.string.VeyraAntiDelete),
                LocaleController.getString("VeyraAntiDeleteDesc", R.string.VeyraAntiDeleteDesc),
                () -> VeyraConfig.antiDelete, v -> VeyraConfig.setAntiDelete(v), true
        ));
        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraGhostMode", R.string.VeyraGhostMode),
                LocaleController.getString("VeyraGhostModeDesc", R.string.VeyraGhostModeDesc),
                () -> VeyraConfig.ghostMode, v -> VeyraConfig.setGhostMode(v), true
        ));
        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraHideTyping", R.string.VeyraHideTyping),
                LocaleController.getString("VeyraHideTypingDesc", R.string.VeyraHideTypingDesc),
                () -> VeyraConfig.hideTyping, v -> VeyraConfig.setHideTyping(v), true
        ));
        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraBlockSecretChat", R.string.VeyraBlockSecretChat),
                LocaleController.getString("VeyraBlockSecretChatDesc", R.string.VeyraBlockSecretChatDesc),
                () -> VeyraConfig.blockSecretChat, v -> VeyraConfig.setBlockSecretChat(v), true
        ));
        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraHideConnectingToProxy", R.string.VeyraHideConnectingToProxy),
                LocaleController.getString("VeyraHideConnectingToProxyDesc", R.string.VeyraHideConnectingToProxyDesc),
                () -> VeyraConfig.hideConnectingToProxy, v -> VeyraConfig.setHideConnectingToProxy(v), false
        ));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraSettingsLock", R.string.VeyraSettingsLock)));
        r.add(VeyraSettingsRow.detail(
                VeyraConfig.hasSettingsLock()
                        ? LocaleController.getString("VeyraSettingsLockChange", R.string.VeyraSettingsLockChange)
                        : LocaleController.getString("VeyraSettingsLockSet", R.string.VeyraSettingsLockSet),
                () -> VeyraConfig.hasSettingsLock()
                        ? LocaleController.getString("VeyraSettingsLockOn", R.string.VeyraSettingsLockOn)
                        : LocaleController.getString("VeyraSettingsLockOff", R.string.VeyraSettingsLockOff),
                !VeyraConfig.hasSettingsLock(),
                () -> presentFragment(new VeyraSettingsLockActivity())
        ));
        if (VeyraConfig.hasSettingsLock()) {
            r.add(VeyraSettingsRow.button(
                    LocaleController.getString("VeyraSettingsLockRemove", R.string.VeyraSettingsLockRemove),
                    true, false,
                    () -> {
                        VeyraConfig.clearSettingsLock();
                        reloadRows();
                        BulletinFactory.of(VeyraPrivacySettingsActivity.this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString("VeyraSettingsLockRemoved", R.string.VeyraSettingsLockRemoved)).show();
                    }
            ));
        }
        r.add(VeyraSettingsRow.shadow());
        return r;
    }

    private String onlineModeLabel(int mode) {
        switch (mode) {
            case 1:
                return LocaleController.getString("VeyraOnlineModeHide", R.string.VeyraOnlineModeHide);
            case 2:
                return LocaleController.getString("VeyraOnlineModeHideAfterMsg", R.string.VeyraOnlineModeHideAfterMsg);
            case 3:
                return LocaleController.getString("VeyraOnlineModeAlways", R.string.VeyraOnlineModeAlways);
            default:
                return LocaleController.getString("VeyraOnlineModeDefault", R.string.VeyraOnlineModeDefault);
        }
    }

    private void showOnlineModeDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] options = new CharSequence[]{
                onlineModeLabel(0), onlineModeLabel(1), onlineModeLabel(2), onlineModeLabel(3)
        };
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("VeyraOnlineStatus", R.string.VeyraOnlineStatus));
        builder.setItems(options, (dialog, which) -> {
            VeyraConfig.setOnlineMode(which);
            MessagesController.getInstance(currentAccount).updateOnlineStatus();
            reloadRows();
        });
        builder.show();
    }
}

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
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraAntiDelete", R.string.VeyraAntiDelete),
                LocaleController.getString("VeyraAntiDeleteDesc", R.string.VeyraAntiDeleteDesc),
                () -> presentFragment(new VeyraAntiDeleteSettingsActivity())
        ));
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraEditHistory", R.string.VeyraEditHistory),
                LocaleController.getString("VeyraEditHistoryDesc", R.string.VeyraEditHistoryDesc),
                () -> presentFragment(new VeyraEditHistorySettingsActivity())
        ));
        r.add(VeyraSettingsRow.category(
                LocaleController.getString("VeyraReactionHistory", R.string.VeyraReactionHistory),
                LocaleController.getString("VeyraReactionHistoryDesc", R.string.VeyraReactionHistoryDesc),
                () -> presentFragment(new VeyraReactionHistorySettingsActivity())
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
        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraBlockScreenCapture", R.string.VeyraBlockScreenCapture),
                LocaleController.getString("VeyraBlockScreenCaptureDesc", R.string.VeyraBlockScreenCaptureDesc),
                () -> VeyraConfig.blockScreenCapture, v -> {
                    VeyraConfig.setBlockScreenCapture(v);
                    org.veyra.client.VeyraSecurityGuard.applyWindowSecurity(getParentActivity());
                }, true
        ));
        r.add(VeyraSettingsRow.shadow());
        return r;
    }
}

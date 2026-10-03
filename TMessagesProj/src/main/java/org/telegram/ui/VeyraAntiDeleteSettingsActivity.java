package org.telegram.ui;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;

import java.util.ArrayList;
import java.util.List;

public class VeyraAntiDeleteSettingsActivity extends VeyraSettingsBaseActivity {

    @Override
    protected String getScreenTitle() {
        return LocaleController.getString("VeyraAntiDelete", R.string.VeyraAntiDelete);
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();

        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraAntiDelete", R.string.VeyraAntiDelete)));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraAntiDeleteEnable", R.string.VeyraAntiDeleteEnable),
                LocaleController.getString("VeyraAntiDeleteEnableDesc", R.string.VeyraAntiDeleteEnableDesc),
                () -> VeyraConfig.antiDelete,
                v -> VeyraConfig.setAntiDelete(v),
                true
        ));

        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraChatTypes", R.string.VeyraChatTypes)));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraChatTypePrivate", R.string.VeyraChatTypePrivate),
                LocaleController.getString("VeyraChatTypePrivateDesc", R.string.VeyraChatTypePrivateDesc),
                () -> VeyraConfig.antiDeletePrivate,
                v -> VeyraConfig.setAntiDeletePrivate(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraChatTypeGroups", R.string.VeyraChatTypeGroups),
                LocaleController.getString("VeyraChatTypeGroupsDesc", R.string.VeyraChatTypeGroupsDesc),
                () -> VeyraConfig.antiDeleteGroups,
                v -> VeyraConfig.setAntiDeleteGroups(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraChatTypeChannels", R.string.VeyraChatTypeChannels),
                LocaleController.getString("VeyraChatTypeChannelsDesc", R.string.VeyraChatTypeChannelsDesc),
                () -> VeyraConfig.antiDeleteChannels,
                v -> VeyraConfig.setAntiDeleteChannels(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraChatTypeBots", R.string.VeyraChatTypeBots),
                LocaleController.getString("VeyraChatTypeBotsDesc", R.string.VeyraChatTypeBotsDesc),
                () -> VeyraConfig.antiDeleteBots,
                v -> VeyraConfig.setAntiDeleteBots(v),
                true
        ));

        r.add(VeyraSettingsRow.shadow());

        return r;
    }
}

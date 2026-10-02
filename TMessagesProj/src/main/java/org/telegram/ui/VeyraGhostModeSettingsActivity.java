package org.telegram.ui;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;

import java.util.ArrayList;
import java.util.List;

public class VeyraGhostModeSettingsActivity extends VeyraSettingsBaseActivity {

    @Override
    protected String getScreenTitle() {
        return LocaleController.getString("VeyraGhostMode", R.string.VeyraGhostMode);
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();

        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraGhostMode", R.string.VeyraGhostMode)));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraGhostHideOnline", R.string.VeyraGhostHideOnline),
                LocaleController.getString("VeyraGhostHideOnlineDesc", R.string.VeyraGhostHideOnlineDesc),
                () -> VeyraConfig.ghostHideOnline,
                v -> VeyraConfig.setGhostHideOnline(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraGhostHideTyping", R.string.VeyraGhostHideTyping),
                LocaleController.getString("VeyraGhostHideTypingDesc", R.string.VeyraGhostHideTypingDesc),
                () -> VeyraConfig.ghostHideTyping,
                v -> VeyraConfig.setGhostHideTyping(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraGhostHideRead", R.string.VeyraGhostHideRead),
                LocaleController.getString("VeyraGhostHideReadDesc", R.string.VeyraGhostHideReadDesc),
                () -> VeyraConfig.ghostHideRead,
                v -> VeyraConfig.setGhostHideRead(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraGhostHideReadContents", R.string.VeyraGhostHideReadContents),
                LocaleController.getString("VeyraGhostHideReadContentsDesc", R.string.VeyraGhostHideReadContentsDesc),
                () -> VeyraConfig.ghostHideReadContents,
                v -> VeyraConfig.setGhostHideReadContents(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraGhostHideStories", R.string.VeyraGhostHideStories),
                LocaleController.getString("VeyraGhostHideStoriesDesc", R.string.VeyraGhostHideStoriesDesc),
                () -> VeyraConfig.ghostHideStories,
                v -> VeyraConfig.setGhostHideStories(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraReadOnReply", R.string.VeyraReadOnReply),
                LocaleController.getString("VeyraReadOnReplyDesc", R.string.VeyraReadOnReplyDesc),
                () -> VeyraConfig.ghostReadOnReply,
                v -> VeyraConfig.setGhostReadOnReply(v),
                true
        ));

        r.add(VeyraSettingsRow.shadow());

        return r;
    }
}

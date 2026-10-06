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

        // Section 1: Master Control
        r.add(VeyraSettingsRow.header("Master Control"));
        r.add(VeyraSettingsRow.toggle(
                "Enable Ghost Mode",
                "Master switch for all stealth and privacy features across the app",
                () -> VeyraConfig.ghostMode,
                v -> VeyraConfig.setGhostMode(v),
                false
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 2: Online & Activity Status
        r.add(VeyraSettingsRow.header("Online & Activity"));
        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraGhostHideOnline", R.string.VeyraGhostHideOnline),
                "Tap to configure chat types and exceptions",
                () -> VeyraConfig.ghostHideOnline,
                v -> VeyraConfig.setGhostHideOnline(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_ONLINE))
        ));
        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraGhostHideTyping", R.string.VeyraGhostHideTyping),
                "Tap to configure chat types and exceptions",
                () -> VeyraConfig.ghostHideTyping,
                v -> VeyraConfig.setGhostHideTyping(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_TYPING))
        ));
        r.add(VeyraSettingsRow.toggleWithAction(
                "Hide Media Uploading",
                "Tap to configure chat types and exceptions",
                () -> VeyraConfig.ghostHideUpload,
                v -> VeyraConfig.setGhostHideUpload(v),
                false,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_UPLOAD))
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 3: Read Receipts
        r.add(VeyraSettingsRow.header("Read Receipts"));
        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraGhostHideRead", R.string.VeyraGhostHideRead),
                "Tap to configure chat types, reply reads and exceptions",
                () -> VeyraConfig.ghostHideRead,
                v -> VeyraConfig.setGhostHideRead(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_READ))
        ));
        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraReadOnReply", R.string.VeyraReadOnReply),
                LocaleController.getString("VeyraReadOnReplyDesc", R.string.VeyraReadOnReplyDesc),
                () -> VeyraConfig.ghostReadOnReply,
                v -> VeyraConfig.setGhostReadOnReply(v),
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
                "Hide Secret Chat Read Receipts",
                "Prevent read confirmations and self-destruct timers in secret chats",
                () -> VeyraConfig.ghostHideSecretRead,
                v -> VeyraConfig.setGhostHideSecretRead(v),
                false
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 4: Stories & Channels
        r.add(VeyraSettingsRow.header("Stories & Channels"));
        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraGhostHideStories", R.string.VeyraGhostHideStories),
                "Tap to configure chat types and exceptions",
                () -> VeyraConfig.ghostHideStories,
                v -> VeyraConfig.setGhostHideStories(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_STORIES))
        ));
        r.add(VeyraSettingsRow.toggleWithAction(
                "Anonymous Channel Browsing",
                "Tap to configure chat types and exceptions",
                () -> VeyraConfig.ghostHideChannelViews,
                v -> VeyraConfig.setGhostHideChannelViews(v),
                false,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS))
        ));
        r.add(VeyraSettingsRow.shadow());

        return r;
    }
}

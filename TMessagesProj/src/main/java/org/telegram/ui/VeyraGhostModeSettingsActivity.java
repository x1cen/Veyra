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

        // Section 2: Chat-Specific Stealth Rules & Exceptions
        r.add(VeyraSettingsRow.header("Chat Stealth Rules & Exceptions"));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraGhostHideRead", R.string.VeyraGhostHideRead),
                "Configure chat types (Private, Groups, Channels) and exceptions",
                () -> VeyraConfig.ghostHideRead,
                v -> VeyraConfig.setGhostHideRead(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_READ))
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraGhostHideTyping", R.string.VeyraGhostHideTyping),
                "Configure chat types (Private, Groups) and exceptions",
                () -> VeyraConfig.ghostHideTyping,
                v -> VeyraConfig.setGhostHideTyping(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_TYPING))
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                "Hide Media Uploading",
                "Configure chat types (Private, Groups) and exceptions",
                () -> VeyraConfig.ghostHideUpload,
                v -> VeyraConfig.setGhostHideUpload(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_UPLOAD))
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraGhostHideStories", R.string.VeyraGhostHideStories),
                "Configure users, channels and exceptions",
                () -> VeyraConfig.ghostHideStories,
                v -> VeyraConfig.setGhostHideStories(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_STORIES))
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                "Anonymous Channel Browsing",
                "Prevent channel view counter increments; manage channel exceptions",
                () -> VeyraConfig.ghostHideChannelViews,
                v -> VeyraConfig.setGhostHideChannelViews(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS))
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                "Secret Chat Read Receipts",
                "Prevent read confirmations in secret chats; manage exceptions",
                () -> VeyraConfig.ghostHideSecretRead,
                v -> VeyraConfig.setGhostHideSecretRead(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_SECRET_READ))
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraGhostHideReadContents", R.string.VeyraGhostHideReadContents),
                "Hide voice message and video note read confirmations; manage exceptions",
                () -> VeyraConfig.ghostHideReadContents,
                v -> VeyraConfig.setGhostHideReadContents(v),
                false,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_HIDE_CONTENTS))
        ));

        r.add(VeyraSettingsRow.shadow());

        // Section 3: Global Account Stealth Rules
        r.add(VeyraSettingsRow.header("Global Account Stealth"));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraGhostHideOnline", R.string.VeyraGhostHideOnline),
                "Prevent sending online presence status to Telegram servers",
                () -> VeyraConfig.ghostHideOnline,
                v -> VeyraConfig.setGhostHideOnline(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_ONLINE))
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraReadOnReply", R.string.VeyraReadOnReply),
                LocaleController.getString("VeyraReadOnReplyDesc", R.string.VeyraReadOnReplyDesc),
                () -> VeyraConfig.ghostReadOnReply,
                v -> VeyraConfig.setGhostReadOnReply(v),
                false,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_READ_ON_REPLY))
        ));

        r.add(VeyraSettingsRow.shadow());

        return r;
    }
}

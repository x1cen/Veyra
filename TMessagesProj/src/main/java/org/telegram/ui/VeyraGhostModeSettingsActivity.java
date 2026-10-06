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

        // Section 2: Configurable Stealth Rules (With Exceptions & Scopes)
        r.add(VeyraSettingsRow.header("Stealth Rules & Exceptions"));
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
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_UPLOAD))
        ));
        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraGhostHideRead", R.string.VeyraGhostHideRead),
                "Tap to configure chat types, reply reads and exceptions",
                () -> VeyraConfig.ghostHideRead,
                v -> VeyraConfig.setGhostHideRead(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_READ))
        ));
        r.add(VeyraSettingsRow.toggleWithAction(
                "Hide Secret Chat Read Receipts",
                "Tap to configure secret chat read confirmations and exceptions",
                () -> VeyraConfig.ghostHideSecretRead,
                v -> VeyraConfig.setGhostHideSecretRead(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_SECRET_READ))
        ));
        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraGhostHideStories", R.string.VeyraGhostHideStories),
                "Tap to configure users, channels and exceptions",
                () -> VeyraConfig.ghostHideStories,
                v -> VeyraConfig.setGhostHideStories(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_STORIES))
        ));
        r.add(VeyraSettingsRow.toggleWithAction(
                "Anonymous Channel Browsing",
                "Tap to configure channel view counters and exceptions",
                () -> VeyraConfig.ghostHideChannelViews,
                v -> VeyraConfig.setGhostHideChannelViews(v),
                false,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS))
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 3: Read Receipt Behaviors
        r.add(VeyraSettingsRow.header("Read Behaviors"));
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
                false
        ));
        r.add(VeyraSettingsRow.shadow());

        return r;
    }
}

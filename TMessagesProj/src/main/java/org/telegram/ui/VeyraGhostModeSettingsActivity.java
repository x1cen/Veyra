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

        // Section 2: Online Status
        r.add(VeyraSettingsRow.header("Online Presence"));
        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraGhostHideOnline", R.string.VeyraGhostHideOnline),
                LocaleController.getString("VeyraGhostHideOnlineDesc", R.string.VeyraGhostHideOnlineDesc),
                () -> VeyraConfig.ghostHideOnline,
                v -> VeyraConfig.setGhostHideOnline(v),
                true
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 3: Stealth Features
        r.add(VeyraSettingsRow.header("Stealth Features"));

        r.add(VeyraSettingsRow.toggleWithAction(
                "Hide Typing & Chat Actions",
                VeyraConfig.getExceptions(VeyraConfig.CATEGORY_GHOST_TYPING).isEmpty()
                        ? "Tap to configure scopes & exceptions"
                        : (VeyraConfig.getExceptions(VeyraConfig.CATEGORY_GHOST_TYPING).size() + " exceptions • Tap to configure"),
                () -> VeyraConfig.ghostHideTyping,
                v -> VeyraConfig.setGhostHideTyping(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_TYPING))
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                "Hide Read Receipts",
                VeyraConfig.getExceptions(VeyraConfig.CATEGORY_GHOST_READ).isEmpty()
                        ? "Tap to configure scopes & exceptions"
                        : (VeyraConfig.getExceptions(VeyraConfig.CATEGORY_GHOST_READ).size() + " exceptions • Tap to configure"),
                () -> VeyraConfig.ghostHideRead,
                v -> VeyraConfig.setGhostHideRead(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_READ))
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                "Anonymous Channel Browsing",
                VeyraConfig.getExceptions(VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS).isEmpty()
                        ? "Tap to configure channels & exceptions"
                        : (VeyraConfig.getExceptions(VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS).size() + " exceptions • Tap to configure"),
                () -> VeyraConfig.ghostHideChannelViews,
                v -> VeyraConfig.setGhostHideChannelViews(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS))
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                "Hide Story Views",
                VeyraConfig.getExceptions(VeyraConfig.CATEGORY_GHOST_STORIES).isEmpty()
                        ? "Tap to configure exceptions"
                        : (VeyraConfig.getExceptions(VeyraConfig.CATEGORY_GHOST_STORIES).size() + " exceptions • Tap to configure"),
                () -> VeyraConfig.ghostHideStories,
                v -> VeyraConfig.setGhostHideStories(v),
                true,
                () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_STORIES))
        ));

        r.add(VeyraSettingsRow.shadow());

        return r;
    }
}

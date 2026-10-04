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
                "Hide Media Uploading",
                "Hide voice recording, video note recording, and file uploading indicators",
                () -> VeyraConfig.ghostHideUpload,
                v -> VeyraConfig.setGhostHideUpload(v),
                false
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 3: Read Receipts
        r.add(VeyraSettingsRow.header("Read Receipts"));
        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraGhostHideRead", R.string.VeyraGhostHideRead),
                LocaleController.getString("VeyraGhostHideReadDesc", R.string.VeyraGhostHideReadDesc),
                () -> VeyraConfig.ghostHideRead,
                v -> VeyraConfig.setGhostHideRead(v),
                true
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
        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraGhostHideStories", R.string.VeyraGhostHideStories),
                LocaleController.getString("VeyraGhostHideStoriesDesc", R.string.VeyraGhostHideStoriesDesc),
                () -> VeyraConfig.ghostHideStories,
                v -> VeyraConfig.setGhostHideStories(v),
                true
        ));
        r.add(VeyraSettingsRow.toggle(
                "Anonymous Channel Browsing",
                "Browse public and private channels without incrementing post view counters",
                () -> VeyraConfig.ghostHideChannelViews,
                v -> VeyraConfig.setGhostHideChannelViews(v),
                false
        ));
        r.add(VeyraSettingsRow.shadow());

        return r;
    }
}

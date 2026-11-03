package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.IconBackgroundColors;
import org.veyra.client.VeyraAntiDelete;
import org.veyra.client.VeyraDatabase;
import org.veyra.client.VeyraEditHistoryManager;
import org.veyra.client.VeyraGhost;
import org.veyra.client.VeyraScope;
import org.veyra.client.VeyraUIConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Veyra: settings hub. SECTION_MAIN lists broad categories in the style of Telegram's main
// settings screen. Categories are intentionally generic so new features fit into an existing
// one instead of needing a new category.
public class VeyraSettingsActivity extends VeyraSettingsBaseActivity {

    public static final int SECTION_MAIN = 0;
    public static final int SECTION_PRIVACY = 1;
    public static final int SECTION_CHATS = 2;
    public static final int SECTION_MEDIA = 3;
    public static final int SECTION_APPEARANCE = 4;
    public static final int SECTION_NETWORK = 5;
    public static final int SECTION_GENERAL = 6;

    private final int section;

    public VeyraSettingsActivity() {
        this(SECTION_MAIN);
    }

    public VeyraSettingsActivity(int section) {
        this.section = section;
    }

    @Override
    protected String getScreenTitle() {
        switch (section) {
            case SECTION_PRIVACY: return "Privacy & Security";
            case SECTION_CHATS: return "Chats";
            case SECTION_MEDIA: return "Media";
            case SECTION_NETWORK: return "Data & Network";
            case SECTION_GENERAL: return "General";
            default: return "Veyra Settings";
        }
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();
        switch (section) {
            case SECTION_PRIVACY: buildPrivacy(r); break;
            case SECTION_CHATS: buildChats(r); break;
            case SECTION_MEDIA: buildMedia(r); break;
            case SECTION_NETWORK: buildNetwork(r); break;
            case SECTION_GENERAL: buildGeneral(r); break;
            default: buildMain(r); break;
        }
        return r;
    }

    private VeyraSettingsRow category(IconBackgroundColors color, int icon, String title, String subtitle, int target) {
        VeyraSettingsRow row = VeyraSettingsRow.category(color.top, color.bottom, icon, title, () -> presentFragment(new VeyraSettingsActivity(target)));
        row.subtitle = subtitle;
        return row;
    }

    private void buildMain(List<VeyraSettingsRow> r) {
        r.add(VeyraSettingsRow.shadow());
        r.add(category(IconBackgroundColors.GREEN, R.drawable.settings_privacy, "Privacy & Security", "Ghost mode, visibility, account protection", SECTION_PRIVACY));
        r.add(category(IconBackgroundColors.ORANGE, R.drawable.settings_chat, "Chats", "Messages, history, chat behavior", SECTION_CHATS));
        r.add(category(IconBackgroundColors.RED, R.drawable.msg_media, "Media", "Photos, videos, voice messages, playback", SECTION_MEDIA));
        r.add(VeyraSettingsRow.shadow());
        r.add(category(IconBackgroundColors.BLUE_DEEP, R.drawable.settings_data, "Data & Network", "Downloads, connection, proxy", SECTION_NETWORK));
        r.add(category(IconBackgroundColors.GRAY, R.drawable.settings_features, "General", "Calls, confirmations, other options", SECTION_GENERAL));
        r.add(VeyraSettingsRow.shadow());
    }

    private void buildPrivacy(List<VeyraSettingsRow> r) {
        r.add(VeyraSettingsRow.header("Ghost Mode"));
        r.add(VeyraSettingsRow.toggle(
                "Ghost Mode",
                "Master switch for all ghost mode features",
                () -> VeyraGhost.ghostMode,
                val -> { VeyraGhost.toggleGhostMode(); reloadRows(); },
                true
        ));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Online Status"));
        r.add(VeyraSettingsRow.toggle(
                "Hide Online Status",
                "Appear offline to other users at all times",
                () -> VeyraGhost.hideOnline,
                val -> { VeyraGhost.toggleHideOnline(); reloadRows(); },
                true
        ).enabledWhen(() -> VeyraGhost.ghostMode));
        r.add(VeyraSettingsRow.toggle(
                "Go Offline After Sending",
                "Sending a message briefly shows you online; switch back to offline right after",
                () -> VeyraGhost.offlineAfterMessage,
                val -> VeyraGhost.toggleOfflineAfterMessage(),
                false
        ).enabledWhen(() -> VeyraGhost.ghostMode && VeyraGhost.hideOnline));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Typing & Actions"));
        r.add(VeyraSettingsRow.toggle(
                "Hide Typing & Actions",
                "Don't show others what you're doing in a chat",
                () -> VeyraGhost.hideTyping,
                val -> { VeyraGhost.toggleHideTyping(); reloadRows(); },
                true
        ).enabledWhen(() -> VeyraGhost.ghostMode));
        r.add(VeyraSettingsRow.toggle("Typing", "Text, stickers and other actions",
                () -> VeyraGhost.hideTypingText, val -> VeyraGhost.toggleHideTypingText(), true
        ).enabledWhen(() -> VeyraGhost.ghostMode && VeyraGhost.hideTyping));
        r.add(VeyraSettingsRow.toggle("Recording Voice", "Recording and uploading voice messages",
                () -> VeyraGhost.hideTypingVoice, val -> VeyraGhost.toggleHideTypingVoice(), true
        ).enabledWhen(() -> VeyraGhost.ghostMode && VeyraGhost.hideTyping));
        r.add(VeyraSettingsRow.toggle("Recording Video", "Videos and video messages",
                () -> VeyraGhost.hideTypingVideo, val -> VeyraGhost.toggleHideTypingVideo(), true
        ).enabledWhen(() -> VeyraGhost.ghostMode && VeyraGhost.hideTyping));
        r.add(VeyraSettingsRow.toggle("Uploading Files", "Photos and documents",
                () -> VeyraGhost.hideTypingFiles, val -> VeyraGhost.toggleHideTypingFiles(), false
        ).enabledWhen(() -> VeyraGhost.ghostMode && VeyraGhost.hideTyping));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Read Receipts"));
        r.add(VeyraSettingsRow.toggle(
                "Hide Read Receipts",
                "Read messages without the sender seeing it",
                () -> VeyraGhost.hideRead,
                val -> { VeyraGhost.toggleHideRead(); reloadRows(); },
                true
        ).enabledWhen(() -> VeyraGhost.ghostMode));
        r.add(VeyraSettingsRow.toggle("Mark as Read on Reply", "Recommended. When you reply, the chat is marked as read",
                () -> VeyraGhost.readOnReply, val -> VeyraGhost.toggleReadOnReply(), true
        ).enabledWhen(() -> VeyraGhost.ghostMode && VeyraGhost.hideRead));
        r.add(VeyraSettingsRow.toggle("Secret Chats", "Also hide read receipts in secret chats",
                () -> VeyraGhost.hideSecretRead, val -> VeyraGhost.toggleHideSecretRead(), true
        ).enabledWhen(() -> VeyraGhost.ghostMode && VeyraGhost.hideRead));
        r.add(VeyraSettingsRow.toggle("Reactions", "Don't report that you've seen reactions to your messages",
                () -> VeyraGhost.hideReadReactions, val -> VeyraGhost.toggleHideReadReactions(), true
        ).enabledWhen(() -> VeyraGhost.ghostMode && VeyraGhost.hideRead));
        r.add(VeyraSettingsRow.toggle("Voice & Video Playback", "Don't report played voice and video messages. Mentions and view-once media are always reported",
                () -> VeyraGhost.hideMediaReceipts, val -> VeyraGhost.toggleHideMediaReceipts(), false
        ).enabledWhen(() -> VeyraGhost.ghostMode && VeyraGhost.hideRead));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Scope"));
        r.add(VeyraSettingsRow.detail(
                "Read Receipt Scope",
                () -> VeyraScope.GHOST_READ.limitEnabled ? "Limited" : "Everywhere",
                false,
                () -> presentFragment(new VeyraScopeSettingsActivity(VeyraScope.SCOPE_GHOST_READ))
        ).enabledWhen(() -> VeyraGhost.ghostMode && VeyraGhost.hideRead));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Views"));
        r.add(VeyraSettingsRow.toggle(
                "Stealth Story Views",
                "View stories anonymously without appearing in viewer list",
                () -> VeyraGhost.hideStories,
                val -> VeyraGhost.toggleHideStories(),
                true
        ).enabledWhen(() -> VeyraGhost.ghostMode));
        r.add(VeyraSettingsRow.toggle(
                "Anonymous Channel Browsing",
                "Reading channel posts doesn't increase their view count",
                () -> VeyraGhost.hideChannelViews,
                val -> VeyraGhost.toggleHideChannelViews(),
                false
        ).enabledWhen(() -> VeyraGhost.ghostMode));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Account"));
        r.add(VeyraSettingsRow.toggle(
                "Hide My Phone Number",
                "Mask your phone number in accounts list and profile screen",
                () -> VeyraUIConfig.hidePhoneNumber,
                val -> VeyraUIConfig.toggleHidePhoneNumber(),
                false
        ));
        r.add(VeyraSettingsRow.shadow());
    }

    private void buildChats(List<VeyraSettingsRow> r) {
        r.add(VeyraSettingsRow.header("Messages"));
        r.add(VeyraSettingsRow.toggle(
                "Message Edit History Log",
                "Log and view previous versions of edited messages",
                () -> VeyraUIConfig.editHistoryEnabled,
                val -> VeyraUIConfig.toggleEditHistory(),
                true
        ));
        r.add(VeyraSettingsRow.detail(
                "History Scope",
                () -> VeyraScope.HISTORY.limitEnabled ? "Limited" : "Everywhere",
                false,
                () -> presentFragment(new VeyraScopeSettingsActivity(VeyraScope.SCOPE_HISTORY))
        ).enabledWhen(() -> VeyraUIConfig.editHistoryEnabled));
        r.add(VeyraSettingsRow.toggle(
                "Ignore Blocked Messages in Groups",
                "Blur media and collapse messages from blocked users",
                () -> VeyraUIConfig.ignoreBlocked,
                val -> VeyraUIConfig.toggleIgnoreBlocked(),
                false
        ));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Anti-Delete"));
        r.add(VeyraSettingsRow.detail(
                "Anti-Delete Scope",
                () -> VeyraScope.ANTI_DELETE.limitEnabled ? "Limited" : "Everywhere",
                false,
                () -> presentFragment(new VeyraScopeSettingsActivity(VeyraScope.SCOPE_ANTI_DELETE))
        ));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Behavior"));
        r.add(VeyraSettingsRow.toggle(
                "Hide Keyboard on Scroll",
                "Dismiss keyboard automatically when dragging message list",
                () -> VeyraUIConfig.hideKeyboardOnChatScroll,
                val -> VeyraUIConfig.toggleHideKeyboardOnChatScroll(),
                true
        ));
        r.add(VeyraSettingsRow.toggle(
                "Disable Jump to Next Channel",
                "Stop pull-to-jump to next unread channel at the bottom",
                () -> VeyraUIConfig.disableJumpToNextChannel,
                val -> VeyraUIConfig.toggleDisableJumpToNextChannel(),
                false
        ));
        r.add(VeyraSettingsRow.shadow());
    }

    private void buildMedia(List<VeyraSettingsRow> r) {
        r.add(VeyraSettingsRow.header("Playback"));
        r.add(VeyraSettingsRow.toggle(
                "Disable Voice Auto-Play",
                "Play only the selected voice note and do not auto-play next",
                () -> VeyraUIConfig.disableVoiceAutoPlay,
                val -> VeyraUIConfig.toggleDisableVoiceAutoPlay(),
                false
        ));
        r.add(VeyraSettingsRow.shadow());
    }

    private void buildNetwork(List<VeyraSettingsRow> r) {
        r.add(VeyraSettingsRow.header("Downloads"));
        r.add(VeyraSettingsRow.detail(
                "Download Speed Boost",
                () -> {
                    switch (VeyraUIConfig.downloadSpeedBoost) {
                        case 2: return "Extreme (12 threads / 1MB)";
                        case 1: return "Fast (8 threads / 512KB)";
                        default: return "Standard (Off)";
                    }
                },
                false,
                () -> {
                    Context context = getParentActivity();
                    if (context == null) return;
                    AlertDialog.Builder b = new AlertDialog.Builder(context);
                    b.setTitle("Download Speed Boost");
                    CharSequence[] options = new CharSequence[]{"Standard (Off)", "Fast (8 connections)", "Extreme (12 connections)"};
                    b.setItems(options, (d, which) -> {
                        VeyraUIConfig.setDownloadSpeedBoost(which);
                        reloadRows();
                    });
                    showDialog(b.create());
                }
        ));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Storage"));
        r.add(VeyraSettingsRow.detail(
                "Veyra Database Size",
                () -> AndroidUtilities.formatFileSize(getVeyraDataSize(currentAccount)),
                true,
                null
        ));
        r.add(VeyraSettingsRow.button(
                "Clear Veyra Database",
                true,
                false,
                this::showClearVeyraDatabaseAlert
        ));
        r.add(VeyraSettingsRow.shadow());
    }

    private long getVeyraDataSize(int account) {
        long size = 0;
        try {
            File filesDir = ApplicationLoader.getFilesDirFixed();
            if (account != 0) {
                filesDir = new File(filesDir, "account" + account + "/");
            }
            File db1 = new File(filesDir, "veyra.db");
            if (db1.exists()) size += db1.length();
            File db1Wal = new File(filesDir, "veyra.db-wal");
            if (db1Wal.exists()) size += db1Wal.length();
            File db2 = ApplicationLoader.applicationContext.getDatabasePath("veyra_edit_history.db");
            if (db2 != null && db2.exists()) size += db2.length();
        } catch (Throwable ignored) {}
        return size;
    }

    private void showClearVeyraDatabaseAlert() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Clear Veyra Database");
        builder.setMessage("This will permanently delete all saved anti-deleted messages, edit history logs and reaction records for this account.");
        builder.setPositiveButton("Clear", (dialog, which) -> {
            try {
                VeyraAntiDelete.clearAll(currentAccount);
                VeyraDatabase.getInstance(currentAccount).clearAll();
                VeyraEditHistoryManager.clearAll(currentAccount);
            } catch (Throwable ignored) {}
            reloadRows();
            try {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Veyra database cleared").show();
            } catch (Throwable t) {
                FileLog.e(t);
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog alert = builder.create();
        showDialog(alert);
        TextView btn = (TextView) alert.getButton(DialogInterface.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    private void buildGeneral(List<VeyraSettingsRow> r) {
        r.add(VeyraSettingsRow.header("Calls"));
        r.add(VeyraSettingsRow.toggle(
                "Ask Before Calling",
                "Show confirmation dialog before initiating voice or video calls",
                () -> VeyraUIConfig.askBeforeCall,
                val -> VeyraUIConfig.toggleAskBeforeCall(),
                false
        ));
        r.add(VeyraSettingsRow.shadow());
    }
}

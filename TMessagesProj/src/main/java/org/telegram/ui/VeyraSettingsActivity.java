package org.telegram.ui;

import android.content.Context;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.IconBackgroundColors;
import org.veyra.client.VeyraGhost;
import org.veyra.client.VeyraUIConfig;

import java.util.ArrayList;
import java.util.List;

// Veyra: settings hub. SECTION_MAIN lists categories in the style of Telegram's main
// settings screen; every other section is a sub-screen opened from that list.
public class VeyraSettingsActivity extends VeyraSettingsBaseActivity {

    public static final int SECTION_MAIN = 0;
    public static final int SECTION_PRIVACY = 1;
    public static final int SECTION_CHATS = 2;
    public static final int SECTION_MEDIA = 3;
    public static final int SECTION_FOLDERS = 4;
    public static final int SECTION_NETWORK = 5;

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
            case SECTION_PRIVACY: return "Privacy & Ghost Mode";
            case SECTION_CHATS: return "Chats";
            case SECTION_MEDIA: return "Media & Calls";
            case SECTION_FOLDERS: return "Chat Folders";
            case SECTION_NETWORK: return "Data & Network";
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
            case SECTION_FOLDERS: buildFolders(r); break;
            case SECTION_NETWORK: buildNetwork(r); break;
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
        r.add(category(IconBackgroundColors.GREEN, R.drawable.settings_privacy, "Privacy & Ghost Mode", "Online status, typing, read receipts, phone number", SECTION_PRIVACY));
        r.add(category(IconBackgroundColors.ORANGE, R.drawable.settings_chat, "Chats", "Keyboard, channel jump, edit history, blocked users", SECTION_CHATS));
        r.add(category(IconBackgroundColors.RED, R.drawable.settings_calls, "Media & Calls", "Voice playback, call confirmation", SECTION_MEDIA));
        r.add(VeyraSettingsRow.shadow());
        r.add(category(IconBackgroundColors.BLUE_ALT, R.drawable.settings_folders, "Chat Folders", "Tab titles, tab position, All Chats tab", SECTION_FOLDERS));
        r.add(category(IconBackgroundColors.BLUE_DEEP, R.drawable.settings_data, "Data & Network", "Download speed boost", SECTION_NETWORK));
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
        if (VeyraGhost.ghostMode) {
            r.add(VeyraSettingsRow.toggle(
                    "Hide Online Status",
                    "Appear offline to other users at all times",
                    () -> VeyraGhost.hideOnline,
                    val -> VeyraGhost.toggleHideOnline(),
                    true
            ));
            r.add(VeyraSettingsRow.toggle(
                    "Hide Typing Status",
                    "Don't send typing, voice recording, or upload actions",
                    () -> VeyraGhost.hideTyping,
                    val -> VeyraGhost.toggleHideTyping(),
                    true
            ));
            r.add(VeyraSettingsRow.toggle(
                    "Mark as Read on Reply",
                    "Keep messages unread until you reply to the conversation",
                    () -> VeyraGhost.readOnReply,
                    val -> VeyraGhost.toggleReadOnReply(),
                    true
            ));
            r.add(VeyraSettingsRow.toggle(
                    "Stealth Story Views",
                    "View stories anonymously without appearing in viewer list",
                    () -> VeyraGhost.hideStories,
                    val -> VeyraGhost.toggleHideStories(),
                    false
            ));
        }
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
        r.add(VeyraSettingsRow.header("Chat Behavior"));
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

        r.add(VeyraSettingsRow.header("Messages"));
        r.add(VeyraSettingsRow.toggle(
                "Message Edit History Log",
                "Log and view previous versions of edited messages",
                () -> VeyraUIConfig.editHistoryEnabled,
                val -> VeyraUIConfig.toggleEditHistory(),
                true
        ));
        r.add(VeyraSettingsRow.toggle(
                "Ignore Blocked Messages in Groups",
                "Blur media and collapse messages from blocked users",
                () -> VeyraUIConfig.ignoreBlocked,
                val -> VeyraUIConfig.toggleIgnoreBlocked(),
                false
        ));
        r.add(VeyraSettingsRow.shadow());
    }

    private void buildMedia(List<VeyraSettingsRow> r) {
        r.add(VeyraSettingsRow.header("Voice Messages"));
        r.add(VeyraSettingsRow.toggle(
                "Disable Voice Auto-Play",
                "Play only the selected voice note and do not auto-play next",
                () -> VeyraUIConfig.disableVoiceAutoPlay,
                val -> VeyraUIConfig.toggleDisableVoiceAutoPlay(),
                false
        ));
        r.add(VeyraSettingsRow.shadow());

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

    private void buildFolders(List<VeyraSettingsRow> r) {
        r.add(VeyraSettingsRow.header("Folder Tabs"));
        r.add(VeyraSettingsRow.detail(
                "Tab Titles",
                () -> {
                    switch (VeyraUIConfig.tabsTitleType) {
                        case VeyraUIConfig.TITLE_TYPE_TEXT: return "Text Only";
                        case VeyraUIConfig.TITLE_TYPE_ICON: return "Icons Only";
                        default: return "Icons & Text";
                    }
                },
                true,
                () -> {
                    Context context = getParentActivity();
                    if (context == null) return;
                    AlertDialog.Builder b = new AlertDialog.Builder(context);
                    b.setTitle("Tab Titles");
                    CharSequence[] options = new CharSequence[]{"Text Only", "Icons Only", "Icons & Text"};
                    b.setItems(options, (d, which) -> {
                        int val;
                        if (which == 0) val = VeyraUIConfig.TITLE_TYPE_TEXT;
                        else if (which == 1) val = VeyraUIConfig.TITLE_TYPE_ICON;
                        else val = VeyraUIConfig.TITLE_TYPE_MIX;
                        VeyraUIConfig.setTabsTitleType(val);
                        reloadRows();
                    });
                    showDialog(b.create());
                }
        ));
        r.add(VeyraSettingsRow.toggle(
                "Bottom Folder Tabs",
                "Move folder tabs to the bottom navigation bar",
                () -> VeyraUIConfig.bottomFilterTabs,
                val -> VeyraUIConfig.toggleBottomFilterTabs(),
                true
        ));
        r.add(VeyraSettingsRow.toggle(
                "Hide 'All Chats' Tab",
                "Hide the default all-chats folder tab from the top bar",
                () -> VeyraUIConfig.hideAllTab,
                val -> VeyraUIConfig.toggleHideAllTab(),
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
    }
}

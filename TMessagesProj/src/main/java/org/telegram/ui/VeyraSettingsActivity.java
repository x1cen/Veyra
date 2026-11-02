package org.telegram.ui;

import android.content.Context;
import org.telegram.ui.ActionBar.AlertDialog;
import org.veyra.client.VeyraGhost;
import org.veyra.client.VeyraUIConfig;

import java.util.ArrayList;
import java.util.List;

public class VeyraSettingsActivity extends VeyraSettingsBaseActivity {

    @Override
    protected String getScreenTitle() {
        return "Veyra Settings";
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();

        // Section 1: Ghost Mode & Stealth
        r.add(VeyraSettingsRow.header("Ghost Mode & Stealth"));
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
                    true
            ));
        }
        r.add(VeyraSettingsRow.toggle(
                "Message Edit History Log",
                "Log and view previous versions of edited messages",
                () -> VeyraUIConfig.editHistoryEnabled,
                val -> VeyraUIConfig.toggleEditHistory(),
                true
        ));
        r.add(VeyraSettingsRow.toggle(
                "Hide My Phone Number",
                "Mask your phone number in accounts list and profile screen",
                () -> VeyraUIConfig.hidePhoneNumber,
                val -> VeyraUIConfig.toggleHidePhoneNumber(),
                false
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 2: Folder Tabs
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

        // Section 3: Chat Usability & Tweaks
        r.add(VeyraSettingsRow.header("Chat Usability"));
        r.add(VeyraSettingsRow.toggle(
                "Hide Keyboard on Scroll",
                "Dismiss keyboard automatically when dragging message list",
                () -> VeyraUIConfig.hideKeyboardOnChatScroll,
                val -> VeyraUIConfig.toggleHideKeyboardOnChatScroll(),
                true
        ));
        r.add(VeyraSettingsRow.toggle(
                "Disable Voice Auto-Play",
                "Play only the selected voice note and do not auto-play next",
                () -> VeyraUIConfig.disableVoiceAutoPlay,
                val -> VeyraUIConfig.toggleDisableVoiceAutoPlay(),
                true
        ));
        r.add(VeyraSettingsRow.toggle(
                "Disable Jump to Next Channel",
                "Stop pull-to-jump to next unread channel at the bottom",
                () -> VeyraUIConfig.disableJumpToNextChannel,
                val -> VeyraUIConfig.toggleDisableJumpToNextChannel(),
                true
        ));
        r.add(VeyraSettingsRow.toggle(
                "Ignore Blocked Messages in Groups",
                "Blur media and collapse messages from blocked users",
                () -> VeyraUIConfig.ignoreBlocked,
                val -> VeyraUIConfig.toggleIgnoreBlocked(),
                true
        ));
        r.add(VeyraSettingsRow.toggle(
                "Ask Before Calling",
                "Show confirmation dialog before initiating voice or video calls",
                () -> VeyraUIConfig.askBeforeCall,
                val -> VeyraUIConfig.toggleAskBeforeCall(),
                false
        ));
        r.add(VeyraSettingsRow.shadow());

        // Section 4: Network & Downloads
        r.add(VeyraSettingsRow.header("Network & Speed"));
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

        return r;
    }
}

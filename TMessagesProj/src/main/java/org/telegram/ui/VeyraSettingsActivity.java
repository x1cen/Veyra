package org.telegram.ui;

import android.content.Context;
import android.view.View;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.Components.UItem;
import org.veyra.client.VeyraGhost;
import org.veyra.client.VeyraUIConfig;

import java.util.ArrayList;

public class VeyraSettingsActivity extends UniversalFragment {

    public static final int ITEM_GHOST_MODE = 1;
    public static final int ITEM_GHOST_ONLINE = 2;
    public static final int ITEM_GHOST_TYPING = 3;
    public static final int ITEM_GHOST_READ = 4;
    public static final int ITEM_GHOST_STORIES = 5;

    public static final int ITEM_TABS_TITLE_TYPE = 10;
    public static final int ITEM_TABS_BOTTOM = 11;
    public static final int ITEM_TABS_HIDE_ALL = 12;

    public static final int ITEM_CHAT_HIDE_KEYBOARD = 20;
    public static final int ITEM_CHAT_DISABLE_VOICE_AUTOPLAY = 21;
    public static final int ITEM_CHAT_DISABLE_JUMP_NEXT = 22;
    public static final int ITEM_CHAT_IGNORE_BLOCKED = 23;
    public static final int ITEM_CHAT_ASK_CALL = 24;

    public static final int ITEM_DOWNLOAD_SPEED = 30;

    @Override
    protected CharSequence getTitle() {
        return "Veyra Settings";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // --- Ghost Mode Section ---
        items.add(UItem.asHeader("Ghost Mode & Stealth"));
        items.add(UItem.asCheck(ITEM_GHOST_MODE, "Ghost Mode").setChecked(VeyraGhost.ghostMode));
        if (VeyraGhost.ghostMode) {
            items.add(UItem.asCheck(ITEM_GHOST_ONLINE, "Hide Online Status").setChecked(VeyraGhost.hideOnline));
            items.add(UItem.asCheck(ITEM_GHOST_TYPING, "Hide Typing Status").setChecked(VeyraGhost.hideTyping));
            items.add(UItem.asCheck(ITEM_GHOST_READ, "Mark as Read on Reply").setChecked(VeyraGhost.readOnReply));
            items.add(UItem.asCheck(ITEM_GHOST_STORIES, "Stealth Story Views").setChecked(VeyraGhost.hideStories));
        }
        items.add(UItem.asShadow("Prevents read receipts, online status broadcasts, typing events, and story views."));

        // --- Folder Tabs Section ---
        items.add(UItem.asHeader("Folder Tabs"));
        String tabTypeStr;
        switch (VeyraUIConfig.tabsTitleType) {
            case VeyraUIConfig.TITLE_TYPE_TEXT:
                tabTypeStr = "Text Only";
                break;
            case VeyraUIConfig.TITLE_TYPE_ICON:
                tabTypeStr = "Icons Only";
                break;
            default:
                tabTypeStr = "Icons & Text";
                break;
        }
        items.add(UItem.asButton(ITEM_TABS_TITLE_TYPE, R.drawable.msg_topics, "Tab Titles", tabTypeStr));
        items.add(UItem.asCheck(ITEM_TABS_BOTTOM, "Bottom Folder Tabs").setChecked(VeyraUIConfig.bottomFilterTabs));
        items.add(UItem.asCheck(ITEM_TABS_HIDE_ALL, "Hide 'All Chats' Tab").setChecked(VeyraUIConfig.hideAllTab));
        items.add(UItem.asShadow("Customize folder tab positioning, titles, and visibility."));

        // --- Chat Usability Section ---
        items.add(UItem.asHeader("Chat Usability"));
        items.add(UItem.asCheck(ITEM_CHAT_HIDE_KEYBOARD, "Hide Keyboard on Scroll").setChecked(VeyraUIConfig.hideKeyboardOnChatScroll));
        items.add(UItem.asCheck(ITEM_CHAT_DISABLE_VOICE_AUTOPLAY, "Disable Voice Auto-Play").setChecked(VeyraUIConfig.disableVoiceAutoPlay));
        items.add(UItem.asCheck(ITEM_CHAT_DISABLE_JUMP_NEXT, "Disable Jump to Next Channel").setChecked(VeyraUIConfig.disableJumpToNextChannel));
        items.add(UItem.asCheck(ITEM_CHAT_IGNORE_BLOCKED, "Ignore Blocked Messages in Groups").setChecked(VeyraUIConfig.ignoreBlocked));
        items.add(UItem.asCheck(ITEM_CHAT_ASK_CALL, "Ask Before Calling").setChecked(VeyraUIConfig.askBeforeCall));
        items.add(UItem.asShadow("Behavior adjustments for messages, voice notes, calls, and blocked users."));

        // --- Network & Speed Section ---
        items.add(UItem.asHeader("Network & Downloads"));
        String speedStr;
        switch (VeyraUIConfig.downloadSpeedBoost) {
            case 2:
                speedStr = "Extreme (12 threads / 1MB chunk)";
                break;
            case 1:
                speedStr = "Fast (8 threads / 512KB chunk)";
                break;
            default:
                speedStr = "Standard (Off)";
                break;
        }
        items.add(UItem.asButton(ITEM_DOWNLOAD_SPEED, R.drawable.msg_download, "Download Speed Boost", speedStr));
        items.add(UItem.asShadow("Accelerate media and file downloads with parallel chunked connections."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ITEM_GHOST_MODE:
                VeyraGhost.toggleGhostMode();
                if (listView != null && listView.adapter != null) listView.adapter.update(true);
                break;
            case ITEM_GHOST_ONLINE:
                VeyraGhost.toggleHideOnline();
                if (listView != null && listView.adapter != null) listView.adapter.update(true);
                break;
            case ITEM_GHOST_TYPING:
                VeyraGhost.toggleHideTyping();
                if (listView != null && listView.adapter != null) listView.adapter.update(true);
                break;
            case ITEM_GHOST_READ:
                VeyraGhost.toggleReadOnReply();
                if (listView != null && listView.adapter != null) listView.adapter.update(true);
                break;
            case ITEM_GHOST_STORIES:
                VeyraGhost.toggleHideStories();
                if (listView != null && listView.adapter != null) listView.adapter.update(true);
                break;

            case ITEM_TABS_TITLE_TYPE: {
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
                    if (listView != null && listView.adapter != null) listView.adapter.update(true);
                });
                showDialog(b.create());
                break;
            }
            case ITEM_TABS_BOTTOM:
                VeyraUIConfig.toggleBottomFilterTabs();
                if (listView != null && listView.adapter != null) listView.adapter.update(true);
                break;
            case ITEM_TABS_HIDE_ALL:
                VeyraUIConfig.toggleHideAllTab();
                if (listView != null && listView.adapter != null) listView.adapter.update(true);
                break;

            case ITEM_CHAT_HIDE_KEYBOARD:
                VeyraUIConfig.toggleHideKeyboardOnChatScroll();
                if (listView != null && listView.adapter != null) listView.adapter.update(true);
                break;
            case ITEM_CHAT_DISABLE_VOICE_AUTOPLAY:
                VeyraUIConfig.toggleDisableVoiceAutoPlay();
                if (listView != null && listView.adapter != null) listView.adapter.update(true);
                break;
            case ITEM_CHAT_DISABLE_JUMP_NEXT:
                VeyraUIConfig.toggleDisableJumpToNextChannel();
                if (listView != null && listView.adapter != null) listView.adapter.update(true);
                break;
            case ITEM_CHAT_IGNORE_BLOCKED:
                VeyraUIConfig.toggleIgnoreBlocked();
                if (listView != null && listView.adapter != null) listView.adapter.update(true);
                break;
            case ITEM_CHAT_ASK_CALL:
                VeyraUIConfig.toggleAskBeforeCall();
                if (listView != null && listView.adapter != null) listView.adapter.update(true);
                break;

            case ITEM_DOWNLOAD_SPEED: {
                Context context = getParentActivity();
                if (context == null) return;
                AlertDialog.Builder b = new AlertDialog.Builder(context);
                b.setTitle("Download Speed Boost");
                CharSequence[] options = new CharSequence[]{"Standard (Off)", "Fast (8 connections)", "Extreme (12 connections)"};
                b.setItems(options, (d, which) -> {
                    VeyraUIConfig.setDownloadSpeedBoost(which);
                    if (listView != null && listView.adapter != null) listView.adapter.update(true);
                });
                showDialog(b.create());
                break;
            }
        }
    }
}

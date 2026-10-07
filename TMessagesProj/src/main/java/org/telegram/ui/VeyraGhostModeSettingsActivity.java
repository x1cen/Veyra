package org.telegram.ui;

import android.content.Context;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;

import java.util.ArrayList;
import java.util.List;

public class VeyraGhostModeSettingsActivity extends VeyraBaseSettingsActivity {

    @Override
    protected String getTitle() {
        return LocaleController.getString("VeyraGhostModeTitle", R.string.VeyraGhostModeTitle);
    }

    @Override
    protected List<SettingItem> buildSettingsList() {
        List<SettingItem> items = new ArrayList<>();

        // Section 1: Master Control
        items.add(SettingItem.header(LocaleController.getString("VeyraGhostModeTitle", R.string.VeyraGhostModeTitle)));
        items.add(SettingItem.toggle(
            LocaleController.getString("VeyraGhostModeMaster", R.string.VeyraGhostModeMaster),
            VeyraConfig.ghostMode,
            VeyraConfig::setGhostMode
        ));
        items.add(SettingItem.info(LocaleController.getString("VeyraGhostModeMasterInfo", R.string.VeyraGhostModeMasterInfo)));

        // Section 2: Stealth Rules (All configurable with scopes & exceptions)
        items.add(SettingItem.header("Stealth Rules"));

        items.add(SettingItem.toggleWithAction(
            LocaleController.getString("VeyraGhostHideOnline", R.string.VeyraGhostHideOnline),
            VeyraConfig.ghostHideOnline,
            VeyraConfig::setGhostHideOnline,
            () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_ONLINE))
        ));

        items.add(SettingItem.toggleWithAction(
            LocaleController.getString("VeyraGhostHideTyping", R.string.VeyraGhostHideTyping),
            VeyraConfig.ghostHideTyping,
            VeyraConfig::setGhostHideTyping,
            () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_TYPING))
        ));

        items.add(SettingItem.toggleWithAction(
            "Hide Media Uploading",
            VeyraConfig.ghostHideUpload,
            VeyraConfig::setGhostHideUpload,
            () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_UPLOAD))
        ));

        items.add(SettingItem.toggleWithAction(
            LocaleController.getString("VeyraGhostHideRead", R.string.VeyraGhostHideRead),
            VeyraConfig.ghostHideRead,
            VeyraConfig::setGhostHideRead,
            () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_READ))
        ));

        items.add(SettingItem.toggleWithAction(
            LocaleController.getString("VeyraReadOnReply", R.string.VeyraReadOnReply),
            VeyraConfig.ghostReadOnReply,
            VeyraConfig::setGhostReadOnReply,
            () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_READ_ON_REPLY))
        ));

        items.add(SettingItem.toggleWithAction(
            LocaleController.getString("VeyraGhostHideReadContents", R.string.VeyraGhostHideReadContents),
            VeyraConfig.ghostHideReadContents,
            VeyraConfig::setGhostHideReadContents,
            () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_HIDE_CONTENTS))
        ));

        items.add(SettingItem.toggleWithAction(
            "Secret Chat Read Receipts",
            VeyraConfig.ghostHideSecretRead,
            VeyraConfig::setGhostHideSecretRead,
            () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_SECRET_READ))
        ));

        items.add(SettingItem.toggleWithAction(
            LocaleController.getString("VeyraGhostHideStories", R.string.VeyraGhostHideStories),
            VeyraConfig.ghostHideStories,
            VeyraConfig::setGhostHideStories,
            () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_STORIES))
        ));

        items.add(SettingItem.toggleWithAction(
            "Anonymous Channel Browsing",
            VeyraConfig.ghostHideChannelViews,
            VeyraConfig::setGhostHideChannelViews,
            () -> presentFragment(new VeyraGhostFeatureActivity(VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS))
        ));

        items.add(SettingItem.info("Tap any rule to configure chat types (Private, Groups, Channels) and manage exceptions."));

        return items;
    }
}

package org.telegram.ui;

import android.content.DialogInterface;
import android.widget.TextView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.veyra.client.VeyraEditHistoryManager;

import java.util.ArrayList;
import java.util.List;

public class VeyraReactionHistorySettingsActivity extends VeyraSettingsBaseActivity {

    @Override
    protected String getScreenTitle() {
        return "Reaction History";
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();

        r.add(VeyraSettingsRow.header("Reaction History"));
        r.add(VeyraSettingsRow.toggle(
                "Enable Reaction History",
                "Keep track of reactions added to messages",
                () -> VeyraConfig.reactionHistoryEnabled,
                v -> VeyraConfig.setReactionHistoryEnabled(v),
                true
        ));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Automatic Storage"));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraChatTypePrivate", R.string.VeyraChatTypePrivate),
                "Tap to configure exceptions",
                () -> VeyraConfig.reactionHistoryPrivate,
                v -> VeyraConfig.setPeerEnabled(VeyraConfig.CATEGORY_REACTION_HISTORY, VeyraConfig.PEER_PRIVATE, v),
                true,
                () -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putInt("category", VeyraConfig.CATEGORY_REACTION_HISTORY);
                    b.putInt("peerType", VeyraConfig.PEER_PRIVATE);
                    presentFragment(new VeyraScopeSettingsActivity(b));
                }
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraChatTypeGroups", R.string.VeyraChatTypeGroups),
                "Tap to configure exceptions and private group filter",
                () -> VeyraConfig.reactionHistoryGroups,
                v -> VeyraConfig.setPeerEnabled(VeyraConfig.CATEGORY_REACTION_HISTORY, VeyraConfig.PEER_GROUP, v),
                true,
                () -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putInt("category", VeyraConfig.CATEGORY_REACTION_HISTORY);
                    b.putInt("peerType", VeyraConfig.PEER_GROUP);
                    presentFragment(new VeyraScopeSettingsActivity(b));
                }
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraChatTypeChannels", R.string.VeyraChatTypeChannels),
                "Tap to configure exceptions and private channel filter",
                () -> VeyraConfig.reactionHistoryChannels,
                v -> VeyraConfig.setPeerEnabled(VeyraConfig.CATEGORY_REACTION_HISTORY, VeyraConfig.PEER_CHANNEL, v),
                true,
                () -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putInt("category", VeyraConfig.CATEGORY_REACTION_HISTORY);
                    b.putInt("peerType", VeyraConfig.PEER_CHANNEL);
                    presentFragment(new VeyraScopeSettingsActivity(b));
                }
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraChatTypeBots", R.string.VeyraChatTypeBots),
                "Tap to configure exceptions",
                () -> VeyraConfig.reactionHistoryBots,
                v -> VeyraConfig.setPeerEnabled(VeyraConfig.CATEGORY_REACTION_HISTORY, VeyraConfig.PEER_BOT, v),
                true,
                () -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putInt("category", VeyraConfig.CATEGORY_REACTION_HISTORY);
                    b.putInt("peerType", VeyraConfig.PEER_BOT);
                    presentFragment(new VeyraScopeSettingsActivity(b));
                }
        ));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Settings & Actions"));
        r.add(VeyraSettingsRow.detail(
                "Max Reactions Per Message",
                () -> String.valueOf(VeyraConfig.reactionHistoryLimit),
                true,
                () -> {
                    if (getParentActivity() == null) return;
                    CharSequence[] options = {"5", "10", "20", "50"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle("Max Reactions Per Message");
                    builder.setItems(options, (dialog, which) -> {
                        VeyraConfig.setReactionHistoryLimit(Integer.parseInt(options[which].toString()));
                        dialog.dismiss();
                        reloadRows();
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                }
        ));

        r.add(VeyraSettingsRow.toggle(
                "Drop Oldest on Overflow",
                "When limit is reached, discard older reactions to record new ones",
                () -> VeyraConfig.reactionHistoryDropOldest,
                v -> VeyraConfig.setReactionHistoryDropOldest(v),
                true
        ));

        r.add(VeyraSettingsRow.button(
                "Clear All Reaction History",
                true,
                false,
                () -> {
                    if (getParentActivity() == null) return;
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle("Clear All Reaction History");
                    builder.setMessage("Are you sure you want to delete all saved reaction history across all chats?");
                    builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
                        VeyraEditHistoryManager.clearAllReactions();
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Reaction history cleared").show();
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    AlertDialog alert = builder.create();
                    showDialog(alert);
                    TextView btn = (TextView) alert.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (btn != null) {
                        btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                    }
                }
        ));
        r.add(VeyraSettingsRow.shadow());

        return r;
    }
}

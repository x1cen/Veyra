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

public class VeyraEditHistorySettingsActivity extends VeyraSettingsBaseActivity {

    @Override
    protected String getScreenTitle() {
        return LocaleController.getString("VeyraEditHistory", R.string.VeyraEditHistory);
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();

        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraEditHistory", R.string.VeyraEditHistory)));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraEditHistoryEnable", R.string.VeyraEditHistoryEnable),
                LocaleController.getString("VeyraEditHistoryEnableDesc", R.string.VeyraEditHistoryEnableDesc),
                () -> VeyraConfig.editHistoryEnabled,
                v -> VeyraConfig.setEditHistoryEnabled(v),
                true
        ));

        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraChatTypes", R.string.VeyraChatTypes)));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraChatTypePrivate", R.string.VeyraChatTypePrivate),
                "Tap to configure exceptions",
                () -> VeyraConfig.editHistoryPrivate,
                v -> VeyraConfig.setPeerEnabled(VeyraConfig.CATEGORY_EDIT_HISTORY, VeyraConfig.PEER_PRIVATE, v),
                true,
                () -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putInt("category", VeyraConfig.CATEGORY_EDIT_HISTORY);
                    b.putInt("peerType", VeyraConfig.PEER_PRIVATE);
                    presentFragment(new VeyraScopeSettingsActivity(b));
                }
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraChatTypeGroups", R.string.VeyraChatTypeGroups),
                "Tap to configure exceptions and private group filter",
                () -> VeyraConfig.editHistoryGroups,
                v -> VeyraConfig.setPeerEnabled(VeyraConfig.CATEGORY_EDIT_HISTORY, VeyraConfig.PEER_GROUP, v),
                true,
                () -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putInt("category", VeyraConfig.CATEGORY_EDIT_HISTORY);
                    b.putInt("peerType", VeyraConfig.PEER_GROUP);
                    presentFragment(new VeyraScopeSettingsActivity(b));
                }
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraChatTypeChannels", R.string.VeyraChatTypeChannels),
                "Tap to configure exceptions and private channel filter",
                () -> VeyraConfig.editHistoryChannels,
                v -> VeyraConfig.setPeerEnabled(VeyraConfig.CATEGORY_EDIT_HISTORY, VeyraConfig.PEER_CHANNEL, v),
                true,
                () -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putInt("category", VeyraConfig.CATEGORY_EDIT_HISTORY);
                    b.putInt("peerType", VeyraConfig.PEER_CHANNEL);
                    presentFragment(new VeyraScopeSettingsActivity(b));
                }
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraChatTypeBots", R.string.VeyraChatTypeBots),
                "Tap to configure exceptions",
                () -> VeyraConfig.editHistoryBots,
                v -> VeyraConfig.setPeerEnabled(VeyraConfig.CATEGORY_EDIT_HISTORY, VeyraConfig.PEER_BOT, v),
                true,
                () -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putInt("category", VeyraConfig.CATEGORY_EDIT_HISTORY);
                    b.putInt("peerType", VeyraConfig.PEER_BOT);
                    presentFragment(new VeyraScopeSettingsActivity(b));
                }
        ));

        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Limits"));

        r.add(VeyraSettingsRow.detail(
                "Max Edits Per Message",
                () -> String.valueOf(VeyraConfig.editHistoryLimit),
                true,
                () -> {
                    if (getParentActivity() == null) return;
                    CharSequence[] options = {"5", "10", "20", "50"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle("Max Edits Per Message");
                    builder.setItems(options, (dialog, which) -> {
                        VeyraConfig.setEditHistoryLimit(Integer.parseInt(options[which].toString()));
                        dialog.dismiss();
                        reloadRows();
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                }
        ));

        r.add(VeyraSettingsRow.toggle(
                "Drop Oldest on Overflow",
                "When limit is reached, discard older edits to record new ones",
                () -> VeyraConfig.editHistoryDropOldest,
                v -> VeyraConfig.setEditHistoryDropOldest(v),
                true
        ));

        r.add(VeyraSettingsRow.button(
                "Clear All Edit History",
                true,
                false,
                () -> {
                    if (getParentActivity() == null) return;
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle("Clear All Edit History");
                    builder.setMessage("Are you sure you want to delete all saved edit history across all chats?");
                    builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
                        VeyraEditHistoryManager.clearAllEdits();
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Edit history cleared").show();
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

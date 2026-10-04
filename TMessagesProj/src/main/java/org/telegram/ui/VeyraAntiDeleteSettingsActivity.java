package org.telegram.ui;

import android.content.DialogInterface;
import android.widget.TextView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.List;

public class VeyraAntiDeleteSettingsActivity extends VeyraSettingsBaseActivity {

    @Override
    protected String getScreenTitle() {
        return LocaleController.getString("VeyraAntiDelete", R.string.VeyraAntiDelete);
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();

        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraAntiDelete", R.string.VeyraAntiDelete)));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraAntiDeleteEnable", R.string.VeyraAntiDeleteEnable),
                LocaleController.getString("VeyraAntiDeleteEnableDesc", R.string.VeyraAntiDeleteEnableDesc),
                () -> VeyraConfig.antiDelete,
                v -> VeyraConfig.setAntiDelete(v),
                true
        ));

        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraChatTypes", R.string.VeyraChatTypes)));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraChatTypePrivate", R.string.VeyraChatTypePrivate),
                "Tap to configure exceptions",
                () -> VeyraConfig.antiDeletePrivate,
                v -> VeyraConfig.setPeerEnabled(VeyraConfig.CATEGORY_ANTI_DELETE, VeyraConfig.PEER_PRIVATE, v),
                true,
                () -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putInt("category", VeyraConfig.CATEGORY_ANTI_DELETE);
                    b.putInt("peerType", VeyraConfig.PEER_PRIVATE);
                    presentFragment(new VeyraScopeSettingsActivity(b));
                }
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraChatTypeGroups", R.string.VeyraChatTypeGroups),
                "Tap to configure exceptions and private group filter",
                () -> VeyraConfig.antiDeleteGroups,
                v -> VeyraConfig.setPeerEnabled(VeyraConfig.CATEGORY_ANTI_DELETE, VeyraConfig.PEER_GROUP, v),
                true,
                () -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putInt("category", VeyraConfig.CATEGORY_ANTI_DELETE);
                    b.putInt("peerType", VeyraConfig.PEER_GROUP);
                    presentFragment(new VeyraScopeSettingsActivity(b));
                }
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraChatTypeChannels", R.string.VeyraChatTypeChannels),
                "Tap to configure exceptions and private channel filter",
                () -> VeyraConfig.antiDeleteChannels,
                v -> VeyraConfig.setPeerEnabled(VeyraConfig.CATEGORY_ANTI_DELETE, VeyraConfig.PEER_CHANNEL, v),
                true,
                () -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putInt("category", VeyraConfig.CATEGORY_ANTI_DELETE);
                    b.putInt("peerType", VeyraConfig.PEER_CHANNEL);
                    presentFragment(new VeyraScopeSettingsActivity(b));
                }
        ));

        r.add(VeyraSettingsRow.toggleWithAction(
                LocaleController.getString("VeyraChatTypeBots", R.string.VeyraChatTypeBots),
                "Tap to configure exceptions",
                () -> VeyraConfig.antiDeleteBots,
                v -> VeyraConfig.setPeerEnabled(VeyraConfig.CATEGORY_ANTI_DELETE, VeyraConfig.PEER_BOT, v),
                true,
                () -> {
                    android.os.Bundle b = new android.os.Bundle();
                    b.putInt("category", VeyraConfig.CATEGORY_ANTI_DELETE);
                    b.putInt("peerType", VeyraConfig.PEER_BOT);
                    presentFragment(new VeyraScopeSettingsActivity(b));
                }
        ));

        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraActions", R.string.VeyraActions)));

        r.add(VeyraSettingsRow.button(
                LocaleController.getString("VeyraClearAllAntiDelete", R.string.VeyraClearAllAntiDelete),
                true,
                false,
                () -> {
                    if (getParentActivity() == null) return;
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("VeyraClearAllAntiDelete", R.string.VeyraClearAllAntiDelete));
                    builder.setMessage(LocaleController.getString("VeyraClearAllAntiDeleteConfirm", R.string.VeyraClearAllAntiDeleteConfirm));
                    builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
                        MessagesStorage.getInstance(currentAccount).clearAllAntiDelete();
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, LocaleController.getString("VeyraAntiDeleteCleared", R.string.VeyraAntiDeleteCleared)).show();
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

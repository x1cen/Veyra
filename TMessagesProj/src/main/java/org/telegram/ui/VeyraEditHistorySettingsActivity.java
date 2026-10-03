package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
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

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraReactionHistoryEnable", R.string.VeyraReactionHistoryEnable),
                LocaleController.getString("VeyraReactionHistoryEnableDesc", R.string.VeyraReactionHistoryEnableDesc),
                () -> VeyraConfig.reactionHistoryEnabled,
                v -> VeyraConfig.setReactionHistoryEnabled(v),
                true
        ));

        r.add(VeyraSettingsRow.detail(
                LocaleController.getString("VeyraReactionHistoryLimit", R.string.VeyraReactionHistoryLimit),
                () -> String.valueOf(VeyraConfig.reactionHistoryLimit),
                true,
                this::showReactionLimitDialog
        ));

        r.add(VeyraSettingsRow.detail(
                LocaleController.getString("VeyraEditHistoryLimit", R.string.VeyraEditHistoryLimit),
                () -> String.valueOf(VeyraConfig.editHistoryLimit),
                true,
                this::showLimitDialog
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraEditHistoryDropOldest", R.string.VeyraEditHistoryDropOldest),
                LocaleController.getString("VeyraEditHistoryDropOldestDesc", R.string.VeyraEditHistoryDropOldestDesc),
                () -> VeyraConfig.editHistoryDropOldest,
                v -> VeyraConfig.setEditHistoryDropOldest(v),
                true
        ));

        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraChatTypes", R.string.VeyraChatTypes)));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraChatTypePrivate", R.string.VeyraChatTypePrivate),
                LocaleController.getString("VeyraChatTypePrivateDesc", R.string.VeyraChatTypePrivateDesc),
                () -> VeyraConfig.editHistoryPrivate,
                v -> VeyraConfig.setEditHistoryPrivate(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraChatTypeGroups", R.string.VeyraChatTypeGroups),
                LocaleController.getString("VeyraChatTypeGroupsDesc", R.string.VeyraChatTypeGroupsDesc),
                () -> VeyraConfig.editHistoryGroups,
                v -> VeyraConfig.setEditHistoryGroups(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraChatTypeChannels", R.string.VeyraChatTypeChannels),
                LocaleController.getString("VeyraChatTypeChannelsDesc", R.string.VeyraChatTypeChannelsDesc),
                () -> VeyraConfig.editHistoryChannels,
                v -> VeyraConfig.setEditHistoryChannels(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraChatTypeBots", R.string.VeyraChatTypeBots),
                LocaleController.getString("VeyraChatTypeBotsDesc", R.string.VeyraChatTypeBotsDesc),
                () -> VeyraConfig.editHistoryBots,
                v -> VeyraConfig.setEditHistoryBots(v),
                true
        ));

        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.button(
                LocaleController.getString("VeyraEditHistoryClear", R.string.VeyraEditHistoryClear),
                true,
                false,
                () -> {
                    VeyraEditHistoryManager.clearAll();
                    BulletinFactory.of(VeyraEditHistorySettingsActivity.this)
                            .createSuccessBulletin(LocaleController.getString("VeyraEditHistoryCleared", R.string.VeyraEditHistoryCleared))
                            .show();
                }
        ));

        r.add(VeyraSettingsRow.shadow());

        return r;
    }

    private void showReactionLimitDialog() {
        if (getParentActivity() == null) return;
        Context context = getParentActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("VeyraReactionHistoryLimit", R.string.VeyraReactionHistoryLimit));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        TextView hint = new TextView(context);
        hint.setText(LocaleController.getString("VeyraReactionHistoryLimitDesc", R.string.VeyraReactionHistoryLimitDesc));
        hint.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        hint.setTextSize(14);
        container.addView(hint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        final EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(18);
        editText.setText(String.valueOf(VeyraConfig.reactionHistoryLimit));
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setGravity(Gravity.CENTER);
        container.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40));

        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            try {
                int val = Integer.parseInt(editText.getText().toString().trim());
                val = Math.max(5, Math.min(100, val));
                VeyraConfig.setReactionHistoryLimit(val);
                if (listView != null && listView.getAdapter() != null) {
                    listView.getAdapter().notifyDataSetChanged();
                }
            } catch (Exception ignore) {
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showLimitDialog() {
        if (getParentActivity() == null) return;
        Context context = getParentActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("VeyraEditHistoryLimit", R.string.VeyraEditHistoryLimit));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        TextView hint = new TextView(context);
        hint.setText(LocaleController.getString("VeyraEditHistoryLimitDesc", R.string.VeyraEditHistoryLimitDesc));
        hint.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        hint.setTextSize(14);
        container.addView(hint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        final EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(18);
        editText.setText(String.valueOf(VeyraConfig.editHistoryLimit));
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setGravity(Gravity.CENTER);
        container.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40));

        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            try {
                int val = Integer.parseInt(editText.getText().toString().trim());
                val = Math.max(5, Math.min(100, val));
                VeyraConfig.setEditHistoryLimit(val);
                if (listView != null && listView.getAdapter() != null) {
                    listView.getAdapter().notifyDataSetChanged();
                }
            } catch (Exception ignore) {
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }
}

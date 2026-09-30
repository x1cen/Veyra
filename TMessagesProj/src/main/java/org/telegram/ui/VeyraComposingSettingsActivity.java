package org.telegram.ui;

import java.util.ArrayList;
import java.util.List;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;

public class VeyraComposingSettingsActivity extends VeyraSettingsBaseActivity {

    @Override
    protected String getScreenTitle() {
        return LocaleController.getString("VeyraComposingMessages", R.string.VeyraComposingMessages);
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();
        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraComposingMessages", R.string.VeyraComposingMessages)));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraMentionByName", R.string.VeyraMentionByName),
                LocaleController.getString("VeyraMentionByNameDesc", R.string.VeyraMentionByNameDesc),
                () -> VeyraConfig.mentionByName,
                v -> VeyraConfig.setMentionByName(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraHideSendAsButton", R.string.VeyraHideSendAsButton),
                LocaleController.getString("VeyraHideSendAsButtonDesc", R.string.VeyraHideSendAsButtonDesc),
                () -> VeyraConfig.hideSendAsButton,
                v -> VeyraConfig.setHideSendAsButton(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraDisableQuickReaction", R.string.VeyraDisableQuickReaction),
                LocaleController.getString("VeyraDisableQuickReactionDesc", R.string.VeyraDisableQuickReactionDesc),
                () -> VeyraConfig.disableQuickReaction,
                v -> VeyraConfig.setDisableQuickReaction(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraFormatTimeWithSeconds", R.string.VeyraFormatTimeWithSeconds),
                LocaleController.getString("VeyraFormatTimeWithSecondsDesc", R.string.VeyraFormatTimeWithSecondsDesc),
                () -> VeyraConfig.formatTimeWithSeconds,
                v -> VeyraConfig.setFormatTimeWithSeconds(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraStripBotLinkParams", R.string.VeyraStripBotLinkParams),
                LocaleController.getString("VeyraStripBotLinkParamsDesc", R.string.VeyraStripBotLinkParamsDesc),
                () -> VeyraConfig.stripBotLinkParams,
                v -> VeyraConfig.setStripBotLinkParams(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraAnonymousForwardNoQuote", R.string.VeyraAnonymousForwardNoQuote),
                LocaleController.getString("VeyraAnonymousForwardNoQuoteDesc", R.string.VeyraAnonymousForwardNoQuoteDesc),
                () -> VeyraConfig.anonymousForwardNoQuote,
                v -> VeyraConfig.setAnonymousForwardNoQuote(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraDisableBigEmoji", R.string.VeyraDisableBigEmoji),
                LocaleController.getString("VeyraDisableBigEmojiDesc", R.string.VeyraDisableBigEmojiDesc),
                () -> VeyraConfig.disableBigEmoji,
                v -> VeyraConfig.setDisableBigEmoji(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraJumpToFirstMessage", R.string.VeyraJumpToFirstMessage),
                LocaleController.getString("VeyraJumpToFirstMessageDesc", R.string.VeyraJumpToFirstMessageDesc),
                () -> VeyraConfig.jumpToFirstMessage,
                v -> VeyraConfig.setJumpToFirstMessage(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraCopyDialogId", R.string.VeyraCopyDialogId),
                LocaleController.getString("VeyraCopyDialogIdDesc", R.string.VeyraCopyDialogIdDesc),
                () -> VeyraConfig.copyDialogId,
                v -> VeyraConfig.setCopyDialogId(v),
                false
        ));

        r.add(VeyraSettingsRow.shadow());
        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraEditHistory", R.string.VeyraEditHistory)));
        r.add(VeyraSettingsRow.detail(
                LocaleController.getString("VeyraEditHistoryLimit", R.string.VeyraEditHistoryLimit),
                () -> String.valueOf(VeyraConfig.editHistoryLimit),
                true,
                this::showEditHistoryLimitDialog
        ));
        r.add(VeyraSettingsRow.shadow());
        return r;
    }

    private void showEditHistoryLimitDialog() {
        if (getParentActivity() == null) return;
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("VeyraEditHistoryLimit", R.string.VeyraEditHistoryLimit));
        final android.widget.EditText input = new android.widget.EditText(getParentActivity());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(VeyraConfig.editHistoryLimit));
        input.selectAll();
        builder.setView(input);
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            try {
                int val = Integer.parseInt(input.getText().toString().trim());
                VeyraConfig.setEditHistoryLimit(val);
                reloadRows();
            } catch (NumberFormatException ignored) {}
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }
}

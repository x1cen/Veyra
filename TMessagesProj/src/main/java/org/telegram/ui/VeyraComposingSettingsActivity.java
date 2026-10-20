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
                LocaleController.getString("VeyraFormatTimeWithSeconds", R.string.VeyraFormatTimeWithSeconds),
                LocaleController.getString("VeyraFormatTimeWithSecondsDesc", R.string.VeyraFormatTimeWithSecondsDesc),
                () -> VeyraConfig.formatTimeWithSeconds,
                v -> VeyraConfig.setFormatTimeWithSeconds(v),
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
                LocaleController.getString("VeyraViewRaw", R.string.VeyraViewRaw),
                LocaleController.getString("VeyraViewRawDesc", R.string.VeyraViewRawDesc),
                () -> VeyraConfig.viewRawEnabled,
                v -> VeyraConfig.setViewRawEnabled(v),
                false
        ));

        r.add(VeyraSettingsRow.shadow());
        return r;
    }
}

package org.telegram.ui;

import java.util.ArrayList;
import java.util.List;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;

public class VeyraControlsSettingsActivity extends VeyraSettingsBaseActivity {

    @Override
    protected String getScreenTitle() {
        return LocaleController.getString("VeyraControlsGeneral", R.string.VeyraControlsGeneral);
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();
        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraControlsGeneral", R.string.VeyraControlsGeneral)));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraConfirmCall", R.string.VeyraConfirmCall),
                LocaleController.getString("VeyraConfirmCallDesc", R.string.VeyraConfirmCallDesc),
                () -> VeyraConfig.confirmCall,
                v -> VeyraConfig.setConfirmCall(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraConfirmLink", R.string.VeyraConfirmLink),
                LocaleController.getString("VeyraConfirmLinkDesc", R.string.VeyraConfirmLinkDesc),
                () -> VeyraConfig.confirmLink,
                v -> VeyraConfig.setConfirmLink(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraCleanUrls", R.string.VeyraCleanUrls),
                LocaleController.getString("VeyraCleanUrlsDesc", R.string.VeyraCleanUrlsDesc),
                () -> VeyraConfig.cleanUrls,
                v -> VeyraConfig.setCleanUrls(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraDisableUndo", R.string.VeyraDisableUndo),
                LocaleController.getString("VeyraDisableUndoDesc", R.string.VeyraDisableUndoDesc),
                () -> VeyraConfig.disableUndo,
                v -> VeyraConfig.setDisableUndo(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraDisableLinkPreview", R.string.VeyraDisableLinkPreview),
                LocaleController.getString("VeyraDisableLinkPreviewDesc", R.string.VeyraDisableLinkPreviewDesc),
                () -> VeyraConfig.disableLinkPreviewByDefault,
                v -> VeyraConfig.setDisableLinkPreviewByDefault(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraDisableVibration", R.string.VeyraDisableVibration),
                LocaleController.getString("VeyraDisableVibrationDesc", R.string.VeyraDisableVibrationDesc),
                () -> VeyraConfig.disableVibration,
                v -> VeyraConfig.setDisableVibration(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraPersianCalendar", R.string.VeyraPersianCalendar),
                LocaleController.getString("VeyraPersianCalendarDesc", R.string.VeyraPersianCalendarDesc),
                () -> VeyraConfig.persianCalendar,
                v -> VeyraConfig.setPersianCalendar(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraShowProfileId", R.string.VeyraShowProfileId),
                LocaleController.getString("VeyraShowProfileIdDesc", R.string.VeyraShowProfileIdDesc),
                () -> VeyraConfig.showProfileId,
                v -> VeyraConfig.setShowProfileId(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraBypassRestrictions", R.string.VeyraBypassRestrictions),
                LocaleController.getString("VeyraBypassRestrictionsDesc", R.string.VeyraBypassRestrictionsDesc),
                () -> VeyraConfig.ignoreContentRestrictions,
                v -> VeyraConfig.setIgnoreContentRestrictions(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraDisableTrending", R.string.VeyraDisableTrending),
                LocaleController.getString("VeyraDisableTrendingDesc", R.string.VeyraDisableTrendingDesc),
                () -> VeyraConfig.disableTrending,
                v -> VeyraConfig.setDisableTrending(v),
                false
        ));

        r.add(VeyraSettingsRow.shadow());
        return r;
    }
}

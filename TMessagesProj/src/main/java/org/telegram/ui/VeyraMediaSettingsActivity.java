package org.telegram.ui;

import java.util.ArrayList;
import java.util.List;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;

public class VeyraMediaSettingsActivity extends VeyraSettingsBaseActivity {

    @Override
    protected String getScreenTitle() {
        return LocaleController.getString("VeyraMediaCamera", R.string.VeyraMediaCamera);
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();
        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraMediaCamera", R.string.VeyraMediaCamera)));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraRearCameraVideoMessages", R.string.VeyraRearCameraVideoMessages),
                LocaleController.getString("VeyraRearCameraVideoMessagesDesc", R.string.VeyraRearCameraVideoMessagesDesc),
                () -> VeyraConfig.rearCameraVideoMessages,
                v -> VeyraConfig.setRearCameraVideoMessages(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraSendCaptionWithSticker", R.string.VeyraSendCaptionWithSticker),
                LocaleController.getString("VeyraSendCaptionWithStickerDesc", R.string.VeyraSendCaptionWithStickerDesc),
                () -> VeyraConfig.sendCaptionWithSticker,
                v -> VeyraConfig.setSendCaptionWithSticker(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraSendCaptionWithGif", R.string.VeyraSendCaptionWithGif),
                LocaleController.getString("VeyraSendCaptionWithGifDesc", R.string.VeyraSendCaptionWithGifDesc),
                () -> VeyraConfig.sendCaptionWithGif,
                v -> VeyraConfig.setSendCaptionWithGif(v),
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                LocaleController.getString("VeyraKeepOriginalFilename", R.string.VeyraKeepOriginalFilename),
                LocaleController.getString("VeyraKeepOriginalFilenameDesc", R.string.VeyraKeepOriginalFilenameDesc),
                () -> VeyraConfig.keepOriginalFilename,
                v -> VeyraConfig.setKeepOriginalFilename(v),
                false
        ));

        r.add(VeyraSettingsRow.shadow());
        return r;
    }
}

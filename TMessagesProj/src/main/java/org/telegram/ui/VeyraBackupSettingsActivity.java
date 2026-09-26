package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class VeyraBackupSettingsActivity extends VeyraSettingsBaseActivity {

    @Override
    protected String getScreenTitle() {
        return LocaleController.getString("VeyraBackupRestore", R.string.VeyraBackupRestore);
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();
        r.add(VeyraSettingsRow.header(LocaleController.getString("VeyraBackupRestore", R.string.VeyraBackupRestore)));

        r.add(VeyraSettingsRow.button(
                LocaleController.getString("VeyraExportSettings", R.string.VeyraExportSettings),
                false, true,
                this::exportSettings
        ));

        r.add(VeyraSettingsRow.button(
                LocaleController.getString("VeyraImportSettings", R.string.VeyraImportSettings),
                false, true,
                this::importSettings
        ));

        r.add(VeyraSettingsRow.button(
                LocaleController.getString("VeyraResetSettings", R.string.VeyraResetSettings),
                true, false,
                this::resetSettings
        ));

        r.add(VeyraSettingsRow.shadow());
        return r;
    }

    private void exportSettings() {
        try {
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("veyraconfig", 0);
            Map<String, ?> all = preferences.getAll();
            JSONObject json = new JSONObject();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                // Do not export settings lock hash/salt for security
                if ("settingsLockHash".equals(entry.getKey()) || "settingsLockSalt".equals(entry.getKey())) {
                    continue;
                }
                json.put(entry.getKey(), entry.getValue());
            }
            String result = json.toString(2);
            AndroidUtilities.addToClipboard(result);
            BulletinFactory.of(VeyraBackupSettingsActivity.this).createCopyBulletin(LocaleController.getString("VeyraExportCopied", R.string.VeyraExportCopied)).show();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void importSettings() {
        if (getParentActivity() == null) return;
        boolean isFarsi = "fa".equals(LocaleController.getInstance().getCurrentLocale().getLanguage());

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("VeyraImportSettings", R.string.VeyraImportSettings));

        final EditTextBoldCursor editText = new EditTextBoldCursor(getParentActivity());
        editText.setTextSize(16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setHint(isFarsi ? "کد JSON تنظیمات را اینجا جای‌گذاری کنید" : "Paste JSON settings here");
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setGravity(Gravity.TOP | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT));
        editText.setMinLines(4);
        editText.setMaxLines(10);

        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), 0);
        container.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(container);

        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            try {
                String text = editText.getText().toString().trim();
                if (text.isEmpty()) return;
                JSONObject json = new JSONObject(text);
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("veyraconfig", 0);
                SharedPreferences.Editor editor = preferences.edit();
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if ("settingsLockHash".equals(key) || "settingsLockSalt".equals(key)) {
                        continue;
                    }
                    Object val = json.get(key);
                    if (val instanceof Boolean) {
                        editor.putBoolean(key, (Boolean) val);
                    } else if (val instanceof Integer) {
                        editor.putInt(key, (Integer) val);
                    } else if (val instanceof Long) {
                        editor.putLong(key, (Long) val);
                    } else if (val instanceof String) {
                        editor.putString(key, (String) val);
                    }
                }
                editor.apply();
                // Reload config in memory
                VeyraConfig.reloadConfig();
                reloadRows();
                BulletinFactory.of(VeyraBackupSettingsActivity.this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString("VeyraImportSuccess", R.string.VeyraImportSuccess)).show();
            } catch (Exception e) {
                FileLog.e(e);
                BulletinFactory.of(VeyraBackupSettingsActivity.this).createErrorBulletin(isFarsi ? "فرمت JSON نامعتبر است" : "Invalid JSON format").show();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    private void resetSettings() {
        if (getParentActivity() == null) return;
        boolean isFarsi = "fa".equals(LocaleController.getInstance().getCurrentLocale().getLanguage());

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("VeyraResetSettings", R.string.VeyraResetSettings));
        builder.setMessage(isFarsi ? "آیا از بازنشانی تمام تنظیمات ویرا به مقادیر اولیه اطمینان دارید؟" : "Are you sure you want to reset all Veyra settings to default values?");
        builder.setPositiveButton(LocaleController.getString("Reset", R.string.Reset), (dialog, which) -> {
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("veyraconfig", 0);
            preferences.edit().clear().apply();
            VeyraConfig.reloadConfig();
            reloadRows();
            BulletinFactory.of(VeyraBackupSettingsActivity.this).createSimpleBulletin(R.raw.chats_infotip, isFarsi ? "تنظیمات با موفقیت بازنشانی شد" : "Settings reset to defaults").show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }
}

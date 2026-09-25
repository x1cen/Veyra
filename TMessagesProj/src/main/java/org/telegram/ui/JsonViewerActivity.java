package org.telegram.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableString;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * Full-screen viewer for the raw JSON export of a message, with color syntax
 * highlighting, unrestricted height, and a proper scroll view.
 */
public class JsonViewerActivity extends BaseFragment {

    public interface JsonSupplier {
        String get();
    }

    private final JsonSupplier jsonSupplier;
    private final long messageId;
    private String customTitle;

    private ScrollView scrollView;
    private TextView jsonTextView;
    private RadialProgressView progressView;
    private FrameLayout container;

    public JsonViewerActivity(JsonSupplier jsonSupplier, long messageId) {
        super();
        this.jsonSupplier = jsonSupplier;
        this.messageId = messageId;
    }

    public JsonViewerActivity(JsonSupplier jsonSupplier, String customTitle) {
        super();
        this.jsonSupplier = jsonSupplier;
        this.messageId = 0;
        this.customTitle = customTitle;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(customTitle != null ? customTitle : LocaleController.getString(R.string.MessageDetails));
        actionBar.setSubtitle("JSON");
        actionBar.setAllowOverlayTitle(true);

        org.telegram.ui.ActionBar.ActionBarMenu menu = actionBar.createMenu();
        org.telegram.ui.ActionBar.ActionBarMenuItem shareItem = menu.addItem(1, R.drawable.msg_share);
        org.telegram.ui.ActionBar.ActionBarMenuItem copyItem = menu.addItem(2, R.drawable.msg_copy);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    shareJson();
                } else if (id == 2) {
                    copyJson();
                }
            }
        });

        container = new FrameLayout(context);
        container.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(true);

        FrameLayout innerCard = new FrameLayout(context);
        innerCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(0),
                Theme.isCurrentThemeDark() ? 0xFF1E1E2E : 0xFFF1F3F5));
        innerCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(24));

        jsonTextView = new TextView(context);
        jsonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        jsonTextView.setTypeface(android.graphics.Typeface.MONOSPACE);
        jsonTextView.setTextIsSelectable(true);
        jsonTextView.setLineSpacing(AndroidUtilities.dp(4), 1.0f);
        jsonTextView.setTextColor(Theme.isCurrentThemeDark() ? 0xFFECEFF1 : 0xFF263238);
        jsonTextView.setGravity(Gravity.LEFT);

        innerCard.addView(jsonTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        scrollView.addView(innerCard, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        container.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        progressView = new RadialProgressView(context);
        progressView.setSize(AndroidUtilities.dp(32));
        container.addView(progressView, LayoutHelper.createFrame(32, 32, Gravity.CENTER, 0, 0, 0, 0));

        fragmentView = container;

        loadJsonAsync();

        return fragmentView;
    }

    private void loadJsonAsync() {
        org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
            String raw;
            try {
                raw = jsonSupplier != null ? jsonSupplier.get() : "";
            } catch (Throwable e) {
                FileLog.e(e);
                raw = "";
            }
            final String finalRaw = raw == null ? "" : raw;
            final SpannableString highlighted = JsonTextSettingsCell.highlightJson(finalRaw, Theme.isCurrentThemeDark());
            AndroidUtilities.runOnUIThread(() -> {
                if (jsonTextView == null) return;
                cachedJson = finalRaw;
                if (finalRaw.isEmpty()) {
                    jsonTextView.setText(LocaleController.getString(R.string.NoResult));
                } else {
                    jsonTextView.setText(highlighted);
                }
                if (progressView != null) {
                    progressView.setVisibility(View.GONE);
                }
            });
        });
    }

    private String cachedJson = "";

    private void copyJson() {
        if (cachedJson.isEmpty()) return;
        try {
            AndroidUtilities.addToClipboard(cachedJson);
            org.telegram.ui.Components.BulletinFactory.of(this)
                    .createCopyBulletin(LocaleController.getString(R.string.TextCopied))
                    .show();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private void shareJson() {
        if (cachedJson.isEmpty() || getParentActivity() == null) return;
        try {
            String outFileName = (messageId != 0 ? "veyra_msg_" + messageId : "veyra_details_" + System.currentTimeMillis()) + ".json";
            File downloadsDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadsDir = getParentActivity().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
            } else {
                downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            }
            if (downloadsDir != null && !downloadsDir.exists()) downloadsDir.mkdirs();
            File outFile = new File(downloadsDir, outFileName);
            try (FileWriter fw = new FileWriter(outFile)) {
                fw.write(cachedJson);
            }
            Uri uri = FileProvider.getUriForFile(
                    getParentActivity(),
                    ApplicationLoader.getApplicationId() + ".provider",
                    outFile
            );
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            getParentActivity().startActivity(Intent.createChooser(shareIntent, LocaleController.getString(R.string.ShareFile)));
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_avatar_actionBarIconBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_avatar_actionBarSelectorBlue));
        return themeDescriptions;
    }
}
package org.telegram.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonViewerActivity extends BaseFragment {

    public interface JsonProvider {
        String getJson();
    }

    private static final int MENU_COPY = 1;
    private static final Pattern JSON_TOKEN_PATTERN = Pattern.compile(
            "(\"(?:\\\\.|[^\"\\\\])*\"\\s*:)" +                       // Group 1: JSON Key ("key":)
            "|(\"(?:\\\\.|[^\"\\\\])*\")" +                            // Group 2: String value ("value")
            "|(\\b-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b)" + // Group 3: Number
            "|(\\btrue\\b|\\bfalse\\b)" +                              // Group 4: Boolean
            "|(\\bnull\\b)"                                            // Group 5: Null
    );

    private final JsonProvider provider;
    private final String title;
    private String cachedJson;

    private TextView jsonTextView;
    private ProgressBar progressBar;
    private FrameLayout bottomBar;

    public JsonViewerActivity(JsonProvider provider, String title) {
        this.provider = provider;
        this.title = title != null ? title : "View Raw";
    }

    public JsonViewerActivity(JsonProvider provider) {
        this(provider, "View Raw");
    }

    @Override
    public View createView(Context context) {
        final boolean isDark = Theme.isCurrentThemeDark();

        // Dynamically synced with user's active theme
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(title);
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false);
        actionBar.setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        actionBar.setCastShadows(true);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_COPY) {
                    copyJsonToClipboard();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(MENU_COPY, R.drawable.msg_copy);

        FrameLayout rootLayout = new FrameLayout(context);
        // Palette synced with theme mode (GitHub Dark #0D1117 vs GitHub Light #F6F8FA)
        int bgColor = isDark ? 0xFF0D1117 : 0xFFF6F8FA;
        rootLayout.setBackgroundColor(bgColor);
        fragmentView = rootLayout;

        // Content ScrollView with word wrapping (no horizontal overflow)
        ScrollView verticalScrollView = new ScrollView(context);
        verticalScrollView.setFillViewport(true);

        jsonTextView = new TextView(context);
        jsonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        jsonTextView.setTypeface(Typeface.MONOSPACE);
        // Contrast-aware text color for light & dark
        int textColor = isDark ? 0xFFE6EDF3 : 0xFF24292F;
        jsonTextView.setTextColor(textColor);
        jsonTextView.setTextIsSelectable(true);
        jsonTextView.setGravity(Gravity.LEFT | Gravity.TOP);
        jsonTextView.setTextDirection(View.TEXT_DIRECTION_LTR);
        jsonTextView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            jsonTextView.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
            jsonTextView.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
        }
        jsonTextView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(80)); // Bottom padding for floating bar

        verticalScrollView.addView(jsonTextView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rootLayout.addView(verticalScrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Loading spinner
        progressBar = new ProgressBar(context);
        rootLayout.addView(progressBar, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        // Floating Bottom Copy Bar (Theme accent styled)
        bottomBar = new FrameLayout(context);
        int btnColor = Theme.getColor(Theme.key_featuredStickers_addButton);
        int btnPressed = Theme.getColor(Theme.key_featuredStickers_addButtonPressed);
        bottomBar.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(10), btnColor, btnPressed));
        bottomBar.setElevation(AndroidUtilities.dp(4));

        LinearLayout buttonContent = new LinearLayout(context);
        buttonContent.setOrientation(LinearLayout.HORIZONTAL);
        buttonContent.setGravity(Gravity.CENTER);

        TextView copyText = new TextView(context);
        copyText.setText(LocaleController.getString("Copy", R.string.Copy) + " JSON");
        copyText.setTextColor(Color.WHITE);
        copyText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        copyText.setTypeface(AndroidUtilities.bold());

        buttonContent.addView(copyText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
        bottomBar.addView(buttonContent, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
        bottomBar.setOnClickListener(v -> copyJsonToClipboard());

        rootLayout.addView(bottomBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 16, 0, 16, 16));

        // Load & highlight JSON asynchronously
        loadAndHighlightJson();

        return fragmentView;
    }

    private void loadAndHighlightJson() {
        progressBar.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.GONE);

        Utilities.globalQueue.postRunnable(() -> {
            String jsonStr;
            try {
                jsonStr = provider != null ? provider.getJson() : "{}";
            } catch (Throwable e) {
                jsonStr = "{\"error\": \"" + e.getMessage() + "\"}";
            }
            if (jsonStr == null) {
                jsonStr = "{}";
            }
            cachedJson = jsonStr;

            final CharSequence formatted = highlightJson(jsonStr);

            AndroidUtilities.runOnUIThread(() -> {
                if (jsonTextView != null) {
                    jsonTextView.setText(formatted);
                    progressBar.setVisibility(View.GONE);
                    bottomBar.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private CharSequence highlightJson(String json) {
        if (json == null || json.length() > 500_000) {
            // For massive objects fallback to raw string without spans to avoid OOM
            return json != null ? json : "";
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder(json);
        Matcher matcher = JSON_TOKEN_PATTERN.matcher(json);

        final boolean isDark = Theme.isCurrentThemeDark();

        // Theme-adaptive code palette (GitHub Dark vs GitHub Light)
        final int COLOR_KEY = isDark ? 0xFF79C0FF : 0xFF0969DA;     // Cyan in dark / Deep Blue in light
        final int COLOR_STRING = isDark ? 0xFF7EE787 : 0xFF1A7F37;  // Mint in dark / Forest Green in light
        final int COLOR_NUMBER = isDark ? 0xFFFFA657 : 0xFF9A6700;  // Gold in dark / Warm Amber in light
        final int COLOR_BOOL = isDark ? 0xFFFF7B72 : 0xFFCF222E;    // Coral Pink in dark / Crimson in light
        final int COLOR_NULL = isDark ? 0xFF8B949E : 0xFF6E7781;    // Silver Slate in dark / Charcoal Slate in light

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Key (ends with :)
                int start = matcher.start(1);
                int end = matcher.end(1);
                int colonIdx = json.lastIndexOf(':', end);
                int keyEnd = colonIdx > start ? colonIdx : end;
                ssb.setSpan(new ForegroundColorSpan(COLOR_KEY), start, keyEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), start, keyEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(2) != null) {
                // String value
                ssb.setSpan(new ForegroundColorSpan(COLOR_STRING), matcher.start(2), matcher.end(2), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(3) != null) {
                // Number
                ssb.setSpan(new ForegroundColorSpan(COLOR_NUMBER), matcher.start(3), matcher.end(3), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(4) != null) {
                // Boolean
                ssb.setSpan(new ForegroundColorSpan(COLOR_BOOL), matcher.start(4), matcher.end(4), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), matcher.start(4), matcher.end(4), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(5) != null) {
                // Null
                ssb.setSpan(new ForegroundColorSpan(COLOR_NULL), matcher.start(5), matcher.end(5), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new StyleSpan(Typeface.ITALIC), matcher.start(5), matcher.end(5), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return ssb;
    }

    private void copyJsonToClipboard() {
        if (cachedJson == null || getParentActivity() == null) return;
        try {
            ClipboardManager clipboard = (ClipboardManager) getParentActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("JSON", cachedJson);
            clipboard.setPrimaryClip(clip);
            BulletinFactory.of(this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
        } catch (Throwable e) {
            // fallback
        }
    }
}

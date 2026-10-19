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
        // Bright clean action bar
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(title);
        actionBar.setItemsColor(0xFF24292F, false);
        actionBar.setItemsBackgroundColor(0x1F000000, false);
        actionBar.setTitleColor(0xFF24292F);
        actionBar.setBackgroundColor(0xFFFFFFFF);
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
        rootLayout.setBackgroundColor(0xFFF6F8FA); // Elegant bright light canvas (GitHub light theme)
        fragmentView = rootLayout;

        // Content ScrollViews
        ScrollView verticalScrollView = new ScrollView(context);
        verticalScrollView.setFillViewport(true);

        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(context);
        horizontalScrollView.setFillViewport(true);

        jsonTextView = new TextView(context);
        jsonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        jsonTextView.setTypeface(Typeface.MONOSPACE);
        jsonTextView.setTextColor(0xFF24292F); // Crisp dark charcoal
        jsonTextView.setTextIsSelectable(true);
        jsonTextView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(80)); // Bottom padding for floating bar

        horizontalScrollView.addView(jsonTextView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        verticalScrollView.addView(horizontalScrollView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rootLayout.addView(verticalScrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Loading spinner
        progressBar = new ProgressBar(context);
        rootLayout.addView(progressBar, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        // Floating Bottom Copy Bar (Bright accent style)
        bottomBar = new FrameLayout(context);
        bottomBar.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(10), 0xFF0969DA, 0xFF0550AE));
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

        // Modern Light Code Palette (GitHub Light style)
        final int COLOR_KEY = 0xFF0969DA;     // Vibrant Deep Blue for keys
        final int COLOR_STRING = 0xFF1A7F37;  // Forest Green for strings
        final int COLOR_NUMBER = 0xFF9A6700;  // Warm Amber/Bronze for numbers
        final int COLOR_BOOL = 0xFFCF222E;    // Crimson Red for booleans
        final int COLOR_NULL = 0xFF6E7781;    // Slate Gray for null

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

package org.telegram.ui;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Components.LayoutHelper;

public class JsonTextSettingsCell extends TextDetailSettingsCell {

    public interface FullJsonProvider {
        String getFullJson();
        void onFullJsonCopied();
    }

    private FullJsonProvider fullJsonProvider;

    private final FrameLayout cardContainer;
    private final TextView jsonTextView;
    private boolean needDivider;
    private int currentRequestId;
    private String currentChunkText;
    private SpannableString currentSpannable;

    public JsonTextSettingsCell(Context context) {
        super(context);

        getTextView().setVisibility(GONE);
        getValueTextView().setVisibility(GONE);

        cardContainer = new FrameLayout(context);
        updateCardBackground();
        cardContainer.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        jsonTextView = new TextView(context);
        jsonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        jsonTextView.setGravity(Gravity.LEFT);
        jsonTextView.setLines(0);
        jsonTextView.setMaxLines(0);
        jsonTextView.setSingleLine(false);
        jsonTextView.setTextIsSelectable(true);
        jsonTextView.setIncludeFontPadding(false);
        jsonTextView.setTypeface(android.graphics.Typeface.MONOSPACE);
        jsonTextView.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        jsonTextView.setCustomSelectionActionModeCallback(buildSelectionActionModeCallback());

        cardContainer.addView(jsonTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        addView(cardContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 14, 6, 14, 8));

        setClickable(false);
        setFocusable(false);
    }

    private void updateCardBackground() {
        boolean dark = Theme.isCurrentThemeDark();
        int cardBg = dark ? 0xFF1E1E2E : 0xFFF1F3F5;
        cardContainer.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), cardBg));
        if (jsonTextView != null) {
            jsonTextView.setTextColor(dark ? 0xFFECEFF1 : 0xFF263238);
        }
    }

    public void setJsonChunk(String rawChunk, SpannableString cached, boolean divider, java.util.Map<String, SpannableString> cacheStore) {
        needDivider = divider;
        setWillNotDraw(!divider);
        updateCardBackground();

        if (rawChunk == null || rawChunk.isEmpty()) {
            currentRequestId++;
            currentChunkText = null;
            currentSpannable = null;
            jsonTextView.setText("");
            return;
        }

        if (cached != null) {
            currentRequestId++;
            currentChunkText = rawChunk;
            currentSpannable = cached;
            jsonTextView.setText(cached);
            return;
        }

        final int requestId = ++currentRequestId;
        currentChunkText = rawChunk;

        // Instant highlight
        boolean dark = Theme.isCurrentThemeDark();
        SpannableString highlighted = highlightJson(rawChunk, dark);
        currentSpannable = highlighted;
        jsonTextView.setText(highlighted);

        if (cacheStore != null) {
            cacheStore.put(rawChunk, highlighted);
        }
    }

    public static SpannableString highlightJson(String json, boolean isDark) {
        if (json == null || json.isEmpty()) return new SpannableString("");
        SpannableString ss = new SpannableString(json);

        int keyColor = isDark ? 0xFF81D4FA : 0xFF0277BD;       // Cyan/Light Blue in dark, Deep Blue in light
        int stringColor = isDark ? 0xFFA5D6A7 : 0xFF2E7D32;    // Pale Green in dark, Forest Green in light
        int numberColor = isDark ? 0xFFFFCC80 : 0xFFE65100;    // Amber in dark, Deep Orange in light
        int boolColor = isDark ? 0xFFCE93D8 : 0xFF7B1FA2;      // Light Purple in dark, Deep Purple in light
        int punctColor = isDark ? 0xFF90A4AE : 0xFF607D8B;     // Muted Blue Grey

        try {
            // 1. Strings and Keys: "([^"\\]|\\.)*"
            java.util.regex.Pattern strPattern = java.util.regex.Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");
            java.util.regex.Matcher strMatcher = strPattern.matcher(json);
            while (strMatcher.find()) {
                int start = strMatcher.start();
                int end = strMatcher.end();
                int pos = end;
                while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                    pos++;
                }
                if (pos < json.length() && json.charAt(pos) == ':') {
                    ss.setSpan(new ForegroundColorSpan(keyColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    ss.setSpan(new ForegroundColorSpan(stringColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            // 2. Numbers
            java.util.regex.Pattern numPattern = java.util.regex.Pattern.compile("-?\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b");
            java.util.regex.Matcher numMatcher = numPattern.matcher(json);
            while (numMatcher.find()) {
                int start = numMatcher.start();
                int end = numMatcher.end();
                if (ss.getSpans(start, end, ForegroundColorSpan.class).length == 0) {
                    ss.setSpan(new ForegroundColorSpan(numberColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            // 3. Booleans and Null
            java.util.regex.Pattern boolPattern = java.util.regex.Pattern.compile("\\b(true|false|null)\\b");
            java.util.regex.Matcher boolMatcher = boolPattern.matcher(json);
            while (boolMatcher.find()) {
                int start = boolMatcher.start();
                int end = boolMatcher.end();
                if (ss.getSpans(start, end, ForegroundColorSpan.class).length == 0) {
                    ss.setSpan(new ForegroundColorSpan(boolColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            // 4. Brackets and Punctuation
            java.util.regex.Pattern punctPattern = java.util.regex.Pattern.compile("[\\{\\}\\[\\]:,]");
            java.util.regex.Matcher punctMatcher = punctPattern.matcher(json);
            while (punctMatcher.find()) {
                int start = punctMatcher.start();
                int end = punctMatcher.end();
                if (ss.getSpans(start, end, ForegroundColorSpan.class).length == 0) {
                    ss.setSpan(new ForegroundColorSpan(punctColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        } catch (Throwable ignored) {
        }

        return ss;
    }

    public void setJsonChunkAsync(String rawChunk, boolean divider) {
        setJsonChunk(rawChunk, null, divider, null);
    }

    public void refreshHighlighting() {
        if (currentChunkText != null) {
            SpannableString highlighted = highlightJson(currentChunkText, Theme.isCurrentThemeDark());
            currentSpannable = highlighted;
            jsonTextView.setText(highlighted);
        }
        updateCardBackground();
        jsonTextView.invalidate();
    }

    public void cacheIfReady(java.util.Map<String, SpannableString> store) {
        if (store == null || currentSpannable == null || currentChunkText == null) {
            return;
        }
        store.put(currentChunkText, currentSpannable);
    }

    public void setTitle(CharSequence title) {
        if (title == null) {
            getTextView().setVisibility(GONE);
            return;
        }
        getTextView().setText(title);
        getTextView().setVisibility(VISIBLE);
    }

    public void setChunkLayout(boolean isFirstChunk, boolean isLastChunk) {
        setMinimumHeight(0);
        int topMargin = isFirstChunk ? AndroidUtilities.dp(6) : 0;
        int bottomMargin = isLastChunk ? AndroidUtilities.dp(8) : 0;
        android.view.ViewGroup.LayoutParams params = cardContainer.getLayoutParams();
        if (params instanceof android.widget.FrameLayout.LayoutParams) {
            android.widget.FrameLayout.LayoutParams lp = (android.widget.FrameLayout.LayoutParams) params;
            lp.topMargin = topMargin;
            lp.bottomMargin = bottomMargin;
            cardContainer.setLayoutParams(lp);
        }
    }

    public void setFullJsonProvider(FullJsonProvider provider) {
        this.fullJsonProvider = provider;
    }

    private ActionMode.Callback buildSelectionActionModeCallback() {
        return new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                if (fullJsonProvider != null && menu.findItem(R_ID_COPY_ALL_JSON) == null) {
                    String label = LocaleController.getString(org.telegram.messenger.R.string.Copy)
                            + " · "
                            + LocaleController.getString(org.telegram.messenger.R.string.ExportAsJson);
                    menu.add(Menu.NONE, R_ID_COPY_ALL_JSON, Menu.NONE, label);
                }
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R_ID_COPY_ALL_JSON) {
                    if (fullJsonProvider != null) {
                        String full = fullJsonProvider.getFullJson();
                        if (full != null && !full.isEmpty()) {
                            try {
                                AndroidUtilities.addToClipboard(full);
                                fullJsonProvider.onFullJsonCopied();
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                    mode.finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        };
    }

    private static final int R_ID_COPY_ALL_JSON = View.generateViewId();

    @Override
    public void invalidate() {
        super.invalidate();
        if (jsonTextView != null) {
            jsonTextView.invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }
}
package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.veyra.client.aether.AetherController;

import java.util.List;

public class AetherLogsActivity extends BaseFragment {

    private ScrollView scrollView;
    private TextView logTextView;
    private SpannableStringBuilder logBuilder = new SpannableStringBuilder();

    private final AetherController.LogListener logListener = line -> {
        AndroidUtilities.runOnUIThread(() -> appendLogLine(line));
    };

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        AetherController.getInstance().addLogListener(logListener);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        AetherController.getInstance().removeLogListener(logListener);
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Aether Terminal");
        actionBar.setBackgroundColor(0xFF0D1117);
        actionBar.setTitleColor(0xFFFFFFFF);
        actionBar.setItemsColor(0xFFFFFFFF, false);
        actionBar.setItemsBackgroundColor(0x22FFFFFF, false);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(0xFF0D1117);
        fragmentView = frameLayout;

        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(true);
        frameLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        logTextView = new TextView(context);
        logTextView.setTypeface(Typeface.MONOSPACE);
        logTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        logTextView.setTextColor(0xFFC9D1D9);
        logTextView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(24));
        logTextView.setTextIsSelectable(true);
        scrollView.addView(logTextView, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        // Load existing logs
        List<String> existingLogs = AetherController.getInstance().getLogs();
        if (existingLogs != null) {
            for (String line : existingLogs) {
                formatLine(line);
            }
        }
        logTextView.setText(logBuilder);
        scrollToBottom();

        return fragmentView;
    }

    private void appendLogLine(String line) {
        if (logTextView == null) return;
        formatLine(line);
        logTextView.setText(logBuilder);
        scrollToBottom();
    }

    private void formatLine(String line) {
        int start = logBuilder.length();
        logBuilder.append(line).append("\n");
        int end = logBuilder.length();

        int color = 0xFFC9D1D9;
        if (line.contains("[+]") || line.contains("200") || line.contains("validated") || line.contains("connected")) {
            color = 0xFF56D364; // Green
        } else if (line.contains("error") || line.contains("failed") || line.contains("timeout") || line.contains("FAILED")) {
            color = 0xFFF85149; // Red
        } else if (line.contains("candidate") || line.contains("rtt") || line.contains("scanning") || line.contains("hunting")) {
            color = 0xFF79C0FF; // Cyan / Light Blue
        } else if (line.contains("[*]") || line.contains("reconnecting") || line.contains("warning") || line.contains("WARN")) {
            color = 0xFFE3B341; // Yellow / Gold
        }

        logBuilder.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void scrollToBottom() {
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }
}

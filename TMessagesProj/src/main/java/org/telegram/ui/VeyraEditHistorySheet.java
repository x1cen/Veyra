package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Bottom sheet showing the full edit history of a single message.
 * Entries are shown oldest-first: index 0 = first version captured (the text right
 * before the first edit), last entry = text right before the most-recent edit.
 * The "current" text is shown at the top as a header for context.
 */
public class VeyraEditHistorySheet extends BottomSheet {

    private final RecyclerListView listView;
    private int[] dates;
    private String[] texts;
    private final String currentText;

    public VeyraEditHistorySheet(Context context, int currentAccount, int mid, long dialogId, String currentText) {
        super(context, false);
        this.currentText = currentText != null ? currentText : "";

        setTitle(LocaleController.getString("VeyraEditHistoryTitle", R.string.VeyraEditHistoryTitle), true);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        // Current version header
        TextView currentLabel = new TextView(context);
        currentLabel.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        currentLabel.setTextSize(12);
        currentLabel.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), 0);
        currentLabel.setText(LocaleController.getString("VeyraEditHistoryCurrent", R.string.VeyraEditHistoryCurrent));
        container.addView(currentLabel);

        TextView currentView = new TextView(context);
        currentView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        currentView.setTextSize(15);
        currentView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(4), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        currentView.setText(this.currentText.isEmpty()
                ? LocaleController.getString("VeyraEditHistoryMediaCaption", R.string.VeyraEditHistoryMediaCaption)
                : this.currentText);
        container.addView(currentView);

        // Divider
        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        container.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1));

        // History list (populated after load)
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setClipToPadding(false);
        listView.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(16));
        listView.setAdapter(new HistoryAdapter());
        container.addView(listView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(container);

        // Load history
        MessagesStorage.getInstance(currentAccount).getEditHistory(mid, dialogId, (datesArr, textsArr) -> {
            this.dates = datesArr;
            this.texts = textsArr;
            listView.getAdapter().notifyDataSetChanged();
        });
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout cell = new LinearLayout(parent.getContext());
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(4));

            TextView timeView = new TextView(parent.getContext());
            timeView.setTag("time");
            timeView.setTextSize(11);
            timeView.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
            cell.addView(timeView);

            TextView textView = new TextView(parent.getContext());
            textView.setTag("text");
            textView.setTextSize(15);
            textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            cell.addView(textView);

            View divider = new View(parent.getContext());
            divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
            cell.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 8, 0, 0));

            cell.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new VH(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            // Show oldest first (index 0 = oldest)
            int idx = getItemCount() - 1 - position; // reverse: newest first
            int ts = dates[idx];
            String txt = texts[idx];

            // Format timestamp
            SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault());
            String timeStr = fmt.format(new Date((long) ts * 1000));
            holder.timeView.setText(timeStr);
            holder.textView.setText(txt.isEmpty()
                    ? LocaleController.getString("VeyraEditHistoryMediaCaption", R.string.VeyraEditHistoryMediaCaption)
                    : txt);
        }

        @Override
        public int getItemCount() {
            return dates == null ? 0 : dates.length;
        }

        class VH extends RecyclerView.ViewHolder {
            TextView timeView, textView;
            VH(View v) {
                super(v);
                timeView = v.findViewWithTag("time");
                textView = v.findViewWithTag("text");
            }
        }
    }
}

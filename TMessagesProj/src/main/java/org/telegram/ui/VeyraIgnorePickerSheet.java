package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

/**
 * Bottom sheet allowing the user to choose which content types from a specific user
 * should be ignored in a group chat.
 *
 * Usage:
 *   new VeyraIgnorePickerSheet(context, currentAccount, groupDialogId, peerId, userName)
 *       .show();
 */
public class VeyraIgnorePickerSheet extends BottomSheet {

    public interface Callback {
        void onFlagsSet(long peerId, int flags);
    }

    private static final String[] OPTION_KEYS = {
            "VeyraIgnoreMessages",
            "VeyraIgnoreReactions",
            "VeyraIgnoreVoice",
            "VeyraIgnoreVideoMsg",
            "VeyraIgnoreStickers",
            "VeyraIgnoreGifs",
            "VeyraIgnorePhotos",
            "VeyraIgnoreVideos",
    };
    private static final int[] OPTION_FLAGS = {
            MessagesStorage.IGNORE_MESSAGES,
            MessagesStorage.IGNORE_REACTIONS,
            MessagesStorage.IGNORE_VOICE,
            MessagesStorage.IGNORE_VIDEO_MSG,
            MessagesStorage.IGNORE_STICKERS,
            MessagesStorage.IGNORE_GIFS,
            MessagesStorage.IGNORE_PHOTOS,
            MessagesStorage.IGNORE_VIDEOS,
    };

    private final boolean[] checked;
    private final long peerId;
    private final long groupDialogId;
    private final int currentAccount;

    public VeyraIgnorePickerSheet(Context context, int currentAccount, long groupDialogId, long peerId, String userName) {
        super(context, false);
        this.currentAccount = currentAccount;
        this.groupDialogId = groupDialogId;
        this.peerId = peerId;
        this.checked = new boolean[OPTION_KEYS.length];

        String title = LocaleController.formatString("VeyraIgnorePickerTitle", R.string.VeyraIgnorePickerTitle, userName);
        setTitle(title, true);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));

        // Check rows
        for (int i = 0; i < OPTION_KEYS.length; i++) {
            final int idx = i;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
            row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));

            CheckBox cb = new CheckBox(context);
            cb.setChecked(false);
            cb.setTag(idx);
            row.addView(cb, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 12, 0));

            TextView label = new TextView(context);
            label.setTextSize(16);
            label.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            label.setText(LocaleController.getString(OPTION_KEYS[idx], getStringId(idx)));
            row.addView(label, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1));

            row.setOnClickListener(v -> {
                checked[idx] = !checked[idx];
                cb.setChecked(checked[idx]);
            });

            container.addView(row);
        }

        // "Select all" button
        TextView selectAll = new TextView(context);
        selectAll.setTextSize(14);
        selectAll.setTextColor(Theme.getColor(Theme.key_dialogTextLink));
        selectAll.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(4), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        selectAll.setText(LocaleController.getString("VeyraIgnoreSelectAll", R.string.VeyraIgnoreSelectAll));
        selectAll.setOnClickListener(v -> {
            boolean allChecked = true;
            for (boolean c : checked) if (!c) { allChecked = false; break; }
            for (int i = 0; i < checked.length; i++) checked[i] = !allChecked;
            container.invalidate();
            // Refresh checkboxes
            for (int i = 0; i < container.getChildCount() - 2; i++) {
                View row = container.getChildAt(i);
                if (row instanceof LinearLayout) {
                    CheckBox cb = (CheckBox) ((LinearLayout) row).getChildAt(0);
                    Object tag = cb.getTag();
                    if (tag instanceof Integer) cb.setChecked(checked[(Integer) tag]);
                }
            }
        });
        container.addView(selectAll);

        // Apply button
        TextView applyBtn = new TextView(context);
        applyBtn.setTextSize(15);
        applyBtn.setGravity(android.view.Gravity.CENTER);
        applyBtn.setTextColor(0xFFFFFFFF);
        applyBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8),
                Theme.getColor(Theme.key_dialogButton), Theme.getColor(Theme.key_dialogButtonSelector)));
        applyBtn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        applyBtn.setText(LocaleController.getString("VeyraIgnoreApply", R.string.VeyraIgnoreApply));
        applyBtn.setOnClickListener(v -> {
            int flags = 0;
            for (int i = 0; i < checked.length; i++) {
                if (checked[i]) flags |= OPTION_FLAGS[i];
            }
            MessagesStorage.getInstance(currentAccount).setIgnoreEntry(groupDialogId, peerId, flags);
            dismiss();
        });
        LinearLayout btnWrap = new LinearLayout(context);
        btnWrap.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), 0);
        btnWrap.addView(applyBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        container.addView(btnWrap);

        setCustomView(container);
    }

    private int getStringId(int idx) {
        switch (idx) {
            case 0: return R.string.VeyraIgnoreMessages;
            case 1: return R.string.VeyraIgnoreReactions;
            case 2: return R.string.VeyraIgnoreVoice;
            case 3: return R.string.VeyraIgnoreVideoMsg;
            case 4: return R.string.VeyraIgnoreStickers;
            case 5: return R.string.VeyraIgnoreGifs;
            case 6: return R.string.VeyraIgnorePhotos;
            case 7: return R.string.VeyraIgnoreVideos;
            default: return R.string.VeyraIgnoreMessages;
        }
    }
}

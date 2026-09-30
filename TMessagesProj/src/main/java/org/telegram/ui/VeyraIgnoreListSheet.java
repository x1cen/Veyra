package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Shows all ignored users for a group and allows removing them.
 */
public class VeyraIgnoreListSheet extends BottomSheet {

    private final int currentAccount;
    private final long groupDialogId;
    private long[] peerIds;
    private int[] flagsList;
    private ListAdapter adapter;

    public VeyraIgnoreListSheet(Context context, int currentAccount, long groupDialogId) {
        super(context, false);
        this.currentAccount = currentAccount;
        this.groupDialogId = groupDialogId;

        setTitle(LocaleController.getString("VeyraIgnoreList", R.string.VeyraIgnoreList), true);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(16));

        RecyclerView listView = new RecyclerView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new ListAdapter(context);
        listView.setAdapter(adapter);
        container.addView(listView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(container);
        reload();
    }

    private void reload() {
        MessagesStorage.getInstance(currentAccount).getIgnoreList(groupDialogId, (peers, flags) -> {
            peerIds = peers;
            flagsList = flags;
            adapter.notifyDataSetChanged();
        });
    }

    private String flagsDescription(int flags) {
        StringBuilder sb = new StringBuilder();
        if ((flags & MessagesStorage.IGNORE_MESSAGES) != 0)  append(sb, LocaleController.getString("VeyraIgnoreMessages", R.string.VeyraIgnoreMessages));
        if ((flags & MessagesStorage.IGNORE_REACTIONS) != 0) append(sb, LocaleController.getString("VeyraIgnoreReactions", R.string.VeyraIgnoreReactions));
        if ((flags & MessagesStorage.IGNORE_VOICE) != 0)     append(sb, LocaleController.getString("VeyraIgnoreVoice", R.string.VeyraIgnoreVoice));
        if ((flags & MessagesStorage.IGNORE_VIDEO_MSG) != 0) append(sb, LocaleController.getString("VeyraIgnoreVideoMsg", R.string.VeyraIgnoreVideoMsg));
        if ((flags & MessagesStorage.IGNORE_STICKERS) != 0)  append(sb, LocaleController.getString("VeyraIgnoreStickers", R.string.VeyraIgnoreStickers));
        if ((flags & MessagesStorage.IGNORE_GIFS) != 0)      append(sb, LocaleController.getString("VeyraIgnoreGifs", R.string.VeyraIgnoreGifs));
        if ((flags & MessagesStorage.IGNORE_PHOTOS) != 0)    append(sb, LocaleController.getString("VeyraIgnorePhotos", R.string.VeyraIgnorePhotos));
        if ((flags & MessagesStorage.IGNORE_VIDEOS) != 0)    append(sb, LocaleController.getString("VeyraIgnoreVideos", R.string.VeyraIgnoreVideos));
        return sb.toString();
    }

    private void append(StringBuilder sb, String s) {
        if (sb.length() > 0) sb.append(", ");
        sb.append(s);
    }

    private class ListAdapter extends RecyclerView.Adapter<ListAdapter.VH> {
        private final Context ctx;
        ListAdapter(Context ctx) { this.ctx = ctx; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(4));
            row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));

            TextView nameView = new TextView(ctx);
            nameView.setTag("name");
            nameView.setTextSize(16);
            nameView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            row.addView(nameView);

            TextView flagsView = new TextView(ctx);
            flagsView.setTag("flags");
            flagsView.setTextSize(12);
            flagsView.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
            row.addView(flagsView);

            TextView removeBtn = new TextView(ctx);
            removeBtn.setTag("remove");
            removeBtn.setTextSize(12);
            removeBtn.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
            removeBtn.setText(LocaleController.getString("VeyraIgnoreRemove", R.string.VeyraIgnoreRemove));
            removeBtn.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4));
            row.addView(removeBtn);

            View divider = new View(ctx);
            divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
            row.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 8, 0, 0));

            row.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new VH(row);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            long pid = peerIds[position];
            int flags = flagsList[position];

            // Resolve user name
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(pid);
            String name = user != null
                    ? (user.first_name + (user.last_name != null && !user.last_name.isEmpty() ? " " + user.last_name : ""))
                    : String.valueOf(pid);

            holder.nameView.setText(name);
            holder.flagsView.setText(flagsDescription(flags));
            holder.removeBtn.setOnClickListener(v -> {
                MessagesStorage.getInstance(currentAccount).setIgnoreEntry(groupDialogId, pid, 0);
                reload();
            });
        }

        @Override
        public int getItemCount() { return peerIds == null ? 0 : peerIds.length; }

        class VH extends RecyclerView.ViewHolder {
            TextView nameView, flagsView, removeBtn;
            VH(View v) {
                super(v);
                nameView = v.findViewWithTag("name");
                flagsView = v.findViewWithTag("flags");
                removeBtn = v.findViewWithTag("remove");
            }
        }
    }
}

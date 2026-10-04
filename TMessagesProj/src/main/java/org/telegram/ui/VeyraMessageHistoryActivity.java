package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.URLSpan;
import android.util.TypedValue;
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
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.VeyraConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.URLSpanMono;
import org.veyra.client.VeyraEditHistoryManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class VeyraMessageHistoryActivity extends BaseFragment {

    private final long dialogId;
    private final MessageObject currentMessageObject;

    private TextView tabMessagesView;
    private TextView tabReactionsView;
    private RecyclerListView messagesListView;
    private FrameLayout reactionsContainer;

    private static class MessageItem {
        final boolean isAction;
        final String actionLabel;
        final MessageObject messageObject;

        MessageItem(String actionLabel) {
            this.isAction = true;
            this.actionLabel = actionLabel;
            this.messageObject = null;
        }

        MessageItem(MessageObject messageObject) {
            this.isAction = false;
            this.actionLabel = null;
            this.messageObject = messageObject;
        }
    }

    private static class ReactionItem {
        final String emoji;
        final String title;
        final int date;
        final int count;
        final String action; // "add" or "remove"

        ReactionItem(String emoji, String title, int date, int count, String action) {
            this.emoji = emoji;
            this.title = title;
            this.date = date;
            this.count = count;
            this.action = action;
        }
    }

    private final ArrayList<MessageItem> messageItems = new ArrayList<>();
    private final ArrayList<ReactionItem> reactionItems = new ArrayList<>();

    public VeyraMessageHistoryActivity(long dialogId, MessageObject messageObject) {
        super();
        this.dialogId = dialogId;
        this.currentMessageObject = messageObject;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        loadData();
        return true;
    }

    private void loadData() {
        messageItems.clear();
        reactionItems.clear();

        if (currentMessageObject == null) return;
        int messageId = currentMessageObject.getId();

        // 1. Load Edit History
        List<VeyraEditHistoryManager.EditEntry> history = VeyraEditHistoryManager.getHistory(dialogId, messageId);
        if (history != null && !history.isEmpty()) {
            for (int i = 0; i < history.size(); i++) {
                VeyraEditHistoryManager.EditEntry entry = history.get(i);
                String label;
                if (i == 0) {
                    label = LocaleController.getString("OriginalMessage", R.string.OriginalMessage) + " • " + LocaleController.formatDateTime(entry.date, false);
                } else {
                    label = "Edit #" + i + " • " + LocaleController.formatDateTime(entry.date, false);
                }
                messageItems.add(new MessageItem(label));
                messageItems.add(new MessageItem(entry.toMessageObject(currentAccount)));
            }

            // Current Version
            int curDate = (currentMessageObject.messageOwner != null && currentMessageObject.messageOwner.edit_date > 0)
                    ? currentMessageObject.messageOwner.edit_date
                    : (currentMessageObject.messageOwner != null ? currentMessageObject.messageOwner.date : 0);
            String currentLabel = "Current Version • " + LocaleController.formatDateTime(curDate, false);
            messageItems.add(new MessageItem(currentLabel));
            messageItems.add(new MessageItem(currentMessageObject));
        } else {
            // No prior edits recorded — show current message as initial
            int date = currentMessageObject.messageOwner != null ? currentMessageObject.messageOwner.date : 0;
            String label = LocaleController.getString("OriginalMessage", R.string.OriginalMessage) + " • " + LocaleController.formatDateTime(date, false);
            messageItems.add(new MessageItem(label));
            messageItems.add(new MessageItem(currentMessageObject));
        }

        // 2. Load Reaction History
        List<VeyraEditHistoryManager.ReactionEntry> dbReactions = VeyraEditHistoryManager.getReactionHistory(dialogId, messageId);
        if (dbReactions != null && !dbReactions.isEmpty()) {
            for (int i = 0; i < dbReactions.size(); i++) {
                VeyraEditHistoryManager.ReactionEntry entry = dbReactions.get(i);
                String userName;
                if (entry.userId != 0) {
                    TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(entry.userId);
                    if (user != null) {
                        if (!TextUtils.isEmpty(user.username)) {
                            userName = "@" + user.username + " (" + UserObject.getUserName(user) + ")";
                        } else {
                            userName = UserObject.getUserName(user);
                        }
                    } else {
                        userName = "User " + entry.userId;
                    }
                } else {
                    userName = LocaleController.getString("Reactions", R.string.Reactions);
                }
                String action = entry.action != null ? entry.action : "add";
                reactionItems.add(new ReactionItem(entry.reaction, userName, entry.date, entry.count, action));
            }
        } else if (currentMessageObject.messageOwner != null && currentMessageObject.messageOwner.reactions != null) {
            // Fallback: load live reactions
            TLRPC.TL_messageReactions reactions = currentMessageObject.messageOwner.reactions;
            HashSet<String> seen = new HashSet<>();
            if (reactions.recent_reactions != null && !reactions.recent_reactions.isEmpty()) {
                for (int i = 0; i < reactions.recent_reactions.size(); i++) {
                    TLRPC.MessagePeerReaction pr = reactions.recent_reactions.get(i);
                    if (pr != null) {
                        String emoji = getReactionEmoji(pr.reaction);
                        long peerId = MessageObject.getPeerId(pr.peer_id);
                        String key = emoji + "_" + peerId;
                        if (seen.add(key)) {
                            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(peerId);
                            String userName;
                            if (user != null) {
                                if (!TextUtils.isEmpty(user.username)) {
                                    userName = "@" + user.username + " (" + UserObject.getUserName(user) + ")";
                                } else {
                                    userName = UserObject.getUserName(user);
                                }
                            } else {
                                userName = peerId != 0 ? ("User " + peerId) : LocaleController.getString("Reactions", R.string.Reactions);
                            }
                            reactionItems.add(new ReactionItem(emoji, userName, pr.date, 1, "add"));
                        }
                    }
                }
            }
            if (reactions.results != null && !reactions.results.isEmpty()) {
                for (int i = 0; i < reactions.results.size(); i++) {
                    TLRPC.ReactionCount rc = reactions.results.get(i);
                    if (rc != null) {
                        String emoji = getReactionEmoji(rc.reaction);
                        String detail = rc.chosen ? " (You)" : "";
                        reactionItems.add(new ReactionItem(emoji, "Reaction" + detail, 0, rc.count, "add"));
                    }
                }
            }
        }
    }

    private static String getReactionEmoji(TLRPC.Reaction reaction) {
        if (reaction instanceof TLRPC.TL_reactionPaid) {
            return "⭐️";
        } else if (reaction instanceof TLRPC.TL_reactionEmoji) {
            return ((TLRPC.TL_reactionEmoji) reaction).emoticon;
        } else if (reaction instanceof TLRPC.TL_reactionCustomEmoji) {
            return "⭐";
        }
        return "❤️";
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("EditHistory", R.string.EditHistory));

        TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
        TLRPC.User user = getMessagesController().getUser(dialogId);
        String subtitle = "";
        if (user != null) {
            subtitle = UserObject.getUserName(user);
        } else if (chat != null) {
            subtitle = chat.title;
        }
        if (!TextUtils.isEmpty(subtitle)) {
            actionBar.setSubtitle(subtitle);
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        SizeNotifierFrameLayout contentView = new SizeNotifierFrameLayout(context) {
            @Override
            protected Drawable getNewDrawable() {
                return Theme.getCachedWallpaperNonBlocking();
            }
        };
        contentView.setBackgroundImage(Theme.getCachedWallpaper(), Theme.isWallpaperMotion());
        fragmentView = contentView;

        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        // Segmented Tab Switcher (Messages vs Reactions)
        LinearLayout tabBar = new LinearLayout(context);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setGravity(Gravity.CENTER);
        tabBar.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(4), AndroidUtilities.dp(6), AndroidUtilities.dp(4));

        GradientDrawable tabBarBg = new GradientDrawable();
        tabBarBg.setCornerRadius(AndroidUtilities.dp(14));
        tabBarBg.setColor(Theme.getColor(Theme.key_chat_inBubble));
        tabBar.setBackground(tabBarBg);

        tabMessagesView = createTabButton(context, LocaleController.getString("VeyraEditHistoryTabMessages", R.string.VeyraEditHistoryTabMessages));
        tabReactionsView = createTabButton(context, LocaleController.getString("VeyraEditHistoryTabReactions", R.string.VeyraEditHistoryTabReactions));

        tabBar.addView(tabMessagesView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 4, 0));
        tabBar.addView(tabReactionsView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 4, 0, 0, 0));

        tabMessagesView.setOnClickListener(v -> selectTab(0));
        tabReactionsView.setOnClickListener(v -> selectTab(1));

        rootLayout.addView(tabBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 14, 10, 14, 8));

        // Content Area
        FrameLayout contentArea = new FrameLayout(context);

        // 1. Messages List (Scheduled Messages Chat Style)
        messagesListView = new RecyclerListView(context);
        messagesListView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        messagesListView.setAdapter(new MessagesAdapter());
        messagesListView.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(16));
        messagesListView.setClipToPadding(false);
        contentArea.addView(messagesListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // 2. Reactions List
        reactionsContainer = new FrameLayout(context);
        RecyclerListView reactionsListView = new RecyclerListView(context);
        reactionsListView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        reactionsListView.setAdapter(new ReactionsAdapter());
        reactionsListView.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(16));
        reactionsListView.setClipToPadding(false);
        reactionsContainer.addView(reactionsListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        EmptyTextProgressView emptyView = new EmptyTextProgressView(context);
        emptyView.setText(LocaleController.getString("VeyraNoReactionHistory", R.string.VeyraNoReactionHistory));
        emptyView.showTextView();
        reactionsContainer.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        reactionsListView.setEmptyView(emptyView);

        contentArea.addView(reactionsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        rootLayout.addView(contentArea, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        contentView.addView(rootLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        selectTab(0);
        return fragmentView;
    }

    private TextView createTabButton(Context context, String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        tv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        return tv;
    }

    private void selectTab(int index) {
        boolean isMessages = (index == 0);
        messagesListView.setVisibility(isMessages ? View.VISIBLE : View.GONE);
        reactionsContainer.setVisibility(isMessages ? View.GONE : View.VISIBLE);

        tabMessagesView.setBackground(createTabPillBg(isMessages));
        tabMessagesView.setTextColor(Theme.getColor(isMessages ? Theme.key_featuredStickers_buttonText : Theme.key_dialogTextGray2));

        tabReactionsView.setBackground(createTabPillBg(!isMessages));
        tabReactionsView.setTextColor(Theme.getColor(!isMessages ? Theme.key_featuredStickers_buttonText : Theme.key_dialogTextGray2));
    }

    private static GradientDrawable createTabPillBg(boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(AndroidUtilities.dp(10));
        d.setColor(selected ? Theme.getColor(Theme.key_featuredStickers_addButton) : 0x00000000);
        return d;
    }

    // --- Messages Adapter ---
    private class MessagesAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public int getItemCount() {
            return messageItems.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == 1;
        }

        @Override
        public int getItemViewType(int position) {
            return messageItems.get(position).isAction ? 0 : 1;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == 0) {
                ChatActionCell actionCell = new ChatActionCell(parent.getContext());
                view = actionCell;
            } else {
                ChatMessageCell messageCell = new ChatMessageCell(parent.getContext(), currentAccount);
                messageCell.isChat = false;
                messageCell.setFullyDraw(true);
                messageCell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
                    @Override
                    public void didLongPress(ChatMessageCell cell, float x, float y) {
                        MessageObject obj = cell.getMessageObject();
                        if (obj != null && !TextUtils.isEmpty(obj.messageText)) {
                            AndroidUtilities.addToClipboard(obj.messageText.toString());
                            BulletinFactory.of(VeyraMessageHistoryActivity.this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
                        }
                    }

                    @Override
                    public void didPressUrl(ChatMessageCell cell, CharacterStyle url, boolean longPress) {
                        if (url == null) return;
                        if (url instanceof URLSpanMono) {
                            ((URLSpanMono) url).copyToClipboard();
                            if (AndroidUtilities.shouldShowClipboardToast()) {
                                BulletinFactory.of(VeyraMessageHistoryActivity.this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
                            }
                        } else if (url instanceof URLSpan) {
                            Browser.openUrl(getParentActivity(), ((URLSpan) url).getURL());
                        }
                    }
                });
                view = messageCell;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MessageItem item = messageItems.get(position);
            if (item.isAction) {
                ChatActionCell actionCell = (ChatActionCell) holder.itemView;
                actionCell.setCustomText(item.actionLabel);
            } else {
                ChatMessageCell messageCell = (ChatMessageCell) holder.itemView;
                messageCell.setMessageObject(item.messageObject, null, false, false, false, false);
            }
        }
    }

    // --- Reactions Adapter ---
    private class ReactionsAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public int getItemCount() {
            return reactionItems.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));
            card.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(AndroidUtilities.dp(12));
            bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            card.setBackground(bg);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView emojiView = new TextView(context);
            emojiView.setId(1);
            emojiView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
            row.addView(emojiView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 10, 0));

            LinearLayout center = new LinearLayout(context);
            center.setOrientation(LinearLayout.VERTICAL);

            TextView titleView = new TextView(context);
            titleView.setId(2);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            center.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            TextView actionView = new TextView(context);
            actionView.setId(3);
            actionView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            center.addView(actionView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            row.addView(center, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

            TextView dateView = new TextView(context);
            dateView.setId(4);
            dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            dateView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            dateView.setGravity(Gravity.END);
            row.addView(dateView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            card.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // Set margins between cards
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) card.getLayoutParams();
            lp.topMargin = AndroidUtilities.dp(4);
            lp.bottomMargin = AndroidUtilities.dp(4);
            card.setLayoutParams(lp);

            return new RecyclerListView.Holder(card);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ReactionItem item = reactionItems.get(position);
            View card = holder.itemView;

            TextView emojiView = card.findViewById(1);
            TextView titleView = card.findViewById(2);
            TextView actionView = card.findViewById(3);
            TextView dateView = card.findViewById(4);

            emojiView.setText(item.emoji);
            titleView.setText(item.title);

            boolean isRemove = "remove".equals(item.action);
            actionView.setText(isRemove ? "- removed" : "+ added");
            actionView.setTextColor(isRemove ? 0xFFFF4444 : 0xFF4CAF50);

            if (item.date > 0) {
                dateView.setText(LocaleController.formatDateTime(item.date, false));
                dateView.setVisibility(View.VISIBLE);
            } else {
                dateView.setVisibility(View.GONE);
            }
        }
    }
}

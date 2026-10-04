package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
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

    public static class VersionEntry {
        public final int versionIndex;
        public final int totalVersions;
        public final int date;
        public final int prevDate;
        public final MessageObject messageObject;
        public final String rawText;

        public VersionEntry(int versionIndex, int totalVersions, int date, int prevDate, MessageObject messageObject, String rawText) {
            this.versionIndex = versionIndex;
            this.totalVersions = totalVersions;
            this.date = date;
            this.prevDate = prevDate;
            this.messageObject = messageObject;
            this.rawText = rawText;
        }
    }

    public static class ReactionItem {
        public final String emoji;
        public final String title;
        public final int date;
        public final int count;
        public final String action; // "add" or "remove"

        public ReactionItem(String emoji, String title, int date, int count, String action) {
            this.emoji = emoji;
            this.title = title;
            this.date = date;
            this.count = count;
            this.action = action;
        }
    }

    private final ArrayList<VersionEntry> versionEntries = new ArrayList<>();
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
        versionEntries.clear();
        reactionItems.clear();

        if (currentMessageObject == null) return;
        int messageId = currentMessageObject.getId();

        // 1. Load Edit History
        List<VeyraEditHistoryManager.EditEntry> history = VeyraEditHistoryManager.getHistory(dialogId, messageId);
        int totalVersions = (history != null ? history.size() : 0) + 1;

        int prevDate = 0;
        if (history != null && !history.isEmpty()) {
            for (int i = 0; i < history.size(); i++) {
                VeyraEditHistoryManager.EditEntry entry = history.get(i);
                MessageObject msgObj = entry.toMessageObject(currentAccount);
                versionEntries.add(new VersionEntry(i, totalVersions, entry.date, prevDate, msgObj, entry.text));
                prevDate = entry.date;
            }

            // Current Version
            int curDate = (currentMessageObject.messageOwner != null && currentMessageObject.messageOwner.edit_date > 0)
                    ? currentMessageObject.messageOwner.edit_date
                    : (currentMessageObject.messageOwner != null ? currentMessageObject.messageOwner.date : 0);
            String curText = currentMessageObject.messageOwner != null ? currentMessageObject.messageOwner.message : "";
            versionEntries.add(new VersionEntry(history.size(), totalVersions, curDate, prevDate, currentMessageObject, curText));
        } else {
            // No prior edits recorded — show current message as initial
            int date = currentMessageObject.messageOwner != null ? currentMessageObject.messageOwner.date : 0;
            String text = currentMessageObject.messageOwner != null ? currentMessageObject.messageOwner.message : "";
            versionEntries.add(new VersionEntry(0, 1, date, 0, currentMessageObject, text));
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

        // Ambient dark/light scrim over wallpaper for high contrast & luxury feel
        View scrim = new View(context);
        int baseBg = Theme.getColor(Theme.key_windowBackgroundGray);
        int scrimColor = (baseBg & 0x00FFFFFF) | 0xEE000000;
        scrim.setBackgroundColor(scrimColor);
        contentView.addView(scrim, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        fragmentView = contentView;

        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        // Segmented Tab Switcher (Floating pill bar)
        LinearLayout tabBar = new LinearLayout(context);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setGravity(Gravity.CENTER);
        tabBar.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));

        GradientDrawable tabBarBg = new GradientDrawable();
        tabBarBg.setCornerRadius(AndroidUtilities.dp(16));
        tabBarBg.setColor(Theme.getColor(Theme.key_chat_inBubble));
        tabBar.setBackground(tabBarBg);

        tabMessagesView = createTabButton(context, LocaleController.getString("VeyraEditHistoryTabMessages", R.string.VeyraEditHistoryTabMessages));
        tabReactionsView = createTabButton(context, LocaleController.getString("VeyraEditHistoryTabReactions", R.string.VeyraEditHistoryTabReactions));

        tabBar.addView(tabMessagesView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 4, 0));
        tabBar.addView(tabReactionsView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 4, 0, 0, 0));

        tabMessagesView.setOnClickListener(v -> selectTab(0));
        tabReactionsView.setOnClickListener(v -> selectTab(1));

        rootLayout.addView(tabBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 20, 14, 20, 14));

        // Content Area
        FrameLayout contentArea = new FrameLayout(context);

        // 1. Messages Timeline List
        messagesListView = new RecyclerListView(context);
        messagesListView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        messagesListView.setAdapter(new VersionsAdapter());
        messagesListView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(4), AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        messagesListView.setClipToPadding(false);
        contentArea.addView(messagesListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // 2. Reactions List
        reactionsContainer = new FrameLayout(context);
        RecyclerListView reactionsListView = new RecyclerListView(context);
        reactionsListView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        reactionsListView.setAdapter(new ReactionsAdapter());
        reactionsListView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(4), AndroidUtilities.dp(16), AndroidUtilities.dp(24));
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
        tv.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
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
        d.setCornerRadius(AndroidUtilities.dp(12));
        d.setColor(selected ? Theme.getColor(Theme.key_featuredStickers_addButton) : 0x00000000);
        return d;
    }

    private static String formatTimeDelta(int prevDate, int curDate) {
        if (prevDate <= 0 || curDate <= prevDate) return "";
        int diff = curDate - prevDate;
        if (diff < 60) return "edited after " + diff + "s";
        if (diff < 3600) return "edited after " + (diff / 60) + "m";
        if (diff < 86400) return "edited after " + (diff / 3600) + "h";
        return "edited after " + (diff / 86400) + "d";
    }

    // --- Version Cards Adapter ---
    private class VersionsAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public int getItemCount() {
            return versionEntries.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();

            LinearLayout wrapper = new LinearLayout(context);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Connector arrow / label between cards
            LinearLayout connector = new LinearLayout(context);
            connector.setId(10);
            connector.setOrientation(LinearLayout.HORIZONTAL);
            connector.setGravity(Gravity.CENTER);
            connector.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));

            TextView connectorText = new TextView(context);
            connectorText.setId(11);
            connectorText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            connectorText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            connectorText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            connector.addView(connectorText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            wrapper.addView(connector, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // Main Version Card
            LinearLayout card = new LinearLayout(context);
            card.setId(20);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setCornerRadius(AndroidUtilities.dp(16));
            cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            card.setBackground(cardBg);

            // 1. Card Header: Status Badge (Left) + Timestamp (Right)
            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView badge = new TextView(context);
            badge.setId(21);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            badge.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            badge.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
            header.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            TextView timeView = new TextView(context);
            timeView.setId(22);
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            timeView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            timeView.setGravity(Gravity.END);
            header.addView(timeView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

            card.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // Subtle divider under header
            View divider = new View(context);
            divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
            card.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 10, 0, 12));

            // 2. Message Text (Rich formatting, RTL/LTR aware)
            TextView messageView = new TextView(context);
            messageView.setId(23);
            messageView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            messageView.setLineSpacing(AndroidUtilities.dp(4), 1.15f);
            messageView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            messageView.setTextIsSelectable(true);
            messageView.setMovementMethod(LinkMovementMethod.getInstance());
            messageView.setTextDirection(View.TEXT_DIRECTION_LOCALE);
            messageView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            card.addView(messageView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // 3. Card Footer: Copy Button
            LinearLayout footer = new LinearLayout(context);
            footer.setOrientation(LinearLayout.HORIZONTAL);
            footer.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            footer.setPadding(0, AndroidUtilities.dp(10), 0, 0);

            LinearLayout copyBtn = new LinearLayout(context);
            copyBtn.setId(24);
            copyBtn.setOrientation(LinearLayout.HORIZONTAL);
            copyBtn.setGravity(Gravity.CENTER_VERTICAL);
            copyBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));

            GradientDrawable copyBg = new GradientDrawable();
            copyBg.setCornerRadius(AndroidUtilities.dp(8));
            copyBg.setColor(Theme.getColor(Theme.key_chat_inBubble));
            copyBtn.setBackground(copyBg);

            ImageView copyIcon = new ImageView(context);
            copyIcon.setImageResource(R.drawable.msg_copy);
            copyIcon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
            copyBtn.addView(copyIcon, LayoutHelper.createLinear(16, 16, 0, 0, 6, 0));

            TextView copyText = new TextView(context);
            copyText.setText(LocaleController.getString("Copy", R.string.Copy));
            copyText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            copyText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            copyText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
            copyBtn.addView(copyText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            footer.addView(copyBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            card.addView(footer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            wrapper.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            return new RecyclerListView.Holder(wrapper);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            VersionEntry entry = versionEntries.get(position);
            View root = holder.itemView;

            LinearLayout connector = root.findViewById(10);
            TextView connectorText = root.findViewById(11);
            TextView badge = root.findViewById(21);
            TextView timeView = root.findViewById(22);
            TextView messageView = root.findViewById(23);
            LinearLayout copyBtn = root.findViewById(24);

            // Configure connector
            if (position == 0) {
                connector.setVisibility(View.GONE);
            } else {
                connector.setVisibility(View.VISIBLE);
                String delta = formatTimeDelta(entry.prevDate, entry.date);
                connectorText.setText(TextUtils.isEmpty(delta) ? "↓ Edited" : ("↓ " + delta));
            }

            // Configure Badge & Colors
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(AndroidUtilities.dp(8));

            boolean isOriginal = (entry.versionIndex == 0);
            boolean isCurrent = (entry.versionIndex == entry.totalVersions - 1);

            if (isOriginal) {
                badge.setText(LocaleController.getString("OriginalMessage", R.string.OriginalMessage));
                badge.setTextColor(0xFF3B82F6);
                badgeBg.setColor(0x223B82F6);
            } else if (isCurrent) {
                badge.setText("Current Version");
                badge.setTextColor(0xFF10B981);
                badgeBg.setColor(0x2210B981);
            } else {
                badge.setText("Edit #" + entry.versionIndex);
                badge.setTextColor(0xFFF59E0B);
                badgeBg.setColor(0x22F59E0B);
            }
            badge.setBackground(badgeBg);

            // Time
            if (entry.date > 0) {
                timeView.setText(LocaleController.formatDateTime(entry.date, false));
                timeView.setVisibility(View.VISIBLE);
            } else {
                timeView.setVisibility(View.GONE);
            }

            // Message Content (Rich entities preserved)
            CharSequence textToDisplay = "";
            if (entry.messageObject != null) {
                if (!TextUtils.isEmpty(entry.messageObject.messageText)) {
                    textToDisplay = entry.messageObject.messageText;
                } else if (!TextUtils.isEmpty(entry.messageObject.caption)) {
                    textToDisplay = entry.messageObject.caption;
                } else if (entry.messageObject.messageOwner != null && !TextUtils.isEmpty(entry.messageObject.messageOwner.message)) {
                    textToDisplay = entry.messageObject.messageOwner.message;
                }
            }
            if (TextUtils.isEmpty(textToDisplay)) {
                textToDisplay = entry.rawText != null ? entry.rawText : "";
            }
            messageView.setText(textToDisplay);

            final String copyContent = textToDisplay.toString();
            copyBtn.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(copyContent)) {
                    AndroidUtilities.addToClipboard(copyContent);
                    BulletinFactory.of(VeyraMessageHistoryActivity.this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
                }
            });
        }
    }

    // --- Reactions Cards Adapter ---
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
            card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
            card.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(AndroidUtilities.dp(14));
            bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            card.setBackground(bg);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView emojiView = new TextView(context);
            emojiView.setId(1);
            emojiView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
            row.addView(emojiView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 12, 0));

            LinearLayout center = new LinearLayout(context);
            center.setOrientation(LinearLayout.VERTICAL);

            TextView titleView = new TextView(context);
            titleView.setId(2);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            center.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            TextView actionView = new TextView(context);
            actionView.setId(3);
            actionView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            center.addView(actionView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            row.addView(center, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

            TextView dateView = new TextView(context);
            dateView.setId(4);
            dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            dateView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            dateView.setGravity(Gravity.END);
            row.addView(dateView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            card.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

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
            actionView.setText(isRemove ? "- removed reaction" : "+ added reaction");
            actionView.setTextColor(isRemove ? 0xFFFF4444 : 0xFF10B981);

            if (item.date > 0) {
                dateView.setText(LocaleController.formatDateTime(item.date, false));
                dateView.setVisibility(View.VISIBLE);
            } else {
                dateView.setVisibility(View.GONE);
            }
        }
    }
}

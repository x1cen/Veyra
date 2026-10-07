package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.URLSpan;
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
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.ItemOptions;
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
    private final int initialTab;
    private int currentTab;

    private LinearLayout tabMessagesBtn;
    private LinearLayout tabReactionsBtn;
    private RecyclerListView messagesListView;
    private FrameLayout reactionsContainer;
    private RecyclerListView reactionsListView;

    public static class Span {
        public final int start;
        public final int end;
        public Span(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

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
        public final long userId;

        public ReactionItem(String emoji, String title, int date, int count, String action, long userId) {
            this.emoji = emoji;
            this.title = title;
            this.date = date;
            this.count = count;
            this.action = action;
            this.userId = userId;
        }
    }

    private final ArrayList<VersionEntry> versionEntries = new ArrayList<>();
    private final ArrayList<ReactionItem> reactionItems = new ArrayList<>();

    public VeyraMessageHistoryActivity(long dialogId, MessageObject messageObject) {
        this(dialogId, messageObject, 0);
    }

    public VeyraMessageHistoryActivity(long dialogId, MessageObject messageObject, int initialTab) {
        super();
        this.dialogId = dialogId;
        this.currentMessageObject = messageObject;
        this.initialTab = initialTab;
        this.currentTab = initialTab;
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
        long mySelfId = getUserConfig().clientUserId;

        // 1. Load Edit History
        List<VeyraEditHistoryManager.EditEntry> history = VeyraEditHistoryManager.getHistory(dialogId, messageId);
        int totalVersions = (history != null ? history.size() : 0) + 1;

        int prevDate = 0;
        if (history != null && !history.isEmpty()) {
            for (int i = 0; i < history.size(); i++) {
                VeyraEditHistoryManager.EditEntry entry = history.get(i);
                MessageObject msgObj = entry.toMessageObject(currentAccount);
                String rawText = entry.text != null ? entry.text : (msgObj.messageOwner != null ? msgObj.messageOwner.message : "");
                versionEntries.add(new VersionEntry(i, totalVersions, entry.date, prevDate, msgObj, rawText));
                prevDate = entry.date;
            }

            // Current Version
            int curDate = (currentMessageObject.messageOwner != null && currentMessageObject.messageOwner.edit_date > 0)
                    ? currentMessageObject.messageOwner.edit_date
                    : (currentMessageObject.messageOwner != null ? currentMessageObject.messageOwner.date : 0);
            String curText = currentMessageObject.messageOwner != null && currentMessageObject.messageOwner.message != null
                    ? currentMessageObject.messageOwner.message : "";
            versionEntries.add(new VersionEntry(history.size(), totalVersions, curDate, prevDate, currentMessageObject, curText));
        } else {
            // No prior edits recorded — show current message as initial
            int date = currentMessageObject.messageOwner != null ? currentMessageObject.messageOwner.date : 0;
            String text = currentMessageObject.messageOwner != null && currentMessageObject.messageOwner.message != null
                    ? currentMessageObject.messageOwner.message : "";
            versionEntries.add(new VersionEntry(0, 1, date, 0, currentMessageObject, text));
        }

        // Apply diff highlighting: compare each version to previous version
        int highlightColor = 0x3310B981; // subtle light translucent emerald highlight
        for (int i = 1; i < versionEntries.size(); i++) {
            VersionEntry curr = versionEntries.get(i);
            VersionEntry prev = versionEntries.get(i - 1);
            String oldT = prev.rawText != null ? prev.rawText : "";
            String newT = curr.rawText != null ? curr.rawText : "";
            List<Span> diffs = computeDiffSpans(oldT, newT);
            if (!diffs.isEmpty() && curr.messageObject != null) {
                CharSequence baseText = curr.messageObject.messageText;
                if (TextUtils.isEmpty(baseText)) {
                    baseText = newT;
                }
                SpannableStringBuilder ssb = new SpannableStringBuilder(baseText);
                int textLen = ssb.length();
                for (Span s : diffs) {
                    Span trimmed = trimSpan(newT, s);
                    if (trimmed != null && trimmed.start >= 0 && trimmed.end <= textLen && trimmed.start < trimmed.end) {
                        ssb.setSpan(new BackgroundColorSpan(highlightColor), trimmed.start, trimmed.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                curr.messageObject.messageText = ssb;
                curr.messageObject.resetLayout();
            }
        }

        // 2. Load Reaction History
        List<VeyraEditHistoryManager.ReactionEntry> dbReactions = VeyraEditHistoryManager.getReactionHistory(dialogId, messageId);
        if (dbReactions != null && !dbReactions.isEmpty()) {
            for (int i = 0; i < dbReactions.size(); i++) {
                VeyraEditHistoryManager.ReactionEntry entry = dbReactions.get(i);
                // Strictly exclude self reactions
                if (entry.userId == mySelfId) {
                    continue;
                }
                String userName;
                if (entry.userId != 0) {
                    TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(entry.userId);
                    if (user != null) {
                        if (!TextUtils.isEmpty(user.username)) {
                            userName = UserObject.getUserName(user) + " (@" + user.username + ")";
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
                reactionItems.add(new ReactionItem(entry.reaction, userName, entry.date, entry.count, action, entry.userId));
            }
        } else if (currentMessageObject.messageOwner != null && currentMessageObject.messageOwner.reactions != null) {
            // Fallback: load live reactions
            TLRPC.TL_messageReactions reactions = currentMessageObject.messageOwner.reactions;
            HashSet<String> seen = new HashSet<>();
            if (reactions.recent_reactions != null && !reactions.recent_reactions.isEmpty()) {
                for (int i = 0; i < reactions.recent_reactions.size(); i++) {
                    TLRPC.MessagePeerReaction pr = reactions.recent_reactions.get(i);
                    if (pr != null) {
                        long peerId = MessageObject.getPeerId(pr.peer_id);
                        if (peerId == mySelfId) {
                            continue; // strictly ignore self reactions
                        }
                        String emoji = getReactionEmoji(pr.reaction);
                        String key = emoji + "_" + peerId;
                        if (seen.add(key)) {
                            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(peerId);
                            String userName;
                            if (user != null) {
                                if (!TextUtils.isEmpty(user.username)) {
                                    userName = UserObject.getUserName(user) + " (@" + user.username + ")";
                                } else {
                                    userName = UserObject.getUserName(user);
                                }
                            } else {
                                userName = peerId != 0 ? ("User " + peerId) : LocaleController.getString("Reactions", R.string.Reactions);
                            }
                            reactionItems.add(new ReactionItem(emoji, userName, pr.date, 1, "add", peerId));
                        }
                    }
                }
            }
            if (reactions.results != null && !reactions.results.isEmpty()) {
                for (int i = 0; i < reactions.results.size(); i++) {
                    TLRPC.ReactionCount rc = reactions.results.get(i);
                    if (rc != null) {
                        String emoji = getReactionEmoji(rc.reaction);
                        int count = rc.count - (rc.chosen ? 1 : 0);
                        if (count > 0) {
                            reactionItems.add(new ReactionItem(emoji, LocaleController.getString("Reactions", R.string.Reactions), 0, count, "add", 0));
                        }
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
            return "⭐️";
        }
        return "❤️";
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);

        TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
        TLRPC.User user = getMessagesController().getUser(dialogId);

        // Header: Profile Avatar + Title "History"
        int topOffset = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
        int barHeightPx = ActionBar.getCurrentActionBarHeight(); // already in px (dp() result)

        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);

        BackupImageView avatarView = new BackupImageView(context);
        avatarView.setRoundRadius(AndroidUtilities.dp(19));
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        if (user != null) {
            avatarDrawable.setInfo(user);
            avatarView.setForUserOrChat(user, avatarDrawable);
        } else if (chat != null) {
            avatarDrawable.setInfo(chat);
            avatarView.setForUserOrChat(chat, avatarDrawable);
        } else {
            avatarDrawable.setInfo(dialogId, "History", null);
            avatarView.setImageDrawable(avatarDrawable);
        }
        headerLayout.addView(avatarView, LayoutHelper.createLinear(38, 38, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

        TextView titleView = new TextView(context);
        titleView.setText("History");
        titleView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        titleView.setTypeface(AndroidUtilities.bold());
        headerLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        // addView with dp-based margin: LayoutHelper.createFrame uses dp internally
        actionBar.addView(headerLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 56, 0, 56, 0));

        // 3-dots Menu on the right
        org.telegram.ui.ActionBar.ActionBarMenu menu = actionBar.createMenu();
        org.telegram.ui.ActionBar.ActionBarMenuItem otherItem = menu.addItem(1, R.drawable.ic_ab_other);
        otherItem.addSubItem(101, R.drawable.msg_copy, LocaleController.getString("Copy", R.string.Copy));
        otherItem.addSubItem(102, R.drawable.msg_info, "Message Details");
        otherItem.addSubItem(103, R.drawable.msg_delete, "Clean");

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 101) {
                    copyLatestMessageText();
                } else if (id == 102) {
                    openLatestMessageDetails();
                } else if (id == 103) {
                    showCleanDialog();
                }
            }
        });

        Theme.createChatResources(context, false);

        SizeNotifierFrameLayout contentView = new SizeNotifierFrameLayout(context) {
            @Override
            protected Drawable getNewDrawable() {
                return Theme.getCachedWallpaperNonBlocking();
            }
        };
        Drawable wp = Theme.getCachedWallpaper();
        if (wp != null) {
            contentView.setBackground(wp);
        }
        contentView.setBackgroundImage(wp, Theme.isWallpaperMotion());
        fragmentView = contentView;

        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        // Floating independent liquid-glass tab pills — no shared container background,
        // wallpaper shows through the gap and behind each pill (same glass style as group topics / bot tabs).
        LinearLayout pillsRow = new LinearLayout(context);
        pillsRow.setOrientation(LinearLayout.HORIZONTAL);
        pillsRow.setGravity(Gravity.CENTER);
        pillsRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));

        tabMessagesBtn = createTopicTabPill(context, R.drawable.msg_edit, LocaleController.getString("VeyraEditHistoryTabMessages", R.string.VeyraEditHistoryTabMessages));
        tabReactionsBtn = createTopicTabPill(context, R.drawable.msg_reactions, LocaleController.getString("VeyraEditHistoryTabReactions", R.string.VeyraEditHistoryTabReactions));

        // 6dp right-margin on Messages + 6dp left-margin on Reactions = 12dp margin between the two independent glass pills
        pillsRow.addView(tabMessagesBtn, LayoutHelper.createLinear(0, AndroidUtilities.dp(36), 1.0f, 0, 0, 6, 0));
        pillsRow.addView(tabReactionsBtn, LayoutHelper.createLinear(0, AndroidUtilities.dp(36), 1.0f, 6, 0, 0, 0));

        tabMessagesBtn.setOnClickListener(v -> selectTab(0));
        tabReactionsBtn.setOnClickListener(v -> selectTab(1));

        rootLayout.addView(pillsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(52)));

        // Content Area
        FrameLayout contentArea = new FrameLayout(context);

        // 1. Messages Timeline List (rendered exactly like chat cells)
        messagesListView = new RecyclerListView(context);
        messagesListView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        messagesListView.setAdapter(new VersionsAdapter());
        messagesListView.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(24));
        messagesListView.setClipToPadding(false);
        contentArea.addView(messagesListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // 2. Reactions List (dedicated modern card list)
        reactionsContainer = new FrameLayout(context);
        reactionsListView = new RecyclerListView(context);
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

        // rootLayout fills the whole contentView; actionBar floats on top.
        // We apply paddingTop via onMeasure so it tracks the real measured actionBar height.
        contentView.addView(rootLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Defer padding update until actionBar is measured (avoids hard-coded px math).
        actionBar.addOnLayoutChangeListener((v, left, top, right, bottom, ol, ot, or2, ob) -> {
            int abH = actionBar.getMeasuredHeight();
            if (abH > 0 && rootLayout.getPaddingTop() != abH) {
                rootLayout.setPadding(0, abH, 0, 0);
            }
        });

        selectTab(initialTab);
        return fragmentView;
    }

    private LinearLayout createTopicTabPill(Context context, int iconRes, String title) {
        LinearLayout pill = new LinearLayout(context);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), 0);

        ImageView icon = new ImageView(context);
        icon.setId(10);
        icon.setImageResource(iconRes);
        pill.addView(icon, LayoutHelper.createLinear(18, 18, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));

        TextView text = new TextView(context);
        text.setId(11);
        text.setText(title);
        text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        text.setTypeface(AndroidUtilities.bold());
        text.setGravity(Gravity.CENTER_VERTICAL);
        pill.addView(text, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        return pill;
    }

    private void selectTab(int index) {
        currentTab = index;
        boolean isMessages = (index == 0);
        messagesListView.setVisibility(isMessages ? View.VISIBLE : View.GONE);
        reactionsContainer.setVisibility(isMessages ? View.GONE : View.VISIBLE);

        updateTabPill(tabMessagesBtn, isMessages);
        updateTabPill(tabReactionsBtn, !isMessages);
    }

    private void updateTabPill(LinearLayout pill, boolean selected) {
        if (pill == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(AndroidUtilities.dp(18));
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);

        ImageView icon = pill.findViewById(10);
        TextView text = pill.findViewById(11);

        if (selected) {
            bg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 235));
            bg.setStroke(AndroidUtilities.dp(1.2f), 0x66FFFFFF);
            pill.setBackground(bg);
            if (icon != null) {
                icon.setColorFilter(new android.graphics.PorterDuffColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN));
            }
            if (text != null) {
                text.setTextColor(0xFFFFFFFF);
                text.setTypeface(AndroidUtilities.bold());
            }
        } else {
            bg.setColor(Theme.isCurrentThemeDark() ? 0x2AFFFFFF : 0x18000000);
            bg.setStroke(AndroidUtilities.dp(1f), Theme.isCurrentThemeDark() ? 0x3DFFFFFF : 0x22000000);
            pill.setBackground(bg);
            int inactiveColor = Theme.isCurrentThemeDark() ? 0xDDFFFFFF : 0x88000000;
            if (icon != null) {
                icon.setColorFilter(new android.graphics.PorterDuffColorFilter(inactiveColor, android.graphics.PorterDuff.Mode.SRC_IN));
            }
            if (text != null) {
                text.setTextColor(inactiveColor);
                text.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            }
        }
    }

    private void copyLatestMessageText() {
        if (currentMessageObject != null && !TextUtils.isEmpty(currentMessageObject.messageText)) {
            AndroidUtilities.addToClipboard(currentMessageObject.messageText.toString());
            BulletinFactory.of(this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
        } else if (!versionEntries.isEmpty()) {
            VersionEntry entry = versionEntries.get(0);
            if (!TextUtils.isEmpty(entry.rawText)) {
                AndroidUtilities.addToClipboard(entry.rawText);
                BulletinFactory.of(this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
            }
        }
    }

    private void openLatestMessageDetails() {
        if (currentMessageObject != null) {
            presentFragment(new MessageDetailsActivity(currentMessageObject));
        }
    }

    private void showCleanDialog() {
        if (getParentActivity() == null) return;
        final boolean hasEdit = currentMessageObject != null && VeyraEditHistoryManager.hasHistory(dialogId, currentMessageObject.getId());
        final boolean hasReact = currentMessageObject != null && VeyraEditHistoryManager.hasReactionHistory(dialogId, currentMessageObject.getId());
        final boolean hasAntiDel = currentMessageObject != null && org.veyra.client.VeyraAntiDelete.hasDeletedMessage(dialogId, currentMessageObject.getId());

        java.util.List<String> items = new java.util.ArrayList<>();
        java.util.List<Runnable> actions = new java.util.ArrayList<>();

        if (hasEdit) {
            items.add("Clear edit history for this message");
            actions.add(() -> { VeyraEditHistoryManager.deleteHistory(dialogId, currentMessageObject.getId()); loadData(); });
        }
        if (hasReact) {
            items.add("Clear reaction history for this message");
            actions.add(() -> { VeyraEditHistoryManager.deleteReactionHistory(dialogId, currentMessageObject.getId()); loadData(); });
        }
        if (hasAntiDel) {
            items.add("Clear deleted message cache");
            actions.add(() -> { org.veyra.client.VeyraAntiDelete.clearMessage(dialogId, currentMessageObject.getId()); loadData(); });
        }
        if (hasEdit || hasReact || hasAntiDel) {
            items.add("Clear all Veyra data for this message");
            actions.add(() -> {
                VeyraEditHistoryManager.deleteHistory(dialogId, currentMessageObject.getId());
                VeyraEditHistoryManager.deleteReactionHistory(dialogId, currentMessageObject.getId());
                org.veyra.client.VeyraAntiDelete.clearMessage(dialogId, currentMessageObject.getId());
                loadData();
            });
        }
        items.add("Clear all history in this chat");
        actions.add(() -> {
            VeyraEditHistoryManager.clearDialog(dialogId);
            org.veyra.client.VeyraAntiDelete.clearDialog(dialogId);
            loadData();
        });
        items.add("Delete message");
        actions.add(this::confirmClearHistory);

        String[] itemArr = items.toArray(new String[0]);
        Runnable[] actionArr = actions.toArray(new Runnable[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setItems(itemArr, (di, which) -> {
            if (which < actionArr.length) actionArr[which].run();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void confirmClearHistory() {
        if (getParentActivity() == null || currentMessageObject == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("Delete", R.string.Delete));
        builder.setMessage("Are you sure you want to clear all history for this message?");
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            int msgId = currentMessageObject.getId();
            VeyraEditHistoryManager.deleteHistory(dialogId, msgId);
            versionEntries.clear();
            reactionItems.clear();
            if (messagesListView != null && messagesListView.getAdapter() != null) {
                messagesListView.getAdapter().notifyDataSetChanged();
            }
            if (reactionsListView != null && reactionsListView.getAdapter() != null) {
                reactionsListView.getAdapter().notifyDataSetChanged();
            }
            BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "History cleared").show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alert = builder.create();
        showDialog(alert);
        TextView btn = (TextView) alert.getButton(DialogInterface.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    // --- Word Diff Computation ---
    private static Span trimSpan(String text, Span span) {
        if (text == null || span == null) return span;
        int s = span.start;
        int e = span.end;
        int len = text.length();
        if (s < 0) s = 0;
        if (e > len) e = len;
        while (s < e && Character.isWhitespace(text.charAt(s))) {
            s++;
        }
        while (e > s && Character.isWhitespace(text.charAt(e - 1))) {
            e--;
        }
        return s < e ? new Span(s, e) : null;
    }

    private static List<Span> computeDiffSpans(String oldText, String newText) {
        List<Span> addedSpans = new ArrayList<>();
        if (newText == null || newText.isEmpty()) return addedSpans;
        if (oldText == null || oldText.isEmpty()) {
            addedSpans.add(new Span(0, newText.length()));
            return addedSpans;
        }
        if (oldText.equals(newText)) return addedSpans;

        List<String> oldTokens = tokenize(oldText);
        List<String> newTokens = tokenize(newText);
        int n = oldTokens.size();
        int m = newTokens.size();
        if (n > 500 || m > 500) {
            addedSpans.add(new Span(0, newText.length()));
            return addedSpans;
        }

        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (oldTokens.get(i).equals(newTokens.get(j))) {
                    dp[i + 1][j + 1] = dp[i][j] + 1;
                } else {
                    dp[i + 1][j + 1] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }

        boolean[] isCommonInNew = new boolean[m];
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (oldTokens.get(i - 1).equals(newTokens.get(j - 1))) {
                isCommonInNew[j - 1] = true;
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }

        int charIdx = 0;
        int currentStart = -1;
        for (int k = 0; k < m; k++) {
            String token = newTokens.get(k);
            int tokenLen = token.length();
            if (!isCommonInNew[k]) {
                if (currentStart == -1) {
                    currentStart = charIdx;
                }
            } else {
                if (currentStart != -1) {
                    addedSpans.add(new Span(currentStart, charIdx));
                    currentStart = -1;
                }
            }
            charIdx += tokenLen;
        }
        if (currentStart != -1) {
            addedSpans.add(new Span(currentStart, charIdx));
        }
        return addedSpans;
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        int len = text.length();
        int start = 0;
        while (start < len) {
            char c = text.charAt(start);
            boolean isSpace = Character.isWhitespace(c);
            int end = start + 1;
            while (end < len && (Character.isWhitespace(text.charAt(end)) == isSpace)) {
                end++;
            }
            tokens.add(text.substring(start, end));
            start = end;
        }
        return tokens;
    }

    // --- Version Messages Adapter (Native ChatMessageCell) ---
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

            // Timeline Header Chip
            TextView chip = new TextView(context);
            chip.setId(100);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            chip.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4));

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(AndroidUtilities.dp(12));
            chipBg.setColor(Theme.getColor(Theme.key_chat_serviceBackground));
            chip.setBackground(chipBg);
            chip.setTextColor(Theme.getColor(Theme.key_chat_serviceText));

            wrapper.addView(chip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 10, 0, 6));

            // Native ChatMessageCell (renders chat bubble, formatted text with diff, media)
            ChatMessageCell cell = new ChatMessageCell(context, currentAccount);
            cell.setId(101);
            cell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
                @Override
                public boolean canPerformActions() {
                    return true;
                }

                @Override
                public void didPressUrl(ChatMessageCell cell, CharacterStyle url, boolean longPress) {
                    if (url instanceof URLSpan) {
                        Browser.openUrl(getParentActivity(), ((URLSpan) url).getURL());
                    }
                }

                @Override
                public void didLongPress(ChatMessageCell cell, float x, float y) {
                    showMessageOptions(cell);
                }
            });
            cell.setOnClickListener(v -> showMessageOptions(cell));
            wrapper.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            return new RecyclerListView.Holder(wrapper);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            VersionEntry entry = versionEntries.get(position);
            View root = holder.itemView;

            TextView chip = root.findViewById(100);
            ChatMessageCell cell = root.findViewById(101);

            String headerText;
            if (entry.versionIndex == 0) {
                headerText = LocaleController.getString("OriginalMessage", R.string.OriginalMessage);
            } else if (entry.versionIndex == entry.totalVersions - 1) {
                headerText = "Current Version";
            } else {
                headerText = "Edit #" + entry.versionIndex;
            }
            if (entry.date > 0) {
                headerText += " • " + LocaleController.formatDateTime(entry.date, false);
            }
            chip.setText(headerText);

            if (entry.messageObject != null) {
                // Always show as group chat message so avatar renders next to bubble
                cell.isChat = true;
                MessageObject mo = entry.messageObject;
                // Make needDrawAvatar() return true: set the flag on messageOwner
                if (mo.messageOwner != null) {
                    mo.messageOwner.flags &= ~0x00000002; // clear out flag so avatar draws
                    if (mo.messageOwner.from_id == null) {
                        TLRPC.TL_peerUser peer = new TLRPC.TL_peerUser();
                        peer.user_id = mo.getSenderId() != 0 ? mo.getSenderId() : dialogId;
                        mo.messageOwner.from_id = peer;
                    }
                }
                cell.setMessageObject(mo, null, false, false, false);
            }
        }
    }

    // --- Reactions Cards Adapter (Modern dedicated UI) ---
    private class ReactionsAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public int getItemCount() {
            return reactionItems.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
            card.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(AndroidUtilities.dp(14));
            bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            card.setBackground(bg);

            // User Avatar
            BackupImageView avatarView = new BackupImageView(context);
            avatarView.setId(1);
            avatarView.setRoundRadius(AndroidUtilities.dp(21));
            card.addView(avatarView, LayoutHelper.createLinear(42, 42, 0, 0, 12, 0));

            // Center details
            LinearLayout center = new LinearLayout(context);
            center.setOrientation(LinearLayout.VERTICAL);

            TextView nameView = new TextView(context);
            nameView.setId(2);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            nameView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            nameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            center.addView(nameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            LinearLayout actionRow = new LinearLayout(context);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setGravity(Gravity.CENTER_VERTICAL);
            actionRow.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));

            TextView emojiView = new TextView(context);
            emojiView.setId(3);
            emojiView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            actionRow.addView(emojiView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));

            TextView actionTextView = new TextView(context);
            actionTextView.setId(4);
            actionTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            actionTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            actionRow.addView(actionTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            center.addView(actionRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
            card.addView(center, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

            // Right timestamp
            TextView timeView = new TextView(context);
            timeView.setId(5);
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            timeView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            timeView.setGravity(Gravity.END);
            card.addView(timeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

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

            BackupImageView avatarView = card.findViewById(1);
            TextView nameView = card.findViewById(2);
            TextView emojiView = card.findViewById(3);
            TextView actionTextView = card.findViewById(4);
            TextView timeView = card.findViewById(5);
            LinearLayout actionRow = (LinearLayout) emojiView.getParent();

            nameView.setText(item.title);
            emojiView.setText(item.emoji);

            boolean isRemove = "remove".equals(item.action);
            actionTextView.setText(isRemove ? "Removed reaction" : "Added reaction");
            actionTextView.setTextColor(isRemove ? 0xFFEF4444 : 0xFF10B981);

            GradientDrawable actionBg = new GradientDrawable();
            actionBg.setCornerRadius(AndroidUtilities.dp(8));
            actionBg.setColor(isRemove ? 0x20EF4444 : 0x2010B981);
            actionRow.setBackground(actionBg);

            if (item.date > 0) {
                timeView.setText(LocaleController.formatDateTime(item.date, false));
                timeView.setVisibility(View.VISIBLE);
            } else {
                timeView.setVisibility(View.GONE);
            }

            if (item.userId != 0) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(item.userId);
                if (user != null) {
                    AvatarDrawable avatarDrawable = new AvatarDrawable();
                    avatarDrawable.setInfo(user);
                    avatarView.setForUserOrChat(user, avatarDrawable);
                } else {
                    AvatarDrawable avatarDrawable = new AvatarDrawable();
                    avatarDrawable.setInfo(item.userId, item.title, null);
                    avatarView.setImageDrawable(avatarDrawable);
                }
            } else {
                AvatarDrawable avatarDrawable = new AvatarDrawable();
                avatarDrawable.setInfo(0, item.title, null);
                avatarView.setImageDrawable(avatarDrawable);
            }

            card.setOnClickListener(v -> showReactionOptions(card, item));
            card.setOnLongClickListener(v -> {
                showReactionOptions(card, item);
                return true;
            });
        }
    }

    private void showMessageOptions(ChatMessageCell cell) {
        if (cell == null) return;
        MessageObject msg = cell.getMessageObject();
        if (msg == null) return;
        final String textToCopy = msg.messageText != null ? msg.messageText.toString() : (msg.messageOwner != null && msg.messageOwner.message != null ? msg.messageOwner.message : "");
        ItemOptions options = ItemOptions.makeOptions(this, cell);
        if (!TextUtils.isEmpty(textToCopy)) {
            options.add(R.drawable.msg_copy, LocaleController.getString("Copy", R.string.Copy), () -> {
                AndroidUtilities.addToClipboard(textToCopy);
                BulletinFactory.of(this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
            });
        }
        options.add(R.drawable.msg_info, LocaleController.getString("ViewDetails", R.string.ViewDetails), () -> {
            presentFragment(new MessageDetailsActivity(msg));
        });
        options.add(R.drawable.msg_delete, LocaleController.getString("Delete", R.string.Delete), true, () -> {
            showDeleteEditAlert(msg);
        });
        options.show();
    }

    private void showDeleteEditAlert(MessageObject msg) {
        if (getParentActivity() == null || msg == null || msg.messageOwner == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("Delete", R.string.Delete));
        builder.setMessage("Are you sure you want to delete this edit version?");
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            int editDate = msg.messageOwner.edit_date > 0 ? msg.messageOwner.edit_date : msg.messageOwner.date;
            VeyraEditHistoryManager.deleteSingleEdit(dialogId, currentMessageObject.getId(), editDate);
            loadData();
            if (messagesListView != null && messagesListView.getAdapter() != null) {
                messagesListView.getAdapter().notifyDataSetChanged();
            }
            BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, LocaleController.getString("Delete", R.string.Delete)).show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alert = builder.create();
        showDialog(alert);
        TextView btn = (TextView) alert.getButton(DialogInterface.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    private void showReactionOptions(View anchor, ReactionItem item) {
        if (anchor == null || item == null) return;
        ItemOptions options = ItemOptions.makeOptions(this, anchor);
        options.add(R.drawable.msg_copy, LocaleController.getString("Copy", R.string.Copy), () -> {
            AndroidUtilities.addToClipboard(item.emoji + " (" + item.title + ")");
            BulletinFactory.of(this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
        });
        if (item.userId != 0) {
            options.add(R.drawable.msg_openprofile, LocaleController.getString("OpenProfile", R.string.OpenProfile), () -> {
                Bundle args = new Bundle();
                args.putLong("user_id", item.userId);
                presentFragment(new ProfileActivity(args));
            });
        }
        options.add(R.drawable.msg_delete, LocaleController.getString("Delete", R.string.Delete), true, () -> {
            showDeleteReactionAlert(item);
        });
        options.show();
    }

    private void showDeleteReactionAlert(ReactionItem item) {
        if (getParentActivity() == null || item == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("Delete", R.string.Delete));
        builder.setMessage("Are you sure you want to delete this reaction record?");
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            VeyraEditHistoryManager.deleteSingleReaction(dialogId, currentMessageObject.getId(), item.userId, item.emoji);
            loadData();
            if (reactionsListView != null && reactionsListView.getAdapter() != null) {
                reactionsListView.getAdapter().notifyDataSetChanged();
            }
            BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, LocaleController.getString("Delete", R.string.Delete)).show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alert = builder.create();
        showDialog(alert);
        TextView btn = (TextView) alert.getButton(DialogInterface.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }
}

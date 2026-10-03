/* Veyra Project */

package org.veyra.client;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.VeyraConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.util.List;

public class MessageEditHistorySheet extends BottomSheet {

    private final TextView tabMessagesView;
    private final TextView tabReactionsView;
    private final ScrollView messagesScrollView;
    private final ScrollView reactionsScrollView;

    public static void show(BaseFragment fragment, MessageObject messageObject) {
        if (fragment == null || fragment.getParentActivity() == null || messageObject == null) {
            return;
        }
        new MessageEditHistorySheet(fragment, messageObject).show();
    }

    private MessageEditHistorySheet(BaseFragment fragment, MessageObject messageObject) {
        super(fragment.getParentActivity(), false);
        this.currentAccount = messageObject.currentAccount;
        Context context = fragment.getParentActivity();

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        TextView titleView = new TextView(context);
        titleView.setText(LocaleController.getString("EditHistory", R.string.EditHistory));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        long dialogId = messageObject.getDialogId();
        int messageId = messageObject.getId();

        boolean hasMsgHistory = VeyraEditHistoryManager.hasHistory(dialogId, messageId);
        boolean reactionHistoryAllowed = VeyraConfig.reactionHistoryEnabled;
        boolean hasReactionHistory = reactionHistoryAllowed && (messageObject.hasReactions() || VeyraEditHistoryManager.hasReactionHistory(dialogId, messageId));

        FrameLayout contentContainer = new FrameLayout(context);

        // Build Messages View
        messagesScrollView = new ScrollView(context);
        LinearLayout messagesItemsContainer = new LinearLayout(context);
        messagesItemsContainer.setOrientation(LinearLayout.VERTICAL);

        List<VeyraEditHistoryManager.EditEntry> history = VeyraEditHistoryManager.getHistory(dialogId, messageId);

        String currentMsgText = "";
        if (messageObject.messageOwner != null && !android.text.TextUtils.isEmpty(messageObject.messageOwner.message)) {
            currentMsgText = messageObject.messageOwner.message;
        } else if (messageObject.caption != null && !android.text.TextUtils.isEmpty(messageObject.caption.toString())) {
            currentMsgText = messageObject.caption.toString();
        } else if (messageObject.messageText != null && !android.text.TextUtils.isEmpty(messageObject.messageText.toString())) {
            currentMsgText = messageObject.messageText.toString();
        }

        if (history.isEmpty()) {
            if (!android.text.TextUtils.isEmpty(currentMsgText)) {
                messagesItemsContainer.addView(createEntryView(context, fragment, 1, messageObject.messageOwner != null ? messageObject.messageOwner.date : 0, currentMsgText, true));
            } else {
                messagesItemsContainer.addView(createEmptyTextView(context, LocaleController.getString("VeyraNoEditHistory", R.string.VeyraNoEditHistory)));
            }
        } else {
            for (int i = 0; i < history.size(); i++) {
                VeyraEditHistoryManager.EditEntry entry = history.get(i);
                boolean isFirst = (i == 0);
                messagesItemsContainer.addView(createEntryView(context, fragment, i + 1, entry.date, entry.text, isFirst));
            }
            if (!android.text.TextUtils.isEmpty(currentMsgText)) {
                int curDate = (messageObject.messageOwner != null && messageObject.messageOwner.edit_date > 0) ? messageObject.messageOwner.edit_date : (messageObject.messageOwner != null ? messageObject.messageOwner.date : 0);
                messagesItemsContainer.addView(createEntryView(context, fragment, history.size() + 1, curDate, currentMsgText, false));
            }
        }
        messagesScrollView.addView(messagesItemsContainer);
        contentContainer.addView(messagesScrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Build Reactions View
        reactionsScrollView = new ScrollView(context);
        LinearLayout reactionsItemsContainer = new LinearLayout(context);
        reactionsItemsContainer.setOrientation(LinearLayout.VERTICAL);

        boolean hasAnyReaction = populateReactions(context, reactionsItemsContainer, messageObject, dialogId, messageId);
        if (!hasAnyReaction) {
            reactionsItemsContainer.addView(createEmptyTextView(context, LocaleController.getString("VeyraNoReactionHistory", R.string.VeyraNoReactionHistory)));
        }
        reactionsScrollView.addView(reactionsItemsContainer);
        contentContainer.addView(reactionsScrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (reactionHistoryAllowed) {
            LinearLayout tabBar = new LinearLayout(context);
            tabBar.setOrientation(LinearLayout.HORIZONTAL);
            tabBar.setGravity(Gravity.CENTER);
            tabBar.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));

            GradientDrawable tabBarBg = new GradientDrawable();
            tabBarBg.setCornerRadius(AndroidUtilities.dp(12));
            tabBarBg.setColor(Theme.getColor(Theme.key_chat_inBubble));
            tabBar.setBackground(tabBarBg);

            tabMessagesView = createTabButton(context, LocaleController.getString("VeyraEditHistoryTabMessages", R.string.VeyraEditHistoryTabMessages));
            tabReactionsView = createTabButton(context, LocaleController.getString("VeyraEditHistoryTabReactions", R.string.VeyraEditHistoryTabReactions));

            tabBar.addView(tabMessagesView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 4, 0));
            tabBar.addView(tabReactionsView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 4, 0, 0, 0));
            root.addView(tabBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

            tabMessagesView.setEnabled(hasMsgHistory);
            tabMessagesView.setAlpha(hasMsgHistory ? 1.0f : 0.4f);

            tabReactionsView.setEnabled(hasReactionHistory);
            tabReactionsView.setAlpha(hasReactionHistory ? 1.0f : 0.4f);

            tabMessagesView.setOnClickListener(v -> selectTab(0));
            tabReactionsView.setOnClickListener(v -> selectTab(1));

            int initialTab = (!hasMsgHistory && hasReactionHistory) ? 1 : 0;
            selectTab(initialTab);
        } else {
            tabMessagesView = null;
            tabReactionsView = null;
            messagesScrollView.setVisibility(View.VISIBLE);
            reactionsScrollView.setVisibility(View.GONE);
        }

        root.addView(contentContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        setCustomView(root);
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

    private void selectTab(int tabIndex) {
        if (tabMessagesView == null || tabReactionsView == null) return;
        boolean isMessages = (tabIndex == 0);

        messagesScrollView.setVisibility(isMessages ? View.VISIBLE : View.GONE);
        reactionsScrollView.setVisibility(isMessages ? View.GONE : View.VISIBLE);

        tabMessagesView.setBackground(createTabPillBg(isMessages));
        tabMessagesView.setTextColor(Theme.getColor(isMessages ? Theme.key_featuredStickers_buttonText : Theme.key_dialogTextGray2));

        tabReactionsView.setBackground(createTabPillBg(!isMessages));
        tabReactionsView.setTextColor(Theme.getColor(!isMessages ? Theme.key_featuredStickers_buttonText : Theme.key_dialogTextGray2));
    }

    private static GradientDrawable createTabPillBg(boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(AndroidUtilities.dp(8));
        d.setColor(selected ? Theme.getColor(Theme.key_featuredStickers_addButton) : 0x00000000);
        return d;
    }

    private boolean populateReactions(Context context, LinearLayout container, MessageObject messageObject, long dialogId, int messageId) {
        boolean found = false;
        java.util.HashSet<String> seen = new java.util.HashSet<>();

        if (messageObject.messageOwner != null && messageObject.messageOwner.reactions != null) {
            TLRPC.TL_messageReactions reactions = messageObject.messageOwner.reactions;
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
                                if (!android.text.TextUtils.isEmpty(user.username)) {
                                    userName = "@" + user.username + " (" + UserObject.getUserName(user) + ")";
                                } else {
                                    userName = UserObject.getUserName(user);
                                }
                            } else {
                                userName = peerId != 0 ? ("User " + peerId) : LocaleController.getString("Reactions", R.string.Reactions);
                            }
                            container.addView(createReactionCard(context, emoji, userName, pr.date, 1));
                            found = true;
                        }
                    }
                }
            }

            if (reactions.results != null && !reactions.results.isEmpty()) {
                for (int i = 0; i < reactions.results.size(); i++) {
                    TLRPC.ReactionCount rc = reactions.results.get(i);
                    if (rc != null) {
                        String emoji = getReactionEmoji(rc.reaction);
                        String detail = (rc.chosen ? " (You)" : "");
                        container.addView(createReactionCountBadge(context, emoji, rc.count, detail));
                        found = true;
                    }
                }
            }
        }

        List<VeyraEditHistoryManager.ReactionEntry> dbHistory = VeyraEditHistoryManager.getReactionHistory(dialogId, messageId);
        if (dbHistory != null && !dbHistory.isEmpty()) {
            for (int i = 0; i < dbHistory.size(); i++) {
                VeyraEditHistoryManager.ReactionEntry entry = dbHistory.get(i);
                String key = entry.reaction + "_" + entry.userId;
                if (entry.userId == 0 || seen.add(key)) {
                    String userName;
                    if (entry.userId != 0) {
                        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(entry.userId);
                        if (user != null) {
                            if (!android.text.TextUtils.isEmpty(user.username)) {
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
                    container.addView(createReactionCard(context, entry.reaction, userName, entry.date, entry.count));
                    found = true;
                }
            }
        }

        return found;
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

    private View createReactionCard(Context context, String emoji, String title, int date, int count) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(10));
        bg.setColor(Theme.getColor(Theme.key_chat_inBubble));
        card.setBackground(bg);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView emojiView = new TextView(context);
        emojiView.setText(emoji);
        emojiView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        row.addView(emojiView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 10, 0));

        LinearLayout centerLayout = new LinearLayout(context);
        centerLayout.setOrientation(LinearLayout.VERTICAL);

        TextView nameView = new TextView(context);
        nameView.setText(!android.text.TextUtils.isEmpty(title) ? title : "Reaction");
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        nameView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        nameView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        centerLayout.addView(nameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        if (count > 1) {
            TextView countView = new TextView(context);
            countView.setText("Count: " + count);
            countView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            countView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
            centerLayout.addView(countView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        }
        row.addView(centerLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        if (date > 0) {
            TextView dateLabel = new TextView(context);
            dateLabel.setText(LocaleController.formatDateTime(date, false));
            dateLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            dateLabel.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
            dateLabel.setGravity(Gravity.END);
            row.addView(dateLabel, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        }

        card.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8);
        card.setLayoutParams(lp);
        return card;
    }

    private View createReactionCountBadge(Context context, String emoji, int count, String detail) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(8));
        bg.setColor(Theme.getColor(Theme.key_chat_inBubble));
        card.setBackground(bg);

        TextView emojiView = new TextView(context);
        emojiView.setText(emoji);
        emojiView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        card.addView(emojiView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        TextView countView = new TextView(context);
        countView.setText(count + detail);
        countView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        countView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        countView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        card.addView(countView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView createEmptyTextView(Context context, String message) {
        TextView tv = new TextView(context);
        tv.setText(message);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        tv.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        return tv;
    }

    private View createEntryView(Context context, BaseFragment fragment, int versionNumber, int date, String text, boolean isOriginal) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(10));
        bg.setColor(Theme.getColor(Theme.key_chat_inBubble));
        card.setBackground(bg);

        LinearLayout metaLayout = new LinearLayout(context);
        metaLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextView versionLabel = new TextView(context);
        String label = isOriginal ? LocaleController.getString("OriginalMessage", R.string.OriginalMessage) : ("Version " + versionNumber);
        versionLabel.setText(label);
        versionLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        versionLabel.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        versionLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlue2));

        TextView dateLabel = new TextView(context);
        dateLabel.setText(LocaleController.formatDateTime(date, false));
        dateLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        dateLabel.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        dateLabel.setGravity(Gravity.END);

        metaLayout.addView(versionLabel, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 1.0f));
        metaLayout.addView(dateLabel, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        card.addView(metaLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        TextView messageText = new TextView(context);
        messageText.setText(text);
        messageText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        messageText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        card.addView(messageText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        card.setOnClickListener(v -> {
            AndroidUtilities.addToClipboard(text);
            BulletinFactory.of(fragment).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
        });

        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10);
        card.setLayoutParams(lp);

        return card;
    }
}

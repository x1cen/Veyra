package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_keyboard;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.veyra.client.VeyraEditHistoryManager;

import java.util.ArrayList;
import java.util.List;

public class VeyraMessageHistoryActivity extends BaseFragment {

    public static final int MODE_MESSAGES = 0;
    public static final int MODE_REACTIONS = 1;
    public static final int MODE_ALL = 2;

    private final long dialogId;
    private MessageObject currentMessageObject;
    private final int mode;

    private RecyclerListView messagesListView;

    public static class VersionEntry {
        public final int versionIndex;
        public final int totalVersions;
        public final int date;
        public final int prevDate;
        public final MessageObject messageObject;
        public final String rawText;
        public boolean isExpanded;
        public final boolean isOut;

        public VersionEntry(int versionIndex, int totalVersions, int date, int prevDate, MessageObject messageObject, String rawText) {
            this.versionIndex = versionIndex;
            this.totalVersions = totalVersions;
            this.date = date;
            this.prevDate = prevDate;
            this.messageObject = messageObject;
            this.rawText = rawText;
            this.isExpanded = false;
            this.isOut = messageObject != null ? messageObject.isOut() : false;
        }
    }

    private final ArrayList<VersionEntry> versionEntries = new ArrayList<>();

    public VeyraMessageHistoryActivity(long dialogId, MessageObject messageObject) {
        this(dialogId, messageObject, MODE_ALL);
    }

    public VeyraMessageHistoryActivity(long dialogId, MessageObject messageObject, int mode) {
        super();
        this.dialogId = dialogId;
        this.mode = mode;
        this.currentMessageObject = deepCopyMessageObject(messageObject);
    }

    private MessageObject deepCopyMessageObject(MessageObject src) {
        if (src == null) return null;
        if (src.messageOwner != null) {
            try {
                int size = src.messageOwner.getObjectSize();
                NativeByteBuffer buf = new NativeByteBuffer(size);
                src.messageOwner.serializeToStream(buf);
                buf.position(0);
                int constructor = buf.readInt32(false);
                TLRPC.Message copy = TLRPC.Message.TLdeserialize(buf, constructor, false);
                buf.reuse();
                if (copy != null) return new MessageObject(currentAccount, copy, true, false);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        TLRPC.TL_message fallback = new TLRPC.TL_message();
        if (src.messageOwner != null) {
            fallback.id = src.messageOwner.id;
            fallback.dialog_id = src.messageOwner.dialog_id;
            fallback.date = src.messageOwner.date;
            fallback.edit_date = src.messageOwner.edit_date;
            fallback.message = src.messageOwner.message;
            fallback.flags = src.messageOwner.flags;
            fallback.from_id = src.messageOwner.from_id;
            fallback.peer_id = src.messageOwner.peer_id;
            fallback.entities = src.messageOwner.entities;
            fallback.reply_markup = src.messageOwner.reply_markup;
        }
        return new MessageObject(currentAccount, fallback, true, false);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        loadData();
        return true;
    }

    // SQLite reads run on globalQueue; results are swapped in on the UI thread.
    private void loadData() {
        if (currentMessageObject == null) return;
        final ArrayList<VersionEntry> versionEntries = new ArrayList<>();
        Utilities.globalQueue.postRunnable(() -> {
            try {
                loadDataInternal(versionEntries);
            } catch (Throwable e) {
                FileLog.e(e);
            }
            AndroidUtilities.runOnUIThread(() -> {
                if (isFinished || finishing) {
                    return;
                }
                this.versionEntries.clear();
                this.versionEntries.addAll(versionEntries);
                if (messagesListView != null && messagesListView.getAdapter() != null) {
                    messagesListView.getAdapter().notifyDataSetChanged();
                }
            });
        });
    }

    private void loadDataInternal(ArrayList<VersionEntry> versionEntries) {
        int messageId = currentMessageObject.getId();

        // 1. Load Edit History
        List<VeyraEditHistoryManager.EditEntry> history = VeyraEditHistoryManager.getHistory(currentAccount, dialogId, messageId);
        int totalVersions = (history != null ? history.size() : 0) + 1;

        int prevDate = 0;
        if (history != null && !history.isEmpty()) {
            for (int i = 0; i < history.size(); i++) {
                VeyraEditHistoryManager.EditEntry entry = history.get(i);
                MessageObject msgObj = deepCopyMessageObject(entry.toMessageObject(currentAccount));
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

        hasOwnBackground = true;
        SizeNotifierFrameLayout contentView = new SizeNotifierFrameLayout(context) {
            @Override
            protected Drawable getNewDrawable() {
                return Theme.getCachedWallpaperNonBlocking();
            }

            @Override
            protected boolean isActionBarVisible() {
                return false;
            }

            @Override
            protected boolean isStatusBarVisible() {
                return false;
            }

            @Override
            protected boolean useRootView() {
                return false;
            }
        };
        contentView.setOccupyStatusBar(false);
        contentView.setBackgroundImage(Theme.getCachedWallpaper(), Theme.isWallpaperMotion());

        fragmentView = contentView;

        messagesListView = new RecyclerListView(context);
        messagesListView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        messagesListView.setAdapter(new VersionsAdapter());
        messagesListView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(20));
        messagesListView.setClipToPadding(false);

        contentView.addView(messagesListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    private static String formatTimeDelta(int prevDate, int curDate) {
        if (prevDate <= 0 || curDate <= 0 || curDate <= prevDate) return "";
        int diff = curDate - prevDate;
        if (diff < 60) return "edited after " + diff + "s";
        if (diff < 3600) return "edited after " + (diff / 60) + "m";
        if (diff < 86400) return "edited after " + (diff / 3600) + "h";
        return "edited after " + (diff / 86400) + "d";
    }

    private static void highlightDifferences(SpannableStringBuilder ssb, String prev, String cur) {
        if (TextUtils.isEmpty(prev) || TextUtils.isEmpty(cur) || prev.equals(cur)) return;
        int minLen = Math.min(prev.length(), cur.length());
        int start = 0;
        while (start < minLen && prev.charAt(start) == cur.charAt(start)) {
            start++;
        }
        int endPrev = prev.length() - 1;
        int endCur = cur.length() - 1;
        while (endCur >= start && endPrev >= start && prev.charAt(endPrev) == cur.charAt(endCur)) {
            endPrev--;
            endCur--;
        }
        int highlightEnd = Math.min(endCur + 1, ssb.length());
        if (start < highlightEnd) {
            ssb.setSpan(new BackgroundColorSpan(0x3310B981), start, highlightEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void showMessageOptions(MessageObject msg, String plainText) {
        if (getParentActivity() == null || msg == null) return;
        ItemOptions options = ItemOptions.makeOptions(this, messagesListView);
        options.add(R.drawable.msg_copy, LocaleController.getString("Copy", R.string.Copy), () -> {
            CharSequence text = !TextUtils.isEmpty(plainText) ? plainText : msg.messageText;
            if (TextUtils.isEmpty(text) && msg.messageOwner != null) {
                text = msg.messageOwner.message;
            }
            if (!TextUtils.isEmpty(text)) {
                AndroidUtilities.addToClipboard(text.toString());
                BulletinFactory.of(this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
            }
        });
        options.add(R.drawable.msg_share, LocaleController.getString("ShareFile", R.string.ShareFile), () -> {
            try {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(android.content.Intent.EXTRA_TEXT, !TextUtils.isEmpty(plainText) ? plainText : msg.messageText);
                getParentActivity().startActivity(android.content.Intent.createChooser(intent, "Share"));
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
        options.show();
    }

    // --- Message Versions Adapter (Native Telegram Chat Style) ---
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
            wrapper.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4));
            wrapper.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // 1. Centered Date / Version Service Pill
            TextView servicePill = new TextView(context);
            servicePill.setId(11);
            servicePill.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            servicePill.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            servicePill.setTextColor(0xFFFFFFFF);
            servicePill.setPadding(AndroidUtilities.dp(11), AndroidUtilities.dp(3), AndroidUtilities.dp(11), AndroidUtilities.dp(3));
            servicePill.setGravity(Gravity.CENTER);
            Drawable pillBg = Theme.createServiceDrawable(AndroidUtilities.dp(11), servicePill, null);
            servicePill.setBackground(pillBg != null ? pillBg : Theme.getRoundRectSelectorDrawable(AndroidUtilities.dp(11), Theme.getColor(Theme.key_chat_serviceBackground)));

            LinearLayout.LayoutParams pillLp = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            pillLp.gravity = Gravity.CENTER_HORIZONTAL;
            pillLp.bottomMargin = AndroidUtilities.dp(6);
            wrapper.addView(servicePill, pillLp);

            // 2. Message Row (Horizontal Layout)
            LinearLayout messageRow = new LinearLayout(context);
            messageRow.setId(12);
            messageRow.setOrientation(LinearLayout.HORIZONTAL);
            messageRow.setGravity(Gravity.BOTTOM);

            // Avatar for incoming messages
            BackupImageView avatarView = new BackupImageView(context);
            avatarView.setId(25);
            avatarView.setRoundRadius(AndroidUtilities.dp(16));
            LinearLayout.LayoutParams avatarLp = LayoutHelper.createLinear(32, 32);
            avatarLp.rightMargin = AndroidUtilities.dp(6);
            avatarLp.bottomMargin = AndroidUtilities.dp(2);
            messageRow.addView(avatarView, avatarLp);

            // Message Bubble
            LinearLayout bubble = new LinearLayout(context);
            bubble.setId(20);
            bubble.setOrientation(LinearLayout.VERTICAL);
            bubble.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(6));

            // Sender Name (for incoming messages)
            TextView nameView = new TextView(context);
            nameView.setId(26);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            nameView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            bubble.addView(nameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

            // Message Body
            TextView messageView = new TextView(context);
            messageView.setId(23);
            messageView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            messageView.setLineSpacing(AndroidUtilities.dp(3), 1.15f);
            messageView.setTextIsSelectable(true);
            messageView.setMovementMethod(LinkMovementMethod.getInstance());
            messageView.setTextDirection(View.TEXT_DIRECTION_LOCALE);
            messageView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            bubble.addView(messageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            // "Show more / Show less" for long text
            TextView showMoreBtn = new TextView(context);
            showMoreBtn.setId(27);
            showMoreBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            showMoreBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            showMoreBtn.setPadding(0, AndroidUtilities.dp(2), 0, AndroidUtilities.dp(2));
            bubble.addView(showMoreBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            // Inline buttons container
            LinearLayout inlineContainer = new LinearLayout(context);
            inlineContainer.setId(28);
            inlineContainer.setOrientation(LinearLayout.VERTICAL);
            bubble.addView(inlineContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));

            // Info row (time, edited label, checkmarks)
            LinearLayout infoRow = new LinearLayout(context);
            infoRow.setOrientation(LinearLayout.HORIZONTAL);
            infoRow.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);

            TextView editedLabel = new TextView(context);
            editedLabel.setId(21);
            editedLabel.setText("edited");
            editedLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            editedLabel.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            infoRow.addView(editedLabel, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 4, 0));

            TextView timeView = new TextView(context);
            timeView.setId(22);
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            infoRow.addView(timeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            ImageView checkmarks = new ImageView(context);
            checkmarks.setId(29);
            checkmarks.setImageResource(R.drawable.msg_check_s);
            infoRow.addView(checkmarks, LayoutHelper.createLinear(14, 14, 4, 0, 0, 0));

            LinearLayout.LayoutParams infoLp = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            infoLp.gravity = Gravity.END;
            infoLp.topMargin = AndroidUtilities.dp(2);
            bubble.addView(infoRow, infoLp);

            LinearLayout.LayoutParams bubbleLp = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            messageRow.addView(bubble, bubbleLp);

            wrapper.addView(messageRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            return new RecyclerListView.Holder(wrapper);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            VersionEntry entry = versionEntries.get(position);
            View root = holder.itemView;

            TextView servicePill = root.findViewById(11);
            LinearLayout messageRow = root.findViewById(12);
            BackupImageView avatarView = root.findViewById(25);
            LinearLayout bubble = root.findViewById(20);
            TextView nameView = root.findViewById(26);
            TextView messageView = root.findViewById(23);
            TextView showMoreBtn = root.findViewById(27);
            LinearLayout inlineContainer = root.findViewById(28);
            TextView editedLabel = root.findViewById(21);
            TextView timeView = root.findViewById(22);
            ImageView checkmarks = root.findViewById(29);

            boolean isOut = entry.isOut;
            boolean isOriginal = (entry.versionIndex == 0);
            boolean isCurrent = (entry.versionIndex == entry.totalVersions - 1);

            // 1. Service Pill Text
            String timeFormatted = entry.date > 0 ? LocaleController.formatDateTime(entry.date, false) : "";
            String pillText;
            if (isOriginal) {
                pillText = TextUtils.isEmpty(timeFormatted) ? LocaleController.getString("OriginalMessage", R.string.OriginalMessage) : (LocaleController.getString("OriginalMessage", R.string.OriginalMessage) + " • " + timeFormatted);
            } else if (isCurrent) {
                pillText = TextUtils.isEmpty(timeFormatted) ? "Current Version" : ("Current Version • " + timeFormatted);
            } else {
                String delta = formatTimeDelta(entry.prevDate, entry.date);
                String label = "Edit #" + entry.versionIndex + (!TextUtils.isEmpty(delta) ? (" (" + delta + ")") : "");
                pillText = TextUtils.isEmpty(timeFormatted) ? label : (label + " • " + timeFormatted);
            }
            servicePill.setText((CharSequence) pillText);

            // 2. Bubble Alignment & Avatar
            messageRow.setGravity(isOut ? (Gravity.END | Gravity.BOTTOM) : (Gravity.START | Gravity.BOTTOM));
            avatarView.setVisibility(isOut ? View.GONE : View.VISIBLE);

            // 3. Bubble Shape & Colors
            GradientDrawable bubbleBg = new GradientDrawable();
            int bubbleColor = Theme.getColor(isOut ? Theme.key_chat_outBubble : Theme.key_chat_inBubble);
            bubbleBg.setColor(bubbleColor);
            float rBig = AndroidUtilities.dp(16);
            float rSmall = AndroidUtilities.dp(4);
            if (isOut) {
                bubbleBg.setCornerRadii(new float[]{ rBig, rBig, rBig, rBig, rSmall, rSmall, rBig, rBig });
            } else {
                bubbleBg.setCornerRadii(new float[]{ rBig, rBig, rBig, rBig, rBig, rBig, rSmall, rSmall });
            }
            bubble.setBackground(bubbleBg);

            // Colors
            int textColor = Theme.getColor(isOut ? Theme.key_chat_messageTextOut : Theme.key_chat_messageTextIn);
            int timeColor = Theme.getColor(isOut ? Theme.key_chat_outTimeText : Theme.key_chat_inTimeText);
            int linkColor = Theme.getColor(isOut ? Theme.key_chat_messageLinkOut : Theme.key_chat_messageLinkIn);

            messageView.setTextColor(textColor);
            timeView.setTextColor(timeColor);
            editedLabel.setTextColor(timeColor);
            checkmarks.setVisibility(isOut ? View.VISIBLE : View.GONE);
            checkmarks.setColorFilter(timeColor);

            if (isOriginal) {
                editedLabel.setVisibility(View.GONE);
            } else {
                editedLabel.setVisibility(View.VISIBLE);
            }

            if (entry.date > 0) {
                timeView.setText(LocaleController.formatDateTime(entry.date, true));
            } else {
                timeView.setText("");
            }

            // Sender Avatar & Name (for incoming)
            if (!isOut) {
                nameView.setVisibility(View.VISIBLE);
                TLRPC.User user = null;
                TLRPC.Chat chat = null;
                long fromId = 0;
                if (entry.messageObject != null && entry.messageObject.messageOwner != null && entry.messageObject.messageOwner.from_id != null) {
                    fromId = MessageObject.getPeerId(entry.messageObject.messageOwner.from_id);
                }
                if (fromId > 0) {
                    user = MessagesController.getInstance(currentAccount).getUser(fromId);
                } else if (fromId < 0) {
                    chat = MessagesController.getInstance(currentAccount).getChat(-fromId);
                }
                if (user == null && chat == null) {
                    if (dialogId > 0) {
                        user = MessagesController.getInstance(currentAccount).getUser(dialogId);
                    } else {
                        chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
                    }
                }
                AvatarDrawable avatarDrawable = new AvatarDrawable();
                if (user != null) {
                    avatarDrawable.setInfo(currentAccount, user);
                    avatarView.setForUserOrChat(user, avatarDrawable);
                    nameView.setText(UserObject.getUserName(user));
                    nameView.setTextColor(AvatarDrawable.getColorForId(user.id));
                } else if (chat != null) {
                    avatarDrawable.setInfo(currentAccount, chat);
                    avatarView.setForUserOrChat(chat, avatarDrawable);
                    nameView.setText(chat.title);
                    nameView.setTextColor(AvatarDrawable.getColorForId(chat.id));
                } else {
                    avatarDrawable.setInfo(dialogId, "User", null);
                    avatarView.setImageDrawable(avatarDrawable);
                    nameView.setText("Sender");
                    nameView.setTextColor(Theme.getColor(Theme.key_chat_inReplyNameText));
                }
            } else {
                nameView.setVisibility(View.GONE);
            }

            // Message text + rich entities + diff highlight
            CharSequence textSource = "";
            if (entry.messageObject != null) {
                if (!TextUtils.isEmpty(entry.messageObject.messageText)) {
                    textSource = entry.messageObject.messageText;
                } else if (!TextUtils.isEmpty(entry.messageObject.caption)) {
                    textSource = entry.messageObject.caption;
                } else if (entry.messageObject.messageOwner != null && !TextUtils.isEmpty(entry.messageObject.messageOwner.message)) {
                    textSource = entry.messageObject.messageOwner.message;
                }
            }
            if (TextUtils.isEmpty(textSource)) {
                textSource = entry.rawText != null ? entry.rawText : "";
            }

            SpannableStringBuilder ssb = new SpannableStringBuilder(textSource);
            if (entry.messageObject != null && entry.messageObject.messageOwner != null && entry.messageObject.messageOwner.entities != null && !entry.messageObject.messageOwner.entities.isEmpty()) {
                MessageObject.addEntitiesToText(ssb, entry.messageObject.messageOwner.entities, false, true, false, false);
            }

            if (position > 0) {
                VersionEntry prevEntry = versionEntries.get(position - 1);
                String prevStr = prevEntry.rawText != null ? prevEntry.rawText : "";
                String curStr = ssb.toString();
                highlightDifferences(ssb, prevStr, curStr);
            }

            CharSequence formatted = Emoji.replaceEmoji(ssb, messageView.getPaint().getFontMetricsInt(), false);
            messageView.setText(formatted);

            // Show More / Show Less
            String plainStr = ssb.toString();
            boolean isLong = plainStr.length() > 250 || (plainStr.contains("\n") && plainStr.split("\n").length > 5);
            if (isLong) {
                showMoreBtn.setVisibility(View.VISIBLE);
                showMoreBtn.setTextColor(linkColor);
                if (entry.isExpanded) {
                    messageView.setMaxLines(Integer.MAX_VALUE);
                    messageView.setEllipsize(null);
                    showMoreBtn.setText("Show less ▲");
                } else {
                    messageView.setMaxLines(5);
                    messageView.setEllipsize(TextUtils.TruncateAt.END);
                    showMoreBtn.setText("Show more ▼");
                }
                showMoreBtn.setOnClickListener(v -> {
                    entry.isExpanded = !entry.isExpanded;
                    notifyItemChanged(position);
                });
            } else {
                showMoreBtn.setVisibility(View.GONE);
                messageView.setMaxLines(Integer.MAX_VALUE);
                messageView.setEllipsize(null);
            }

            // Inline buttons rendering
            inlineContainer.removeAllViews();
            if (entry.messageObject != null && entry.messageObject.messageOwner != null && entry.messageObject.messageOwner.reply_markup instanceof TLRPC.TL_replyInlineMarkup) {
                TLRPC.TL_replyInlineMarkup markup = (TLRPC.TL_replyInlineMarkup) entry.messageObject.messageOwner.reply_markup;
                if (markup.rows != null && !markup.rows.isEmpty()) {
                    inlineContainer.setVisibility(View.VISIBLE);
                    Context ctx = root.getContext();
                    for (int r = 0; r < markup.rows.size(); r++) {
                        TL_keyboard.KeyboardInlineButtonRow row = markup.rows.get(r);
                        if (row == null || row.buttons == null || row.buttons.isEmpty()) continue;
                        LinearLayout rowLayout = new LinearLayout(ctx);
                        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                        for (int b = 0; b < row.buttons.size(); b++) {
                            TL_keyboard.KeyboardInlineButton btn = row.buttons.get(b);
                            if (btn == null) continue;
                            TextView btnView = new TextView(ctx);
                            btnView.setText(btn.text);
                            btnView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                            btnView.setTextColor(linkColor);
                            btnView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                            btnView.setGravity(Gravity.CENTER);
                            btnView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
                            GradientDrawable btnBg = new GradientDrawable();
                            btnBg.setCornerRadius(AndroidUtilities.dp(8));
                            btnBg.setColor(isOut ? 0x22FFFFFF : 0x15000000);
                            btnView.setBackground(btnBg);
                            if (btn.type instanceof TL_keyboard.TL_inlineButtonTypeUrl) {
                                String url = ((TL_keyboard.TL_inlineButtonTypeUrl) btn.type).url;
                                btnView.setOnClickListener(v -> Browser.openUrl(ctx, url));
                            }
                            rowLayout.addView(btnView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 2, 2, 2, 2));
                        }
                        inlineContainer.addView(rowLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 2));
                    }
                } else {
                    inlineContainer.setVisibility(View.GONE);
                }
            } else {
                inlineContainer.setVisibility(View.GONE);
            }

            final String copyContent = ssb.toString();
            bubble.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(copyContent)) {
                    AndroidUtilities.addToClipboard(copyContent);
                    BulletinFactory.of(VeyraMessageHistoryActivity.this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
                }
            });

            bubble.setOnLongClickListener(v -> {
                if (entry.messageObject != null) {
                    showMessageOptions(entry.messageObject, copyContent);
                    return true;
                }
                return false;
            });
        }
    }
}

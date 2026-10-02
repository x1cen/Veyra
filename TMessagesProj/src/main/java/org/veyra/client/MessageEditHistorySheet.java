/* Veyra Project */

package org.veyra.client;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.util.List;

public class MessageEditHistorySheet extends BottomSheet {

    public static void show(BaseFragment fragment, MessageObject messageObject) {
        if (fragment == null || fragment.getParentActivity() == null || messageObject == null) {
            return;
        }
        new MessageEditHistorySheet(fragment, messageObject).show();
    }

    private MessageEditHistorySheet(BaseFragment fragment, MessageObject messageObject) {
        super(fragment.getParentActivity(), false);
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
        root.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        List<VeyraEditHistoryManager.EditEntry> history = VeyraEditHistoryManager.getHistory(messageObject.getDialogId(), messageObject.getId());

        ScrollView scrollView = new ScrollView(context);
        LinearLayout itemsContainer = new LinearLayout(context);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);

        if (history.isEmpty() && (messageObject.messageOwner != null && messageObject.messageOwner.message != null)) {
            itemsContainer.addView(createEntryView(context, fragment, 1, messageObject.messageOwner.date, messageObject.messageOwner.message, true));
        } else {
            for (int i = 0; i < history.size(); i++) {
                VeyraEditHistoryManager.EditEntry entry = history.get(i);
                boolean isFirst = (i == 0);
                itemsContainer.addView(createEntryView(context, fragment, i + 1, entry.date, entry.text, isFirst));
            }
            if (messageObject.messageOwner != null && messageObject.messageOwner.message != null) {
                int curDate = messageObject.messageOwner.edit_date > 0 ? messageObject.messageOwner.edit_date : messageObject.messageOwner.date;
                itemsContainer.addView(createEntryView(context, fragment, history.size() + 1, curDate, messageObject.messageOwner.message, false));
            }
        }

        scrollView.addView(itemsContainer);
        root.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(root);
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
        dateLabel.setText(LocaleController.formatDateTime(date));
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

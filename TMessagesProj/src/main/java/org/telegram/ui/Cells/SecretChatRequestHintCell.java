package org.telegram.ui.Cells;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SecretChatHelper;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.veyra.client.VeyraSecretChatRequests;

import java.util.ArrayList;

// Veyra: chat list alert for incoming secret chat requests, laid out like UnconfirmedAuthHintCell.
public class SecretChatRequestHintCell extends FrameLayout {

    private final LinearLayout linearLayout;
    private final TextView titleTextView;
    private final TextView messageTextView;
    private final TextView yesButton;
    private final TextView noButton;
    private int height;

    public SecretChatRequestHintCell(Context context) {
        super(context);

        setClickable(true);

        linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        titleTextView = new TextView(context);
        titleTextView.setGravity(Gravity.CENTER);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        titleTextView.setTypeface(AndroidUtilities.bold());
        titleTextView.setText(LocaleController.getString(R.string.VeyraSecretChatRequestTitle));
        linearLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, Gravity.TOP | Gravity.FILL_HORIZONTAL, 28, 8, 28, 0));

        messageTextView = new TextView(context);
        messageTextView.setGravity(Gravity.CENTER);
        messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        messageTextView.setLineSpacing(dpf2(2), 1);
        linearLayout.addView(messageTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, Gravity.TOP | Gravity.FILL_HORIZONTAL, 28, 2, 28, 0));

        LinearLayout buttonsLayout = new LinearLayout(context);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(Gravity.CENTER);

        buttonsLayout.addView(new Space(context), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 1, Gravity.CENTER, 1));

        yesButton = createButton(context, LocaleController.getString(R.string.VeyraSecretChatRequestAccept));
        buttonsLayout.addView(yesButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 30));

        buttonsLayout.addView(new Space(context), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 1, Gravity.CENTER, 1));

        noButton = createButton(context, LocaleController.getString(R.string.VeyraSecretChatRequestDecline));
        buttonsLayout.addView(noButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 30));

        buttonsLayout.addView(new Space(context), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 1, Gravity.CENTER, 1));

        linearLayout.addView(buttonsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 28, 4, 28, 8));

        addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        updateColors();
    }

    private static TextView createButton(Context context, CharSequence text) {
        TextView button = new TextView(context);
        button.setPadding(dp(10), dp(5), dp(10), dp(7));
        button.setTypeface(AndroidUtilities.bold());
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.22f);
        button.setText(text);
        return button;
    }

    public void set(final BaseFragment fragment, final int currentAccount) {
        ArrayList<VeyraSecretChatRequests.Request> requests = VeyraSecretChatRequests.getInstance(currentAccount).getRequests();
        if (requests.isEmpty()) {
            return;
        }
        final VeyraSecretChatRequests.Request request = requests.get(0);
        TLRPC.User user = request.user;
        if (user == null) {
            user = MessagesController.getInstance(currentAccount).getUser(request.chat.user_id);
        }
        final String name = user != null ? UserObject.getUserName(user) : String.valueOf(request.chat.user_id);

        if (requests.size() == 1) {
            messageTextView.setText(LocaleController.formatString(R.string.VeyraSecretChatRequestSingle, name));
        } else {
            messageTextView.setText(LocaleController.formatString(R.string.VeyraSecretChatRequestMultiple, name, requests.size() - 1));
        }

        final int chatId = request.chat.id;
        yesButton.setOnClickListener(v -> {
            VeyraSecretChatRequests.Request r = VeyraSecretChatRequests.getInstance(currentAccount).remove(chatId);
            if (r == null) {
                return;
            }
            SecretChatHelper.getInstance(currentAccount).acceptRequestedSecretChat(r.chat, r.user, r.date);
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, LocaleController.formatString(R.string.VeyraSecretChatRequestAccepted, name)).show();
        });
        noButton.setOnClickListener(v -> {
            VeyraSecretChatRequests.Request r = VeyraSecretChatRequests.getInstance(currentAccount).remove(chatId);
            if (r == null) {
                return;
            }
            SecretChatHelper.getInstance(currentAccount).declineSecretChat(r.chat.id, false);
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.ic_ban, LocaleController.formatString(R.string.VeyraSecretChatRequestDeclined, name)).show();
        });
    }

    public void updateColors() {
        titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        messageTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        yesButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
        yesButton.setBackground(Theme.createSelectorDrawable(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteValueText), Theme.isCurrentThemeDark() ? .3f : .15f), Theme.RIPPLE_MASK_ROUNDRECT_6DP, dp(8)));
        noButton.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        noButton.setBackground(Theme.createSelectorDrawable(Theme.multAlpha(Theme.getColor(Theme.key_text_RedBold), Theme.isCurrentThemeDark() ? .3f : .15f), Theme.RIPPLE_MASK_ROUNDRECT_6DP, dp(8)));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width <= 0) {
            width = AndroidUtilities.displaySize.x;
        }
        linearLayout.measure(
            MeasureSpec.makeMeasureSpec(width - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(AndroidUtilities.displaySize.y, MeasureSpec.AT_MOST)
        );
        this.height = linearLayout.getMeasuredHeight() + getPaddingTop() + getPaddingBottom() + 1;
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    public int height() {
        if (getVisibility() != View.VISIBLE) {
            return 0;
        }
        if (height <= 0) {
            height = dp(72) + 1;
        }
        return height;
    }
}

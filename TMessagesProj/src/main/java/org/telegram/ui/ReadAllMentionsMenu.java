package org.telegram.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.graphics.PointF;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.chat.ViewPositionWatcher;

public class ReadAllMentionsMenu {

    public final static int TYPE_REACTIONS = 0;
    public final static int TYPE_MENTIONS = 1;
    public final static int TYPE_POLL_VOTES = 2;

    public static ActionBarPopupWindow show(int type, Activity activity, INavigationLayout navigationLayout, FrameLayout contentView, View mentionButton, Theme.ResourcesProvider resourcesProvider, Runnable onRead) {
        ActionBarPopupWindow.ActionBarPopupWindowLayout popupWindowLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(activity);
        popupWindowLayout.setMinimumWidth(AndroidUtilities.dp(200));

        ActionBarMenuSubItem cell = new ActionBarMenuSubItem(activity, true,true, resourcesProvider);
        cell.setMinimumWidth(AndroidUtilities.dp(200));

        final String text;
        if (type == TYPE_REACTIONS) {
            text = getString(R.string.ReadAllReactions);
        } else if (type == TYPE_MENTIONS) {
            text = getString(R.string.ReadAllMentions);
        } else {
            text = getString(R.string.ReadAllPollVotes);
        }

        cell.setTextAndIcon(text, R.drawable.msg_seen);
        cell.setOnClickListener(view -> {
            if (onRead != null) {
                onRead.run();
            }
        });
        popupWindowLayout.addView(cell);

        ActionBarPopupWindow scrimPopupWindow = new ActionBarPopupWindow(popupWindowLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        scrimPopupWindow.setPauseNotifications(true);
        scrimPopupWindow.setDismissAnimationDuration(220);
        scrimPopupWindow.setOutsideTouchable(true);
        scrimPopupWindow.setClippingEnabled(true);
        scrimPopupWindow.setAnimationStyle(R.style.PopupContextAnimation);
        scrimPopupWindow.setFocusable(true);
        popupWindowLayout.measure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST));
        scrimPopupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        scrimPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
        scrimPopupWindow.getContentView().setFocusableInTouchMode(true);

        int[] location = new int[2];
        mentionButton.getLocationInWindow(location);
        int popupWidth = popupWindowLayout.getMeasuredWidth();
        int popupHeight = popupWindowLayout.getMeasuredHeight();

        int x;
        if (LocaleController.isRTL) {
            x = location[0] - AndroidUtilities.dp(8);
        } else {
            x = location[0] + mentionButton.getWidth() - popupWidth + AndroidUtilities.dp(8);
        }
        x = Math.max(AndroidUtilities.dp(8), Math.min(x, AndroidUtilities.displaySize.x - popupWidth - AndroidUtilities.dp(8)));

        int y = location[1] - popupHeight - AndroidUtilities.dp(4);
        if (y < AndroidUtilities.statusBarHeight + AndroidUtilities.dp(48)) {
            y = location[1] + mentionButton.getHeight() + AndroidUtilities.dp(4);
        }
        scrimPopupWindow.showAtLocation(mentionButton, Gravity.LEFT | Gravity.TOP, x, y);
        return scrimPopupWindow;
    }
}

package org.veyra.client;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.ui.LaunchActivity;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class VeyraForwardProgress {

    private static final int NOTIFICATION_ID = 10044;
    private static WeakReference<View> currentBannerRef;
    private static volatile boolean isCancelled = false;

    public interface OnCancelListener {
        void onCancel();
    }

    private static OnCancelListener activeCancelListener;

    public static boolean isCancelled() {
        return isCancelled;
    }

    public static void show(int total, OnCancelListener cancelListener) {
        AndroidUtilities.runOnUIThread(() -> {
            isCancelled = false;
            activeCancelListener = cancelListener;
            dismissImmediate();

            updateNotification(0, total);
            createBanner(total);
        });
    }

    public static void update(int done, int total) {
        if (isCancelled) return;
        AndroidUtilities.runOnUIThread(() -> {
            updateNotification(done, total);
            updateBanner(done, total);
        });
    }

    public static void complete(int total, int failed) {
        AndroidUtilities.runOnUIThread(() -> {
            cancelNotification();
            completeBanner(total, failed);
        });
    }

    public static void cancel() {
        isCancelled = true;
        AndroidUtilities.runOnUIThread(() -> {
            cancelNotification();
            if (activeCancelListener != null) {
                try {
                    activeCancelListener.onCancel();
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
            cancelBanner();
        });
    }

    private static void updateNotification(int done, int total) {
        try {
            NotificationsController.checkOtherNotificationsChannel();
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(
                    ApplicationLoader.applicationContext,
                    NotificationsController.OTHER_NOTIFICATIONS_CHANNEL
            );
            builder.setSmallIcon(android.R.drawable.stat_sys_upload);
            builder.setWhen(System.currentTimeMillis());
            builder.setContentTitle(LocaleController.getString(R.string.AppName) + " - فوروارد در پس‌زمینه");
            final int percent = total > 0 ? (done * 100 / total) : 0;
            builder.setContentText(String.format(Locale.US, "در حال ارسال %d از %d پیام (%d%%)", done, total, percent));
            builder.setProgress(total, done, false);
            builder.setOngoing(true);
            NotificationManagerCompat.from(ApplicationLoader.applicationContext).notify(NOTIFICATION_ID, builder.build());
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void cancelNotification() {
        try {
            NotificationManagerCompat.from(ApplicationLoader.applicationContext).cancel(NOTIFICATION_ID);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void createBanner(int total) {
        final Activity activity = LaunchActivity.instance;
        if (activity == null || activity.isFinishing()) {
            return;
        }

        try {
            final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            if (decorView == null) {
                return;
            }

            final FrameLayout container = new FrameLayout(activity);
            container.setTag("VeyraForwardProgressBar");

            // Rounded Card Background with subtle border
            final GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xF0182430); // Dark translucent surface
            bg.setCornerRadius(AndroidUtilities.dp(16));
            bg.setStroke(AndroidUtilities.dp(1), 0x30FFFFFF);
            container.setBackground(bg);
            container.setElevation(AndroidUtilities.dp(8));
            container.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));

            final FrameLayout.LayoutParams containerLp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.CENTER_HORIZONTAL
            );
            containerLp.topMargin = AndroidUtilities.statusBarHeight + AndroidUtilities.dp(8);
            containerLp.leftMargin = AndroidUtilities.dp(14);
            containerLp.rightMargin = AndroidUtilities.dp(14);

            final LinearLayout contentLayout = new LinearLayout(activity);
            contentLayout.setOrientation(LinearLayout.VERTICAL);

            // Top row: [Forward Icon] [Status Text] [Cancel Button]
            final LinearLayout topRow = new LinearLayout(activity);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            final ImageView iconView = new ImageView(activity);
            iconView.setImageResource(R.drawable.msg_forward);
            iconView.setColorFilter(new PorterDuffColorFilter(0xFF3390EC, PorterDuff.Mode.SRC_IN));
            topRow.addView(iconView, new LinearLayout.LayoutParams(AndroidUtilities.dp(20), AndroidUtilities.dp(20)));

            final TextView titleView = new TextView(activity);
            titleView.setTag("titleView");
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
            titleView.setTextColor(Color.WHITE);
            titleView.setSingleLine(true);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            titleView.setText(String.format(Locale.US, "در حال فوروارد... ۰ از %d (۰٪)", total));

            final LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            titleLp.leftMargin = AndroidUtilities.dp(10);
            titleLp.rightMargin = AndroidUtilities.dp(10);
            topRow.addView(titleView, titleLp);

            final ImageView closeBtn = new ImageView(activity);
            closeBtn.setImageResource(R.drawable.ic_ab_close);
            closeBtn.setColorFilter(new PorterDuffColorFilter(0x80FFFFFF, PorterDuff.Mode.SRC_IN));
            closeBtn.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
            closeBtn.setOnClickListener(v -> cancel());
            topRow.addView(closeBtn, new LinearLayout.LayoutParams(AndroidUtilities.dp(26), AndroidUtilities.dp(26)));

            contentLayout.addView(topRow, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            // Bottom row: Sleek Progress Track & Fill
            final FrameLayout progressTrack = new FrameLayout(activity);
            progressTrack.setTag("progressTrack");
            final GradientDrawable trackBg = new GradientDrawable();
            trackBg.setColor(0x28FFFFFF);
            trackBg.setCornerRadius(AndroidUtilities.dp(3));
            progressTrack.setBackground(trackBg);

            final View progressFill = new View(activity);
            progressFill.setTag("progressFill");
            final GradientDrawable fillBg = new GradientDrawable();
            fillBg.setColor(0xFF3390EC); // Telegram blue
            fillBg.setCornerRadius(AndroidUtilities.dp(3));
            progressFill.setBackground(fillBg);

            final FrameLayout.LayoutParams fillLp = new FrameLayout.LayoutParams(0, AndroidUtilities.dp(4));
            progressTrack.addView(progressFill, fillLp);

            final LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(4));
            trackLp.topMargin = AndroidUtilities.dp(8);
            contentLayout.addView(progressTrack, trackLp);

            container.addView(contentLayout, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

            // Animate entry from top
            container.setAlpha(0f);
            container.setTranslationY(-AndroidUtilities.dp(30));
            decorView.addView(container, containerLp);
            container.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(250)
                    .start();

            currentBannerRef = new WeakReference<>(container);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void updateBanner(int done, int total) {
        final View banner = currentBannerRef != null ? currentBannerRef.get() : null;
        if (banner == null) return;
        try {
            final TextView titleView = banner.findViewWithTag("titleView");
            if (titleView != null) {
                int percent = total > 0 ? (int) ((done / (float) total) * 100) : 0;
                if (percent > 100) percent = 100;
                titleView.setText(String.format(Locale.US, "در حال فوروارد... %d از %d (%d%%)", done, total, percent));
            }

            final View progressTrack = banner.findViewWithTag("progressTrack");
            final View progressFill = banner.findViewWithTag("progressFill");
            if (progressTrack != null && progressFill != null) {
                int trackWidth = progressTrack.getWidth();
                if (trackWidth <= 0) {
                    trackWidth = AndroidUtilities.displaySize.x - AndroidUtilities.dp(56);
                }
                float ratio = total > 0 ? (done / (float) total) : 0f;
                if (ratio > 1f) ratio = 1f;
                int targetWidth = (int) (trackWidth * ratio);
                progressFill.getLayoutParams().width = targetWidth;
                progressFill.requestLayout();
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void completeBanner(int total, int failed) {
        final View banner = currentBannerRef != null ? currentBannerRef.get() : null;
        if (banner == null) return;
        try {
            final TextView titleView = banner.findViewWithTag("titleView");
            final View progressFill = banner.findViewWithTag("progressFill");
            final View progressTrack = banner.findViewWithTag("progressTrack");

            if (progressTrack != null && progressFill != null) {
                int trackWidth = progressTrack.getWidth();
                if (trackWidth <= 0) {
                    trackWidth = AndroidUtilities.displaySize.x - AndroidUtilities.dp(56);
                }
                progressFill.getLayoutParams().width = trackWidth;
                final GradientDrawable fillBg = new GradientDrawable();
                fillBg.setColor(failed > 0 ? 0xFFFFA726 : 0xFF4CAF50); // Orange if warnings, Green if all succeeded
                fillBg.setCornerRadius(AndroidUtilities.dp(3));
                progressFill.setBackground(fillBg);
                progressFill.requestLayout();
            }

            if (titleView != null) {
                if (failed > 0) {
                    titleView.setText(String.format(Locale.US, "فوروارد پایان یافت (%d موفق، %d خطا)", total - failed, failed));
                } else {
                    titleView.setText(String.format(Locale.US, "✓ فوروارد با موفقیت انجام شد (%d پیام)", total));
                }
            }

            dismissDelayed(2200);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void cancelBanner() {
        final View banner = currentBannerRef != null ? currentBannerRef.get() : null;
        if (banner == null) return;
        try {
            final TextView titleView = banner.findViewWithTag("titleView");
            if (titleView != null) {
                titleView.setText("فوروارد متوقف شد");
            }
            dismissDelayed(1200);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void dismissDelayed(long delay) {
        AndroidUtilities.runOnUIThread(() -> {
            final View banner = currentBannerRef != null ? currentBannerRef.get() : null;
            if (banner == null) return;
            banner.animate()
                    .alpha(0f)
                    .translationY(-AndroidUtilities.dp(30))
                    .setDuration(300)
                    .withEndAction(VeyraForwardProgress::dismissImmediate)
                    .start();
        }, delay);
    }

    public static void dismissImmediate() {
        AndroidUtilities.runOnUIThread(() -> {
            final View banner = currentBannerRef != null ? currentBannerRef.get() : null;
            if (banner != null) {
                if (banner.getParent() instanceof ViewGroup) {
                    ((ViewGroup) banner.getParent()).removeView(banner);
                }
            }
            currentBannerRef = null;
        });
    }
}

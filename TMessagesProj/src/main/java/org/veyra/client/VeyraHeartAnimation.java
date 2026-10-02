/* Veyra Project — Private */
package org.veyra.client;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;

import java.util.ArrayList;
import java.util.Random;

/**
 * Veyra private feature: heart rain animation.
 *
 * Triggered once per account lifetime for LOVE_USER_IDs when they first log in.
 * Also exposed as a static method for the "Love" button in ProfileActivity.
 *
 * IDs are embedded at build time from the LOVE_USER_IDS secret (never in source).
 */
public class VeyraHeartAnimation {

    // ---- IDs baked in at build time from GitHub secret LOVE_USER_IDS ----
    // Format: "id1,id2" → parsed once.
    private static final String LOVE_USER_IDS_RAW = BuildConfig_LOVE_USER_IDS();
    private static long LOVE_ID_A = 0L;
    private static long LOVE_ID_B = 0L;

    static {
        try {
            String[] parts = LOVE_USER_IDS_RAW.split(",");
            if (parts.length >= 2) {
                LOVE_ID_A = Long.parseLong(parts[0].trim());
                LOVE_ID_B = Long.parseLong(parts[1].trim());
            }
        } catch (Exception ignored) {}
    }

    /** Returns true if userId is one of the two love-pair accounts. */
    public static boolean isLoveUser(long userId) {
        return userId == LOVE_ID_A || userId == LOVE_ID_B;
    }

    /** Returns true if user A and B are the specific pair (order-insensitive). */
    public static boolean areLovePair(long a, long b) {
        return (a == LOVE_ID_A && b == LOVE_ID_B) || (a == LOVE_ID_B && b == LOVE_ID_A);
    }

    private static final String PREFS_KEY_PREFIX = "veyra_heart_rain_shown_";

    /** Called from LaunchActivity / dialogs screen once user is authorised. */
    public static void maybeShowFirstLoginRain(Context context, ViewGroup rootView, int account) {
        long selfId = UserConfig.getInstance(account).getClientUserId();
        if (!isLoveUser(selfId)) return;

        String key = PREFS_KEY_PREFIX + selfId;
        android.content.SharedPreferences prefs =
                context.getSharedPreferences("veyra_heart_prefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean(key, false)) return; // already shown

        prefs.edit().putBoolean(key, true).apply();

        // Post slightly delayed so the UI is ready
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (rootView == null || rootView.getWindowToken() == null) return;
            showHeartRain(context, rootView, 30_000L, true);
        }, 800);
    }

    /**
     * Show a heart rain on rootView.
     * @param durationMs how long to rain (ms)
     * @param showFinalBigHeart if true, after rain ends show a large pulsing heart
     */
    public static void showHeartRain(Context context, ViewGroup rootView, long durationMs, boolean showFinalBigHeart) {
        // Overlay that intercepts nothing (click-through)
        FrameLayout overlay = new FrameLayout(context);
        overlay.setClickable(false);
        rootView.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        Handler handler = new Handler(Looper.getMainLooper());
        Random rng = new Random();
        ArrayList<Runnable> spawners = new ArrayList<>();

        int[] heartColors = {0xFFE53935, 0xFFE91E63, 0xFFFF4081, 0xFFFF80AB, 0xFFFF1744};

        // Spawn a heart particle every 150ms
        Runnable spawnHeart = new Runnable() {
            @Override
            public void run() {
                if (overlay.getParent() == null) return;
                spawnHeartParticle(context, overlay, rng, heartColors);
                handler.postDelayed(this, 120 + rng.nextInt(80));
            }
        };
        spawners.add(spawnHeart);
        handler.post(spawnHeart);

        // Stop rain after durationMs
        handler.postDelayed(() -> {
            for (Runnable r : spawners) handler.removeCallbacks(r);
            if (showFinalBigHeart) {
                showBigHeart(context, overlay, () -> {
                    // remove overlay after big heart done
                    handler.postDelayed(() -> {
                        if (overlay.getParent() != null)
                            rootView.removeView(overlay);
                    }, 1500);
                });
            } else {
                handler.postDelayed(() -> {
                    if (overlay.getParent() != null)
                        rootView.removeView(overlay);
                }, 800);
            }
        }, durationMs);
    }

    /** Spawn one falling heart emoji particle. */
    private static void spawnHeartParticle(Context context, FrameLayout overlay,
                                            Random rng, int[] colors) {
        int w = overlay.getWidth();
        int h = overlay.getHeight();
        if (w == 0 || h == 0) return;

        TextView heart = new TextView(context);
        heart.setText("❤️");
        int sizeDp = 18 + rng.nextInt(22); // 18–40dp
        heart.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, sizeDp);
        heart.setAlpha(0.85f + rng.nextFloat() * 0.15f);

        int startX = rng.nextInt(w);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = startX;
        lp.topMargin = -AndroidUtilities.dp(60);
        heart.setLayoutParams(lp);
        overlay.addView(heart);

        long fallDuration = 2500 + rng.nextInt(2000);
        float wobbleAmount = AndroidUtilities.dp(30 + rng.nextInt(40));

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(fallDuration);
        anim.addUpdateListener(va -> {
            float p = (float) va.getAnimatedValue();
            float y = -AndroidUtilities.dp(60) + p * (h + AndroidUtilities.dp(80));
            float wobble = (float) Math.sin(p * Math.PI * 3) * wobbleAmount;
            lp.topMargin = (int) y;
            lp.leftMargin = startX + (int) wobble;
            heart.setLayoutParams(lp);
            if (p > 0.8f) heart.setAlpha(1f - (p - 0.8f) / 0.2f);
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (overlay.getParent() != null) overlay.removeView(heart);
            }
        });
        anim.start();

        // Gentle spin
        ObjectAnimator spin = ObjectAnimator.ofFloat(heart, "rotation", -15f, 15f);
        spin.setDuration(700 + rng.nextInt(400));
        spin.setRepeatMode(ValueAnimator.REVERSE);
        spin.setRepeatCount(ValueAnimator.INFINITE);
        spin.start();
    }

    /** Show a big pulsing heart in the center of the overlay, then fade out. */
    private static void showBigHeart(Context context, FrameLayout overlay, Runnable onDone) {
        TextView bigHeart = new TextView(context);
        bigHeart.setText("❤️");
        bigHeart.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 120);
        bigHeart.setGravity(Gravity.CENTER);
        bigHeart.setAlpha(0f);
        bigHeart.setScaleX(0.3f);
        bigHeart.setScaleY(0.3f);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        overlay.addView(bigHeart, lp);

        // Appear
        AnimatorSet appear = new AnimatorSet();
        appear.playTogether(
                ObjectAnimator.ofFloat(bigHeart, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(bigHeart, "scaleX", 0.3f, 1.15f),
                ObjectAnimator.ofFloat(bigHeart, "scaleY", 0.3f, 1.15f)
        );
        appear.setDuration(600);
        appear.setInterpolator(new DecelerateInterpolator());

        // Settle
        AnimatorSet settle = new AnimatorSet();
        settle.playTogether(
                ObjectAnimator.ofFloat(bigHeart, "scaleX", 1.15f, 1f),
                ObjectAnimator.ofFloat(bigHeart, "scaleY", 1.15f, 1f)
        );
        settle.setDuration(250);

        // Pulse
        AnimatorSet pulse = new AnimatorSet();
        pulse.playTogether(
                ObjectAnimator.ofFloat(bigHeart, "scaleX", 1f, 1.08f, 1f),
                ObjectAnimator.ofFloat(bigHeart, "scaleY", 1f, 1.08f, 1f)
        );
        pulse.setDuration(600);

        // Fade out
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(bigHeart, "alpha", 1f, 0f);
        fadeOut.setDuration(800);
        fadeOut.setStartDelay(1500);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (overlay.getParent() != null) overlay.removeView(bigHeart);
                if (onDone != null) onDone.run();
            }
        });

        AnimatorSet full = new AnimatorSet();
        full.playSequentially(appear, settle, pulse, fadeOut);
        full.start();
    }

    /** Side-burst hearts from left and right edges (Love button). */
    public static void showSideBurst(Context context, ViewGroup rootView, int durationMs) {
        FrameLayout overlay = new FrameLayout(context);
        overlay.setClickable(false);
        rootView.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        Handler handler = new Handler(Looper.getMainLooper());
        Random rng = new Random();

        Runnable spawn = new Runnable() {
            @Override public void run() {
                if (overlay.getParent() == null) return;
                spawnSideHeart(context, overlay, rng, true);
                spawnSideHeart(context, overlay, rng, false);
                handler.postDelayed(this, 100 + rng.nextInt(100));
            }
        };
        handler.post(spawn);

        handler.postDelayed(() -> {
            handler.removeCallbacks(spawn);
            handler.postDelayed(() -> {
                if (overlay.getParent() != null) rootView.removeView(overlay);
            }, 1200);
        }, durationMs);
    }

    private static void spawnSideHeart(Context context, FrameLayout overlay,
                                        Random rng, boolean fromLeft) {
        int w = overlay.getWidth();
        int h = overlay.getHeight();
        if (w == 0 || h == 0) return;

        TextView heart = new TextView(context);
        heart.setText("❤️");
        int sizeDp = 16 + rng.nextInt(20);
        heart.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, sizeDp);

        int startX = fromLeft ? -AndroidUtilities.dp(40) : w;
        int startY = h / 4 + rng.nextInt(h / 2);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = startX;
        lp.topMargin = startY;
        heart.setLayoutParams(lp);
        overlay.addView(heart);

        float targetX = fromLeft ? w / 2f + rng.nextInt(w / 4) : w / 2f - rng.nextInt(w / 4);
        float targetY = startY - AndroidUtilities.dp(80 + rng.nextInt(120));

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(1000 + rng.nextInt(600));
        anim.addUpdateListener(va -> {
            float p = (float) va.getAnimatedValue();
            lp.leftMargin = (int) (startX + (targetX - startX) * p);
            lp.topMargin = (int) (startY + (targetY - startY) * p);
            heart.setLayoutParams(lp);
            if (p > 0.7f) heart.setAlpha(1f - (p - 0.7f) / 0.3f);
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (overlay.getParent() != null) overlay.removeView(heart);
            }
        });
        anim.start();
    }

    // Placeholder — replaced by build-time injection from gradle
    private static String BuildConfig_LOVE_USER_IDS() {
        try {
            return org.telegram.messenger.BuildVars.LOVE_USER_IDS;
        } catch (Throwable t) {
            return "0,0";
        }
    }
}

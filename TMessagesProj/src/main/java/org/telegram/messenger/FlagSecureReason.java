package org.telegram.messenger;

import android.view.Window;
import android.view.WindowManager;

import org.telegram.ui.LaunchActivity;

import java.util.HashMap;

public class FlagSecureReason {

    private static HashMap<Window, Integer> currentSecureReasons;

    private final Window window;
    private final FlagSecureCondition condition;

    public FlagSecureReason(Window window, FlagSecureCondition condition) {
        this.window = window;
        this.condition = condition;
    }

    private boolean attached = false;
    private boolean value = false;

    public static boolean isScreenProtectionActive() {
        return SharedConfig.passcodeHash != null && SharedConfig.passcodeHash.length() > 0 && !SharedConfig.allowScreenCapture;
    }

    public static void updateAllWindows() {
        if (LaunchActivity.instance != null) {
            updateWindowSecure(LaunchActivity.instance.getWindow());
        }
    }

    public void invalidate() {
        boolean newValue = attached && condition != null && condition.run();
        if (newValue != value) {
            update((value = newValue) ? +1 : -1);
        }
    }

    public void attach() {
        if (attached) {
            return;
        }
        attached = true;
        invalidate();
    }

    public void detach() {
        if (!attached) {
            return;
        }
        attached = false;
        invalidate();
    }

    private void update(int add) {
        if (currentSecureReasons == null) {
            currentSecureReasons = new HashMap<>();
        }

        Integer count = currentSecureReasons.get(window);
        int newCount = Math.max(0, (count == null ? 0 : count) + add);
        if (newCount <= 0) {
            currentSecureReasons.remove(window);
        } else {
            currentSecureReasons.put(window, newCount);
        }

        updateWindowSecure(window);
    }

    public static void updateWindowSecure(Window window) {
        if (window == null) {
            return;
        }

        if (isScreenProtectionActive()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        AndroidUtilities.logFlagSecure();
    }

    public static boolean isSecuredNow(Window window) {
        return isScreenProtectionActive();
    }

    public interface FlagSecureCondition {
        boolean run();
    }

}

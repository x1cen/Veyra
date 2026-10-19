package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.DEFAULT);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        try {
            org.telegram.messenger.FileLog.d("LauncherIconController: switching icon to " + icon.key);
            // Enable target first, then disable others — this order forces a launcher refresh on Samsung/OneUI
            pm.setComponentEnabledSetting(icon.getComponentName(ctx), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            for (LauncherIcon i : LauncherIcon.values()) {
                if (i != icon) {
                    pm.setComponentEnabledSetting(i.getComponentName(ctx), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
            }
            // Samsung One UI fix: activity-alias via DONT_KILL_APP often does not invalidate the
            // launcher icon cache until a second PACKAGE_CHANGED. Re-assert after a short tick.
            try {
                android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                android.content.ComponentName target = icon.getComponentName(ctx);
                h.postDelayed(() -> {
                    try {
                        pm.setComponentEnabledSetting(target, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    } catch (Throwable ignore) {}
                }, 600);
            } catch (Throwable ignore) {}
            org.telegram.messenger.FileLog.d("LauncherIconController: successfully switched icon to " + icon.key);
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e("LauncherIconController: failed to switch icon", e);
        }
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconDefault),
        VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium, false),
        TURBO("TurboIcon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground_sa, R.string.AppIconTurbo, false),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconNox, false);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}

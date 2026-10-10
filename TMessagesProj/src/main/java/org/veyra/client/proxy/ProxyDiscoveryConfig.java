package org.veyra.client.proxy;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public class ProxyDiscoveryConfig {

    private static final String PREF_NAME = "veyra_proxy_discovery";

    public static boolean isEnabled() {
        return getPrefs().getBoolean("enabled", false);
    }

    public static void setEnabled(boolean enabled) {
        getPrefs().edit().putBoolean("enabled", enabled).apply();
    }

    public static boolean isPruneDeadEnabled() {
        // Default: prune/remove non-working proxies automatically
        return getPrefs().getBoolean("prune_dead", true);
    }

    public static void setPruneDeadEnabled(boolean prune) {
        getPrefs().edit().putBoolean("prune_dead", prune).apply();
    }

    public static boolean isSpeedTestEnabled() {
        return getPrefs().getBoolean("speed_test", true);
    }

    public static void setSpeedTestEnabled(boolean speedTest) {
        getPrefs().edit().putBoolean("speed_test", speedTest).apply();
    }

    public static int getIntervalMinutes() {
        return getPrefs().getInt("interval_minutes", 15);
    }

    public static void setIntervalMinutes(int minutes) {
        getPrefs().edit().putInt("interval_minutes", minutes).apply();
    }

    public static long getLastRunTime() {
        return getPrefs().getLong("last_run_time", 0);
    }

    public static void setLastRunTime(long timestamp) {
        getPrefs().edit().putLong("last_run_time", timestamp).apply();
    }

    public static String getLastWinner() {
        return getPrefs().getString("last_winner", "");
    }

    public static void setLastWinner(String winner) {
        getPrefs().edit().putString("last_winner", winner != null ? winner : "").apply();
    }

    public static int getLastPrunedCount() {
        return getPrefs().getInt("last_pruned_count", 0);
    }

    public static void setLastPrunedCount(int count) {
        getPrefs().edit().putInt("last_pruned_count", count).apply();
    }

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}

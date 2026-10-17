package org.veyra.client.proxy;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public class ProxyDiscoveryConfig {

    private static final String PREF_NAME = "veyra_proxy_discovery";

    public static final int STRATEGY_LOWEST_LATENCY = 0;
    public static final int STRATEGY_STABILITY = 1;
    public static final int STRATEGY_BALANCED = 2;

    public static boolean isEnabled() {
        return getPrefs().getBoolean("enabled", false);
    }

    public static void setEnabled(boolean enabled) {
        getPrefs().edit().putBoolean("enabled", enabled).apply();
    }

    public static int getStrategy() {
        return getPrefs().getInt("strategy", STRATEGY_BALANCED);
    }

    public static void setStrategy(int strategy) {
        getPrefs().edit().putInt("strategy", strategy).apply();
    }

    public static boolean isAutoSwitchOnFailure() {
        return getPrefs().getBoolean("auto_switch_failure", true);
    }

    public static void setAutoSwitchOnFailure(boolean autoSwitch) {
        getPrefs().edit().putBoolean("auto_switch_failure", autoSwitch).apply();
    }

    public static int getTimeoutMs() {
        return getPrefs().getInt("timeout_ms", 2500);
    }

    public static void setTimeoutMs(int timeoutMs) {
        getPrefs().edit().putInt("timeout_ms", timeoutMs).apply();
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

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}

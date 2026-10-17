package org.veyra.client.proxy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.utils.proxy.ProxySettings;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public final class ProxyStatusManager {

    private static final String PREF_NAME = "veyra_proxy_status";
    private static final String KEY_INACTIVE_SET = "inactive_proxy_keys";
    private static final String KEY_ACTIVE_COLLAPSED = "active_collapsed";
    private static final String KEY_INACTIVE_COLLAPSED = "inactive_collapsed";

    private static Set<String> inactiveKeysCache = null;
    private static final Object lock = new Object();

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static Set<String> getInactiveKeys() {
        synchronized (lock) {
            if (inactiveKeysCache == null) {
                Set<String> saved = getPrefs().getStringSet(KEY_INACTIVE_SET, null);
                inactiveKeysCache = saved != null ? new HashSet<>(saved) : new HashSet<>();
            }
            return inactiveKeysCache;
        }
    }

    private static void saveInactiveKeys() {
        synchronized (lock) {
            if (inactiveKeysCache != null) {
                getPrefs().edit().putStringSet(KEY_INACTIVE_SET, new HashSet<>(inactiveKeysCache)).apply();
            }
        }
    }

    public static String getProxyKey(SharedConfig.ProxyInfo info) {
        if (info == null || info.settings == null) return "";
        String addr = info.settings.getAddress() != null ? info.settings.getAddress() : "";
        int port = info.settings.getPort();
        String secret = info.settings.getSecret() != null ? info.settings.getSecret() : "";
        return addr + ":" + port + ":" + secret;
    }

    public static boolean isAether(ProxySettings settings) {
        if (settings == null) return false;
        String addr = settings.getAddress();
        int port = settings.getPort();
        return (org.veyra.client.aether.AetherConfig.getSocksHost().equals(addr) || "127.0.0.1".equals(addr)) &&
                (port == org.veyra.client.aether.AetherConfig.getSocksPort() ||
                 port == org.veyra.client.aether.AetherController.getInstance().getActivePort());
    }

    public static boolean isAether(SharedConfig.ProxyInfo info) {
        if (info == null || info.settings == null) return false;
        return isAether(info.settings);
    }

    public static boolean isInactive(SharedConfig.ProxyInfo info) {
        if (info == null || isAether(info)) return false;
        String key = getProxyKey(info);
        if (TextUtils.isEmpty(key)) return false;
        return getInactiveKeys().contains(key);
    }

    public static void setInactive(SharedConfig.ProxyInfo info, boolean inactive) {
        if (info == null || isAether(info)) return;
        String key = getProxyKey(info);
        if (TextUtils.isEmpty(key)) return;

        synchronized (lock) {
            Set<String> set = getInactiveKeys();
            if (inactive) {
                set.add(key);
            } else {
                set.remove(key);
            }
            saveInactiveKeys();
        }
    }

    public static void markInactive(SharedConfig.ProxyInfo info) {
        setInactive(info, true);
    }

    public static void markActive(SharedConfig.ProxyInfo info) {
        setInactive(info, false);
    }

    public static boolean isActiveCollapsed() {
        return getPrefs().getBoolean(KEY_ACTIVE_COLLAPSED, false);
    }

    public static void setActiveCollapsed(boolean collapsed) {
        getPrefs().edit().putBoolean(KEY_ACTIVE_COLLAPSED, collapsed).apply();
    }

    public static boolean isInactiveCollapsed() {
        return getPrefs().getBoolean(KEY_INACTIVE_COLLAPSED, false);
    }

    public static void setInactiveCollapsed(boolean collapsed) {
        getPrefs().edit().putBoolean(KEY_INACTIVE_COLLAPSED, collapsed).apply();
    }

    public static List<SharedConfig.ProxyInfo> getActiveProxies(List<SharedConfig.ProxyInfo> all) {
        List<SharedConfig.ProxyInfo> result = new ArrayList<>();
        if (all == null) return result;
        for (SharedConfig.ProxyInfo info : all) {
            if (!isInactive(info)) {
                result.add(info);
            }
        }
        return result;
    }

    public static List<SharedConfig.ProxyInfo> getInactiveProxies(List<SharedConfig.ProxyInfo> all) {
        List<SharedConfig.ProxyInfo> result = new ArrayList<>();
        if (all == null) return result;
        for (SharedConfig.ProxyInfo info : all) {
            if (isInactive(info)) {
                result.add(info);
            }
        }
        return result;
    }

    public static void recheckAllInactive(Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                SharedConfig.loadProxyList();
                List<SharedConfig.ProxyInfo> inactives = getInactiveProxies(SharedConfig.proxyList);
                for (SharedConfig.ProxyInfo info : inactives) {
                    if (info.settings == null || isAether(info)) continue;
                    String host = info.settings.getAddress();
                    int port = info.settings.getPort();
                    long start = SystemClock.elapsedRealtime();
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(host, port), 2500);
                        long ping = SystemClock.elapsedRealtime() - start;
                        info.ping = ping;
                        info.available = true;
                        info.availableCheckTime = SystemClock.elapsedRealtime();
                        markActive(info);
                    } catch (Exception e) {
                        info.available = false;
                        info.ping = 0;
                        info.availableCheckTime = SystemClock.elapsedRealtime();
                    }
                }
                SharedConfig.saveProxyList();
            } catch (Exception e) {
                FileLog.e("ProxyStatusManager", e);
            } finally {
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                    if (onDone != null) onDone.run();
                });
            }
        });
    }

    public static void clearAllInactive(Runnable onDone) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                SharedConfig.loadProxyList();
                List<SharedConfig.ProxyInfo> toRemove = getInactiveProxies(SharedConfig.proxyList);
                for (SharedConfig.ProxyInfo info : toRemove) {
                    SharedConfig.deleteProxy(info);
                }
                synchronized (lock) {
                    if (inactiveKeysCache != null) {
                        inactiveKeysCache.clear();
                    }
                    getPrefs().edit().remove(KEY_INACTIVE_SET).apply();
                }
            } catch (Exception e) {
                FileLog.e("ProxyStatusManager", e);
            } finally {
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                if (onDone != null) onDone.run();
            }
        });
    }
}

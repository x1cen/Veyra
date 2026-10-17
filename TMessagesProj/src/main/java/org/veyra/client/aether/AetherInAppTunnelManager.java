package org.veyra.client.aether;

import android.net.Uri;
import android.text.TextUtils;

import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

public final class AetherInAppTunnelManager {

    private static volatile boolean isApplied = false;
    private static ProxySelector defaultProxySelector = null;
    private static final Object lock = new Object();

    private static final Executor UI_EXECUTOR = AndroidUtilities::runOnUIThread;

    public static boolean isTunnelActive() {
        return isApplied && AetherConfig.isTunnelAllTraffic() && AetherController.getInstance().getState() == AetherController.STATE_CONNECTED;
    }

    public static void apply(String host, int port) {
        if (!AetherConfig.isTunnelAllTraffic()) {
            clear();
            return;
        }
        if (TextUtils.isEmpty(host) || port <= 0) {
            return;
        }

        synchronized (lock) {
            try {
                System.setProperty("socksProxyHost", host);
                System.setProperty("socksProxyPort", String.valueOf(port));
                System.setProperty("java.net.socks.username", "");
                System.setProperty("java.net.socks.password", "");

                if (defaultProxySelector == null) {
                    defaultProxySelector = ProxySelector.getDefault();
                }

                final List<Proxy> aetherProxyList = Collections.singletonList(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port)));
                final List<Proxy> directProxyList = Collections.singletonList(Proxy.NO_PROXY);

                ProxySelector.setDefault(new ProxySelector() {
                    @Override
                    public List<Proxy> select(URI uri) {
                        if (uri == null) {
                            return aetherProxyList;
                        }
                        String targetHost = uri.getHost();
                        if (targetHost == null) {
                            return aetherProxyList;
                        }
                        if ("127.0.0.1".equals(targetHost) || "localhost".equalsIgnoreCase(targetHost)) {
                            return directProxyList;
                        }
                        if (AetherConfig.isBypassIran() && isDomestic(targetHost)) {
                            return directProxyList;
                        }
                        return aetherProxyList;
                    }

                    @Override
                    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                        if (defaultProxySelector != null) {
                            defaultProxySelector.connectFailed(uri, sa, ioe);
                        }
                    }
                });

                isApplied = true;
                FileLog.d("[AetherInAppTunnel] System properties and ProxySelector applied (" + host + ":" + port + ")");
            } catch (Exception e) {
                FileLog.e("AetherInAppTunnelManager", e);
            }
        }

        AndroidUtilities.runOnUIThread(() -> {
            try {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                    ProxyConfig.Builder builder = new ProxyConfig.Builder();
                    builder.addProxyRule("socks://" + host + ":" + port);
                    if (AetherConfig.isBypassIran()) {
                        builder.addBypassRule("*.ir");
                        builder.addBypassRule("<local>");
                        builder.addBypassRule("10.*");
                        builder.addBypassRule("192.168.*");
                        builder.addBypassRule("172.16.*");
                        builder.addBypassRule("127.*");
                        builder.addBypassRule("localhost");
                    }
                    ProxyConfig config = builder.build();
                    ProxyController.getInstance().setProxyOverride(config, UI_EXECUTOR, () -> {
                        FileLog.d("[AetherInAppTunnel] WebView proxy override applied (" + host + ":" + port + ")");
                    });
                }
            } catch (Exception e) {
                FileLog.e("AetherInAppTunnelManager", e);
            }
        });
    }

    public static void clear() {
        synchronized (lock) {
            try {
                System.clearProperty("socksProxyHost");
                System.clearProperty("socksProxyPort");
                System.clearProperty("java.net.socks.username");
                System.clearProperty("java.net.socks.password");

                if (defaultProxySelector != null) {
                    ProxySelector.setDefault(defaultProxySelector);
                    defaultProxySelector = null;
                }
                isApplied = false;
                FileLog.d("[AetherInAppTunnel] System properties and ProxySelector cleared");
            } catch (Exception e) {
                FileLog.e("AetherInAppTunnelManager", e);
            }
        }

        AndroidUtilities.runOnUIThread(() -> {
            try {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                    ProxyController.getInstance().clearProxyOverride(UI_EXECUTOR, () -> {
                        FileLog.d("[AetherInAppTunnel] WebView proxy override cleared");
                    });
                }
            } catch (Exception e) {
                FileLog.e("AetherInAppTunnelManager", e);
            }
        });
    }

    public static void update() {
        if (AetherController.getInstance().getState() == AetherController.STATE_CONNECTED) {
            String host = AetherConfig.getSocksHost();
            int port = AetherController.getInstance().getActivePort();
            if (AetherConfig.isTunnelAllTraffic()) {
                apply(host, port);
            } else {
                clear();
            }
        } else {
            clear();
        }
    }

    private static boolean isDomestic(String host) {
        if (host == null) return false;
        host = host.toLowerCase().trim();
        if (host.endsWith(".ir")) return true;
        if (host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("127.")) return true;
        if (host.startsWith("172.")) {
            try {
                String[] parts = host.split("\\.");
                if (parts.length >= 2) {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) return true;
                }
            } catch (Exception ignore) {}
        }
        return false;
    }
}

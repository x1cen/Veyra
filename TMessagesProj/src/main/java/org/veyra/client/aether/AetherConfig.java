package org.veyra.client.aether;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AetherConfig {

    private static final String PREFS_NAME = "aether_config";

    public static final int BACKEND_AETHER = 0;
    public static final int BACKEND_AETHER_PSIPHON = 1;
    public static final int BACKEND_TOR = 2;
    public static final int BACKEND_AETHER_TOR = 3;
    public static final int BACKEND_TOR_PSIPHON = 4;
    public static final int BACKEND_TOR_AETHER = 5;

    public static final int PROTOCOL_AUTO = 0;
    public static final int PROTOCOL_MASQUE = 1;
    public static final int PROTOCOL_WIREGUARD = 2;
    public static final int PROTOCOL_GOOL = 3;
    public static final int PROTOCOL_MIM = 4;

    public static final int CARRIER_H3 = 0;
    public static final int CARRIER_H2 = 1;

    public static final int SCAN_BALANCED = 0;
    public static final int SCAN_TURBO = 1;
    public static final int SCAN_THOROUGH = 2;
    public static final int SCAN_VERIFIED = 3;
    public static final int SCAN_IRONCLAD = 4;

    public static final int IP_V4 = 0;
    public static final int IP_V6 = 1;
    public static final int IP_DUAL = 2;

    public static final int NOIZE_MASQUE_FIREWALL = 0;
    public static final int NOIZE_MASQUE_GFW = 1;
    public static final int NOIZE_MASQUE_OFF = 2;

    public static final int NOIZE_WG_AGGRESSIVE = 0;
    public static final int NOIZE_WG_BALANCED = 1;
    public static final int NOIZE_WG_LIGHT = 2;
    public static final int NOIZE_WG_OFF = 3;

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isUserInIran() {
        try {
            int account = UserConfig.selectedAccount;
            TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
            if (user != null && !TextUtils.isEmpty(user.phone)) {
                String phone = user.phone.trim();
                if (phone.startsWith("+98") || phone.startsWith("98")) {
                    return true;
                }
            }
        } catch (Exception ignore) {}
        return false;
    }

    public static void checkInitDefaults() {
        SharedPreferences prefs = getPrefs();
        if (!prefs.contains("initialized")) {
            boolean isIran = isUserInIran();
            SharedPreferences.Editor ed = prefs.edit();
            ed.putBoolean("initialized", true);
            ed.putString("socks_host", "127.0.0.1");
            ed.putInt("socks_port", 1819);

            if (isIran) {
                ed.putInt("protocol", PROTOCOL_AUTO);
                ed.putInt("masque_carrier", CARRIER_H2);
                ed.putBoolean("masque_fragment", true);
                ed.putString("masque_fragment_size", "8-24");
                ed.putString("masque_fragment_delay", "5-15");
                ed.putInt("noize_masque", NOIZE_MASQUE_FIREWALL);
                ed.putBoolean("ech", true);
                ed.putBoolean("bypass_iran", true);
            }
            ed.apply();
        }
    }

    public static boolean isEnabled() {
        return getPrefs().getBoolean("enabled", false);
    }

    public static void setEnabled(boolean enabled) {
        checkInitDefaults();
        getPrefs().edit().putBoolean("enabled", enabled).apply();
    }

    public static String getSocksHost() {
        return getPrefs().getString("socks_host", "127.0.0.1");
    }

    public static void setSocksHost(String host) {
        getPrefs().edit().putString("socks_host", TextUtils.isEmpty(host) ? "127.0.0.1" : host.trim()).apply();
    }

    public static int getSocksPort() {
        return getPrefs().getInt("socks_port", 1819);
    }

    public static void setSocksPort(int port) {
        getPrefs().edit().putInt("socks_port", port > 0 && port < 65536 ? port : 1819).apply();
    }

    public static int getBackend() {
        return getPrefs().getInt("backend", BACKEND_AETHER);
    }

    public static boolean isPsiphonBackend() {
        int b = getBackend();
        return b == BACKEND_AETHER_PSIPHON || b == BACKEND_TOR_PSIPHON;
    }

    public static void setBackend(int backend) {
        getPrefs().edit().putInt("backend", backend).apply();
    }

    public static String getExitCountry() {
        return getPrefs().getString("exit_country", "AUTO");
    }

    public static void setExitCountry(String code) {
        getPrefs().edit().putString("exit_country", code != null ? code.toUpperCase().trim() : "AUTO").apply();
    }

    public static int getProtocol() {
        return getPrefs().getInt("protocol", PROTOCOL_AUTO);
    }

    public static void setProtocol(int protocol) {
        getPrefs().edit().putInt("protocol", protocol).apply();
    }

    public static int getMasqueCarrier() {
        return getPrefs().getInt("masque_carrier", CARRIER_H2);
    }

    public static void setMasqueCarrier(int carrier) {
        getPrefs().edit().putInt("masque_carrier", carrier).apply();
    }

    public static boolean isMasqueFragment() {
        return getPrefs().getBoolean("masque_fragment", true);
    }

    public static void setMasqueFragment(boolean fragment) {
        getPrefs().edit().putBoolean("masque_fragment", fragment).apply();
    }

    public static String getMasqueFragmentSize() {
        return getPrefs().getString("masque_fragment_size", "8-24");
    }

    public static void setMasqueFragmentSize(String size) {
        getPrefs().edit().putString("masque_fragment_size", size != null ? size.trim() : "8-24").apply();
    }

    public static String getMasqueFragmentDelay() {
        return getPrefs().getString("masque_fragment_delay", "5-15");
    }

    public static void setMasqueFragmentDelay(String delay) {
        getPrefs().edit().putString("masque_fragment_delay", delay != null ? delay.trim() : "5-15").apply();
    }

    public static int getNoizeMasque() {
        return getPrefs().getInt("noize_masque", NOIZE_MASQUE_FIREWALL);
    }

    public static void setNoizeMasque(int noize) {
        getPrefs().edit().putInt("noize_masque", noize).apply();
    }

    public static int getNoizeWg() {
        return getPrefs().getInt("noize_wg", NOIZE_WG_AGGRESSIVE);
    }

    public static void setNoizeWg(int noize) {
        getPrefs().edit().putInt("noize_wg", noize).apply();
    }

    public static int getScanMode() {
        return getPrefs().getInt("scan_mode", SCAN_BALANCED);
    }

    public static void setScanMode(int mode) {
        getPrefs().edit().putInt("scan_mode", mode).apply();
    }

    public static int getIpVersion() {
        return getPrefs().getInt("ip_version", IP_V4);
    }

    public static void setIpVersion(int ip) {
        getPrefs().edit().putInt("ip_version", ip).apply();
    }

    public static boolean isQuickReconnect() {
        return getPrefs().getBoolean("quick_reconnect", true);
    }

    public static void setQuickReconnect(boolean quick) {
        getPrefs().edit().putBoolean("quick_reconnect", quick).apply();
    }

    public static boolean isEch() {
        return getPrefs().getBoolean("ech", true);
    }

    public static void setEch(boolean ech) {
        getPrefs().edit().putBoolean("ech", ech).apply();
    }

    public static boolean isBypassIran() {
        return getPrefs().getBoolean("bypass_iran", true);
    }

    public static void setBypassIran(boolean bypass) {
        getPrefs().edit().putBoolean("bypass_iran", bypass).apply();
    }

    public static boolean isBlockAds() {
        return getPrefs().getBoolean("block_ads", false);
    }

    public static void setBlockAds(boolean block) {
        getPrefs().edit().putBoolean("block_ads", block).apply();
    }

    public static String getManualPeer() {
        return getPrefs().getString("manual_peer", "");
    }

    public static void setManualPeer(String peer) {
        getPrefs().edit().putString("manual_peer", peer != null ? peer.trim() : "").apply();
    }

    public static String getWiwOuter() {
        return getPrefs().getString("wiw_outer", "");
    }

    public static void setWiwOuter(String peer) {
        getPrefs().edit().putString("wiw_outer", peer != null ? peer.trim() : "").apply();
    }

    public static String getWiwInner() {
        return getPrefs().getString("wiw_inner", "");
    }

    public static void setWiwInner(String peer) {
        getPrefs().edit().putString("wiw_inner", peer != null ? peer.trim() : "").apply();
    }

    public static String getDns() {
        return getPrefs().getString("dns", "1.1.1.1,1.0.0.1");
    }

    public static void setDns(String dns) {
        getPrefs().edit().putString("dns", dns != null ? dns.trim() : "").apply();
    }

    public static int getKeepalive() {
        return getPrefs().getInt("keepalive", 5);
    }

    public static void setKeepalive(int sec) {
        getPrefs().edit().putInt("keepalive", sec > 0 ? sec : 5).apply();
    }

    public static List<String> toArgs(int effectiveProto, boolean forceH2, boolean forceFragment) {
        List<String> args = new ArrayList<>();

        args.add("--bind");
        args.add(getSocksHost() + ":" + getSocksPort());

        int backend = getBackend();
        if (backend == BACKEND_TOR) {
            args.add("--tor-only");
        } else if (backend == BACKEND_AETHER_TOR) {
            args.add("--tor");
        } else if (backend == BACKEND_TOR_AETHER) {
            args.add("--tor-reverse");
        } else if (backend == BACKEND_AETHER_PSIPHON) {
            args.add("--psiphon");
        } else if (backend == BACKEND_TOR_PSIPHON) {
            args.add("--tor");
            args.add("--psiphon");
        }

        if (backend == BACKEND_AETHER_PSIPHON || backend == BACKEND_TOR_PSIPHON) {
            String country = getExitCountry();
            if (!TextUtils.isEmpty(country) && !"AUTO".equalsIgnoreCase(country)) {
                args.add("--psiphon-region");
                args.add(country);
            }
        }

        int protocol = effectiveProto;
        if (protocol == PROTOCOL_MASQUE) {
            args.add("--masque");
            String peer = getManualPeer();
            if (!TextUtils.isEmpty(peer)) {
                args.add("--peer");
                args.add(peer);
            }
        } else if (protocol == PROTOCOL_WIREGUARD) {
            args.add("--wg");
            String peer = getManualPeer();
            if (!TextUtils.isEmpty(peer)) {
                args.add("--wg-peer");
                args.add(peer);
            }
        } else if (protocol == PROTOCOL_GOOL) {
            args.add("--gool");
            String outer = getWiwOuter();
            String inner = getWiwInner();
            if (!TextUtils.isEmpty(outer)) {
                args.add("--wiw-outer");
                args.add(outer);
            }
            if (!TextUtils.isEmpty(inner)) {
                args.add("--wiw-inner");
                args.add(inner);
            }
            if (TextUtils.isEmpty(outer) && TextUtils.isEmpty(inner)) {
                args.add("--wiw-scan");
            }
        } else if (protocol == PROTOCOL_MIM) {
            args.add("--mim");
            args.add("--mim-scan");
        }

        int ip = getIpVersion();
        if (ip == IP_V4) {
            args.add("-4");
        } else if (ip == IP_V6) {
            args.add("-6");
        } else if (ip == IP_DUAL) {
            args.add("--dual");
        }

        int scan = getScanMode();
        if (scan == SCAN_TURBO) {
            args.add("--turbo");
        } else if (scan == SCAN_BALANCED) {
            args.add("--balanced");
        } else if (scan == SCAN_THOROUGH) {
            args.add("--thorough");
        } else if (scan == SCAN_VERIFIED) {
            args.add("--verified");
        } else if (scan == SCAN_IRONCLAD) {
            args.add("--ironclad");
        }

        if (isQuickReconnect()) {
            args.add("--quick-reconnect");
        } else {
            args.add("--no-quick-reconnect");
        }

        if (protocol == PROTOCOL_MASQUE || protocol == PROTOCOL_MIM) {
            boolean useH2 = forceH2 || getMasqueCarrier() == CARRIER_H2;
            if (useH2) {
                args.add("--h2");
                if (forceFragment || isMasqueFragment()) {
                    args.add("--fragment");
                    String fSize = getMasqueFragmentSize();
                    if (!TextUtils.isEmpty(fSize)) {
                        args.add("--fragment-size");
                        args.add(fSize);
                    }
                    String fDelay = getMasqueFragmentDelay();
                    if (!TextUtils.isEmpty(fDelay)) {
                        args.add("--fragment-delay");
                        args.add(fDelay);
                    }
                } else {
                    args.add("--no-fragment");
                }
            } else {
                args.add("--h3");
            }

            int noize = getNoizeMasque();
            args.add("--noize");
            if (noize == NOIZE_MASQUE_FIREWALL) {
                args.add("firewall");
            } else if (noize == NOIZE_MASQUE_GFW) {
                args.add("gfw");
            } else {
                args.add("off");
            }
        } else if (protocol == PROTOCOL_WIREGUARD || protocol == PROTOCOL_GOOL) {
            int noize = getNoizeWg();
            args.add("--noize");
            if (noize == NOIZE_WG_AGGRESSIVE) {
                args.add("aggressive");
            } else if (noize == NOIZE_WG_BALANCED) {
                args.add("balanced");
            } else if (noize == NOIZE_WG_LIGHT) {
                args.add("light");
            } else {
                args.add("off");
            }

            int keepalive = getKeepalive();
            if (keepalive > 0) {
                args.add("--keepalive");
                args.add(String.valueOf(keepalive));
            }
        }

        if (isEch()) {
            args.add("--ech");
            args.add("auto");
        }

        if (isBypassIran()) {
            args.add("--route-direct");
            args.add("private,ir");
        }

        if (isBlockAds()) {
            args.add("--route-block");
            args.add("doubleclick.net,adservice.google.com");
        }

        String dns = getDns();
        if (!TextUtils.isEmpty(dns)) {
            args.add("--dns");
            args.add(dns);
        }

        return args;
    }

    public static Map<String, String> toEnv(int effectiveProto, boolean forceH2, boolean forceFragment) {
        Map<String, String> env = new HashMap<>();
        env.put("AETHER_SOCKS", getSocksHost() + ":" + getSocksPort());
        env.put("RUST_LOG", "info");
        return env;
    }
}

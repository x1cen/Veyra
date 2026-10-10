package org.veyra.client.aether;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AetherConfig {

    private static final String PREF_NAME = "veyra_aether";

    public static final int PROTOCOL_MASQUE = 0;
    public static final int PROTOCOL_WIREGUARD = 1;
    public static final int PROTOCOL_GOOL = 2;
    public static final int PROTOCOL_MIM = 3;

    public static final int BACKEND_AETHER = 0;
    public static final int BACKEND_AETHER_PSIPHON = 1;
    public static final int BACKEND_TOR = 2;
    public static final int BACKEND_AETHER_TOR = 3;
    public static final int BACKEND_TOR_PSIPHON = 4;
    public static final int BACKEND_TOR_AETHER = 5;

    public static int getBackend() {
        return getPrefs().getInt("backend", BACKEND_AETHER);
    }

    public static void setBackend(int backend) {
        getPrefs().edit().putInt("backend", backend).apply();
    }

    public static String getExitCountry() {
        return getPrefs().getString("exit_country", "");
    }

    public static void setExitCountry(String country) {
        getPrefs().edit().putString("exit_country", country != null ? country.trim() : "").apply();
    }

    public static final int CARRIER_H3 = 0;
    public static final int CARRIER_H2 = 1;

    public static final int SCAN_BALANCED = 0;
    public static final int SCAN_TURBO = 1;
    public static final int SCAN_THOROUGH = 2;
    public static final int SCAN_STEALTH = 3;
    public static final int SCAN_IRONCLAD = 4;

    public static final int IP_V4 = 0;
    public static final int IP_V6 = 1;
    public static final int IP_DUAL = 2;

    public static final int NOIZE_MASQUE_FIREWALL = 0;
    public static final int NOIZE_MASQUE_GFW = 1;
    public static final int NOIZE_MASQUE_OFF = 2;

    public static final int NOIZE_WG_BALANCED = 0;
    public static final int NOIZE_WG_AGGRESSIVE = 1;
    public static final int NOIZE_WG_LIGHT = 2;
    public static final int NOIZE_WG_OFF = 3;

    public static boolean isEnabled() {
        return getPrefs().getBoolean("enabled", false);
    }

    public static void setEnabled(boolean enabled) {
        getPrefs().edit().putBoolean("enabled", enabled).apply();
    }

    public static int getProtocol() {
        return getPrefs().getInt("protocol", PROTOCOL_MASQUE);
    }

    public static void setProtocol(int protocol) {
        getPrefs().edit().putInt("protocol", protocol).apply();
    }

    public static int getMasqueCarrier() {
        return getPrefs().getInt("masque_carrier", CARRIER_H3);
    }

    public static void setMasqueCarrier(int carrier) {
        getPrefs().edit().putInt("masque_carrier", carrier).apply();
    }

    public static boolean isMasqueFragment() {
        return getPrefs().getBoolean("masque_fragment", false);
    }

    public static void setMasqueFragment(boolean fragment) {
        getPrefs().edit().putBoolean("masque_fragment", fragment).apply();
    }

    public static String getMasqueFragmentSize() {
        return getPrefs().getString("masque_fragment_size", "8-24");
    }

    public static void setMasqueFragmentSize(String size) {
        getPrefs().edit().putString("masque_fragment_size", size).apply();
    }

    public static String getMasqueFragmentDelay() {
        return getPrefs().getString("masque_fragment_delay", "5-15");
    }

    public static void setMasqueFragmentDelay(String delay) {
        getPrefs().edit().putString("masque_fragment_delay", delay).apply();
    }

    public static int getScanMode() {
        return getPrefs().getInt("scan_mode", SCAN_BALANCED);
    }

    public static void setScanMode(int scanMode) {
        getPrefs().edit().putInt("scan_mode", scanMode).apply();
    }

    public static int getIpVersion() {
        return getPrefs().getInt("ip_version", IP_V4);
    }

    public static void setIpVersion(int ipVersion) {
        getPrefs().edit().putInt("ip_version", ipVersion).apply();
    }

    public static int getNoizeMasque() {
        return getPrefs().getInt("noize_masque", NOIZE_MASQUE_FIREWALL);
    }

    public static void setNoizeMasque(int noize) {
        getPrefs().edit().putInt("noize_masque", noize).apply();
    }

    public static int getNoizeWg() {
        return getPrefs().getInt("noize_wg", NOIZE_WG_BALANCED);
    }

    public static void setNoizeWg(int noize) {
        getPrefs().edit().putInt("noize_wg", noize).apply();
    }

    public static String getPeer() {
        return getPrefs().getString("peer", "");
    }

    public static void setPeer(String peer) {
        getPrefs().edit().putString("peer", peer != null ? peer.trim() : "").apply();
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

    public static int getKeepalive() {
        return getPrefs().getInt("keepalive", 0);
    }

    public static void setKeepalive(int keepalive) {
        getPrefs().edit().putInt("keepalive", keepalive).apply();
    }

    public static boolean isQuickReconnect() {
        return getPrefs().getBoolean("quick_reconnect", true);
    }

    public static void setQuickReconnect(boolean reconnect) {
        getPrefs().edit().putBoolean("quick_reconnect", reconnect).apply();
    }

    public static int getSocksPort() {
        return getPrefs().getInt("socks_port", 1819);
    }

    public static void setSocksPort(int port) {
        getPrefs().edit().putInt("socks_port", port).apply();
    }

    public static String getDns() {
        return getPrefs().getString("dns", "1.1.1.1,1.0.0.1");
    }

    public static void setDns(String dns) {
        getPrefs().edit().putString("dns", dns != null ? dns.trim() : "").apply();
    }

    public static List<String> toArgs() {
        List<String> args = new ArrayList<>();
        int port = getSocksPort();
        args.add("--bind");
        args.add("127.0.0.1:" + port);

        int backend = getBackend();
        if (backend == BACKEND_AETHER_PSIPHON) {
            args.add("--psiphon");
        } else if (backend == BACKEND_TOR) {
            args.add("--tor-only");
        } else if (backend == BACKEND_AETHER_TOR) {
            args.add("--tor");
        } else if (backend == BACKEND_TOR_PSIPHON) {
            args.add("--tor-only");
            args.add("--psiphon");
        } else if (backend == BACKEND_TOR_AETHER) {
            args.add("--tor-reverse");
        }

        String exit = getExitCountry();
        if (!TextUtils.isEmpty(exit)) {
            if (backend == BACKEND_AETHER_PSIPHON || backend == BACKEND_TOR_PSIPHON) {
                args.add("--psiphon-region");
                args.add(exit);
            } else {
                args.add("--exit-loc");
                args.add(exit);
            }
        }

        int protocol = getProtocol();
        if (protocol == PROTOCOL_MASQUE) {
            args.add("--masque");
        } else if (protocol == PROTOCOL_WIREGUARD) {
            args.add("--wg");
        } else if (protocol == PROTOCOL_GOOL) {
            args.add("--gool");
        } else if (protocol == PROTOCOL_MIM) {
            args.add("--mim");
        }

        boolean hasManualPeer = false;
        if (protocol == PROTOCOL_MASQUE || protocol == PROTOCOL_WIREGUARD) {
            String p = getPeer();
            if (!TextUtils.isEmpty(p)) {
                args.add("--peer");
                args.add(p);
                hasManualPeer = true;
            }
        } else if (protocol == PROTOCOL_GOOL) {
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
            if (!TextUtils.isEmpty(outer) && !TextUtils.isEmpty(inner)) {
                hasManualPeer = true;
            }
        }

        if (!hasManualPeer) {
            int scan = getScanMode();
            if (scan == SCAN_TURBO) {
                args.add("--turbo");
            } else if (scan == SCAN_BALANCED) {
                args.add("--balanced");
            } else if (scan == SCAN_THOROUGH) {
                args.add("--thorough");
            } else if (scan == SCAN_STEALTH) {
                args.add("--stealth");
            } else if (scan == SCAN_IRONCLAD) {
                args.add("--ironclad");
            }
        }

        int ip = getIpVersion();
        if (ip == IP_V4) {
            args.add("-4");
        } else if (ip == IP_V6) {
            args.add("-6");
        } else if (ip == IP_DUAL) {
            args.add("--dual");
        }

        args.add(isQuickReconnect() ? "--quick-reconnect" : "--no-quick-reconnect");

        if (protocol == PROTOCOL_MASQUE) {
            int noize = getNoizeMasque();
            if (noize == NOIZE_MASQUE_FIREWALL) {
                args.add("--noize");
                args.add("firewall");
            } else if (noize == NOIZE_MASQUE_GFW) {
                args.add("--noize");
                args.add("gfw");
            } else if (noize == NOIZE_MASQUE_OFF) {
                args.add("--noize");
                args.add("off");
            }

            if (getMasqueCarrier() == CARRIER_H2) {
                args.add("--h2");
                if (isMasqueFragment()) {
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
                }
            }
        } else {
            int noize = getNoizeWg();
            if (noize == NOIZE_WG_BALANCED) {
                args.add("--noize");
                args.add("balanced");
            } else if (noize == NOIZE_WG_AGGRESSIVE) {
                args.add("--noize");
                args.add("aggressive");
            } else if (noize == NOIZE_WG_LIGHT) {
                args.add("--noize");
                args.add("light");
            } else if (noize == NOIZE_WG_OFF) {
                args.add("--noize");
                args.add("off");
            }

            int keepalive = getKeepalive();
            if (keepalive > 0) {
                args.add("--keepalive");
                args.add(String.valueOf(keepalive));
            }
        }

        String dns = getDns();
        if (!TextUtils.isEmpty(dns)) {
            args.add("--dns");
            args.add(dns);
        }

        return args;
    }

    public static Map<String, String> toEnv() {
        Map<String, String> env = new HashMap<>();
        if (getProtocol() == PROTOCOL_MASQUE && getMasqueCarrier() == CARRIER_H2) {
            env.put("AETHER_MASQUE_HTTP2", "1");
            if (isMasqueFragment()) {
                env.put("AETHER_MASQUE_H2_FRAGMENT", "1");
                String fSize = getMasqueFragmentSize();
                if (!TextUtils.isEmpty(fSize)) {
                    env.put("AETHER_MASQUE_H2_FRAGMENT_SIZE", fSize);
                }
                String fDelay = getMasqueFragmentDelay();
                if (!TextUtils.isEmpty(fDelay)) {
                    env.put("AETHER_MASQUE_H2_FRAGMENT_DELAY", fDelay);
                }
            }
        } else {
            env.put("AETHER_MASQUE_HTTP2", "0");
        }
        return env;
    }

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}

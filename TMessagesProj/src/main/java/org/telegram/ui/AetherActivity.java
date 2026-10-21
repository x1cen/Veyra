package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.OutlineTextContainerView;
import org.telegram.ui.Components.LayoutHelper;
import org.veyra.client.aether.AetherConfig;
import org.veyra.client.aether.AetherController;

import java.util.ArrayList;
import java.util.List;

public class AetherActivity extends VeyraSettingsBaseActivity {

    private final AetherController.StatusListener statusListener = (state, statusText, pingMs) -> {
        AndroidUtilities.runOnUIThread(this::reloadRows);
    };

    @Override
    public boolean onFragmentCreate() {
        AetherController.getInstance().addStatusListener(statusListener);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        AetherController.getInstance().removeStatusListener(statusListener);
        super.onFragmentDestroy();
    }

    @Override
    protected String getScreenTitle() {
        return "Aether";
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();

        // 1. Status Section
        r.add(VeyraSettingsRow.header("Status"));

        boolean isEnabled = AetherConfig.isEnabled();
        AetherController ctrl = AetherController.getInstance();
        int state = ctrl.getState();
        long ping = ctrl.getPing();

        String statusStr;
        if (state == AetherController.STATE_CONNECTED) {
            statusStr = "Connected" + (ping > 0 ? " • " + ping + " ms" : "");
        } else if (state == AetherController.STATE_DISCONNECTED) {
            statusStr = "Disconnected";
        } else if (state == AetherController.STATE_CONNECTING) {
            statusStr = "Verifying tunnel...";
        } else if (state == AetherController.STATE_SCANNING) {
            statusStr = "Scanning gateways...";
        } else if (state == AetherController.STATE_RECONNECTING) {
            statusStr = "Reconnecting...";
        } else {
            statusStr = ctrl.getStatusText();
        }

        r.add(VeyraSettingsRow.toggle(
                "Enable Aether",
                statusStr,
                AetherConfig::isEnabled,
                isChecked -> {
                    if (isChecked) {
                        AetherController.getInstance().start();
                    } else {
                        AetherController.getInstance().stop();
                    }
                    reloadRows();
                },
                true
        ));

        String gw = ctrl.getGateway();
        if (!TextUtils.isEmpty(gw) && state == AetherController.STATE_CONNECTED) {
            r.add(VeyraSettingsRow.detail(
                    "Connected Gateway",
                    () -> gw,
                    true,
                    null
            ));
        }

        r.add(VeyraSettingsRow.detail(
                "SOCKS5 Host",
                AetherConfig::getSocksHost,
                true,
                () -> promptTextInput("SOCKS5 Host", AetherConfig.getSocksHost(), "127.0.0.1", s -> {
                    if (!TextUtils.isEmpty(s)) {
                        AetherConfig.setSocksHost(s);
                    }
                })
        ));

        r.add(VeyraSettingsRow.detail(
                "SOCKS5 Port",
                () -> String.valueOf(AetherConfig.getSocksPort()),
                true,
                () -> promptTextInput("SOCKS5 Port", String.valueOf(AetherConfig.getSocksPort()), "1819", s -> {
                    try {
                        int p = Integer.parseInt(s);
                        if (p > 0 && p < 65536) {
                            AetherConfig.setSocksPort(p);
                        }
                    } catch (Exception ignore) {}
                })
        ));

        r.add(VeyraSettingsRow.detail(
                "Core Engine Logs",
                () -> "Live Terminal",
                false,
                () -> presentFragment(new AetherLogsActivity())
        ));

        r.add(VeyraSettingsRow.shadow());

        // 2. Network Backend & Egress
        r.add(VeyraSettingsRow.header("Backend & Egress"));
        int backend = AetherConfig.getBackend();
        String backendStr;
        if (backend == AetherConfig.BACKEND_AETHER_PSIPHON) {
            backendStr = "Aether → Psiphon";
        } else if (backend == AetherConfig.BACKEND_TOR) {
            backendStr = "Tor";
        } else if (backend == AetherConfig.BACKEND_AETHER_TOR) {
            backendStr = "Aether → Tor";
        } else if (backend == AetherConfig.BACKEND_TOR_PSIPHON) {
            backendStr = "Tor → Psiphon";
        } else if (backend == AetherConfig.BACKEND_TOR_AETHER) {
            backendStr = "Tor → Aether";
        } else {
            backendStr = "Aether (WARP)";
        }

        r.add(VeyraSettingsRow.detail(
                "Backend",
                () -> backendStr,
                AetherConfig.isPsiphonBackend(),
                this::showBackendPicker
        ));

        // Smart conditional: Exit Country ONLY shown when Psiphon is in the backend chain!
        if (AetherConfig.isPsiphonBackend()) {
            String exit = AetherConfig.getExitCountry();
            String exitDisplay = exit.isEmpty() ? "Auto" : getCountryDisplayName(exit);
            r.add(VeyraSettingsRow.detail(
                    "Exit Country",
                    () -> exitDisplay,
                    false,
                    this::showExitCountryPicker
            ));
        }

        r.add(VeyraSettingsRow.shadow());

        // 3. Protocol Selection & Obfuscation
        r.add(VeyraSettingsRow.header("Protocol & Obfuscation"));
        int proto = AetherConfig.getProtocol();
        String protoStr;
        if (proto == AetherConfig.PROTOCOL_GOOL) {
            protoStr = "WARP×2 (Gool)";
        } else if (proto == AetherConfig.PROTOCOL_WIREGUARD) {
            protoStr = "WireGuard";
        } else if (proto == AetherConfig.PROTOCOL_MIM) {
            protoStr = "MASQUE×2 (MIM)";
        } else {
            protoStr = "MASQUE";
        }

        r.add(VeyraSettingsRow.detail(
                "Transport Protocol",
                () -> protoStr,
                true,
                this::showProtocolPicker
        ));

        if (proto == AetherConfig.PROTOCOL_GOOL) {
            int goolMode = AetherConfig.getGoolMode();
            String goolModeStr = (goolMode == AetherConfig.GOOL_CLASSIC) ? "Classic (WireGuard in WireGuard)" : "Modern (WireGuard in MASQUE)";
            r.add(VeyraSettingsRow.detail(
                    "WARP×2 Mode",
                    () -> goolModeStr,
                    true,
                    this::showGoolModePicker
            ));

            if (goolMode == AetherConfig.GOOL_MODERN) {
                int carrier = AetherConfig.getMasqueCarrier();
                String carrierStr = (carrier == AetherConfig.CARRIER_H2) ? "HTTP/2 (TCP 443)" : "HTTP/3 (QUIC / UDP)";
                r.add(VeyraSettingsRow.detail(
                        "Carrier Transport",
                        () -> carrierStr,
                        true,
                        this::showCarrierPicker
                ));

                if (carrier == AetherConfig.CARRIER_H2) {
                    r.add(VeyraSettingsRow.toggle(
                            "TLS Fragmentation",
                            "Split ClientHello across TCP frames",
                            AetherConfig::isMasqueFragment,
                            isChecked -> {
                                AetherConfig.setMasqueFragment(isChecked);
                                reloadRows();
                            },
                            AetherConfig.isMasqueFragment()
                    ));

                    if (AetherConfig.isMasqueFragment()) {
                        String fragSize = AetherConfig.getMasqueFragmentSize();
                        r.add(VeyraSettingsRow.detail(
                                "Fragment Size",
                                () -> fragSize,
                                true,
                                () -> promptTextInput("Fragment Size", fragSize, "e.g. 8-24", AetherConfig::setMasqueFragmentSize)
                        ));

                        String fragDelay = AetherConfig.getMasqueFragmentDelay();
                        r.add(VeyraSettingsRow.detail(
                                "Fragment Delay (ms)",
                                () -> fragDelay,
                                true,
                                () -> promptTextInput("Fragment Delay", fragDelay, "e.g. 5-15", AetherConfig::setMasqueFragmentDelay)
                        ));
                    }
                }
            }

            int noize = AetherConfig.getNoizeWg();
            String noizeStr = (noize == AetherConfig.NOIZE_WG_AGGRESSIVE) ? "Aggressive" :
                    (noize == AetherConfig.NOIZE_WG_BALANCED ? "Balanced" :
                            (noize == AetherConfig.NOIZE_WG_LIGHT ? "Light" : "Off"));
            r.add(VeyraSettingsRow.detail(
                    "Noise Obfuscation",
                    () -> noizeStr,
                    true,
                    this::showWgNoisePicker
            ));

            int keepalive = AetherConfig.getKeepalive();
            String keepStr = keepalive > 0 ? keepalive + "s" : "Off";
            r.add(VeyraSettingsRow.detail(
                    "Persistent Keepalive",
                    () -> keepStr,
                    false,
                    () -> promptTextInput("Keepalive (seconds)", String.valueOf(keepalive), "0 to disable", s -> {
                        try {
                            AetherConfig.setKeepalive(Integer.parseInt(s));
                        } catch (Exception ignore) {}
                    })
            ));
        } else if (proto == AetherConfig.PROTOCOL_MASQUE || proto == AetherConfig.PROTOCOL_MIM) {
            int carrier = AetherConfig.getMasqueCarrier();
            String carrierStr = (carrier == AetherConfig.CARRIER_H2) ? "HTTP/2 (TCP 443)" : "HTTP/3 (QUIC / UDP)";
            r.add(VeyraSettingsRow.detail(
                    "Carrier Transport",
                    () -> carrierStr,
                    true,
                    this::showCarrierPicker
            ));

            if (carrier == AetherConfig.CARRIER_H2) {
                r.add(VeyraSettingsRow.toggle(
                        "TLS Fragmentation",
                        "Split ClientHello across TCP frames",
                        AetherConfig::isMasqueFragment,
                        isChecked -> {
                            AetherConfig.setMasqueFragment(isChecked);
                            reloadRows();
                        },
                        AetherConfig.isMasqueFragment()
                ));

                if (AetherConfig.isMasqueFragment()) {
                    String fragSize = AetherConfig.getMasqueFragmentSize();
                    r.add(VeyraSettingsRow.detail(
                            "Fragment Size",
                            () -> fragSize,
                            true,
                            () -> promptTextInput("Fragment Size", fragSize, "e.g. 8-24", AetherConfig::setMasqueFragmentSize)
                    ));

                    String fragDelay = AetherConfig.getMasqueFragmentDelay();
                    r.add(VeyraSettingsRow.detail(
                            "Fragment Delay (ms)",
                            () -> fragDelay,
                            true,
                            () -> promptTextInput("Fragment Delay", fragDelay, "e.g. 5-15", AetherConfig::setMasqueFragmentDelay)
                    ));
                }
            }

            int noize = AetherConfig.getNoizeMasque();
            String noizeStr = (noize == AetherConfig.NOIZE_MASQUE_FIREWALL) ? "Firewall" :
                    (noize == AetherConfig.NOIZE_MASQUE_GFW ? "GFW" : "Off");
            r.add(VeyraSettingsRow.detail(
                    "Noise Obfuscation",
                    () -> noizeStr,
                    false,
                    this::showMasqueNoisePicker
            ));
        } else {
            int noize = AetherConfig.getNoizeWg();
            String noizeStr = (noize == AetherConfig.NOIZE_WG_AGGRESSIVE) ? "Aggressive" :
                    (noize == AetherConfig.NOIZE_WG_BALANCED ? "Balanced" :
                            (noize == AetherConfig.NOIZE_WG_LIGHT ? "Light" : "Off"));
            r.add(VeyraSettingsRow.detail(
                    "Noise Obfuscation",
                    () -> noizeStr,
                    true,
                    this::showWgNoisePicker
            ));

            int keepalive = AetherConfig.getKeepalive();
            String keepStr = keepalive > 0 ? keepalive + "s" : "Off";
            r.add(VeyraSettingsRow.detail(
                    "Persistent Keepalive",
                    () -> keepStr,
                    false,
                    () -> promptTextInput("Keepalive (seconds)", String.valueOf(keepalive), "0 to disable", s -> {
                        try {
                            AetherConfig.setKeepalive(Integer.parseInt(s));
                        } catch (Exception ignore) {}
                    })
            ));
        }

        r.add(VeyraSettingsRow.shadow());

        // 4. Routing & Privacy
        r.add(VeyraSettingsRow.header("Routing & Privacy"));

        r.add(VeyraSettingsRow.toggle(
                "Tunnel All In-App Traffic",
                "Route in-app browser, YouTube & media through Aether",
                AetherConfig::isTunnelAllTraffic,
                isChecked -> {
                    AetherConfig.setTunnelAllTraffic(isChecked);
                    org.veyra.client.aether.AetherInAppTunnelManager.update();
                    reloadRows();
                },
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                "Encrypted Client Hello",
                "Hide destination name (SNI) from DPI",
                AetherConfig::isEch,
                isChecked -> {
                    AetherConfig.setEch(isChecked);
                    reloadRows();
                },
                true
        ));

        if (AetherConfig.isEch()) {
            String echCfg = AetherConfig.getEchConfig();
            r.add(VeyraSettingsRow.detail(
                    "ECH Config (base64)",
                    () -> TextUtils.isEmpty(echCfg) ? "Auto (fetch)" : "Custom",
                    true,
                    () -> promptTextInput("ECH Config (base64)", echCfg, "Leave empty for auto", AetherConfig::setEchConfig)
            ));
        }

        r.add(VeyraSettingsRow.toggle(
                "Direct Iranian Sites",
                "Bypass Iran domestic LAN & sites",
                AetherConfig::isBypassIran,
                isChecked -> {
                    AetherConfig.setBypassIran(isChecked);
                    org.veyra.client.aether.AetherInAppTunnelManager.update();
                    reloadRows();
                },
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                "Block Ads & Trackers",
                "In-tunnel ad-blocking rules",
                AetherConfig::isBlockAds,
                isChecked -> {
                    AetherConfig.setBlockAds(isChecked);
                    reloadRows();
                },
                true
        ));

        String dns = AetherConfig.getDns();
        r.add(VeyraSettingsRow.detail(
                "Tunnel DNS",
                () -> dns,
                false,
                () -> promptTextInput("Tunnel DNS Servers", dns, "1.1.1.1,1.0.0.1", AetherConfig::setDns)
        ));

        r.add(VeyraSettingsRow.shadow());

        // 5. Scanner & Network
        r.add(VeyraSettingsRow.header("Scanner & Network"));

        int scan = AetherConfig.getScanMode();
        String scanStr = (scan == AetherConfig.SCAN_TURBO) ? "Turbo" :
                (scan == AetherConfig.SCAN_THOROUGH ? "Thorough" :
                        (scan == AetherConfig.SCAN_STEALTH ? "Stealth" :
                                (scan == AetherConfig.SCAN_IRONCLAD ? "Ironclad" : "Balanced")));
        r.add(VeyraSettingsRow.detail(
                "Scan Mode",
                () -> scanStr,
                true,
                this::showScanModePicker
        ));

        int ip = AetherConfig.getIpVersion();
        String ipStr = (ip == AetherConfig.IP_V6) ? "IPv6" : (ip == AetherConfig.IP_DUAL ? "Dual-stack" : "IPv4");
        r.add(VeyraSettingsRow.detail(
                "IP Version",
                () -> ipStr,
                true,
                this::showIpPicker
        ));

        r.add(VeyraSettingsRow.toggle(
                "Quick Reconnect",
                "Reuse last healthy IP endpoint",
                AetherConfig::isQuickReconnect,
                isChecked -> {
                    AetherConfig.setQuickReconnect(isChecked);
                    reloadRows();
                },
                true
        ));

        if (AetherConfig.isQuickReconnect()) {
            r.add(VeyraSettingsRow.toggle(
                    "Require Fast Endpoint",
                    "Skip slow cached endpoints; rescan instead",
                    AetherConfig::isFastEndpointOnly,
                    isChecked -> {
                        AetherConfig.setFastEndpointOnly(isChecked);
                        reloadRows();
                    },
                    true
            ));
        }

        r.add(VeyraSettingsRow.shadow());

        // 7. Tor Boost / Bridges (visible when Tor is involved)
        boolean isTorBackend = (backend == AetherConfig.BACKEND_TOR || backend == AetherConfig.BACKEND_AETHER_TOR
                || backend == AetherConfig.BACKEND_TOR_AETHER || backend == AetherConfig.BACKEND_TOR_PSIPHON);
        if (isTorBackend) {
            r.add(VeyraSettingsRow.header("Tor Options"));
            r.add(VeyraSettingsRow.detail(
                    "Bridge Mode",
                    () -> {
                        int b = AetherConfig.getTorBridges();
                        return b == 1 ? "Always use bridges" : (b == 2 ? "Never use bridges" : "Auto");
                    },
                    true,
                    this::showTorBridgesPicker
            ));
            String tc = AetherConfig.getTorCountry().trim();
            r.add(VeyraSettingsRow.detail(
                    "Bridge Country",
                    () -> TextUtils.isEmpty(tc) ? "Auto-detect" : tc.toLowerCase(),
                    true,
                    () -> promptTextInput("Bridge Country (2-letter)", tc, "e.g. ir", s -> AetherConfig.setTorCountry(s.trim()))
            ));
            String tcheck = AetherConfig.getTorCheck().trim();
            r.add(VeyraSettingsRow.detail(
                    "Reachability Check",
                    () -> TextUtils.isEmpty(tcheck) ? "check.torproject.org:443" : tcheck,
                    true,
                    () -> promptTextInput("Reachability Check", tcheck, "check.torproject.org:443", AetherConfig::setTorCheck)
            ));
            r.add(VeyraSettingsRow.detail(
                    "Bootstrap Patience",
                    () -> {
                        int v = AetherConfig.getTorDirectSecs();
                        return v > 0 ? v + "s" : "Auto (75s)";
                    },
                    true,
                    this::showTorDirectSecsPicker
            ));
            String bl = AetherConfig.getTorBridgeLines().trim();
            r.add(VeyraSettingsRow.detail(
                    "Custom Bridges",
                    () -> TextUtils.isEmpty(bl) ? "None" : (bl.split("\\n").length + " line(s)"),
                    false,
                    () -> promptTextInput("Custom Bridge Lines", bl, "obfs4 ...", AetherConfig::setTorBridgeLines)
            ));
            r.add(VeyraSettingsRow.shadow());
        }

        // 8. Manual Peers & Maintenance
        r.add(VeyraSettingsRow.header("Endpoints & Maintenance"));

        if (proto == AetherConfig.PROTOCOL_GOOL) {
            String outer = AetherConfig.getWiwOuter();
            String inner = AetherConfig.getWiwInner();
            r.add(VeyraSettingsRow.detail(
                    "Outer Peer",
                    () -> TextUtils.isEmpty(outer) ? "Auto" : outer,
                    true,
                    () -> promptTextInput("Outer Peer", outer, "IP:Port", AetherConfig::setWiwOuter)
            ));
            r.add(VeyraSettingsRow.detail(
                    "Inner Peer",
                    () -> TextUtils.isEmpty(inner) ? "Auto" : inner,
                    true,
                    () -> promptTextInput("Inner Peer", inner, "IP:Port", AetherConfig::setWiwInner)
            ));
        } else {
            String peer = AetherConfig.getPeer();
            r.add(VeyraSettingsRow.detail(
                    "Manual Peer",
                    () -> TextUtils.isEmpty(peer) ? "Auto" : peer,
                    true,
                    () -> promptTextInput("Manual Peer", peer, "IP:Port", AetherConfig::setPeer)
            ));
        }

        return r;
    }

    private void showBackendPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Select Backend");
        String[] options = new String[]{
                "Aether (WARP)",
                "Aether → Psiphon",
                "Tor",
                "Aether → Tor",
                "Tor → Psiphon",
                "Tor → Aether"
        };
        builder.setItems(options, (d, which) -> {
            AetherConfig.setBackend(which);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showExitCountryPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Exit Country");
        String[] codes = new String[]{"", "GB", "DE", "US", "NL", "CA", "FR", "TR", "JP", "SG"};
        String[] options = new String[]{
                "Auto (Best)",
                "🇬🇧 United Kingdom",
                "🇩🇪 Germany",
                "🇺🇸 United States",
                "🇳🇱 Netherlands",
                "🇨🇦 Canada",
                "🇫🇷 France",
                "🇹🇷 Turkey",
                "🇯🇵 Japan",
                "🇸🇬 Singapore"
        };
        builder.setItems(options, (d, which) -> {
            AetherConfig.setExitCountry(codes[which]);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private String getCountryDisplayName(String code) {
        if ("GB".equalsIgnoreCase(code)) return "🇬🇧 UK";
        if ("DE".equalsIgnoreCase(code)) return "🇩🇪 DE";
        if ("US".equalsIgnoreCase(code)) return "🇺🇸 US";
        if ("NL".equalsIgnoreCase(code)) return "🇳🇱 NL";
        if ("CA".equalsIgnoreCase(code)) return "🇨🇦 CA";
        if ("FR".equalsIgnoreCase(code)) return "🇫🇷 FR";
        if ("TR".equalsIgnoreCase(code)) return "🇹🇷 TR";
        if ("JP".equalsIgnoreCase(code)) return "🇯🇵 JP";
        if ("SG".equalsIgnoreCase(code)) return "🇸🇬 SG";
        return code;
    }

    private void showProtocolPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Transport Protocol");
        String[] options = new String[]{
                "WARP×2 (Gool) — Recommended",
                "MASQUE (HTTP/2 & HTTP/3)",
                "WireGuard",
                "MASQUE×2 (MIM)"
        };
        int[] protos = new int[]{
                AetherConfig.PROTOCOL_GOOL,
                AetherConfig.PROTOCOL_MASQUE,
                AetherConfig.PROTOCOL_WIREGUARD,
                AetherConfig.PROTOCOL_MIM
        };
        builder.setItems(options, (d, which) -> {
            AetherConfig.setProtocol(protos[which]);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showCarrierPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Carrier Transport");
        String[] options = new String[]{
                "HTTP/2 (TCP 443) — Recommended for Iran",
                "HTTP/3 (QUIC / UDP)"
        };
        builder.setItems(options, (d, which) -> {
            AetherConfig.setMasqueCarrier(which);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showMasqueNoisePicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("MASQUE Noise");
        String[] options = new String[]{"Firewall (Recommended)", "GFW", "Off"};
        builder.setItems(options, (d, which) -> {
            AetherConfig.setNoizeMasque(which);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showWgNoisePicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("WireGuard Noise");
        String[] options = new String[]{"Aggressive (Recommended)", "Balanced", "Light", "Off"};
        builder.setItems(options, (d, which) -> {
            AetherConfig.setNoizeWg(which);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showScanModePicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Scan Mode");
        String[] options = new String[]{"Balanced", "Turbo", "Thorough", "Stealth", "Ironclad"};
        builder.setItems(options, (d, which) -> {
            AetherConfig.setScanMode(which);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showIpPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("IP Version");
        String[] options = new String[]{"IPv4", "IPv6", "Dual-stack"};
        builder.setItems(options, (d, which) -> {
            AetherConfig.setIpVersion(which);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showGoolModePicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Gool Mode");
        String[] options = new String[]{"Modern (WireGuard over MASQUE)", "Classic (WireGuard over WireGuard)"};
        builder.setItems(options, (d, which) -> {
            AetherConfig.setGoolMode(which);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showTorBridgesPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Bridge Mode");
        String[] options = new String[]{"Auto", "Always use bridges", "Never use bridges"};
        builder.setItems(options, (d, which) -> {
            AetherConfig.setTorBridges(which);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showTorDirectSecsPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Bootstrap Patience");
        int[] secs = new int[]{0, 20, 45, 75, 120, 240};
        String[] options = new String[]{"Auto (75s)", "20s", "45s", "75s", "120s", "240s"};
        builder.setItems(options, (d, which) -> {
            AetherConfig.setTorDirectSecs(secs[which]);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void promptTextInput(String title, String currentVal, String hint, OnTextEntered callback) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);

        // Match global Veyra input style: OutlineTextContainerView (rounded outline, same as Custom Header)
        OutlineTextContainerView outlineView = new OutlineTextContainerView(getParentActivity());
        outlineView.setText(hint != null ? hint : title);

        final EditTextBoldCursor editText = new EditTextBoldCursor(getParentActivity());
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        editText.setText(currentVal != null ? currentVal : "");
        editText.setSelection(editText.getText().length());
        editText.setHint(hint != null ? hint : "Auto");
        editText.setHintColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setCursorWidth(1.5f);
        editText.setBackground(null);
        editText.setSingleLine(true);
        editText.setMaxLines(1);
        editText.setLines(1);
        editText.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        editText.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
        editText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        outlineView.attachEditText(editText);
        outlineView.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        boolean hasInitialText = !TextUtils.isEmpty(currentVal);
        outlineView.animateSelection(false, hasInitialText, false);
        editText.setOnFocusChangeListener((v, hasFocus) -> outlineView.animateSelection(hasFocus, hasFocus || editText.length() > 0));
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { outlineView.animateSelection(editText.hasFocus(), editText.hasFocus() || s.length() > 0); }
        });

        FrameLayout frame = new FrameLayout(getParentActivity());
        frame.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(12));
        frame.addView(outlineView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 58));
        builder.setView(frame);

        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (d, which) -> {
            callback.onEntered(editText.getText().toString().trim());
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        showDialog(dialog);
        // Auto-focus and show keyboard
        editText.requestFocus();
        AndroidUtilities.runOnUIThread(() -> {
            try { AndroidUtilities.showKeyboard(editText); } catch (Throwable ignore) {}
        }, 150);
    }

    private void showLogsDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Engine Logs");

        List<String> logs = AetherController.getInstance().getLogs();
        StringBuilder sb = new StringBuilder();
        for (String line : logs) {
            sb.append(line).append("\n");
        }
        if (sb.length() == 0) {
            sb.append("No logs captured yet.");
        }

        ScrollView scrollView = new ScrollView(getParentActivity());
        TextView textView = new TextView(getParentActivity());
        textView.setText(sb.toString());
        textView.setTextSize(12);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        scrollView.addView(textView);

        builder.setView(scrollView);
        builder.setPositiveButton(LocaleController.getString("Close", R.string.Close), null);
        showDialog(builder.create());
    }

    private interface OnTextEntered {
        void onEntered(String text);
    }
}

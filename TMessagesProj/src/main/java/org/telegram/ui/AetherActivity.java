package org.telegram.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.veyra.client.aether.AetherConfig;
import org.veyra.client.aether.AetherController;
import org.veyra.client.aether.AetherDownloader;

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
    protected String getTitle() {
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
                isEnabled,
                (view, isChecked) -> {
                    if (isChecked) {
                        AetherController.getInstance().start();
                    } else {
                        AetherController.getInstance().stop();
                    }
                    reloadRows();
                }
        ));

        r.add(VeyraSettingsRow.detail(
                "Core Engine Logs",
                () -> "View output",
                false,
                this::showLogsDialog
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
                true,
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

        // 3. Protocol Selection
        r.add(VeyraSettingsRow.header("Protocol"));
        int proto = AetherConfig.getProtocol();
        String protoStr;
        if (proto == AetherConfig.PROTOCOL_AUTO) {
            protoStr = "Smart Auto";
        } else if (proto == AetherConfig.PROTOCOL_WIREGUARD) {
            protoStr = "WireGuard";
        } else if (proto == AetherConfig.PROTOCOL_GOOL) {
            protoStr = "Gool (WARP+WARP)";
        } else if (proto == AetherConfig.PROTOCOL_MIM) {
            protoStr = "MIM";
        } else {
            protoStr = "MASQUE";
        }

        r.add(VeyraSettingsRow.detail(
                "Active Protocol",
                () -> protoStr,
                false,
                this::showProtocolPicker
        ));

        if (proto == AetherConfig.PROTOCOL_MASQUE || proto == AetherConfig.PROTOCOL_AUTO || proto == AetherConfig.PROTOCOL_MIM) {
            int carrier = AetherConfig.getMasqueCarrier();
            String carrierStr = (carrier == AetherConfig.CARRIER_H2) ? "HTTP/2 (TCP)" : "HTTP/3 (QUIC)";
            r.add(VeyraSettingsRow.detail(
                    "Carrier",
                    () -> carrierStr,
                    false,
                    this::showCarrierPicker
            ));

            if (carrier == AetherConfig.CARRIER_H2) {
                r.add(VeyraSettingsRow.toggle(
                        "TLS Fragmentation",
                        "Split ClientHello",
                        AetherConfig.isMasqueFragment(),
                        (view, isChecked) -> {
                            AetherConfig.setMasqueFragment(isChecked);
                            reloadRows();
                        }
                ));
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
            String noizeStr = (noize == AetherConfig.NOIZE_WG_BALANCED) ? "Balanced" :
                    (noize == AetherConfig.NOIZE_WG_AGGRESSIVE ? "Aggressive" :
                            (noize == AetherConfig.NOIZE_WG_LIGHT ? "Light" : "Off"));
            r.add(VeyraSettingsRow.detail(
                    "Noise Obfuscation",
                    () -> noizeStr,
                    false,
                    this::showWgNoisePicker
            ));
        }

        r.add(VeyraSettingsRow.shadow());

        // 4. Routing & Privacy
        r.add(VeyraSettingsRow.header("Routing & Privacy"));

        r.add(VeyraSettingsRow.toggle(
                "Encrypted Client Hello",
                "Hide SNI via ECH",
                AetherConfig.isEch(),
                (view, isChecked) -> {
                    AetherConfig.setEch(isChecked);
                    reloadRows();
                }
        ));

        r.add(VeyraSettingsRow.toggle(
                "Direct Iranian Sites",
                "Bypass Iran LAN & sites",
                AetherConfig.isBypassIran(),
                (view, isChecked) -> {
                    AetherConfig.setBypassIran(isChecked);
                    reloadRows();
                }
        ));

        r.add(VeyraSettingsRow.toggle(
                "Block Ads & Trackers",
                "In-tunnel ad-block",
                AetherConfig.isBlockAds(),
                (view, isChecked) -> {
                    AetherConfig.setBlockAds(isChecked);
                    reloadRows();
                }
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
                false,
                this::showScanModePicker
        ));

        int ip = AetherConfig.getIpVersion();
        String ipStr = (ip == AetherConfig.IP_V6) ? "IPv6" : (ip == AetherConfig.IP_DUAL ? "Dual-stack" : "IPv4");
        r.add(VeyraSettingsRow.detail(
                "IP Version",
                () -> ipStr,
                false,
                this::showIpPicker
        ));

        r.add(VeyraSettingsRow.toggle(
                "Quick Reconnect",
                "Reuse last healthy IP",
                AetherConfig.isQuickReconnect(),
                (view, isChecked) -> {
                    AetherConfig.setQuickReconnect(isChecked);
                    reloadRows();
                }
        ));

        r.add(VeyraSettingsRow.shadow());

        // 6. Manual Peers & Maintenance
        r.add(VeyraSettingsRow.header("Endpoints & Maintenance"));

        if (proto == AetherConfig.PROTOCOL_GOOL) {
            String outer = AetherConfig.getWiwOuter();
            String inner = AetherConfig.getWiwInner();
            r.add(VeyraSettingsRow.detail(
                    "Outer Peer",
                    () -> TextUtils.isEmpty(outer) ? "Auto" : outer,
                    false,
                    () -> promptTextInput("Outer Peer", outer, AetherConfig::setWiwOuter)
            ));
            r.add(VeyraSettingsRow.detail(
                    "Inner Peer",
                    () -> TextUtils.isEmpty(inner) ? "Auto" : inner,
                    false,
                    () -> promptTextInput("Inner Peer", inner, AetherConfig::setWiwInner)
            ));
        } else {
            String peer = AetherConfig.getPeer();
            r.add(VeyraSettingsRow.detail(
                    "Manual Peer",
                    () -> TextUtils.isEmpty(peer) ? "Auto" : peer,
                    false,
                    () -> promptTextInput("Manual Peer (IP:Port)", peer, AetherConfig::setPeer)
            ));
        }

        r.add(VeyraSettingsRow.detail(
                "Reinstall Engine",
                () -> "Download fresh binary",
                false,
                this::redownloadCore
        ));

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
        builder.setNegativeButton("Cancel", null);
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
        builder.setNegativeButton("Cancel", null);
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
        builder.setTitle("Select Protocol");
        String[] options = new String[]{
                "Smart Auto (Recommended)",
                "MASQUE",
                "WireGuard",
                "Gool (WARP+WARP)",
                "MIM (MASQUE+MASQUE)"
        };
        builder.setItems(options, (d, which) -> {
            AetherConfig.setProtocol(which);
            reloadRows();
        });
        builder.setNegativeButton("Cancel", null);
        showDialog(builder.create());
    }

    private void showCarrierPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Carrier Transport");
        String[] options = new String[]{
                "HTTP/3 (QUIC / UDP)",
                "HTTP/2 (TCP 443)"
        };
        builder.setItems(options, (d, which) -> {
            AetherConfig.setMasqueCarrier(which);
            reloadRows();
        });
        builder.setNegativeButton("Cancel", null);
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
        builder.setNegativeButton("Cancel", null);
        showDialog(builder.create());
    }

    private void showWgNoisePicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("WireGuard Noise");
        String[] options = new String[]{"Balanced", "Aggressive", "Light", "Off"};
        builder.setItems(options, (d, which) -> {
            AetherConfig.setNoizeWg(which);
            reloadRows();
        });
        builder.setNegativeButton("Cancel", null);
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
        builder.setNegativeButton("Cancel", null);
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
        builder.setNegativeButton("Cancel", null);
        showDialog(builder.create());
    }

    private void promptTextInput(String title, String currentVal, OnTextEntered callback) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);
        final org.telegram.ui.Components.EditTextBoldCursor editText = new org.telegram.ui.Components.EditTextBoldCursor(getParentActivity());
        editText.setTextSize(16);
        editText.setText(currentVal != null ? currentVal : "");
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHint("Auto");
        editText.setHintColor(Theme.getColor(Theme.key_dialogTextHint));

        FrameLayout frame = new FrameLayout(getParentActivity());
        frame.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));
        frame.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(frame);

        builder.setPositiveButton("Save", (d, which) -> {
            callback.onEntered(editText.getText().toString().trim());
            reloadRows();
        });
        builder.setNegativeButton("Cancel", null);
        showDialog(builder.create());
    }

    private void showLogsDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Engine Logs");

        List<String> logs = AetherController.getInstance().getRecentLogs();
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
        textView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        scrollView.addView(textView);

        builder.setView(scrollView);
        builder.setPositiveButton("Close", null);
        showDialog(builder.create());
    }

    private void redownloadCore() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Redownload Core");
        builder.setMessage("Fetch the latest release binary from GitHub?");
        builder.setPositiveButton("Download", (d, which) -> {
            BulletinFactory.of(AetherActivity.this).createSimpleBulletin(R.raw.chats_infotip, "Downloading core...").show();
            AetherDownloader.downloadCore(new AetherDownloader.DownloadCallback() {
                @Override
                public void onProgress(int percent) {}

                @Override
                public void onComplete(java.io.File file) {
                    AndroidUtilities.runOnUIThread(() -> {
                        BulletinFactory.of(AetherActivity.this).createSimpleBulletin(R.raw.download_finish, "Core downloaded").show();
                    });
                }

                @Override
                public void onError(Exception error) {
                    AndroidUtilities.runOnUIThread(() -> {
                        BulletinFactory.of(AetherActivity.this).createSimpleBulletin(R.raw.error, "Failed: " + error.getMessage()).show();
                    });
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        showDialog(builder.create());
    }

    private interface OnTextEntered {
        void onEntered(String text);
    }
}

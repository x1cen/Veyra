package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.veyra.client.aether.AetherConfig;
import org.veyra.client.aether.AetherController;
import org.veyra.client.aether.AetherDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AetherActivity extends VeyraSettingsBaseActivity implements AetherController.StatusListener {

    @Override
    protected String getScreenTitle() {
        return "Aether";
    }

    @Override
    public boolean onFragmentCreate() {
        AetherController.getInstance().addStatusListener(this);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        AetherController.getInstance().removeStatusListener(this);
    }

    @Override
    public void onStateChanged(int state, String statusText, long pingMs) {
        AndroidUtilities.runOnUIThread(this::reloadRows);
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();
        AetherController controller = AetherController.getInstance();

        // 1. Status Section
        r.add(VeyraSettingsRow.header("Aether Tunnel"));
        String statusSub;
        int state = controller.getState();
        if (state == AetherController.STATE_CONNECTED) {
            long ping = controller.getPing();
            String gw = controller.getActiveGateway();
            statusSub = "Connected" + (ping > 0 ? " • " + ping + " ms" : "") + (!TextUtils.isEmpty(gw) ? " • " + gw : "");
        } else if (state == AetherController.STATE_STARTING) {
            statusSub = "Starting engine...";
        } else if (state == AetherController.STATE_DOWNLOADING) {
            statusSub = controller.getStatusText();
        } else if (state == AetherController.STATE_SCANNING) {
            statusSub = "Scanning gateways...";
        } else if (state == AetherController.STATE_CONNECTING) {
            statusSub = "Verifying tunnel...";
        } else if (state == AetherController.STATE_RECONNECTING) {
            statusSub = "Reconnecting / Rescanning...";
        } else if (state == AetherController.STATE_ERROR) {
            statusSub = controller.getStatusText();
        } else {
            statusSub = "Disconnected (Tap switch to connect)";
        }

        r.add(VeyraSettingsRow.toggle(
                "Enable Aether",
                statusSub,
                AetherConfig::isEnabled,
                enabled -> {
                    if (enabled) {
                        controller.start();
                    } else {
                        controller.stop();
                    }
                },
                false,
                this::reloadRows
        ));

        r.add(VeyraSettingsRow.shadow());

        // 2. Protocol Selection
        r.add(VeyraSettingsRow.header("Protocol"));
        int proto = AetherConfig.getProtocol();
        String protoStr = (proto == AetherConfig.PROTOCOL_WIREGUARD) ? "WireGuard" :
                (proto == AetherConfig.PROTOCOL_GOOL ? "Gool (WARP in WARP)" : "MASQUE (HTTP/3 over QUIC)");

        r.add(VeyraSettingsRow.detail(
                "Active Protocol",
                () -> protoStr,
                false,
                this::showProtocolPicker
        ));

        r.add(VeyraSettingsRow.shadow());

        // 3. MASQUE Settings (Smart: only when MASQUE is active)
        if (proto == AetherConfig.PROTOCOL_MASQUE) {
            r.add(VeyraSettingsRow.header("MASQUE Configuration"));
            int carrier = AetherConfig.getMasqueCarrier();
            String carrierStr = (carrier == AetherConfig.CARRIER_H2) ? "HTTP/2 (TCP / TLS)" : "HTTP/3 (QUIC / UDP) [Recommended]";
            r.add(VeyraSettingsRow.detail(
                    "Carrier Transport",
                    () -> carrierStr,
                    carrier == AetherConfig.CARRIER_H2,
                    this::showCarrierPicker
            ));

            if (carrier == AetherConfig.CARRIER_H2) {
                r.add(VeyraSettingsRow.toggle(
                        "TLS ClientHello Fragmentation",
                        "Fragment TLS handshake to bypass SNI blocking",
                        AetherConfig::isMasqueFragment,
                        AetherConfig::setMasqueFragment,
                        true,
                        this::reloadRows
                ));

                if (AetherConfig.isMasqueFragment()) {
                    r.add(VeyraSettingsRow.detail(
                            "Fragment Chunk Size",
                            AetherConfig::getMasqueFragmentSize,
                            true,
                            () -> showTextInputDialog("Fragment Chunk Size", "Enter byte range, e.g. 8-24", AetherConfig.getMasqueFragmentSize(), AetherConfig::setMasqueFragmentSize)
                    ));
                    r.add(VeyraSettingsRow.detail(
                            "Fragment Delay (ms)",
                            AetherConfig::getMasqueFragmentDelay,
                            false,
                            () -> showTextInputDialog("Fragment Delay (ms)", "Enter delay range, e.g. 5-15", AetherConfig.getMasqueFragmentDelay(), AetherConfig::setMasqueFragmentDelay)
                    ));
                }
            }
            r.add(VeyraSettingsRow.shadow());
        }

        // 4. Obfuscation Profile (Noise System)
        r.add(VeyraSettingsRow.header("Obfuscation & Anti-DPI"));
        String noiseStr;
        if (proto == AetherConfig.PROTOCOL_MASQUE) {
            int n = AetherConfig.getNoizeMasque();
            noiseStr = (n == AetherConfig.NOIZE_MASQUE_GFW) ? "GFW (Heavy DPI bypass)" :
                    (n == AetherConfig.NOIZE_MASQUE_OFF ? "Off (No obfuscation)" : "Firewall (Default / Iran)");
        } else {
            int n = AetherConfig.getNoizeWg();
            noiseStr = (n == AetherConfig.NOIZE_WG_AGGRESSIVE) ? "Aggressive (Heavy decoy packets)" :
                    (n == AetherConfig.NOIZE_WG_LIGHT ? "Light (Minimal)" :
                            (n == AetherConfig.NOIZE_WG_OFF ? "Off" : "Balanced (Default / Iran)"));
        }
        r.add(VeyraSettingsRow.detail(
                "Noise Profile",
                () -> noiseStr,
                false,
                this::showNoisePicker
        ));

        r.add(VeyraSettingsRow.shadow());

        // 5. Scanning & Routing
        r.add(VeyraSettingsRow.header("Discovery & Scanning"));
        boolean manualPeerActive = (proto == AetherConfig.PROTOCOL_GOOL) ?
                (!TextUtils.isEmpty(AetherConfig.getWiwOuter()) && !TextUtils.isEmpty(AetherConfig.getWiwInner())) :
                !TextUtils.isEmpty(AetherConfig.getPeer());

        String scanStr;
        if (manualPeerActive) {
            scanStr = "Ignored (Manual peer is pinned)";
        } else {
            int sm = AetherConfig.getScanMode();
            scanStr = (sm == AetherConfig.SCAN_TURBO) ? "Turbo (Fastest, first responding)" :
                    (sm == AetherConfig.SCAN_THOROUGH ? "Thorough (Deep scan, best latency)" :
                            (sm == AetherConfig.SCAN_STEALTH ? "Stealth (Calm and quiet)" :
                                    (sm == AetherConfig.SCAN_IRONCLAD ? "Ironclad (Strict E2E HTTP validation)" : "Balanced (Default)")));
        }

        r.add(VeyraSettingsRow.detail(
                "Scan Mode",
                () -> scanStr,
                true,
                () -> {
                    if (manualPeerActive) {
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, "Scan mode is skipped when a manual peer is set").show();
                    } else {
                        showScanModePicker();
                    }
                }
        ));

        int ipv = AetherConfig.getIpVersion();
        String ipStr = (ipv == AetherConfig.IP_V6) ? "IPv6 only" : (ipv == AetherConfig.IP_DUAL ? "Dual-stack (IPv4 + IPv6)" : "IPv4 only (Default)");
        r.add(VeyraSettingsRow.detail(
                "IP Version",
                () -> ipStr,
                true,
                this::showIpVersionPicker
        ));

        r.add(VeyraSettingsRow.toggle(
                "Quick Reconnect",
                "Reuse last known-good gateway before rescanning",
                AetherConfig::isQuickReconnect,
                AetherConfig::setQuickReconnect,
                false
        ));

        r.add(VeyraSettingsRow.shadow());

        // 6. Custom Endpoints
        r.add(VeyraSettingsRow.header("Custom Endpoints (Optional)"));
        if (proto == AetherConfig.PROTOCOL_MASQUE || proto == AetherConfig.PROTOCOL_WIREGUARD) {
            r.add(VeyraSettingsRow.detail(
                    "Forced Gateway Peer",
                    () -> TextUtils.isEmpty(AetherConfig.getPeer()) ? "Auto-discover (Recommended)" : AetherConfig.getPeer(),
                    proto == AetherConfig.PROTOCOL_WIREGUARD,
                    () -> showTextInputDialog("Forced Peer", "Enter IP:PORT (e.g. 162.159.192.1:2408) or leave empty for auto-scan", AetherConfig.getPeer(), AetherConfig::setPeer)
            ));
            if (proto == AetherConfig.PROTOCOL_WIREGUARD) {
                r.add(VeyraSettingsRow.detail(
                        "Persistent Keepalive",
                        () -> AetherConfig.getKeepalive() > 0 ? AetherConfig.getKeepalive() + "s" : "Default (5s)",
                        false,
                        () -> showTextInputDialog("Keepalive (seconds)", "Enter keepalive seconds (0 for default)", String.valueOf(AetherConfig.getKeepalive()), val -> {
                            try {
                                AetherConfig.setKeepalive(Integer.parseInt(val.trim()));
                            } catch (Exception ignore) {}
                        })
                ));
            }
        } else if (proto == AetherConfig.PROTOCOL_GOOL) {
            r.add(VeyraSettingsRow.detail(
                    "Outer Hop Peer",
                    () -> TextUtils.isEmpty(AetherConfig.getWiwOuter()) ? "Auto-discover" : AetherConfig.getWiwOuter(),
                    true,
                    () -> showTextInputDialog("Outer Hop Peer", "Enter outer IP:PORT (e.g. 162.159.192.1:2408)", AetherConfig.getWiwOuter(), AetherConfig::setWiwOuter)
            ));
            r.add(VeyraSettingsRow.detail(
                    "Inner Hop Peer",
                    () -> TextUtils.isEmpty(AetherConfig.getWiwInner()) ? "Auto-discover" : AetherConfig.getWiwInner(),
                    false,
                    () -> showTextInputDialog("Inner Hop Peer", "Enter inner IP:PORT (e.g. 188.114.96.1:2408)", AetherConfig.getWiwInner(), AetherConfig::setWiwInner)
            ));
        }

        r.add(VeyraSettingsRow.shadow());

        // 7. Network Plumbing
        r.add(VeyraSettingsRow.header("Network Plumbing"));
        r.add(VeyraSettingsRow.detail(
                "Local SOCKS5 Port",
                () -> String.valueOf(AetherConfig.getSocksPort()),
                true,
                () -> showTextInputDialog("SOCKS5 Port", "Enter port number (default 1819)", String.valueOf(AetherConfig.getSocksPort()), val -> {
                    try {
                        int p = Integer.parseInt(val.trim());
                        if (p > 1024 && p <= 65535) {
                            AetherConfig.setSocksPort(p);
                        }
                    } catch (Exception ignore) {}
                })
        ));

        r.add(VeyraSettingsRow.detail(
                "In-Tunnel DNS Resolvers",
                AetherConfig::getDns,
                false,
                () -> showTextInputDialog("DNS Resolvers", "Comma-separated IPs (e.g. 1.1.1.1,1.0.0.1)", AetherConfig.getDns(), AetherConfig::setDns)
        ));

        r.add(VeyraSettingsRow.shadow());

        // 8. Core Management & Diagnostics
        r.add(VeyraSettingsRow.header("Core Management & Logs"));
        File bin = controller.findCoreBinary();
        String coreInfo = bin != null ?
                (bin.getAbsolutePath().contains("libaether.so") ? "Built-in / Native Library" : "Installed Core (" + (bin.length() / 1024 / 1024) + " MB)") :
                "Not Installed (Will download on first run)";

        r.add(VeyraSettingsRow.detail("Core Binary Status", () -> coreInfo, true, null));

        r.add(VeyraSettingsRow.button("Download / Update Aether Core", false, true, this::showDownloadCoreDialog));
        r.add(VeyraSettingsRow.button("View Live Engine Logs", false, false, this::showLogsDialog));

        return r;
    }

    private void showProtocolPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Select Protocol");
        String[] options = new String[]{
                "MASQUE (HTTP/3 over QUIC) — Modern & Recommended",
                "WireGuard — Classic & High-speed UDP",
                "Gool (WARP in WARP) — Two encrypted hops"
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
        builder.setTitle("MASQUE Carrier Transport");
        String[] options = new String[]{
                "HTTP/3 over QUIC/UDP (Default & Fastest)",
                "HTTP/2 over TCP/TLS (TCP 443 — for restricted UDP networks)"
        };
        builder.setItems(options, (d, which) -> {
            AetherConfig.setMasqueCarrier(which);
            reloadRows();
        });
        builder.setNegativeButton("Cancel", null);
        showDialog(builder.create());
    }

    private void showNoisePicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Anti-DPI Noise Profile");
        int proto = AetherConfig.getProtocol();
        if (proto == AetherConfig.PROTOCOL_MASQUE) {
            String[] options = new String[]{
                    "Firewall (Default — Balanced for Iran)",
                    "GFW (Aggressive obfuscation)",
                    "Off (No obfuscation)"
            };
            builder.setItems(options, (d, which) -> {
                AetherConfig.setNoizeMasque(which);
                reloadRows();
            });
        } else {
            String[] options = new String[]{
                    "Balanced (Default — Recommended for Iran)",
                    "Aggressive (Maximum decoy traffic)",
                    "Light (Minimal overhead)",
                    "Off (No obfuscation)"
            };
            builder.setItems(options, (d, which) -> {
                AetherConfig.setNoizeWg(which);
                reloadRows();
            });
        }
        builder.setNegativeButton("Cancel", null);
        showDialog(builder.create());
    }

    private void showScanModePicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Select Scan Mode");
        String[] options = new String[]{
                "Balanced (Default — balanced speed and ping)",
                "Turbo (Fastest — picks first responding edge)",
                "Thorough (Deep sweep — searches for lowest latency)",
                "Stealth (Calm & quiet — low network footprint)",
                "Ironclad (Rigorous — verifies real end-to-end HTTP traffic)"
        };
        builder.setItems(options, (d, which) -> {
            AetherConfig.setScanMode(which);
            reloadRows();
        });
        builder.setNegativeButton("Cancel", null);
        showDialog(builder.create());
    }

    private void showIpVersionPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Select IP Version");
        String[] options = new String[]{"IPv4 only (Default)", "IPv6 only", "Dual-stack (Both)"};
        builder.setItems(options, (d, which) -> {
            AetherConfig.setIpVersion(which);
            reloadRows();
        });
        builder.setNegativeButton("Cancel", null);
        showDialog(builder.create());
    }

    private void showDownloadCoreDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Download Aether Core");
        builder.setMessage("Download official Aether core v2.3.0 (" + AetherDownloader.getTargetAssetFileName() + ") from CluvexStudio GitHub releases?");
        builder.setPositiveButton("Download", (dialog, which) -> {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, "Downloading Aether core...").show();
            AetherDownloader.downloadCore(new AetherDownloader.DownloadListener() {
                @Override
                public void onProgress(int percent, long downloadedBytes, long totalBytes) {}

                @Override
                public void onSuccess(File binaryFile) {
                    AndroidUtilities.runOnUIThread(() -> {
                        BulletinFactory.of(AetherActivity.this).createSimpleBulletin(R.raw.download_finish, "Aether core installed successfully").show();
                        reloadRows();
                    });
                }

                @Override
                public void onError(Exception error) {
                    AndroidUtilities.runOnUIThread(() -> {
                        BulletinFactory.of(AetherActivity.this).createSimpleBulletin(R.raw.error, "Download failed: " + error.getMessage()).show();
                    });
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        showDialog(builder.create());
    }

    private void showLogsDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Live Engine Logs");

        List<String> logs = AetherController.getInstance().getLogs();
        StringBuilder sb = new StringBuilder();
        if (logs.isEmpty()) {
            sb.append("No engine logs recorded yet. Start Aether to see live output.");
        } else {
            for (String line : logs) {
                sb.append(line).append("\n");
            }
        }

        ScrollView scrollView = new ScrollView(getParentActivity());
        TextView textView = new TextView(getParentActivity());
        textView.setTextSize(12);
        textView.setText(sb.toString());
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        scrollView.addView(textView);

        builder.setView(scrollView);
        builder.setPositiveButton("Close", null);
        showDialog(builder.create());
    }

    private void showTextInputDialog(String title, String hint, String initialValue, ValueCallback callback) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);

        final EditTextBoldCursor editText = new EditTextBoldCursor(getParentActivity());
        editText.setTextSize(16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setHint(hint);
        editText.setText(initialValue != null ? initialValue : "");
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setGravity(Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT));

        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), 0);
        container.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(container);

        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            String text = editText.getText().toString().trim();
            if (callback != null) {
                callback.onValue(text);
                reloadRows();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private interface ValueCallback {
        void onValue(String val);
    }
}

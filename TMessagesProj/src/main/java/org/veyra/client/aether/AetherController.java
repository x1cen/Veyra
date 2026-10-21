package org.veyra.client.aether;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.utils.proxy.ProxySettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class AetherController {

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_DOWNLOADING = 2;
    public static final int STATE_STARTING = 3;
    public static final int STATE_SCANNING = 4;
    public static final int STATE_CONNECTING = 5;
    public static final int STATE_CONNECTED = 6;
    public static final int STATE_RECONNECTING = 7;
    public static final int STATE_ERROR = 8;

    public interface StatusListener {
        void onStateChanged(int state, String statusText, long pingMs);
    }

    public interface LogListener {
        void onLog(String line);
    }

    private static volatile AetherController Instance;

    public static AetherController getInstance() {
        AetherController local = Instance;
        if (local == null) {
            synchronized (AetherController.class) {
                local = Instance;
                if (local == null) {
                    Instance = local = new AetherController();
                }
            }
        }
        return local;
    }

    private final List<StatusListener> statusListeners = new CopyOnWriteArrayList<>();
    private final List<LogListener> logListeners = new CopyOnWriteArrayList<>();
    private final LinkedList<String> logBuffer = new LinkedList<>();

    private volatile int currentState = STATE_DISCONNECTED;
    private volatile String currentStatusText = "Disconnected";
    private volatile long currentPing = -1;
    private volatile String activeGateway = "";
    private volatile int activeSocksPort = 0;
    private static final java.util.regex.Pattern SOCKS_PORT_PATTERN = java.util.regex.Pattern.compile("socks5 (?:server listening on|proxy at) 127\\.0\\.0\\.1:(\\d+)");

    private Process process;
    private Thread supervisorThread;
    private volatile boolean shouldRun = false;
    private int consecutiveProbeFailures = 0;

    // Multi-target ping like reference PingMonitor.kt TARGETS (1.1.1.1, 8.8.8.8, 9.9.9.9) port 443
    private static final String[] PING_TARGETS = {"1.1.1.1", "8.8.8.8", "9.9.9.9"};
    private static final int PING_TARGET_PORT = 443;
    private static final int PING_TARGET_PORT_HIGH = 0x01BB; // 443

    private AetherController() {
    }

    public void addStatusListener(StatusListener listener) {
        if (listener != null && !statusListeners.contains(listener)) {
            statusListeners.add(listener);
            listener.onStateChanged(currentState, currentStatusText, currentPing);
        }
    }

    public void removeStatusListener(StatusListener listener) {
        statusListeners.remove(listener);
    }

    public void addLogListener(LogListener listener) {
        if (listener != null && !logListeners.contains(listener)) {
            logListeners.add(listener);
        }
    }

    public void removeLogListener(LogListener listener) {
        logListeners.remove(listener);
    }

    public List<String> getLogs() {
        synchronized (logBuffer) {
            return new ArrayList<>(logBuffer);
        }
    }

    public int getState() {
        return currentState;
    }

    public String getGateway() {
        return activeGateway;
    }

    public int getActivePort() {
        return activeSocksPort > 0 ? activeSocksPort : AetherConfig.getSocksPort();
    }

    public int getLocalSocks5Port() {
        return getActivePort();
    }

    public String getStatusText() {
        return currentStatusText;
    }

    public long getPing() {
        return currentPing;
    }

    public String getActiveGateway() {
        return activeGateway;
    }

    public File findCoreBinary() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return null;

        try {
            String nativeDir = context.getApplicationInfo().nativeLibraryDir;
            if (nativeDir != null) {
                File nativeLib = new File(nativeDir, "libaether.so");
                if (nativeLib.exists()) {
                    return nativeLib;
                }
            }
        } catch (Throwable ignore) {}

        // On Android 10+/Samsung the files/ dir is often mounted noexec — try code_cache first
        try {
            File codeCache = context.getCodeCacheDir();
            if (codeCache != null) {
                File ccBin = new File(codeCache, "libaether.so");
                if (ccBin.exists()) {
                    try { ccBin.setExecutable(true, false); } catch (Throwable ignore) {}
                    return ccBin;
                }
                // If we have a downloaded copy in files/aether, promote it to code_cache (executable)
                File downloaded = AetherDownloader.getDownloadedBinaryFile();
                if (downloaded != null && downloaded.exists() && downloaded.length() > 0) {
                    try {
                        if (copyToExecutable(downloaded, ccBin)) {
                            return ccBin;
                        }
                    } catch (Throwable ignore) {}
                }
            }
        } catch (Throwable ignore) {}

        File downloaded = AetherDownloader.getDownloadedBinaryFile();
        if (downloaded != null && downloaded.exists()) {
            try {
                downloaded.setExecutable(true, false);
            } catch (Throwable ignore) {}
            return downloaded;
        }

        File alt = new File(context.getFilesDir(), "aether/aether");
        if (alt.exists()) {
            try {
                alt.setExecutable(true, false);
            } catch (Throwable ignore) {}
            return alt;
        }

        return null;
    }

    private static boolean copyToExecutable(File src, File dst) {
        java.io.FileInputStream in = null;
        java.io.FileOutputStream out = null;
        try {
            dst.getParentFile().mkdirs();
            in = new java.io.FileInputStream(src);
            out = new java.io.FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
            dst.setReadable(true, false);
            return dst.setExecutable(true, false);
        } catch (Throwable e) {
            try { if (dst.exists()) dst.delete(); } catch (Throwable ignore) {}
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignore) {}
            try { if (out != null) out.close(); } catch (Throwable ignore) {}
        }
    }

    public synchronized void start() {
        if (shouldRun || currentState == STATE_STARTING || currentState == STATE_CONNECTED) {
            return;
        }
        shouldRun = true;
        AetherConfig.setEnabled(true);

        AndroidUtilities.runOnUIThread(() -> {
            try {
                SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                boolean proxyEnabled = preferences.getBoolean("proxy_enabled", false);
                if (SharedConfig.currentProxy == null || !proxyEnabled) {
                    String host = AetherConfig.getSocksHost();
                    int activeP = getActivePort();
                    int port = activeP != 0 ? activeP : AetherConfig.getSocksPort();
                    ProxySettings settings = ProxySettings.builder()
                            .setAddress(host)
                            .setPort(port)
                            .setType(ProxySettings.Type.SOCKS5)
                            .build();
                    SharedConfig.ProxyInfo proxyInfo = new SharedConfig.ProxyInfo(settings);
                    SharedConfig.ProxyInfo actual = SharedConfig.addProxy(proxyInfo);
                    SharedConfig.currentProxy = actual != null ? actual : proxyInfo;
                    preferences.edit().putBoolean("proxy_enabled", true).commit();
                    ConnectionsManager.setProxySettings(true, (actual != null ? actual : proxyInfo).settings);
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                }
            } catch (Exception ignore) {}
        });

        AetherService.start(ApplicationLoader.applicationContext);
        updateState(STATE_PREPARING, "Preparing Aether...", -1);

        File binary = findCoreBinary();
        if (binary == null) {
            updateState(STATE_DOWNLOADING, "Downloading Aether core...", -1);
            AetherDownloader.downloadCore(new AetherDownloader.DownloadListener() {
                @Override
                public void onProgress(int percent, long downloadedBytes, long totalBytes) {
                    if (shouldRun) {
                        updateState(STATE_DOWNLOADING, "Downloading core: " + percent + "%", -1);
                    }
                }

                @Override
                public void onSuccess(File binaryFile) {
                    if (shouldRun) {
                        launchProcess(binaryFile);
                    }
                }

                @Override
                public void onError(Exception error) {
                    if (shouldRun) {
                        updateState(STATE_ERROR, "Download failed: " + error.getMessage(), -1);
                        shouldRun = false;
                        AetherConfig.setEnabled(false);
                    }
                }
            });
        } else {
            launchProcess(binary);
        }
    }

    public synchronized void stop() {
        shouldRun = false;
        AetherConfig.setEnabled(false);
        AetherService.stop(ApplicationLoader.applicationContext);
        updateState(STATE_DISCONNECTED, "Disconnected", -1);
        activeGateway = "";

        if (process != null) {
            try {
                process.destroy();
            } catch (Exception ignore) {}
            process = null;
        }

        if (supervisorThread != null) {
            supervisorThread.interrupt();
            supervisorThread = null;
        }

        detachTelegramProxy();
        AetherInAppTunnelManager.clear();
    }

    private boolean probeUdpDirect() {
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
            socket.setSoTimeout(1200);
            byte[] dns = new byte[]{
                    0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x01, 'a', 0x01, 'b', 0x00, 0x00, 0x01, 0x00, 0x01
            };
            java.net.DatagramPacket p = new java.net.DatagramPacket(dns, dns.length, java.net.InetAddress.getByName("1.1.1.1"), 53);
            socket.send(p);
            byte[] buf = new byte[512];
            java.net.DatagramPacket r = new java.net.DatagramPacket(buf, buf.length);
            socket.receive(r);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    private int awaitActiveSocksPort(int defaultPort, long timeoutMs) {
        long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        while (SystemClock.elapsedRealtime() < deadline && shouldRun) {
            int detected = activeSocksPort;
            if (detected > 0 && isPortOpen("127.0.0.1", detected)) {
                return detected;
            }
            if (isPortOpen("127.0.0.1", defaultPort)) {
                activeSocksPort = defaultPort;
                return defaultPort;
            }
            SystemClock.sleep(300);
        }
        return activeSocksPort > 0 ? activeSocksPort : (isPortOpen("127.0.0.1", defaultPort) ? defaultPort : 0);
    }

    private void launchProcess(File binary) {
        updateState(STATE_STARTING, "Starting engine...", -1);
        activeSocksPort = 0;
        supervisorThread = new Thread(() -> {
            try {
                Context context = ApplicationLoader.applicationContext;
                File workDir = AetherDownloader.getAetherDirectory();

                int requestedProto = AetherConfig.getProtocol();
                int effectiveProto = requestedProto;
                boolean forceH2 = false;
                boolean forceFragment = false;

                if (requestedProto == AetherConfig.PROTOCOL_AUTO) {
                    effectiveProto = AetherConfig.PROTOCOL_GOOL;
                }

                List<String> cmd = new ArrayList<>();
                cmd.add(binary.getAbsolutePath());
                cmd.addAll(AetherConfig.toArgs(effectiveProto, forceH2, forceFragment));

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(workDir);
                pb.redirectErrorStream(true);

                Map<String, String> env = pb.environment();
                env.putAll(AetherConfig.toEnv(forceH2, forceFragment));
                env.put("HOME", workDir.getAbsolutePath());
                env.put("TMPDIR", workDir.getAbsolutePath());
                env.put("AETHER_CONFIG", new File(workDir, "aether.toml").getAbsolutePath());
                // 1.5.0 ROOT-CAUSE FIX: engine links libc++_shared.so which lives in nativeLibraryDir.
                // As an executable (not dlopen) the linker only searches LD_LIBRARY_PATH.
                try {
                    String nativeDir = context.getApplicationInfo().nativeLibraryDir;
                    if (nativeDir != null && !nativeDir.isEmpty()) {
                        String existing = env.get("LD_LIBRARY_PATH");
                        String ld = (existing != null && !existing.trim().isEmpty()) ? nativeDir + ":" + existing : nativeDir;
                        env.put("LD_LIBRARY_PATH", ld);
                    }
                } catch (Throwable ignore) {}

                appendLog("[engine] Spawning: " + binary.getName() + " with " + cmd.size() + " args");
                try {
                    process = pb.start();
                } catch (Exception e) {
                    FileLog.e("AetherController", e);
                    appendLog("[error] Failed to launch binary: " + e.getMessage());
                    updateState(STATE_ERROR, "Cannot execute binary: " + e.getMessage(), -1);
                    stop();
                    return;
                }

                startLogReader(process.getInputStream());

                int defaultPort = AetherConfig.getSocksPort();
                int port = awaitActiveSocksPort(defaultPort, 45000);
                if (port <= 0 || !shouldRun) {
                    if (shouldRun) {
                        updateState(STATE_ERROR, "Port timeout", -1);
                        stop();
                    }
                    return;
                }

                updateState(STATE_CONNECTING, "Verifying tunnel...", -1);
                // Multi-target probe so a single blocked anycast does not fake a dead tunnel.
                // Never report CONNECTED via isPortOpen alone - that was the 1-2ms bug (localhost
                // fast-path). Always do a full SOCKS CONNECT to external 443 and show that RTT.
                long probeLatency = probeSocks5Multi("127.0.0.1", port, 15000);
                if (probeLatency >= 5 && shouldRun) {
                    currentPing = probeLatency;
                    updateState(STATE_CONNECTED, "Connected", probeLatency);
                    attachTelegramProxy(port);
                } else if (probeLatency >= 0 && probeLatency < 5 && shouldRun) {
                    // Implausible 1-2ms through a tunnel - re-probe before accepting
                    appendLog("[verify] Implausible ping " + probeLatency + "ms, re-probing...");
                    long second = probeSocks5Multi("127.0.0.1", port, 15000);
                    if (second >= 5) {
                        currentPing = second;
                        updateState(STATE_CONNECTED, "Connected", second);
                    } else if (second >= 0) {
                        updateState(STATE_CONNECTED, "Connected", second);
                    } else {
                        updateState(STATE_RECONNECTING, "Scanning / Reconnecting...", -1);
                    }
                    attachTelegramProxy(port);
                } else if (shouldRun) {
                    updateState(STATE_RECONNECTING, "Scanning / Reconnecting...", -1);
                }

                runWatchdogLoop(port);

            } catch (Exception e) {
                FileLog.e("AetherController", e);
                appendLog("[engine-error] " + e.getMessage());
                if (shouldRun) {
                    updateState(STATE_ERROR, "Error: " + e.getMessage(), -1);
                    stop();
                }
            }
        }, "aether-supervisor");
        supervisorThread.start();
    }

    private void startLogReader(InputStream in) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLog(line);
                    parseEngineOutput(line);
                }
            } catch (Exception ignore) {}
        }, "aether-log-drain").start();
    }

    private void parseEngineOutput(String line) {
        java.util.regex.Matcher m = SOCKS_PORT_PATTERN.matcher(line);
        if (m.find()) {
            try {
                int detected = Integer.parseInt(m.group(1));
                if (detected > 0 && detected != activeSocksPort) {
                    activeSocksPort = detected;
                    appendLog("[controller] Detected live SOCKS5 port from engine: " + detected);
                    if (currentState == STATE_CONNECTED) {
                        attachTelegramProxy(detected);
                    }
                }
            } catch (Exception ignore) {}
        }

        if (line.contains("using cloudflare edge")) {
            int idx = line.indexOf("edge ");
            if (idx >= 0) {
                activeGateway = line.substring(idx + 5).trim();
            }
        } else if (line.contains("hunting MASQUE gateway") || line.contains("scanning")) {
            if (currentState != STATE_CONNECTED) {
                updateState(STATE_SCANNING, "Scanning gateways...", currentPing);
            }
        } else if (line.contains("tunnel closed; reconnecting") || line.contains("reconnecting")) {
            updateState(STATE_RECONNECTING, "Reconnecting...", currentPing);
        }
    }

    private void appendLog(String line) {
        synchronized (logBuffer) {
            logBuffer.add(line);
            if (logBuffer.size() > 200) {
                logBuffer.removeFirst();
            }
        }
        for (LogListener l : logListeners) {
            l.onLog(line);
        }
    }

    private boolean awaitPort(String host, int port, long timeoutMs) {
        long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        while (SystemClock.elapsedRealtime() < deadline && shouldRun) {
            if (isPortOpen(host, port)) {
                return true;
            }
            if (process == null || !process.isAlive()) {
                return false;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return false;
    }

    private boolean isPortOpen(String host, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private long probeSocks5(String host, int port, int timeoutMs) {
        return probeSocks5Target(host, port, PING_TARGETS[0], timeoutMs);
    }

    private long probeSocks5Target(String host, int port, String targetIp, int timeoutMs) {
        long start = SystemClock.elapsedRealtime();
        try (Socket s = new Socket()) {
            s.setSoTimeout(timeoutMs);
            s.connect(new InetSocketAddress(host, port), timeoutMs);

            OutputStream out = s.getOutputStream();
            InputStream in = s.getInputStream();

            out.write(new byte[]{0x05, 0x01, 0x00});
            out.flush();

            byte[] methodResp = new byte[2];
            int r = in.read(methodResp);
            if (r != 2 || methodResp[0] != 0x05 || methodResp[1] != 0x00) {
                return -1;
            }

            // Build CONNECT to targetIp:443 (0x01 = IPv4)
            String[] parts = targetIp.split("\\.");
            if (parts.length != 4) return -1;
            byte[] connectReq = new byte[]{
                    0x05, 0x01, 0x00, 0x01,
                    (byte) Integer.parseInt(parts[0]),
                    (byte) Integer.parseInt(parts[1]),
                    (byte) Integer.parseInt(parts[2]),
                    (byte) Integer.parseInt(parts[3]),
                    0x01, (byte) 0xBB
            };
            out.write(connectReq);
            out.flush();

            byte[] connectResp = new byte[10];
            r = in.read(connectResp);
            if (r >= 4 && connectResp[0] == 0x05 && connectResp[1] == 0x00) {
                return SystemClock.elapsedRealtime() - start;
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    // Multi-target probe like reference PingMonitor.kt TARGETS: try 1.1.1.1, 8.8.8.8, 9.9.9.9 on 443.
    // Returns first success latency, or -1 if all fail. Ensures a single unreachable anycast
    // target cannot fake a dead tunnel, and verifies ping is real external latency not localhost.
    private long probeSocks5Multi(String host, int port, int timeoutMs) {
        int p = port > 0 ? port : getActivePort();
        // Always resolve fresh active port so watchdog never measures a stale listener
        int active = activeSocksPort;
        if (active > 0) p = active;
        for (String target : PING_TARGETS) {
            long lat = probeSocks5Target(host, p, target, timeoutMs);
            if (lat >= 0) {
                // Filter implausible 1-2ms localhost shortcut: a real tunnel to anycast
                // 443 cannot be < 5ms; anything below is a SOCKS impl that replied
                // without dialing, so try next target before accepting it.
                if (lat < 5 && target.equals(PING_TARGETS[0])) continue;
                return lat;
            }
        }
        return -1;
    }

    private void runWatchdogLoop(int launchPort) {
        consecutiveProbeFailures = 0;
        while (shouldRun) {
            try {
                // 15s like reference AetherVpnService WATCHDOG_INTERVAL_MS, plus jitter
                // to avoid thundering-herd flapping when many devices wake together.
                long jitter = (long) (Math.random() * 3000);
                Thread.sleep(15000 + jitter);
            } catch (InterruptedException e) {
                break;
            }

            if (!shouldRun) break;

            if (process == null || !process.isAlive()) {
                appendLog("[watchdog] Engine died unexpectedly, restarting...");
                updateState(STATE_RECONNECTING, "Restarting engine...", -1);
                File binary = findCoreBinary();
                if (binary != null && shouldRun) {
                    launchProcess(binary);
                }
                break;
            }

            // Always resolve fresh port - parseEngineOutput may have updated activeSocksPort
            // after launch (reference PingMonitor.tunnelPort fix: probe was hardcoded to 1819).
            int curPort = activeSocksPort > 0 ? activeSocksPort : launchPort;
            if (curPort <= 0) curPort = getActivePort();

            // In background (app paused / screen off), do NOT drop connection just because external probe is delayed by Android Doze!
            // Reference does awaitExit(WATCHDOG_INTERVAL_MS) instead of polling; we keep the poll but gate on Doze.
            boolean isBackground = org.telegram.messenger.ApplicationLoader.mainInterfacePaused;
            if (isBackground) {
                if (isPortOpen("127.0.0.1", curPort)) {
                    consecutiveProbeFailures = 0;
                }
                continue;
            }

            // Multi-target probe like reference probeTunnelCycle (3 anycast 443 targets).
            // One unreachable target cannot fake a dead tunnel; needs all 3 to fail.
            long latency = probeSocks5Multi("127.0.0.1", curPort, 8000);
            if (latency >= 0) {
                consecutiveProbeFailures = 0;
                // Filter 1-2ms localhost shortcut in watchdog too
                if (latency < 5) {
                    appendLog("[watchdog] Implausible ping " + latency + "ms, ignoring");
                    continue;
                }
                currentPing = latency;
                if (currentState != STATE_CONNECTED) {
                    updateState(STATE_CONNECTED, "Connected", latency);
                    attachTelegramProxy(curPort);
                } else {
                    notifyState();
                }
            } else {
                consecutiveProbeFailures++;
                // Tuned from 3 to 4: with 3 targets per cycle that's 12 probe failures before reconnect,
                // vs old 3 single-target failures. Matches reference hardening (WATCHDOG_FAIL_CYCLES=2
                // with PROBE_ATTEMPTS=3 => 6 probes minimum). Reduces flapping on lossy/DPI links.
                if (consecutiveProbeFailures >= 4) {
                    appendLog("[watchdog] Consecutive probe failures (" + consecutiveProbeFailures + "), reconnecting...");
                    updateState(STATE_RECONNECTING, "Reconnecting...", -1);
                }
            }
        }
    }

    private void attachTelegramProxy(int port) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                String host = AetherConfig.getSocksHost();
                ProxySettings settings = ProxySettings.builder()
                        .setAddress(host)
                        .setPort(port)
                        .setType(ProxySettings.Type.SOCKS5)
                        .build();

                SharedConfig.ProxyInfo proxyInfo = new SharedConfig.ProxyInfo(settings);
                SharedConfig.ProxyInfo actual = SharedConfig.addProxy(proxyInfo);
                if (actual != null) {
                    proxyInfo = actual;
                }

                SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                boolean proxyEnabled = preferences.getBoolean("proxy_enabled", false);
                SharedConfig.ProxyInfo cur = SharedConfig.currentProxy;

                boolean isCurAether = cur != null && cur.settings != null &&
                        (host.equals(cur.settings.getAddress()) || "127.0.0.1".equals(cur.settings.getAddress())) &&
                        (cur.settings.getPort() == port || cur.settings.getPort() == AetherConfig.getSocksPort());

                if (cur == null || !proxyEnabled || isCurAether) {
                    SharedConfig.currentProxy = proxyInfo;

                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("proxy_enabled", true);
                    settings.toSharedPreferences(editor);
                    editor.commit();

                    ConnectionsManager.setProxySettings(true, settings);
                    for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                        if (UserConfig.getInstance(a).isClientActivated()) {
                            ConnectionsManager.getInstance(a).checkConnection();
                        }
                    }
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                    AetherInAppTunnelManager.apply(host, port);
                    appendLog("[controller] Auto-switched and activated Aether proxy (" + host + ":" + port + ")");
                }
            } catch (Exception e) {
                FileLog.e("AetherController", e);
            }
        });
    }

    private void detachTelegramProxy() {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                String host = AetherConfig.getSocksHost();
                if (SharedConfig.currentProxy != null && SharedConfig.currentProxy.settings != null && (host.equals(SharedConfig.currentProxy.settings.getAddress()) || "127.0.0.1".equals(SharedConfig.currentProxy.settings.getAddress()))) {
                    SharedConfig.currentProxy = null;
                    SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                    preferences.edit()
                            .putBoolean("proxy_enabled", false)
                            .putString("proxy_ip", "")
                            .putInt("proxy_port", 1080)
                            .apply();
                    ConnectionsManager.setProxySettings(false, null);
                    AetherInAppTunnelManager.clear();
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                }
            } catch (Exception e) {
                FileLog.e("AetherController", e);
            }
        });
    }

    private void updateState(int state, String statusText, long pingMs) {
        currentState = state;
        currentStatusText = statusText;
        currentPing = pingMs;
        AndroidUtilities.runOnUIThread(this::notifyState);
    }

    private void notifyState() {
        for (StatusListener l : statusListeners) {
            l.onStateChanged(currentState, currentStatusText, currentPing);
        }
    }
}

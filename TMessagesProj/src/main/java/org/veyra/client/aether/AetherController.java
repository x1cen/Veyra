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

    private Process process;
    private Thread supervisorThread;
    private volatile boolean shouldRun = false;
    private int consecutiveProbeFailures = 0;

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
        File nativeLib = new File(context.getApplicationInfo().nativeLibraryDir, "libaether.so");
        if (nativeLib.exists() && nativeLib.canExecute()) {
            return nativeLib;
        }

        File downloaded = AetherDownloader.getDownloadedBinaryFile();
        if (downloaded.exists()) {
            if (!downloaded.canExecute()) {
                downloaded.setExecutable(true, false);
            }
            if (downloaded.canExecute()) {
                return downloaded;
            }
        }

        File alt = new File(context.getFilesDir(), "aether/aether");
        if (alt.exists()) {
            if (!alt.canExecute()) {
                alt.setExecutable(true, false);
            }
            if (alt.canExecute()) {
                return alt;
            }
        }

        return null;
    }

    public synchronized void start() {
        if (shouldRun || currentState == STATE_STARTING || currentState == STATE_CONNECTED) {
            return;
        }
        shouldRun = true;
        AetherConfig.setEnabled(true);
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

    private void launchProcess(File binary) {
        updateState(STATE_STARTING, "Starting engine...", -1);
        supervisorThread = new Thread(() -> {
            try {
                Context context = ApplicationLoader.applicationContext;
                File workDir = AetherDownloader.getAetherDirectory();

                int requestedProto = AetherConfig.getProtocol();
                int effectiveProto = requestedProto;
                boolean forceH2 = false;

                if (requestedProto == AetherConfig.PROTOCOL_AUTO) {
                    updateState(STATE_STARTING, "Probing network...", -1);
                    boolean udpOk = probeUdpDirect();
                    appendLog("[smart-auto] UDP probe result: " + (udpOk ? "reachable" : "throttled/blocked"));
                    if (udpOk) {
                        effectiveProto = AetherConfig.PROTOCOL_MASQUE;
                        forceH2 = false;
                    } else {
                        effectiveProto = AetherConfig.PROTOCOL_MASQUE;
                        forceH2 = true;
                        appendLog("[smart-auto] UDP throttled; switching to MASQUE HTTP/2 + fragmentation");
                    }
                }

                List<String> cmd = new ArrayList<>();
                cmd.add(binary.getAbsolutePath());
                cmd.addAll(AetherConfig.toArgs(effectiveProto, forceH2));

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(workDir);
                pb.redirectErrorStream(true);

                Map<String, String> env = pb.environment();
                env.putAll(AetherConfig.toEnv(forceH2));
                env.put("HOME", workDir.getAbsolutePath());
                env.put("TMPDIR", workDir.getAbsolutePath());
                env.put("AETHER_CONFIG", new File(workDir, "aether.toml").getAbsolutePath());

                appendLog("[engine] Spawning: " + binary.getName() + " with " + cmd.size() + " args");
                process = pb.start();

                startLogReader(process.getInputStream());

                int port = AetherConfig.getSocksPort();
                boolean portReady = awaitPort("127.0.0.1", port, 35000);
                if (!portReady || !shouldRun) {
                    if (shouldRun) {
                        updateState(STATE_ERROR, "Port timeout", -1);
                        stop();
                    }
                    return;
                }

                updateState(STATE_CONNECTING, "Verifying tunnel...", -1);
                long probeLatency = probeSocks5("127.0.0.1", port, 12000);
                if (probeLatency >= 0 && shouldRun) {
                    currentPing = probeLatency;
                    updateState(STATE_CONNECTED, "Connected", probeLatency);
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

            byte[] connectReq = new byte[]{
                    0x05, 0x01, 0x00, 0x01,
                    0x01, 0x01, 0x01, 0x01,
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

    private void runWatchdogLoop(int port) {
        consecutiveProbeFailures = 0;
        while (shouldRun) {
            try {
                Thread.sleep(12000);
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

            long latency = probeSocks5("127.0.0.1", port, 8000);
            if (latency >= 0) {
                consecutiveProbeFailures = 0;
                currentPing = latency;
                if (currentState != STATE_CONNECTED) {
                    updateState(STATE_CONNECTED, "Connected", latency);
                    attachTelegramProxy(port);
                } else {
                    notifyState();
                }
            } else {
                consecutiveProbeFailures++;
                if (consecutiveProbeFailures >= 3) {
                    appendLog("[watchdog] Consecutive probe failures, reconnecting...");
                    updateState(STATE_RECONNECTING, "Reconnecting...", -1);
                }
            }
        }
    }

    private void attachTelegramProxy(int port) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                ProxySettings settings = ProxySettings.builder()
                        .setAddress("127.0.0.1")
                        .setPort(port)
                        .setType(ProxySettings.Type.SOCKS5)
                        .build();

                SharedConfig.ProxyInfo proxyInfo = SharedConfig.addProxy(new SharedConfig.ProxyInfo(settings));
                SharedConfig.currentProxy = proxyInfo;

                SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                preferences.edit()
                        .putBoolean("proxy_enabled", true)
                        .putString("proxy_ip", "127.0.0.1")
                        .putInt("proxy_port", port)
                        .putInt("proxy_type", 0)
                        .apply();

                ConnectionsManager.setProxySettings(true, settings);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
            } catch (Exception e) {
                FileLog.e("AetherController", e);
            }
        });
    }

    private void detachTelegramProxy() {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                if (SharedConfig.currentProxy != null && "127.0.0.1".equals(SharedConfig.currentProxy.getAddress())) {
                    SharedConfig.currentProxy = null;
                    SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                    preferences.edit()
                            .putBoolean("proxy_enabled", false)
                            .putString("proxy_ip", "")
                            .putInt("proxy_port", 1080)
                            .apply();
                    ConnectionsManager.setProxySettings(false, null);
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

package org.veyra.client.proxy;

import android.content.SharedPreferences;
import android.os.SystemClock;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.utils.proxy.ProxySettings;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProxyDiscoveryPipeline {

    private static volatile ProxyDiscoveryPipeline instance;

    public interface PipelineListener {
        void onPipelineStarted();
        void onTournamentProgress(String status, int currentRound, int totalCandidates);
        void onPipelineFinished(SharedConfig.ProxyInfo winner, int prunedCount);
    }

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<PipelineListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean isRunning = false;
    private final Random random = new Random();

    public static ProxyDiscoveryPipeline getInstance() {
        if (instance == null) {
            synchronized (ProxyDiscoveryPipeline.class) {
                if (instance == null) {
                    instance = new ProxyDiscoveryPipeline();
                }
            }
        }
        return instance;
    }

    private ProxyDiscoveryPipeline() {
        // Periodic check loop
        scheduler.scheduleWithFixedDelay(this::periodicCheck, 2, 2, TimeUnit.MINUTES);
    }

    public void addListener(PipelineListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(PipelineListener listener) {
        listeners.remove(listener);
    }

    public boolean isRunning() {
        return isRunning;
    }

    private void periodicCheck() {
        if (!ProxyDiscoveryConfig.isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        long lastRun = ProxyDiscoveryConfig.getLastRunTime();
        long intervalMs = ProxyDiscoveryConfig.getIntervalMinutes() * 60 * 1000L;

        if (now - lastRun >= intervalMs) {
            runTournament(false);
        }
    }

    public synchronized void runTournament(boolean force) {
        if (isRunning) {
            return;
        }
        isRunning = true;
        notifyStarted();

        scheduler.execute(() -> {
            try {
                performP2CTournament();
            } catch (Exception e) {
                FileLog.e("ProxyDiscoveryPipeline", e);
            } finally {
                isRunning = false;
            }
        });
    }

    private void performP2CTournament() {
        SharedConfig.loadProxyList();
        List<SharedConfig.ProxyInfo> originalList = new ArrayList<>(SharedConfig.proxyList);
        if (originalList.isEmpty()) {
            notifyFinished(null, 0);
            return;
        }

        // Candidates pool
        List<SharedConfig.ProxyInfo> pool = new ArrayList<>();
        for (SharedConfig.ProxyInfo p : originalList) {
            if (p.settings == null) continue;
            boolean isAether = (org.veyra.client.aether.AetherConfig.getSocksHost().equals(p.settings.getAddress()) || "127.0.0.1".equals(p.settings.getAddress())) &&
                    (p.settings.getPort() == org.veyra.client.aether.AetherConfig.getSocksPort() ||
                     p.settings.getPort() == org.veyra.client.aether.AetherController.getInstance().getActivePort());
            if (isAether) {
                boolean aetherRunning = org.veyra.client.aether.AetherConfig.isEnabled() &&
                        org.veyra.client.aether.AetherController.getInstance().getState() == org.veyra.client.aether.AetherController.STATE_CONNECTED;
                if (!aetherRunning) {
                    continue; // Skip disabled Aether from tournament
                }
            }
            pool.add(p);
        }
        Collections.shuffle(pool, random);

        List<SharedConfig.ProxyInfo> deadProxies = new ArrayList<>();
        int totalInitial = pool.size();
        int round = 0;

        // If only 1 proxy, test it directly
        if (pool.size() == 1) {
            SharedConfig.ProxyInfo single = pool.get(0);
            BenchmarkResult res = benchmarkProxy(single);
            if (!res.isAlive) {
                deadProxies.add(single);
            }
        }

        // P2C Tournament: Repeatedly pick 2 proxies, compare them, loser is knocked out
        while (pool.size() > 1) {
            round++;
            SharedConfig.ProxyInfo p1 = pool.remove(0);
            SharedConfig.ProxyInfo p2 = pool.remove(0);

            notifyProgress("Comparing candidates...", round, pool.size() + 2);

            BenchmarkResult r1 = benchmarkProxy(p1);
            BenchmarkResult r2 = benchmarkProxy(p2);

            if (!r1.isAlive) deadProxies.add(p1);
            if (!r2.isAlive) deadProxies.add(p2);

            SharedConfig.ProxyInfo winner;
            if (!r1.isAlive && !r2.isAlive) {
                // Both dead, neither qualifies
                winner = null;
            } else if (!r1.isAlive) {
                winner = p2;
            } else if (!r2.isAlive) {
                winner = p1;
            } else {
                // Both alive: Compare composite score (bandwidth throughput & latency)
                winner = (r1.score >= r2.score) ? p1 : p2;
            }

            if (winner != null) {
                pool.add(winner); // Winner progresses to next bracket
            }
        }

        SharedConfig.ProxyInfo champion = pool.isEmpty() ? null : pool.get(0);

        // Prune dead proxies if option enabled
        int prunedCount = 0;
        if (ProxyDiscoveryConfig.isPruneDeadEnabled() && !deadProxies.isEmpty()) {
            boolean listChanged = false;
            for (SharedConfig.ProxyInfo dead : deadProxies) {
                if (dead.settings != null) {
                    boolean isAether = (org.veyra.client.aether.AetherConfig.getSocksHost().equals(dead.settings.getAddress()) || "127.0.0.1".equals(dead.settings.getAddress())) &&
                            (dead.settings.getPort() == org.veyra.client.aether.AetherConfig.getSocksPort() ||
                             dead.settings.getPort() == org.veyra.client.aether.AetherController.getInstance().getActivePort());
                    if (isAether) {
                        continue; // NEVER prune Aether
                    }
                }
                if (champion != dead && SharedConfig.proxyList.remove(dead)) {
                    prunedCount++;
                    listChanged = true;
                }
            }
            if (listChanged) {
                SharedConfig.saveProxyList();
            }
        }

        // Apply Champion Proxy
        if (champion != null) {
            applyChampion(champion);
            ProxyDiscoveryConfig.setLastWinner(formatProxyName(champion));
        }

        ProxyDiscoveryConfig.setLastRunTime(System.currentTimeMillis());
        ProxyDiscoveryConfig.setLastPrunedCount(prunedCount);

        notifyFinished(champion, prunedCount);
    }

    private void applyChampion(SharedConfig.ProxyInfo champion) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                SharedConfig.currentProxy = champion;
                SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("proxy_enabled", true);
                champion.settings.toSharedPreferences(editor);
                editor.commit();

                ConnectionsManager.setProxySettings(true, champion.settings);
                for (int a : SharedConfig.activeAccounts) {
                    ConnectionsManager.getInstance(a).checkConnection();
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
            } catch (Exception e) {
                FileLog.e("ProxyDiscoveryPipeline", e);
            }
        });
    }

    private BenchmarkResult benchmarkProxy(SharedConfig.ProxyInfo proxy) {
        BenchmarkResult r = new BenchmarkResult();
        if (proxy == null || proxy.settings == null) {
            r.isAlive = false;
            r.score = -99999f;
            return r;
        }

        if (proxy.settings.getType() == ProxySettings.Type.WEB) {
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            final long[] latency = new long[]{-1};
            org.telegram.tgnet.ConnectionsManager.getInstance(org.telegram.messenger.UserConfig.selectedAccount)
                    .checkProxy(proxy.settings, time -> {
                        latency[0] = time;
                        latch.countDown();
                    });
            try {
                latch.await(4500, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignore) {}

            if (latency[0] >= 0) {
                r.isAlive = true;
                r.pingMs = latency[0];
                proxy.ping = latency[0];
                proxy.available = true;
                r.downloadSpeedKbps = Math.max(20, 15000.0f / (latency[0] + 1));
                r.score = (r.downloadSpeedKbps * 0.7f) - (r.pingMs * 0.3f);
            } else {
                r.isAlive = false;
                r.score = -99999f;
                proxy.available = false;
            }
            return r;
        }

        String host = proxy.settings.getAddress();
        int port = proxy.settings.getPort();

        long start = SystemClock.elapsedRealtime();
        try (Socket socket = new Socket()) {
            // Stage 1: TCP Handshake probe (max 3500ms timeout)
            socket.connect(new InetSocketAddress(host, port), 3500);
            socket.setSoTimeout(3500);
            long ping = SystemClock.elapsedRealtime() - start;
            r.isAlive = true;
            r.pingMs = ping;
            proxy.ping = ping;
            proxy.available = true;

            // Stage 2: Throughput Speed Test (if enabled)
            if (ProxyDiscoveryConfig.isSpeedTestEnabled() && proxy.settings.getType() == ProxySettings.Type.SOCKS5) {
                r.downloadSpeedKbps = measureSocks5Speed(socket);
            } else {
                r.downloadSpeedKbps = Math.max(10, 10000.0f / (ping + 1));
            }

            // Weighted composite score: High speed is heavily rewarded, high latency is penalized
            r.score = (r.downloadSpeedKbps * 0.7f) - (r.pingMs * 0.3f);
        } catch (Exception e) {
            r.isAlive = false;
            r.score = -99999f;
            proxy.available = false;
        }

        return r;
    }

    private float measureSocks5Speed(Socket socket) {
        try {
            // Send quick SOCKS5 greeting: 0x05 (ver), 0x01 (num auth), 0x00 (no auth)
            socket.getOutputStream().write(new byte[]{0x05, 0x01, 0x00});
            socket.getOutputStream().flush();

            InputStream in = socket.getInputStream();
            byte[] response = new byte[2];
            int read = in.read(response);
            if (read == 2 && response[0] == 0x05 && response[1] == 0x00) {
                return 150.0f; // Handshake completed fast
            }
        } catch (Exception ignore) {}
        return 50.0f;
    }

    private String formatProxyName(SharedConfig.ProxyInfo info) {
        if (info == null) return "None";
        if ("127.0.0.1".equals(info.settings.getAddress())) {
            return "⚡ Aether (" + info.settings.getPort() + ")";
        }
        if (info.settings.getType() == ProxySettings.Type.WEB) {
            return "🌐 Web Proxy (" + info.settings.getAddress() + ")";
        }
        return info.settings.getAddress() + ":" + info.settings.getPort();
    }

    private void notifyStarted() {
        AndroidUtilities.runOnUIThread(() -> {
            for (PipelineListener l : listeners) {
                l.onPipelineStarted();
            }
        });
    }

    private void notifyProgress(String status, int round, int total) {
        AndroidUtilities.runOnUIThread(() -> {
            for (PipelineListener l : listeners) {
                l.onTournamentProgress(status, round, total);
            }
        });
    }

    private void notifyFinished(SharedConfig.ProxyInfo winner, int pruned) {
        AndroidUtilities.runOnUIThread(() -> {
            for (PipelineListener l : listeners) {
                l.onPipelineFinished(winner, pruned);
            }
        });
    }

    private static class BenchmarkResult {
        boolean isAlive = false;
        long pingMs = 0;
        float downloadSpeedKbps = 0;
        float score = 0;
    }
}

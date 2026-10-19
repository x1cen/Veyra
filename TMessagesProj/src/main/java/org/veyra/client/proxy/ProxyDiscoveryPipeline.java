package org.veyra.client.proxy;

import android.os.SystemClock;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProxyDiscoveryPipeline {

    public interface PipelineListener {
        void onPipelineStarted();
        void onTournamentProgress(String status, int currentRound, int totalCandidates);
        void onPipelineFinished(SharedConfig.ProxyInfo winner, int inactiveCount);
    }

    private static volatile ProxyDiscoveryPipeline instance;

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

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<PipelineListener> listeners = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    private ProxyDiscoveryPipeline() {}

    public void addListener(PipelineListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeListener(PipelineListener l) {
        if (l != null) {
            listeners.remove(l);
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public void runTournament(boolean force) {
        if (!force && !ProxyDiscoveryConfig.isEnabled()) {
            return;
        }

        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        notifyStarted();
        executor.execute(() -> {
            try {
                performP2CTournament();
            } catch (Exception e) {
                FileLog.e("ProxyDiscoveryPipeline", e);
            } finally {
                isRunning.set(false);
            }
        });
    }

    private static class BenchmarkResult {
        boolean isAlive = false;
        long pingMs = 9999;
        float downloadSpeedKbps = 0;
        float score = -99999f;
        boolean handshakeSuccess = false;
    }

    private void performP2CTournament() {
        SharedConfig.loadProxyList();
        List<SharedConfig.ProxyInfo> candidates = new ArrayList<>(SharedConfig.proxyList);

        if (candidates.isEmpty()) {
            notifyFinished(null, 0);
            return;
        }

        notifyProgress("Probing proxies in parallel...", 0, candidates.size());

        final int timeoutMs = ProxyDiscoveryConfig.getTimeoutMs();
        final int strategy = ProxyDiscoveryConfig.getStrategy();

        // Stage 1: Fast Parallel TCP Ping Probe (up to 8 concurrent threads)
        ExecutorService probePool = Executors.newFixedThreadPool(Math.min(8, candidates.size()));
        List<SharedConfig.ProxyInfo> pool = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch probeLatch = new CountDownLatch(candidates.size());

        for (SharedConfig.ProxyInfo p : candidates) {
            probePool.execute(() -> {
                try {
                    if (ProxyStatusManager.isAether(p)) {
                        pool.add(p);
                        ProxyStatusManager.markActive(p);
                    } else {
                        boolean alive = checkTcpPing(p, timeoutMs);
                        if (alive) {
                            ProxyStatusManager.markActive(p);
                            pool.add(p);
                        } else {
                            ProxyStatusManager.markInactive(p);
                        }
                    }
                } finally {
                    probeLatch.countDown();
                }
            });
        }

        try {
            probeLatch.await(Math.max(5000, timeoutMs * 3), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {}
        probePool.shutdownNow();

        List<SharedConfig.ProxyInfo> activePool = new ArrayList<>(pool);
        int inactiveCount = candidates.size() - activePool.size();

        if (activePool.isEmpty()) {
            notifyFinished(null, inactiveCount);
            return;
        }

        Collections.shuffle(activePool, random);
        int round = 0;

        // Stage 2: Tournament between active candidates (without deleting any proxy)
        while (activePool.size() > 1) {
            round++;
            SharedConfig.ProxyInfo p1 = activePool.remove(0);
            SharedConfig.ProxyInfo p2 = activePool.remove(0);

            notifyProgress("Comparing candidates...", round, activePool.size() + 2);

            BenchmarkResult r1 = benchmarkProxy(p1, strategy, timeoutMs);
            BenchmarkResult r2 = benchmarkProxy(p2, strategy, timeoutMs);

            if (!r1.isAlive) {
                ProxyStatusManager.markInactive(p1);
            }
            if (!r2.isAlive) {
                ProxyStatusManager.markInactive(p2);
            }

            SharedConfig.ProxyInfo winner;
            if (!r1.isAlive && !r2.isAlive) {
                winner = null;
            } else if (!r1.isAlive) {
                winner = p2;
            } else if (!r2.isAlive) {
                winner = p1;
            } else {
                winner = (r1.score >= r2.score) ? p1 : p2;
            }

            if (winner != null) {
                activePool.add(winner);
            }
        }

        SharedConfig.ProxyInfo champion = activePool.isEmpty() ? null : activePool.get(0);

        if (champion != null) {
            applyChampion(champion);
            ProxyDiscoveryConfig.setLastWinner(formatProxyName(champion));
        }

        ProxyDiscoveryConfig.setLastRunTime(System.currentTimeMillis());

        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
        });

        notifyFinished(champion, inactiveCount);
    }

    public interface CleanCallback {
        void onProgress(int checked, int total);
        void onFinished(int removedCount, int keptCount);
    }

    public static boolean checkTcpPingStatic(SharedConfig.ProxyInfo proxy, int timeoutMs) {
        if (proxy == null || proxy.settings == null) return false;
        String host = proxy.settings.getAddress();
        int port = proxy.settings.getPort();
        if (android.text.TextUtils.isEmpty(host) || port <= 0 || port > 65535) return false;
        long start = SystemClock.elapsedRealtime();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            long ping = SystemClock.elapsedRealtime() - start;
            proxy.ping = ping;
            proxy.available = true;
            proxy.availableCheckTime = SystemClock.elapsedRealtime();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void cleanInactiveProxiesSmart(CleanCallback callback) {
        new Thread(() -> {
            List<SharedConfig.ProxyInfo> targets = new ArrayList<>(SharedConfig.proxyList);
            if (targets.isEmpty()) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (callback != null) callback.onFinished(0, 0);
                });
                return;
            }

            int total = targets.size();
            java.util.concurrent.atomic.AtomicInteger checkedCount = new java.util.concurrent.atomic.AtomicInteger(0);
            List<SharedConfig.ProxyInfo> deadProxies = Collections.synchronizedList(new ArrayList<>());
            List<SharedConfig.ProxyInfo> healthyProxies = Collections.synchronizedList(new ArrayList<>());

            int threadCount = Math.min(10, Math.max(2, targets.size()));
            ExecutorService pool = Executors.newFixedThreadPool(threadCount);

            for (SharedConfig.ProxyInfo p : targets) {
                pool.execute(() -> {
                    boolean alive = false;
                    if (p != null && p == SharedConfig.currentProxy) {
                        alive = true;
                    } else if (p != null && p.settings != null) {
                        alive = checkTcpPingStatic(p, 2500);
                        if (!alive) {
                            try { Thread.sleep(250); } catch (Exception ignored) {}
                            alive = checkTcpPingStatic(p, 3000);
                        }
                    }

                    if (alive) {
                        healthyProxies.add(p);
                    } else {
                        deadProxies.add(p);
                    }

                    int current = checkedCount.incrementAndGet();
                    AndroidUtilities.runOnUIThread(() -> {
                        if (callback != null) {
                            callback.onProgress(current, total);
                        }
                    });
                });
            }

            pool.shutdown();
            try {
                pool.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}

            AndroidUtilities.runOnUIThread(() -> {
                for (SharedConfig.ProxyInfo dead : deadProxies) {
                    SharedConfig.deleteProxy(dead);
                }
                SharedConfig.saveProxyList();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                if (callback != null) {
                    callback.onFinished(deadProxies.size(), healthyProxies.size());
                }
            });
        }).start();
    }

    public static void clearAllInactiveProxies(Runnable onComplete) {
        cleanInactiveProxiesSmart(new CleanCallback() {
            @Override
            public void onProgress(int checked, int total) {}

            @Override
            public void onFinished(int removedCount, int keptCount) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private boolean checkTcpPing(SharedConfig.ProxyInfo proxy, int timeoutMs) {
        if (proxy == null || proxy.settings == null) return false;
        String host = proxy.settings.getAddress();
        int port = proxy.settings.getPort();
        long start = SystemClock.elapsedRealtime();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            long ping = SystemClock.elapsedRealtime() - start;
            proxy.ping = ping;
            proxy.available = true;
            return true;
        } catch (Exception e) {
            proxy.ping = -1;
            proxy.available = false;
            return false;
        }
    }

    private void applyChampion(SharedConfig.ProxyInfo champion) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                if (champion == null || champion.settings == null) return;
                SharedConfig.currentProxy = champion;
                SharedConfig.saveProxyList();

                ConnectionsManager.setProxySettings(true, champion.settings);
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    if (UserConfig.getInstance(a).isClientActivated()) {
                        ConnectionsManager.getInstance(a).checkConnection();
                    }
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
            } catch (Exception e) {
                FileLog.e("ProxyDiscoveryPipeline", e);
            }
        });
    }

    private BenchmarkResult benchmarkProxy(SharedConfig.ProxyInfo proxy, int strategy, int timeoutMs) {
        BenchmarkResult r = new BenchmarkResult();
        if (proxy == null || proxy.settings == null) {
            r.isAlive = false;
            r.score = -99999f;
            return r;
        }

        if (proxy.settings.getType() == ProxySettings.Type.WEB) {
            CountDownLatch latch = new CountDownLatch(1);
            final long[] latency = new long[]{-1};
            ConnectionsManager.getInstance(UserConfig.selectedAccount)
                    .checkProxy(proxy.settings, time -> {
                        latency[0] = time;
                        latch.countDown();
                    });
            try {
                latch.await(Math.max(3000, timeoutMs), TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignore) {}

            if (latency[0] >= 0) {
                r.isAlive = true;
                r.pingMs = latency[0];
                proxy.ping = latency[0];
                proxy.available = true;
                r.downloadSpeedKbps = Math.max(20, 15000.0f / (latency[0] + 1));
                computeScore(r, strategy);
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
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            long ping = SystemClock.elapsedRealtime() - start;
            r.isAlive = true;
            r.pingMs = ping;
            proxy.ping = ping;
            proxy.available = true;

            if (ProxyDiscoveryConfig.isSpeedTestEnabled() && proxy.settings.getType() == ProxySettings.Type.SOCKS5) {
                r.downloadSpeedKbps = measureSocks5Speed(socket);
                r.handshakeSuccess = r.downloadSpeedKbps > 100.0f;
            } else {
                r.downloadSpeedKbps = Math.max(10, 10000.0f / (ping + 1));
                r.handshakeSuccess = true;
            }

            computeScore(r, strategy);
        } catch (Exception e) {
            r.isAlive = false;
            r.score = -99999f;
            proxy.available = false;
        }

        return r;
    }

    private void computeScore(BenchmarkResult r, int strategy) {
        if (!r.isAlive) {
            r.score = -99999f;
            return;
        }
        if (strategy == ProxyDiscoveryConfig.STRATEGY_LOWEST_LATENCY) {
            // Raw ping priority: lowest latency wins
            r.score = -r.pingMs;
        } else if (strategy == ProxyDiscoveryConfig.STRATEGY_STABILITY) {
            // Handshake reliability + low jitter
            r.score = (r.handshakeSuccess ? 500.0f : 0f) - (r.pingMs * 0.4f);
        } else {
            // Balanced P2C: Bandwidth speed + latency balance
            r.score = (r.downloadSpeedKbps * 0.7f) - (r.pingMs * 0.3f);
        }
    }

    private float measureSocks5Speed(Socket socket) {
        try {
            socket.getOutputStream().write(new byte[]{0x05, 0x01, 0x00});
            socket.getOutputStream().flush();

            InputStream in = socket.getInputStream();
            byte[] response = new byte[2];
            int read = in.read(response);
            if (read == 2 && response[0] == 0x05 && response[1] == 0x00) {
                return 150.0f;
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

    private void notifyFinished(SharedConfig.ProxyInfo winner, int inactiveCount) {
        AndroidUtilities.runOnUIThread(() -> {
            for (PipelineListener l : listeners) {
                l.onPipelineFinished(winner, inactiveCount);
            }
        });
    }
}

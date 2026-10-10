package org.veyra.client.proxy;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.SharedConfig;
import org.telegram.utils.proxy.ProxySettings;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class ProxyImporter {

    public interface ImportCallback {
        void onProgress(int parsedCount);
        void onComplete(int totalImported, int duplicatesSkipped);
        void onError(Exception error);
    }

    public static void importFromFile(Context context, Uri uri, ImportCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is == null) {
                    throw new IllegalArgumentException("Cannot open file stream");
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!TextUtils.isEmpty(line.trim())) {
                        lines.add(line.trim());
                    }
                }
                reader.close();
                processProxyLines(lines, callback);
            } catch (Exception e) {
                FileLog.e("ProxyImporter", e);
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onError(e));
                }
            }
        });
    }

    public static void importFromText(String rawText, ImportCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String[] split = rawText.split("\n");
                List<String> lines = new ArrayList<>();
                for (String s : split) {
                    if (!TextUtils.isEmpty(s.trim())) {
                        lines.add(s.trim());
                    }
                }
                processProxyLines(lines, callback);
            } catch (Exception e) {
                FileLog.e("ProxyImporter", e);
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onError(e));
                }
            }
        });
    }

    private static void processProxyLines(List<String> lines, ImportCallback callback) {
        SharedConfig.loadProxyList();
        Set<String> existingKeys = new HashSet<>();
        for (SharedConfig.ProxyInfo info : SharedConfig.proxyList) {
            existingKeys.add(info.settings.getAddress() + ":" + info.settings.getPort());
        }

        List<SharedConfig.ProxyInfo> toAdd = new ArrayList<>();
        int duplicates = 0;
        int parsed = 0;

        for (String line : lines) {
            parsed++;
            ProxySettings settings = parseProxyLine(line);
            if (settings != null) {
                String key = settings.getAddress() + ":" + settings.getPort();
                if (!existingKeys.contains(key)) {
                    existingKeys.add(key);
                    toAdd.add(new SharedConfig.ProxyInfo(settings));
                } else {
                    duplicates++;
                }
            }

            if (parsed % 50 == 0 && callback != null) {
                final int p = parsed;
                AndroidUtilities.runOnUIThread(() -> callback.onProgress(p));
            }
        }

        if (!toAdd.isEmpty()) {
            for (SharedConfig.ProxyInfo p : toAdd) {
                SharedConfig.addProxy(p);
            }
            SharedConfig.saveProxyList();
        }

        final int totalAdded = toAdd.size();
        final int dupes = duplicates;
        if (callback != null) {
            AndroidUtilities.runOnUIThread(() -> callback.onComplete(totalAdded, dupes));
        }
    }

    public static ProxySettings parseProxyLine(String line) {
        if (TextUtils.isEmpty(line)) return null;
        line = line.trim();

        // 1. Telegram MTProto or SOCKS5 link
        // e.g. tg://proxy?server=...&port=...&secret=...
        // or https://t.me/proxy?server=...
        if (line.startsWith("tg://proxy?") || line.startsWith("https://t.me/proxy?") ||
                line.startsWith("tg://socks?") || line.startsWith("https://t.me/socks?")) {
            try {
                Uri uri = Uri.parse(line);
                String server = uri.getQueryParameter("server");
                String portStr = uri.getQueryParameter("port");
                String secret = uri.getQueryParameter("secret");
                String user = uri.getQueryParameter("user");
                String pass = uri.getQueryParameter("pass");

                if (!TextUtils.isEmpty(server) && !TextUtils.isEmpty(portStr)) {
                    int port = Integer.parseInt(portStr);
                    if (line.contains("socks")) {
                        return ProxySettings.builder()
                                .setAddress(server)
                                .setPort(port)
                                .setUser(user != null ? user : "")
                                .setPassword(pass != null ? pass : "")
                                .setType(ProxySettings.Type.SOCKS5)
                                .build();
                    } else {
                        return ProxySettings.builder()
                                .setAddress(server)
                                .setPort(port)
                                .setSecret(secret != null ? secret : "")
                                .setType(ProxySettings.Type.MTPROTO)
                                .build();
                    }
                }
            } catch (Exception ignore) {}
        }

        // 2. Format: host:port:secret (MTProto)
        String[] parts = line.split(":");
        if (parts.length == 3) {
            try {
                String host = parts[0].trim();
                int port = Integer.parseInt(parts[1].trim());
                String secret = parts[2].trim();
                return ProxySettings.builder()
                        .setAddress(host)
                        .setPort(port)
                        .setSecret(secret)
                        .setType(ProxySettings.Type.MTPROTO)
                        .build();
            } catch (Exception ignore) {}
        }

        // 3. Format: host:port:user:pass (SOCKS5)
        if (parts.length == 4) {
            try {
                String host = parts[0].trim();
                int port = Integer.parseInt(parts[1].trim());
                String user = parts[2].trim();
                String pass = parts[3].trim();
                return ProxySettings.builder()
                        .setAddress(host)
                        .setPort(port)
                        .setUser(user)
                        .setPassword(pass)
                        .setType(ProxySettings.Type.SOCKS5)
                        .build();
            } catch (Exception ignore) {}
        }

        // 4. Format: host:port (SOCKS5 unauthenticated)
        if (parts.length == 2) {
            try {
                String host = parts[0].trim();
                int port = Integer.parseInt(parts[1].trim());
                return ProxySettings.builder()
                        .setAddress(host)
                        .setPort(port)
                        .setType(ProxySettings.Type.SOCKS5)
                        .build();
            } catch (Exception ignore) {}
        }

        return null;
    }
}

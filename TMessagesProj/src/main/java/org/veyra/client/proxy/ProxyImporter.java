package org.veyra.client.proxy;

import android.content.ClipboardManager;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyImporter {

    public interface ImportCallback {
        void onProgress(int parsedCount);
        void onComplete(int totalImported, int duplicatesSkipped);
        void onError(Exception error);
    }

    private static final Pattern TG_LINK_PATTERN = Pattern.compile(
            "(?:(?:tg|https?)://(?:[a-zA-Z0-9.-]+\\.)?(?:t\\.me|telegram\\.me|telegram\\.dog)?/(?:proxy|socks|webproxy)\\?[^\\s\"'<>]+)|(?:tg://(?:proxy|socks|webproxy)\\?[^\\s\"'<>]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SOCKS5_URI_PATTERN = Pattern.compile(
            "socks5://(?:([^:@\\s]+):([^@\\s]+)@)?([a-zA-Z0-9.-]+):(\\d{1,5})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COLON_LINE_PATTERN = Pattern.compile(
            "^([a-zA-Z0-9.-]+):(\\d{1,5})(?::([^\\s:]+))?(?::([^\\s:]+))?$"
    );

    public static void importFromFile(Context context, Uri uri, ImportCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is == null) {
                    throw new IllegalArgumentException("Cannot open file stream");
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                processProxyText(sb.toString(), callback);
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
                processProxyText(rawText, callback);
            } catch (Exception e) {
                FileLog.e("ProxyImporter", e);
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onError(e));
                }
            }
        });
    }

    public static void importFromClipboard(Context context, ImportCallback callback) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
                if (!TextUtils.isEmpty(text)) {
                    importFromText(text.toString(), callback);
                    return;
                }
            }
            if (callback != null) {
                callback.onError(new Exception("Clipboard is empty or does not contain text"));
            }
        } catch (Exception e) {
            FileLog.e("ProxyImporter", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    public static int getClipboardProxyCount(Context context) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
                if (!TextUtils.isEmpty(text)) {
                    List<ProxySettings> found = extractAllProxies(text.toString());
                    return found.size();
                }
            }
        } catch (Exception ignore) {}
        return 0;
    }

    public static List<ProxySettings> extractAllProxies(String text) {
        List<ProxySettings> result = new ArrayList<>();
        if (TextUtils.isEmpty(text)) return result;

        Set<String> seen = new HashSet<>();

        // 1. Scan for Telegram proxy and webproxy URLs
        Matcher tgMatcher = TG_LINK_PATTERN.matcher(text);
        while (tgMatcher.find()) {
            String url = tgMatcher.group();
            ProxySettings settings = parseProxyUrl(url);
            if (settings != null) {
                String key = getSettingsKey(settings);
                if (seen.add(key)) {
                    result.add(settings);
                }
            }
        }

        // 2. Scan for socks5:// URIs
        Matcher socksMatcher = SOCKS5_URI_PATTERN.matcher(text);
        while (socksMatcher.find()) {
            try {
                String user = socksMatcher.group(1);
                String pass = socksMatcher.group(2);
                String host = socksMatcher.group(3);
                int port = Integer.parseInt(socksMatcher.group(4));
                ProxySettings settings = ProxySettings.builder()
                        .setAddress(host)
                        .setPort(port)
                        .setUser(user != null ? user : "")
                        .setPassword(pass != null ? pass : "")
                        .setType(ProxySettings.Type.SOCKS5)
                        .build();
                String key = getSettingsKey(settings);
                if (seen.add(key)) {
                    result.add(settings);
                }
            } catch (Exception ignore) {}
        }

        // 3. Scan line by line for structured lines (host:port:secret, host:port:user:pass, host:port)
        String[] lines = text.split("\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (TextUtils.isEmpty(line)) continue;

            ProxySettings s = parseProxyLine(line);
            if (s != null) {
                String key = getSettingsKey(s);
                if (seen.add(key)) {
                    result.add(s);
                }
            }
        }

        return result;
    }

    private static void processProxyText(String text, ImportCallback callback) {
        SharedConfig.loadProxyList();
        Set<String> existingKeys = new HashSet<>();
        for (SharedConfig.ProxyInfo info : SharedConfig.proxyList) {
            if (info != null && info.settings != null) {
                existingKeys.add(getSettingsKey(info.settings));
            }
        }

        List<ProxySettings> extracted = extractAllProxies(text);
        List<SharedConfig.ProxyInfo> toAdd = new ArrayList<>();
        int duplicates = 0;

        for (int i = 0; i < extracted.size(); i++) {
            ProxySettings settings = extracted.get(i);
            String key = getSettingsKey(settings);
            if (!existingKeys.contains(key)) {
                existingKeys.add(key);
                toAdd.add(new SharedConfig.ProxyInfo(settings));
            } else {
                duplicates++;
            }

            if ((i + 1) % 50 == 0 && callback != null) {
                final int p = i + 1;
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

    private static String getSettingsKey(ProxySettings settings) {
        if (settings == null) return "";
        if (settings.getType() == ProxySettings.Type.WEB) {
            return "web:" + settings.getAddress();
        }
        return settings.getAddress() + ":" + settings.getPort();
    }

    public static ProxySettings parseProxyUrl(String url) {
        if (TextUtils.isEmpty(url)) return null;
        try {
            Uri uri = Uri.parse(url);
            ProxySettings built = ProxySettings.builder(uri).build();
            if (built != null) {
                return built;
            }

            String server = uri.getQueryParameter("server");
            String portStr = uri.getQueryParameter("port");
            String secret = uri.getQueryParameter("secret");
            String user = uri.getQueryParameter("user");
            String pass = uri.getQueryParameter("pass");

            if (!TextUtils.isEmpty(server) && !TextUtils.isEmpty(portStr)) {
                int port = Integer.parseInt(portStr);
                if (url.contains("socks")) {
                    return ProxySettings.builder()
                            .setAddress(server)
                            .setPort(port)
                            .setUser(user != null ? user : "")
                            .setPassword(pass != null ? pass : "")
                            .setType(ProxySettings.Type.SOCKS5)
                            .build();
                } else if (url.contains("webproxy")) {
                    return ProxySettings.builder()
                            .setAddress(server)
                            .setPort(port)
                            .setSecret(secret != null ? secret : "")
                            .setType(ProxySettings.Type.WEB)
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
        return null;
    }

    public static ProxySettings parseProxyLine(String line) {
        if (TextUtils.isEmpty(line)) return null;
        line = line.trim();

        if (line.startsWith("tg://") || line.startsWith("https://t.me/") || line.startsWith("http://t.me/")) {
            return parseProxyUrl(line);
        }

        Matcher m = COLON_LINE_PATTERN.matcher(line);
        if (m.matches()) {
            String host = m.group(1);
            int port;
            try {
                port = Integer.parseInt(m.group(2));
            } catch (Exception e) {
                return null;
            }
            String p3 = m.group(3);
            String p4 = m.group(4);

            if (p3 == null) {
                // host:port (SOCKS5 unauthenticated)
                return ProxySettings.builder()
                        .setAddress(host)
                        .setPort(port)
                        .setType(ProxySettings.Type.SOCKS5)
                        .build();
            } else if (p4 == null) {
                // host:port:secret (MTProto)
                return ProxySettings.builder()
                        .setAddress(host)
                        .setPort(port)
                        .setSecret(p3)
                        .setType(ProxySettings.Type.MTPROTO)
                        .build();
            } else {
                // host:port:user:pass (SOCKS5 authenticated)
                return ProxySettings.builder()
                        .setAddress(host)
                        .setPort(port)
                        .setUser(p3)
                        .setPassword(p4)
                        .setType(ProxySettings.Type.SOCKS5)
                        .build();
            }
        }

        return null;
    }
}

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
            "(?:https?://(?:[a-zA-Z0-9.-]+\\.)?(?:t\\.me|telegram\\.me|telegram\\.dog)/(?:proxy|socks|webproxy)|tg://(?:proxy|socks|webproxy))\\?[^\\s\"'<>]+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SOCKS5_URI_PATTERN = Pattern.compile(
            "socks5://(?:([^:@\\s]+):([^@\\s]+)@)?([a-zA-Z0-9.-]+):(\\d{1,5})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RAW_QUERY_PATTERN = Pattern.compile(
            "(?:[?&]|\\b)(?:server|host)=([a-zA-Z0-9.-]+)&(?:[^\\s\"'<>]*&)?port=(\\d{1,5})(?:(?:[^\\s\"'<>]*&)?secret=([a-zA-Z0-9_=+~/-]+)|(?:(?:[^\\s\"'<>]*&)?user=([^&\\s\"'<>]+)&pass=([^&\\s\"'<>]+)))?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COLON_PATTERN = Pattern.compile(
            "\\b((?:\\d{1,3}\\.){3}\\d{1,3}|[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}):(\\d{1,5})(?::([0-9a-fA-F]{32,}|[a-zA-Z0-9_=-]{16,}))?(?::([^\\s:]+))?\\b"
    );

    private static final Pattern PERSIAN_KV_PATTERN = Pattern.compile(
            "(?:سرور|server)\\s*[:=]\\s*([a-zA-Z0-9.-]+)[^\\n\\r]*?(?:پورت|port)\\s*[:=]\\s*(\\d{1,5})(?:[^\\n\\r]*?(?:سکرت|secret|رمز)\\s*[:=]\\s*([a-zA-Z0-9_=-]+))?",
            Pattern.CASE_INSENSITIVE
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

        // 1. Scan for Telegram URLs (https://t.me/proxy, tg://proxy, webproxy, socks)
        Matcher tgMatcher = TG_LINK_PATTERN.matcher(text);
        while (tgMatcher.find()) {
            String url = cleanValue(tgMatcher.group());
            ProxySettings settings = parseProxyUrl(url);
            if (settings != null && isValidProxySettings(settings)) {
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
                String user = cleanValue(socksMatcher.group(1));
                String pass = cleanValue(socksMatcher.group(2));
                String host = cleanValue(socksMatcher.group(3));
                int port = Integer.parseInt(cleanValue(socksMatcher.group(4)));
                if (isValidHostAndPort(host, port)) {
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
                }
            } catch (Exception ignore) {}
        }

        // 3. Scan for raw query parameters (server=...&port=...&secret=...)
        Matcher queryMatcher = RAW_QUERY_PATTERN.matcher(text);
        while (queryMatcher.find()) {
            try {
                String host = cleanValue(queryMatcher.group(1));
                int port = Integer.parseInt(cleanValue(queryMatcher.group(2)));
                String secret = cleanValue(queryMatcher.group(3));
                String user = cleanValue(queryMatcher.group(4));
                String pass = cleanValue(queryMatcher.group(5));

                if (isValidHostAndPort(host, port)) {
                    ProxySettings.Type type;
                    if (!TextUtils.isEmpty(secret)) {
                        type = ProxySettings.Type.MTPROTO;
                    } else {
                        type = ProxySettings.Type.SOCKS5;
                    }
                    ProxySettings settings = ProxySettings.builder()
                            .setAddress(host)
                            .setPort(port)
                            .setSecret(secret != null ? secret : "")
                            .setUser(user != null ? user : "")
                            .setPassword(pass != null ? pass : "")
                            .setType(type)
                            .build();
                    String key = getSettingsKey(settings);
                    if (seen.add(key)) {
                        result.add(settings);
                    }
                }
            } catch (Exception ignore) {}
        }

        // 4. Scan for Persian / key-value format (سرور: ... پورت: ... سکرت: ...)
        Matcher pkvMatcher = PERSIAN_KV_PATTERN.matcher(text);
        while (pkvMatcher.find()) {
            try {
                String host = cleanValue(pkvMatcher.group(1));
                int port = Integer.parseInt(cleanValue(pkvMatcher.group(2)));
                String secret = cleanValue(pkvMatcher.group(3));

                if (isValidHostAndPort(host, port)) {
                    ProxySettings.Type type = !TextUtils.isEmpty(secret) ? ProxySettings.Type.MTPROTO : ProxySettings.Type.SOCKS5;
                    ProxySettings settings = ProxySettings.builder()
                            .setAddress(host)
                            .setPort(port)
                            .setSecret(secret != null ? secret : "")
                            .setType(type)
                            .build();
                    String key = getSettingsKey(settings);
                    if (seen.add(key)) {
                        result.add(settings);
                    }
                }
            } catch (Exception ignore) {}
        }

        // 5. Scan for colon separated patterns (host:port:secret or host:port)
        Matcher colMatcher = COLON_PATTERN.matcher(text);
        while (colMatcher.find()) {
            try {
                String host = cleanValue(colMatcher.group(1));
                int port = Integer.parseInt(cleanValue(colMatcher.group(2)));
                String p3 = cleanValue(colMatcher.group(3));
                String p4 = cleanValue(colMatcher.group(4));

                if (isValidHostAndPort(host, port)) {
                    ProxySettings settings;
                    if (TextUtils.isEmpty(p3)) {
                        settings = ProxySettings.builder()
                                .setAddress(host)
                                .setPort(port)
                                .setType(ProxySettings.Type.SOCKS5)
                                .build();
                    } else if (TextUtils.isEmpty(p4)) {
                        settings = ProxySettings.builder()
                                .setAddress(host)
                                .setPort(port)
                                .setSecret(p3)
                                .setType(ProxySettings.Type.MTPROTO)
                                .build();
                    } else {
                        settings = ProxySettings.builder()
                                .setAddress(host)
                                .setPort(port)
                                .setUser(p3)
                                .setPassword(p4)
                                .setType(ProxySettings.Type.SOCKS5)
                                .build();
                    }
                    String key = getSettingsKey(settings);
                    if (seen.add(key)) {
                        result.add(settings);
                    }
                }
            } catch (Exception ignore) {}
        }

        return result;
    }

    private static boolean isValidHostAndPort(String host, int port) {
        if (TextUtils.isEmpty(host) || port <= 0 || port > 65535) {
            return false;
        }
        if ("0.0.0.0".equals(host)) {
            return false;
        }
        return true;
    }

    private static boolean isValidProxySettings(ProxySettings settings) {
        if (settings == null) return false;
        return isValidHostAndPort(settings.getAddress(), settings.getPort());
    }

    private static String cleanValue(String val) {
        if (val == null) return null;
        val = val.trim();
        while (val.endsWith(".") || val.endsWith(",") || val.endsWith(")") || val.endsWith("]") ||
                val.endsWith("}") || val.endsWith(">\"") || val.endsWith(">") || val.endsWith("\"") ||
                val.endsWith("'") || val.endsWith("،") || val.endsWith("؛")) {
            val = val.substring(0, val.length() - 1).trim();
        }
        while (val.startsWith("(") || val.startsWith("[") || val.startsWith("{") ||
                val.startsWith("<") || val.startsWith("\"") || val.startsWith("'")) {
            val = val.substring(1).trim();
        }
        return val;
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

            if ((i + 1) % 25 == 0 && callback != null) {
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
        return settings.getType() + ":" + settings.getAddress() + ":" + settings.getPort() + ":" + (settings.getSecret() != null ? settings.getSecret() : "");
    }

    public static ProxySettings parseProxyUrl(String url) {
        if (TextUtils.isEmpty(url)) return null;
        try {
            Uri uri = Uri.parse(url);
            ProxySettings built = ProxySettings.fromUri(uri);
            if (built != null) {
                return built;
            }

            String server = cleanValue(uri.getQueryParameter("server"));
            if (TextUtils.isEmpty(server)) {
                server = cleanValue(uri.getQueryParameter("host"));
            }
            String portStr = cleanValue(uri.getQueryParameter("port"));
            String secret = cleanValue(uri.getQueryParameter("secret"));
            String user = cleanValue(uri.getQueryParameter("user"));
            String pass = cleanValue(uri.getQueryParameter("pass"));

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
}

package org.veyra.client.aether;

import android.os.Build;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

public class AetherDownloader {

    public interface DownloadListener {
        void onProgress(int percent, long downloadedBytes, long totalBytes);
        void onSuccess(File binaryFile);
        void onError(Exception error);
    }

    private static final String BASE_RELEASE_URL = "https://github.com/CluvexStudio/Aether/releases/download/v2.3.0/";

    public static String getTargetAssetFileName() {
        String[] abis = Build.SUPPORTED_ABIS;
        if (abis != null) {
            for (String abi : abis) {
                if ("arm64-v8a".equalsIgnoreCase(abi)) {
                    return "aether-android-arm64.tar.gz";
                } else if ("armeabi-v7a".equalsIgnoreCase(abi)) {
                    return "aether-android-armv7.tar.gz";
                } else if ("x86_64".equalsIgnoreCase(abi)) {
                    return "aether-android-x86_64.tar.gz";
                }
            }
        }
        return "aether-android-arm64.tar.gz";
    }

    public static File getAetherDirectory() {
        File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "aether");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getDownloadedBinaryFile() {
        return new File(getAetherDirectory(), "libaether.so");
    }

    public static void downloadCore(DownloadListener listener) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream in = null;
            try {
                String asset = getTargetAssetFileName();
                String downloadUrl = BASE_RELEASE_URL + asset;
                URL url = new URL(downloadUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Veyra Messenger)");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == 307 || responseCode == 308) {
                    String newUrl = connection.getHeaderField("Location");
                    connection.disconnect();
                    url = new URL(newUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(30000);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Veyra Messenger)");
                    responseCode = connection.getResponseCode();
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException("Server returned HTTP " + responseCode);
                }

                long totalBytes = connection.getContentLength();
                in = new BufferedInputStream(connection.getInputStream());

                File targetFile = getDownloadedBinaryFile();
                File tempArchive = new File(getAetherDirectory(), "core.tar.gz");

                FileOutputStream archiveOut = new FileOutputStream(tempArchive);
                byte[] buffer = new byte[16384];
                long downloaded = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    archiveOut.write(buffer, 0, read);
                    downloaded += read;
                    if (totalBytes > 0 && listener != null) {
                        int percent = (int) ((downloaded * 100) / totalBytes);
                        listener.onProgress(percent, downloaded, totalBytes);
                    }
                }
                archiveOut.flush();
                archiveOut.close();

                extractAetherBinary(tempArchive, targetFile);
                tempArchive.delete();

                if (!targetFile.exists() || targetFile.length() == 0) {
                    throw new IllegalStateException("Extraction failed: binary not found in archive");
                }

                targetFile.setReadable(true, false);
                targetFile.setExecutable(true, false);

                if (listener != null) {
                    listener.onSuccess(targetFile);
                }
            } catch (Exception e) {
                FileLog.e("AetherDownloader", e);
                if (listener != null) {
                    listener.onError(e);
                }
            } finally {
                if (in != null) {
                    try { in.close(); } catch (Exception ignore) {}
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }, "aether-downloader").start();
    }

    private static void extractAetherBinary(File tarGzFile, File destFile) throws Exception {
        GZIPInputStream gzip = new GZIPInputStream(new java.io.FileInputStream(tarGzFile));
        byte[] header = new byte[512];
        byte[] buffer = new byte[8192];

        boolean found = false;
        while (true) {
            int offset = 0;
            while (offset < 512) {
                int r = gzip.read(header, offset, 512 - offset);
                if (r < 0) break;
                offset += r;
            }
            if (offset < 512) break;

            boolean allZeros = true;
            for (byte b : header) {
                if (b != 0) {
                    allZeros = false;
                    break;
                }
            }
            if (allZeros) break;

            String name = new String(header, 0, 100).trim();
            int nullIdx = name.indexOf('\0');
            if (nullIdx >= 0) {
                name = name.substring(0, nullIdx);
            }

            String sizeStr = new String(header, 124, 12).trim();
            int sizeNull = sizeStr.indexOf('\0');
            if (sizeNull >= 0) {
                sizeStr = sizeStr.substring(0, sizeNull);
            }
            sizeStr = sizeStr.trim();
            long fileSize = 0;
            if (!sizeStr.isEmpty()) {
                try {
                    fileSize = Long.parseLong(sizeStr, 8);
                } catch (Exception ignore) {}
            }

            byte typeFlag = header[156];

            if (name.equals("aether") || name.endsWith("/aether")) {
                FileOutputStream fos = new FileOutputStream(destFile);
                long remaining = fileSize;
                while (remaining > 0) {
                    int toRead = (int) Math.min(buffer.length, remaining);
                    int r = gzip.read(buffer, 0, toRead);
                    if (r < 0) break;
                    fos.write(buffer, 0, r);
                    remaining -= r;
                }
                fos.flush();
                fos.close();
                found = true;
            } else {
                long remaining = fileSize;
                while (remaining > 0) {
                    long skipped = gzip.skip(remaining);
                    if (skipped <= 0) {
                        int r = gzip.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (r < 0) break;
                        remaining -= r;
                    } else {
                        remaining -= skipped;
                    }
                }
            }

            int pad = (int) ((512 - (fileSize % 512)) % 512);
            while (pad > 0) {
                long skipped = gzip.skip(pad);
                if (skipped <= 0) {
                    int r = gzip.read(buffer, 0, Math.min(buffer.length, pad));
                    if (r < 0) break;
                    pad -= r;
                } else {
                    pad -= (int) skipped;
                }
            }

            if (found) {
                break;
            }
        }
        gzip.close();
    }
}

/* Veyra Project */

package org.veyra.client;

import android.text.TextUtils;

import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class SecretMediaExport implements AutoCloseable {

    private final File file;
    private final boolean temporary;

    private SecretMediaExport(File file, boolean temporary) {
        this.file = file;
        this.temporary = temporary;
    }

    public File getFile() {
        return file;
    }

    @Override
    public void close() {
        if (temporary && file != null && file.exists() && !file.delete()) {
            file.deleteOnExit();
        }
    }

    public static SecretMediaExport open(int account, MessageObject message) throws IOException {
        if (message == null || message.messageOwner == null) {
            throw new IOException("no message");
        }

        File found = findMediaFile(account, message);
        if (found != null && found.isFile() && found.length() > 0) {
            if (!found.getName().endsWith(".enc")) {
                return new SecretMediaExport(found, false);
            }
            File keyFile = findKeyFile(found);
            if (keyFile != null && keyFile.isFile()) {
                File tmp = File.createTempFile("veyra_ttl_", "." + extension(message, found), FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE));
                decryptCtrCopy(found, keyFile, tmp);
                return new SecretMediaExport(tmp, true);
            }
        }

        throw new IOException("ttl media file is not on disk");
    }

    private static File findMediaFile(int account, MessageObject message) {
        if (!TextUtils.isEmpty(message.messageOwner.attachPath)) {
            File attach = new File(message.messageOwner.attachPath);
            if (attach.isFile() && attach.length() > 0) return attach;
            File enc = new File(message.messageOwner.attachPath + ".enc");
            if (enc.isFile() && enc.length() > 0) return enc;
        }

        File plain = FileLoader.getInstance(account).getPathToMessage(message.messageOwner);
        if (plain != null) {
            if (plain.isFile() && plain.length() > 0) return plain;
            File enc = new File(plain.getAbsolutePath() + ".enc");
            if (enc.isFile() && enc.length() > 0) return enc;

            File cacheEnc = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), plain.getName() + ".enc");
            if (cacheEnc.isFile() && cacheEnc.length() > 0) return cacheEnc;

            File cachePlain = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), plain.getName());
            if (cachePlain.isFile() && cachePlain.length() > 0) return cachePlain;
        }

        File cachePath = FileLoader.getInstance(account).getPathToMessage(message.messageOwner, true, false);
        if (cachePath != null) {
            if (cachePath.isFile() && cachePath.length() > 0) return cachePath;
            File enc = new File(cachePath.getAbsolutePath() + ".enc");
            if (enc.isFile() && enc.length() > 0) return enc;
        }

        if (message.isPhoto() && message.photoThumbs != null) {
            for (int i = 0; i < message.photoThumbs.size(); i++) {
                TLRPC.PhotoSize size = message.photoThumbs.get(i);
                File f = FileLoader.getInstance(account).getPathToAttach(size, null, true, false);
                if (f != null) {
                    if (f.isFile() && f.length() > 0) return f;
                    File enc = new File(f.getAbsolutePath() + ".enc");
                    if (enc.isFile() && enc.length() > 0) return enc;
                }
            }
        }

        if (message.getDocument() != null) {
            File f = FileLoader.getInstance(account).getPathToAttach(message.getDocument(), null, true, false);
            if (f != null) {
                if (f.isFile() && f.length() > 0) return f;
                File enc = new File(f.getAbsolutePath() + ".enc");
                if (enc.isFile() && enc.length() > 0) return enc;
            }
        }
        return null;
    }

    private static File findKeyFile(File mediaFile) {
        if (mediaFile == null) return null;
        String name = mediaFile.getName();
        File internalDir = FileLoader.getInternalCacheDir();

        File k1 = new File(internalDir, name + ".key");
        if (k1.isFile() && k1.length() >= 48) return k1;

        if (name.endsWith(".enc")) {
            String withoutEnc = name.substring(0, name.length() - 4);
            File k2 = new File(internalDir, withoutEnc + ".key");
            if (k2.isFile() && k2.length() >= 48) return k2;
            File k3 = new File(internalDir, withoutEnc + ".enc.key");
            if (k3.isFile() && k3.length() >= 48) return k3;
        }

        String prefix = name.contains(".") ? name.substring(0, name.indexOf('.')) : name;
        File[] files = internalDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith(prefix) && f.getName().endsWith(".key") && f.length() >= 48) {
                    return f;
                }
            }
        }
        return null;
    }

    private static void decryptCtrCopy(File enc, File keyFile, File out) throws IOException {
        final byte[] key = new byte[32];
        final byte[] iv = new byte[16];
        try (RandomAccessFile raf = new RandomAccessFile(keyFile, "r")) {
            raf.readFully(key);
            raf.readFully(iv);
        }
        try (FileInputStream in = new FileInputStream(enc);
             FileOutputStream os = new FileOutputStream(out)) {
            final byte[] buf = new byte[64 * 1024];
            int offset = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                Utilities.aesCtrDecryptionByteArray(buf, key, iv, 0, n, offset);
                os.write(buf, 0, n);
                offset += n;
            }
            os.getFD().sync();
        }
    }

    private static String extension(MessageObject message, File fallback) {
        String name = message.getFileName();
        int dot = name != null ? name.lastIndexOf('.') : -1;
        if (dot >= 0 && dot < name.length() - 1) {
            return name.substring(dot + 1);
        }
        String fromPath = FileLoader.getFileExtension(fallback);
        if (TextExists(fromPath)) {
            return fromPath;
        }
        TLRPC.Document doc = message.getDocument();
        if (doc != null && doc.mime_type != null) {
            if (doc.mime_type.contains("video")) {
                return "mp4";
            }
            if (doc.mime_type.contains("gif")) {
                return "mp4";
            }
            if (doc.mime_type.contains("png")) {
                return "png";
            }
        }
        return message.isVideo() ? "mp4" : "jpg";
    }

    private static boolean TextExists(String s) {
        return s != null && !s.isEmpty();
    }
}

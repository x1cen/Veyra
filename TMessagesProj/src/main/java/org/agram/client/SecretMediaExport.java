/* AGram Project */

package org.agram.client;

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

        if (TextExists(message.messageOwner.attachPath)) {
            File attach = new File(message.messageOwner.attachPath);
            if (attach.isFile()) {
                return new SecretMediaExport(attach, false);
            }
        }

        File plain = FileLoader.getInstance(account).getPathToMessage(message.messageOwner);
        if (plain != null && plain.isFile()) {
            return new SecretMediaExport(plain, false);
        }

        if (plain != null) {
            File enc = new File(plain.getAbsolutePath() + ".enc");
            File key = new File(FileLoader.getInternalCacheDir(), plain.getName() + ".enc.key");
            if (enc.isFile() && key.isFile()) {
                File tmp = File.createTempFile("agram_ttl_", "." + extension(message, plain), FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE));
                decryptCtrCopy(enc, key, tmp);
                return new SecretMediaExport(tmp, true);
            }
        }

        throw new IOException("ttl media file is not on disk");
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

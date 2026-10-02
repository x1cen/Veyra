/* Veyra Project */

package org.veyra.client;

import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class VeyraMediaSaver {

    public static final int SAVE_TYPE_NONE = 0;
    public static final int SAVE_TYPE_GALLERY = 1;
    public static final int SAVE_TYPE_MUSIC = 2;
    public static final int SAVE_TYPE_DOWNLOADS = 3;

    public static final String FOLDER_VIDEO_NOTES = "Telegram Video Notes";
    public static final String FOLDER_VOICE = "Telegram Voice";
    public static final String FOLDER_AUDIO = "Telegram Audio";
    public static final String FOLDER_VIDEO = "Telegram Video";
    public static final String FOLDER_IMAGES = "Telegram Images";
    public static final String FOLDER_ANIMATIONS = "Telegram Animations";
    public static final String FOLDER_DOCUMENTS = "Telegram Documents";

    public static int getSaveOptionType(MessageObject messageObject) {
        if (messageObject == null) {
            return SAVE_TYPE_NONE;
        }
        if (messageObject.isPhoto() || messageObject.isVideo() || messageObject.isRoundVideo()
                || messageObject.isGif() || messageObject.isNewGif() || messageObject.isLivePhoto()) {
            return SAVE_TYPE_GALLERY;
        } else if (messageObject.isMusic()) {
            return SAVE_TYPE_MUSIC;
        } else if (messageObject.isVoice() || messageObject.isDocument() || messageObject.getDocument() != null) {
            return SAVE_TYPE_DOWNLOADS;
        }
        return SAVE_TYPE_NONE;
    }

    public static String getSubfolder(MessageObject messageObject) {
        if (messageObject == null) {
            return FOLDER_DOCUMENTS;
        }
        if (messageObject.isRoundVideo()) {
            return FOLDER_VIDEO_NOTES;
        }
        if (messageObject.isVoice()) {
            return FOLDER_VOICE;
        }
        if (messageObject.isMusic()) {
            return FOLDER_AUDIO;
        }
        if (messageObject.isVideo()) {
            return FOLDER_VIDEO;
        }
        if (messageObject.isPhoto() || messageObject.isLivePhoto()) {
            return FOLDER_IMAGES;
        }
        if (messageObject.isGif() || messageObject.isNewGif()) {
            return FOLDER_ANIMATIONS;
        }
        return FOLDER_DOCUMENTS;
    }

    public static File getExistingMediaFile(MessageObject messageObject, int currentAccount) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return null;
        }

        if (!TextUtils.isEmpty(messageObject.messageOwner.attachPath)) {
            File f = new File(messageObject.messageOwner.attachPath);
            if (f.isFile() && f.length() > 0) {
                return f;
            }
        }

        if (messageObject.cachedQuality != null && messageObject.cachedQuality.isCached()) {
            File f = new File(messageObject.cachedQuality.uri.getPath());
            if (f.isFile() && f.length() > 0) {
                return f;
            }
        }

        if (messageObject.qualityToSave != null) {
            File f = FileLoader.getInstance(currentAccount).getPathToAttach(messageObject.qualityToSave, null, false, true);
            if (f != null && f.isFile() && f.length() > 0) {
                return f;
            }
        }

        File plain = FileLoader.getInstance(currentAccount).getPathToMessage(messageObject.messageOwner, false, false);
        if (plain != null && plain.isFile() && plain.length() > 0) {
            return plain;
        }

        File cachePlain = FileLoader.getInstance(currentAccount).getPathToMessage(messageObject.messageOwner, true, false);
        if (cachePlain != null && cachePlain.isFile() && cachePlain.length() > 0) {
            return cachePlain;
        }

        TLRPC.Document doc = messageObject.getDocument();
        if (doc != null) {
            File f = FileLoader.getInstance(currentAccount).getPathToAttach(doc, null, false, false);
            if (f != null && f.isFile() && f.length() > 0) {
                return f;
            }
            f = FileLoader.getInstance(currentAccount).getPathToAttach(doc, null, true, false);
            if (f != null && f.isFile() && f.length() > 0) {
                return f;
            }
        }

        if (messageObject.isPhoto() && messageObject.photoThumbs != null && !messageObject.photoThumbs.isEmpty()) {
            TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, AndroidUtilities.getPhotoSize(true));
            if (size != null) {
                File f = FileLoader.getInstance(currentAccount).getPathToAttach(size, null, false, false);
                if (f != null && f.isFile() && f.length() > 0) {
                    return f;
                }
                f = FileLoader.getInstance(currentAccount).getPathToAttach(size, null, true, false);
                if (f != null && f.isFile() && f.length() > 0) {
                    return f;
                }
            }
        }

        // Encrypted / TTL file check
        if (plain != null) {
            File enc = new File(plain.getAbsolutePath() + ".enc");
            File key = new File(FileLoader.getInternalCacheDir(), plain.getName() + ".enc.key");
            if (enc.isFile() && enc.length() > 0 && key.isFile()) {
                return enc;
            }
        }
        if (cachePlain != null) {
            File enc = new File(cachePlain.getAbsolutePath() + ".enc");
            File key = new File(FileLoader.getInternalCacheDir(), cachePlain.getName() + ".enc.key");
            if (enc.isFile() && enc.length() > 0 && key.isFile()) {
                return enc;
            }
        }

        return null;
    }

    public static boolean canSave(MessageObject messageObject, int currentAccount) {
        if (messageObject == null || getSaveOptionType(messageObject) == SAVE_TYPE_NONE) {
            return false;
        }
        File f = getExistingMediaFile(messageObject, currentAccount);
        return f != null && f.exists() && f.length() > 0;
    }

    public static String getExtension(MessageObject messageObject, File file) {
        String name = null;
        if (messageObject != null) {
            if (messageObject.getDocument() != null) {
                name = FileLoader.getDocumentFileName(messageObject.getDocument());
            }
            if (TextUtils.isEmpty(name)) {
                name = messageObject.getFileName();
            }
        }
        if (!TextUtils.isEmpty(name)) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) {
                return name.substring(dot + 1).toLowerCase(Locale.US);
            }
        }
        if (file != null) {
            String ext = FileLoader.getFileExtension(file);
            if (!TextUtils.isEmpty(ext) && !ext.equalsIgnoreCase("enc") && !ext.equalsIgnoreCase("temp")) {
                return ext.toLowerCase(Locale.US);
            }
        }
        if (messageObject != null) {
            if (messageObject.isRoundVideo() || messageObject.isVideo() || messageObject.isGif() || messageObject.isNewGif()) {
                return "mp4";
            }
            if (messageObject.isVoice()) {
                return "ogg";
            }
            if (messageObject.isMusic()) {
                return "mp3";
            }
            if (messageObject.isPhoto() || messageObject.isLivePhoto()) {
                return "jpg";
            }
            TLRPC.Document doc = messageObject.getDocument();
            if (doc != null && doc.mime_type != null) {
                String extFromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(doc.mime_type);
                if (!TextUtils.isEmpty(extFromMime)) {
                    return extFromMime.toLowerCase(Locale.US);
                }
            }
        }
        return "bin";
    }

    public static String getMimeType(MessageObject messageObject, String extension) {
        if (messageObject != null && messageObject.getDocument() != null && !TextUtils.isEmpty(messageObject.getDocument().mime_type)) {
            return messageObject.getDocument().mime_type;
        }
        if (!TextUtils.isEmpty(extension)) {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.US));
            if (!TextUtils.isEmpty(mime)) {
                return mime;
            }
            switch (extension.toLowerCase(Locale.US)) {
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "webp":
                    return "image/webp";
                case "mp4":
                    return "video/mp4";
                case "mkv":
                    return "video/x-matroska";
                case "ogg":
                case "oga":
                case "opus":
                    return "audio/ogg";
                case "mp3":
                    return "audio/mpeg";
                case "pdf":
                    return "application/pdf";
                case "apk":
                    return "application/vnd.android.package-archive";
            }
        }
        return "application/octet-stream";
    }

    public static File getUniqueFile(File dir, String filename) {
        File file = new File(dir, filename);
        if (!file.exists()) {
            return file;
        }
        String nameWithoutExt = filename;
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) {
            nameWithoutExt = filename.substring(0, dot);
            ext = filename.substring(dot);
        }
        for (int i = 1; i < 1000; i++) {
            File candidate = new File(dir, nameWithoutExt + " (" + i + ")" + ext);
            if (!candidate.exists()) {
                return candidate;
            }
        }
        return file;
    }

    public static void saveMedia(Context context, int currentAccount, MessageObject messageObject, Utilities.Callback<Uri> onSaved, Runnable onError) {
        if (messageObject == null) {
            if (onError != null) {
                AndroidUtilities.runOnUIThread(onError);
            }
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            SecretMediaExport export = null;
            File sourceFile = null;
            try {
                File candidate = getExistingMediaFile(messageObject, currentAccount);
                if (candidate != null && candidate.getName().endsWith(".enc")) {
                    try {
                        export = SecretMediaExport.open(currentAccount, messageObject);
                        sourceFile = export.getFile();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                } else if (candidate != null && candidate.isFile()) {
                    sourceFile = candidate;
                }

                if (sourceFile == null || !sourceFile.isFile() || sourceFile.length() <= 0) {
                    try {
                        export = SecretMediaExport.open(currentAccount, messageObject);
                        sourceFile = export.getFile();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }

                if (sourceFile == null || !sourceFile.isFile() || sourceFile.length() <= 0) {
                    if (onError != null) {
                        AndroidUtilities.runOnUIThread(onError);
                    }
                    return;
                }

                String ext = getExtension(messageObject, sourceFile);
                String mimeType = getMimeType(messageObject, ext);

                String displayName = null;
                if (messageObject.getDocument() != null) {
                    displayName = FileLoader.getDocumentFileName(messageObject.getDocument());
                }
                if (TextUtils.isEmpty(displayName)) {
                    displayName = messageObject.getFileName();
                }
                if (TextUtils.isEmpty(displayName) || displayName.endsWith(".temp") || displayName.endsWith(".enc") || !displayName.contains(".")) {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                    if (messageObject.isRoundVideo()) {
                        displayName = "VID_NOTE_" + timeStamp + "." + ext;
                    } else if (messageObject.isVoice()) {
                        displayName = "VOICE_" + timeStamp + "." + ext;
                    } else if (messageObject.isVideo()) {
                        displayName = "VID_" + timeStamp + "." + ext;
                    } else if (messageObject.isPhoto()) {
                        displayName = "IMG_" + timeStamp + "." + ext;
                    } else if (messageObject.isMusic()) {
                        displayName = "AUD_" + timeStamp + "." + ext;
                    } else {
                        displayName = "DOC_" + timeStamp + "." + ext;
                    }
                } else {
                    if (displayName.endsWith(".temp") || displayName.endsWith(".enc")) {
                        displayName = displayName.substring(0, displayName.lastIndexOf('.')) + "." + ext;
                    }
                }

                String subfolder = getSubfolder(messageObject);
                Uri finalUri = null;

                // 1. Try direct File write to Documents/Telegram/<Subfolder>
                try {
                    File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    File telegramDir = new File(documentsDir, "Telegram");
                    File targetDir = new File(telegramDir, subfolder);
                    if (!targetDir.exists()) {
                        targetDir.mkdirs();
                    }
                    if (targetDir.exists() && targetDir.canWrite()) {
                        File destFile = getUniqueFile(targetDir, displayName);
                        try (FileInputStream in = new FileInputStream(sourceFile);
                             FileOutputStream out = new FileOutputStream(destFile)) {
                            FileChannel inChannel = in.getChannel();
                            FileChannel outChannel = out.getChannel();
                            inChannel.transferTo(0, inChannel.size(), outChannel);
                            out.getFD().sync();
                        }
                        if (destFile.exists() && destFile.length() > 0) {
                            finalUri = Uri.fromFile(destFile);
                            MediaScannerConnection.scanFile(
                                    ApplicationLoader.applicationContext,
                                    new String[]{destFile.getAbsolutePath()},
                                    new String[]{mimeType},
                                    null
                            );
                            AndroidUtilities.addMediaToGallery(destFile);
                        }
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                    finalUri = null;
                }

                // 2. Scoped Storage / MediaStore fallback for Android 10+ (API 29+)
                if (finalUri == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        ContentValues values = new ContentValues();
                        String relativePath = Environment.DIRECTORY_DOCUMENTS + File.separator + "Telegram" + File.separator + subfolder + File.separator;
                        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
                        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);

                        Uri volume = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                        Uri insertedUri = ApplicationLoader.applicationContext.getContentResolver().insert(volume, values);
                        if (insertedUri != null) {
                            try (InputStream in = new FileInputStream(sourceFile);
                                 OutputStream out = ApplicationLoader.applicationContext.getContentResolver().openOutputStream(insertedUri)) {
                                AndroidUtilities.copyFile(in, out);
                            }
                            finalUri = insertedUri;
                        }
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                }

                if (export != null) {
                    export.close();
                }

                final Uri resultUri = finalUri;
                AndroidUtilities.runOnUIThread(() -> {
                    if (resultUri != null) {
                        if (onSaved != null) {
                            onSaved.run(resultUri);
                        }
                    } else {
                        if (onError != null) {
                            onError.run();
                        }
                    }
                });

            } catch (Throwable e) {
                FileLog.e(e);
                if (export != null) {
                    export.close();
                }
                if (onError != null) {
                    AndroidUtilities.runOnUIThread(onError);
                }
            }
        });
    }

    public static void saveMediaBatch(Context context, int currentAccount, List<MessageObject> messageObjects, Utilities.Callback<Integer> onComplete) {
        if (messageObjects == null || messageObjects.isEmpty()) {
            if (onComplete != null) {
                AndroidUtilities.runOnUIThread(() -> onComplete.run(0));
            }
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            int savedCount = 0;
            for (MessageObject msg : messageObjects) {
                if (msg == null) continue;
                final Object lock = new Object();
                final boolean[] success = new boolean[1];
                saveMedia(context, currentAccount, msg, uri -> {
                    synchronized (lock) {
                        success[0] = true;
                        lock.notifyAll();
                    }
                }, () -> {
                    synchronized (lock) {
                        success[0] = false;
                        lock.notifyAll();
                    }
                });
                synchronized (lock) {
                    try {
                        lock.wait(15000);
                    } catch (InterruptedException ignored) {}
                }
                if (success[0]) {
                    savedCount++;
                }
            }
            final int totalSaved = savedCount;
            AndroidUtilities.runOnUIThread(() -> {
                if (onComplete != null) {
                    onComplete.run(totalSaved);
                }
            });
        });
    }
}

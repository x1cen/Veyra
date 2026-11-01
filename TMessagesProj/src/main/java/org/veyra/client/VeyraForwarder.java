package org.veyra.client;

import android.text.TextUtils;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class VeyraForwarder {

    public static boolean isVeyraForwardNeeded(int currentAccount, ArrayList<MessageObject> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        long dialogId = messages.get(0).getDialogId();
        if (org.telegram.messenger.DialogObject.isEncryptedDialog(dialogId)) {
            return true;
        }
        if (dialogId < 0) {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            if (chat != null && chat.noforwards) {
                return true;
            }
        } else if (dialogId > 0) {
            if (MessagesController.getInstance(currentAccount).isPeerNoForwards(dialogId)) {
                return true;
            }
        }

        for (int i = 0; i < messages.size(); i++) {
            MessageObject msg = messages.get(i);
            if (msg == null) continue;
            if (msg.messageOwner instanceof TLRPC.TL_message_secret || msg.isSecret() || org.telegram.messenger.DialogObject.isEncryptedDialog(msg.getDialogId())) {
                return true;
            }
            if (msg.isDeleted()) {
                return true;
            }
            if (msg.messageOwner != null && msg.messageOwner.noforwards) {
                return true;
            }
            if (msg.needDrawBluredPreview() || msg.isRoundOnce() || msg.isVoiceOnce()) {
                return true;
            }
        }
        return false;
    }

    public static void startForward(
            int currentAccount,
            ArrayList<MessageObject> messages,
            long peer,
            boolean forwardFromMyName,
            boolean hideCaption,
            boolean notify,
            int scheduleDate,
            MessageObject replyToTopMsg
    ) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        final ArrayList<MessageObject> list = new ArrayList<>(messages);
        Utilities.globalQueue.postRunnable(() -> {
            try {
                forwardMessagesInternal(currentAccount, list, peer, forwardFromMyName, hideCaption, notify, scheduleDate, replyToTopMsg);
            } catch (Throwable t) {
                FileLog.e(t);
            }
        });
    }

    private static void forwardMessagesInternal(
            int currentAccount,
            ArrayList<MessageObject> messages,
            long peer,
            boolean forwardFromMyName,
            boolean hideCaption,
            boolean notify,
            int scheduleDate,
            MessageObject replyToTopMsg
    ) {
        ArrayList<MessageObject> toBeDownloaded = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            MessageObject msg = messages.get(i);
            if (msg == null) continue;

            if (isMediaDownloadable(msg)) {
                File path = getMessageFilePath(currentAccount, msg);
                if (path == null || !path.exists() || path.length() == 0) {
                    toBeDownloaded.add(msg);
                }
            }
        }

        if (!toBeDownloaded.isEmpty()) {
            downloadMediaSync(currentAccount, toBeDownloaded);
        }

        final AccountInstance accountInstance = AccountInstance.getInstance(currentAccount);

        int i = 0;
        while (i < messages.size()) {
            MessageObject msg = messages.get(i);
            if (msg == null || msg.messageOwner == null) {
                i++;
                continue;
            }

            CharSequence textSeq = msg.messageText;
            String text = (msg.messageOwner != null && msg.messageOwner.message != null && !msg.isSticker() && !msg.isAnimatedSticker() && !isVisualMedia(msg)) ? msg.messageOwner.message : "";
            if (hideCaption && isMediaDownloadable(msg)) {
                text = "";
            }
            ArrayList<TLRPC.MessageEntity> entities = hideCaption ? null : msg.messageOwner.entities;

            if (msg.isRoundVideo()) {
                File itemPath = getMessageFilePath(currentAccount, msg);
                if (itemPath != null && itemPath.exists()) {
                    org.telegram.messenger.VideoEditedInfo videoEditedInfo = new org.telegram.messenger.VideoEditedInfo();
                    videoEditedInfo.roundVideo = true;
                    videoEditedInfo.startTime = -1;
                    videoEditedInfo.endTime = -1;
                    SendMessagesHelper.SendMessageParams sendParams = SendMessagesHelper.SendMessageParams.of(
                            (TLRPC.TL_document) msg.getDocument(),
                            videoEditedInfo,
                            itemPath.getAbsolutePath(),
                            peer,
                            null,
                            replyToTopMsg,
                            null,
                            null,
                            null,
                            null,
                            notify,
                            scheduleDate,
                            0,
                            0,
                            null,
                            null,
                            false,
                            false
                    );
                    AndroidUtilities.runOnUIThread(() -> SendMessagesHelper.getInstance(currentAccount).sendMessage(sendParams));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignored) {}
                }
                i++;
            } else if (msg.isVoice()) {
                File itemPath = getMessageFilePath(currentAccount, msg);
                if (itemPath != null && itemPath.exists()) {
                    SendMessagesHelper.SendMessageParams sendParams = SendMessagesHelper.SendMessageParams.of(
                            (TLRPC.TL_document) msg.getDocument(),
                            null,
                            itemPath.getAbsolutePath(),
                            peer,
                            null,
                            replyToTopMsg,
                            null,
                            null,
                            null,
                            null,
                            notify,
                            scheduleDate,
                            0,
                            0,
                            null,
                            null,
                            false,
                            false
                    );
                    AndroidUtilities.runOnUIThread(() -> SendMessagesHelper.getInstance(currentAccount).sendMessage(sendParams));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignored) {}
                }
                i++;
            } else if (msg.isSticker() || msg.isAnimatedSticker()) {
                TLRPC.Document document = msg.getDocument();
                if (document instanceof TLRPC.TL_document && !msg.isSecret()) {
                    AndroidUtilities.runOnUIThread(() -> {
                        SendMessagesHelper.getInstance(currentAccount).sendSticker(
                                document,
                                null,
                                peer,
                                null,
                                replyToTopMsg,
                                null,
                                null,
                                null,
                                notify,
                                scheduleDate,
                                0,
                                true,
                                null,
                                null,
                                0,
                                0,
                                null
                        );
                    });
                } else {
                    File itemPath = getMessageFilePath(currentAccount, msg);
                    if (itemPath != null && itemPath.exists()) {
                        String mime = "image/webp";
                        if (itemPath.getName().endsWith(".tgs")) {
                            mime = "application/x-tgsticker";
                        } else if (itemPath.getName().endsWith(".webm")) {
                            mime = "video/webm";
                        }
                        final String finalMime = mime;
                        AndroidUtilities.runOnUIThread(() -> {
                            SendMessagesHelper.prepareSendingDocument(
                                    accountInstance,
                                    itemPath.getAbsolutePath(),
                                    itemPath.getAbsolutePath(),
                                    null,
                                    null,
                                    finalMime,
                                    peer,
                                    null,
                                    replyToTopMsg,
                                    null,
                                    null,
                                    null,
                                    notify,
                                    scheduleDate,
                                    null,
                                    null,
                                    false
                            );
                        });
                    }
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {}
                i++;
            } else if (isVisualMedia(msg)) {
                ArrayList<SendMessagesHelper.SendingMediaInfo> albumMedia = new ArrayList<>();
                long currentGroupId = msg.getGroupId();

                while (i < messages.size()) {
                    MessageObject item = messages.get(i);
                    if (item == null || item.messageOwner == null || !isVisualMedia(item)) {
                        break;
                    }
                    if (currentGroupId != 0 && item.getGroupId() != currentGroupId && !albumMedia.isEmpty()) {
                        break;
                    }
                    if (currentGroupId == 0 && !albumMedia.isEmpty()) {
                        break;
                    }

                    File itemPath = getMessageFilePath(currentAccount, item);
                    if (itemPath != null && itemPath.exists()) {
                        SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
                        info.path = itemPath.getAbsolutePath();
                        String itemCaption = hideCaption || TextUtils.isEmpty(item.caption) ? "" : item.caption.toString();
                        info.caption = itemCaption;
                        info.entities = hideCaption || TextUtils.isEmpty(item.caption) ? null : item.messageOwner.entities;
                        info.isVideo = item.isVideo();
                        info.hasMediaSpoilers = item.hasMediaSpoilers();
                        albumMedia.add(info);
                    }
                    i++;
                    if (currentGroupId == 0) {
                        break;
                    }
                }

                if (!albumMedia.isEmpty()) {
                    final ArrayList<SendMessagesHelper.SendingMediaInfo> finalMedia = albumMedia;
                    final boolean isGroup = finalMedia.size() > 1;
                    AndroidUtilities.runOnUIThread(() -> {
                        SendMessagesHelper.prepareSendingMedia(
                                accountInstance,
                                finalMedia,
                                peer,
                                null,
                                replyToTopMsg,
                                null,
                                null,
                                false,
                                isGroup,
                                null,
                                notify,
                                scheduleDate,
                                0,
                                0,
                                false,
                                null,
                                null,
                                0,
                                false,
                                0,
                                0,
                                null
                        );
                    });
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignored) {}
                }
            } else if (isDocumentOrAudio(msg)) {
                File itemPath = getMessageFilePath(currentAccount, msg);
                if (itemPath != null && itemPath.exists()) {
                    SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
                    info.path = itemPath.getAbsolutePath();
                    String itemCaption = hideCaption ? "" : (!TextUtils.isEmpty(msg.caption) ? msg.caption.toString() : text);
                    info.caption = itemCaption;
                    info.entities = entities;
                    info.hasMediaSpoilers = msg.hasMediaSpoilers();

                    ArrayList<SendMessagesHelper.SendingMediaInfo> docList = new ArrayList<>();
                    docList.add(info);

                    AndroidUtilities.runOnUIThread(() -> {
                        SendMessagesHelper.prepareSendingMedia(
                                accountInstance,
                                docList,
                                peer,
                                null,
                                replyToTopMsg,
                                null,
                                null,
                                true,
                                false,
                                null,
                                notify,
                                scheduleDate,
                                0,
                                0,
                                false,
                                null,
                                null,
                                0,
                                false,
                                0,
                                0,
                                null
                        );
                    });
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignored) {}
                }
                i++;
            } else if (!TextUtils.isEmpty(text)) {
                TLRPC.WebPage webPage = (msg.messageOwner.media != null) ? msg.messageOwner.media.webpage : null;
                final SendMessagesHelper.SendMessageParams sendParams = SendMessagesHelper.SendMessageParams.of(
                        text,
                        peer,
                        null,
                        replyToTopMsg,
                        webPage,
                        webPage != null,
                        entities,
                        null,
                        null,
                        notify,
                        scheduleDate,
                        0,
                        null,
                        false
                );
                AndroidUtilities.runOnUIThread(() -> SendMessagesHelper.getInstance(currentAccount).sendMessage(sendParams));
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ignored) {}
                i++;
            } else {
                i++;
            }
        }
    }

    private static File getMessageFilePath(int currentAccount, MessageObject message) {
        if (message == null || message.messageOwner == null) return null;
        if (message.messageOwner instanceof TLRPC.TL_message_secret || message.isSecret()) {
            try {
                SecretMediaExport export = SecretMediaExport.open(currentAccount, message);
                if (export != null && export.getFile() != null && export.getFile().exists()) {
                    return export.getFile();
                }
            } catch (Throwable ignored) {}
        }
        File file = FileLoader.getInstance(currentAccount).getPathToMessage(message.messageOwner);
        if (file != null && file.exists() && file.length() > 0) {
            return file;
        }
        if (!TextUtils.isEmpty(message.messageOwner.attachPath)) {
            File f = new File(message.messageOwner.attachPath);
            if (f.exists() && f.length() > 0) {
                return f;
            }
        }
        if (message.getDocument() != null) {
            File f = FileLoader.getInstance(currentAccount).getPathToAttach(message.getDocument());
            if (f != null && f.exists() && f.length() > 0) {
                return f;
            }
        }
        if (message.photoThumbs != null && !message.photoThumbs.isEmpty()) {
            TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(message.photoThumbs, AndroidUtilities.getPhotoSize(true), false, null, true);
            if (size != null) {
                File f = FileLoader.getInstance(currentAccount).getPathToAttach(size);
                if (f != null && f.exists() && f.length() > 0) {
                    return f;
                }
            }
        }
        return file;
    }

    private static void downloadMediaSync(int currentAccount, ArrayList<MessageObject> toBeDownloaded) {
        if (toBeDownloaded == null || toBeDownloaded.isEmpty()) return;
        final CountDownLatch latch = new CountDownLatch(toBeDownloaded.size());
        final NotificationCenter.NotificationCenterDelegate delegate = (id, account, args) -> {
            if (id == NotificationCenter.fileLoaded || id == NotificationCenter.fileLoadFailed) {
                latch.countDown();
            }
        };

        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(currentAccount).addObserver(delegate, NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(currentAccount).addObserver(delegate, NotificationCenter.fileLoadFailed);
            for (int i = 0; i < toBeDownloaded.size(); i++) {
                MessageObject msg = toBeDownloaded.get(i);
                TLRPC.MessageMedia media = MessageObject.getMedia(msg.messageOwner);
                if (media instanceof TLRPC.TL_messageMediaPhoto && media.photo != null) {
                    TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(media.photo.sizes, AndroidUtilities.getPhotoSize(true), false, null, true);
                    ImageLocation loc = ImageLocation.getForPhoto(size, media.photo);
                    if (loc != null) {
                        FileLoader.getInstance(currentAccount).loadFile(loc, msg, null, FileLoader.PRIORITY_HIGH, 0);
                    } else {
                        latch.countDown();
                    }
                } else if (media instanceof TLRPC.TL_messageMediaDocument && media.document != null) {
                    FileLoader.getInstance(currentAccount).loadFile(media.document, msg, FileLoader.PRIORITY_HIGH, 0);
                } else if (msg.getDocument() != null) {
                    FileLoader.getInstance(currentAccount).loadFile(msg.getDocument(), msg, FileLoader.PRIORITY_HIGH, 0);
                } else {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(currentAccount).removeObserver(delegate, NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(currentAccount).removeObserver(delegate, NotificationCenter.fileLoadFailed);
        });
    }

    public static boolean isVisualMedia(MessageObject message) {
        if (message == null || message.messageOwner == null || message.messageOwner.media == null) {
            return false;
        }
        return (message.isPhoto() || message.isVideo() || message.isGif() || message.isNewGif()) && !message.isRoundVideo();
    }

    public static boolean isDocumentOrAudio(MessageObject message) {
        if (message == null || message.messageOwner == null || message.messageOwner.media == null) {
            return false;
        }
        return (message.isDocument() || message.isMusic()) && !message.isVoice();
    }

    public static boolean isMediaDownloadable(MessageObject message) {
        if (message == null || message.messageOwner == null || message.messageOwner.media == null) {
            return false;
        }
        if (message.messageOwner.media.photo instanceof TLRPC.TL_photoEmpty) {
            return false;
        }
        if (message.messageOwner.media.document instanceof TLRPC.TL_documentEmpty) {
            return false;
        }
        if (MessageObject.isMediaEmpty(message.messageOwner)) {
            return false;
        }
        return isVisualMedia(message) || isDocumentOrAudio(message) || message.isRoundVideo() || message.isVoice() || message.isSticker() || message.isAnimatedSticker();
    }
}

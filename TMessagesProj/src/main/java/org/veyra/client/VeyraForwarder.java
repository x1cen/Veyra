package org.veyra.client;

import android.text.TextUtils;
import android.util.LongSparseArray;

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
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class VeyraForwarder {

    public static boolean isVeyraForwardNeeded(int currentAccount, ArrayList<MessageObject> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        long dialogId = messages.get(0).getDialogId();
        if (dialogId < 0) {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            if (chat != null && chat.noforwards) {
                return true;
            }
        } else if (dialogId > 0) {
            if (MessagesController.getInstance(currentAccount).isPeerNoForwards(dialogId)) {
                return true;
            }
            TLRPC.UserFull userFull = MessagesController.getInstance(currentAccount).getUserFull(dialogId);
            if (userFull != null && userFull.noforwards) {
                return true;
            }
        }

        for (int i = 0; i < messages.size(); i++) {
            MessageObject msg = messages.get(i);
            if (msg == null) continue;
            if (msg.isDeleted()) {
                return true;
            }
            if (msg.messageOwner != null && msg.messageOwner.noforwards) {
                return true;
            }
            if (msg.needDrawBluredPreview()) {
                return true;
            }
        }
        return false;
    }

    public static void startForward(int currentAccount, ArrayList<MessageObject> messages, long peer, boolean forwardFromMyName, boolean hideCaption, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
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
        LongSparseArray<Integer> groupCounts = new LongSparseArray<>();
        LongSparseArray<Long> groupNewIds = new LongSparseArray<>();

        for (int i = 0; i < messages.size(); i++) {
            MessageObject msg = messages.get(i);
            if (msg == null) continue;

            if (isMediaDownloadable(msg)) {
                File path = FileLoader.getInstance(currentAccount).getPathToMessage(msg.messageOwner);
                if (path == null || !path.exists() || path.length() == 0) {
                    toBeDownloaded.add(msg);
                }
            }

            long gid = msg.getGroupId();
            if (gid != 0) {
                Integer count = groupCounts.get(gid);
                groupCounts.put(gid, count == null ? 1 : count + 1);
                if (groupNewIds.get(gid) == null) {
                    groupNewIds.put(gid, Utilities.random.nextLong());
                }
            }
        }

        if (!toBeDownloaded.isEmpty()) {
            downloadMediaSync(currentAccount, toBeDownloaded);
        }

        for (int i = 0; i < messages.size(); i++) {
            MessageObject msg = messages.get(i);
            if (msg == null || msg.messageOwner == null) continue;

            CharSequence textSeq = msg.messageText;
            String text = textSeq != null ? textSeq.toString() : (msg.messageOwner.message != null ? msg.messageOwner.message : "");
            if (hideCaption && isMediaDownloadable(msg)) {
                text = "";
            }
            ArrayList<TLRPC.MessageEntity> entities = hideCaption ? null : msg.messageOwner.entities;

            long gid = msg.getGroupId();
            Long newGid = gid != 0 ? groupNewIds.get(gid) : null;
            boolean isFinalInGroup = false;
            if (gid != 0) {
                int left = groupCounts.get(gid) - 1;
                groupCounts.put(gid, left);
                isFinalInGroup = (left <= 0);
            }

            HashMap<String, String> params = null;
            if (newGid != null && newGid != 0L) {
                params = new HashMap<>();
                params.put("groupId", String.valueOf(newGid));
                if (isFinalInGroup) {
                    params.put("final", "true");
                }
            }

            File messagePath = FileLoader.getInstance(currentAccount).getPathToMessage(msg.messageOwner);
            String filePath = (messagePath != null && messagePath.exists()) ? messagePath.getAbsolutePath() : null;

            if (msg.isPhoto()) {
                TLRPC.Photo origPhoto = MessageObject.getPhoto(msg.messageOwner);
                TLRPC.TL_photo photo = mapPhoto(currentAccount, origPhoto, messagePath);
                String caption = hideCaption ? "" : (!TextUtils.isEmpty(msg.caption) ? msg.caption.toString() : text);
                final SendMessagesHelper.SendMessageParams sendParams = SendMessagesHelper.SendMessageParams.of(
                        photo,
                        filePath,
                        peer,
                        null,
                        replyToTopMsg,
                        caption,
                        entities,
                        null,
                        params,
                        notify,
                        scheduleDate,
                        0,
                        0,
                        null,
                        false,
                        msg.hasMediaSpoilers()
                );
                AndroidUtilities.runOnUIThread(() -> SendMessagesHelper.getInstance(currentAccount).sendMessage(sendParams));
            } else if (msg.getDocument() != null) {
                TLRPC.TL_document doc = mapDocument(currentAccount, msg.getDocument(), messagePath);
                String caption = hideCaption ? "" : (!TextUtils.isEmpty(msg.caption) ? msg.caption.toString() : text);
                final SendMessagesHelper.SendMessageParams sendParams = SendMessagesHelper.SendMessageParams.of(
                        doc,
                        null,
                        filePath,
                        peer,
                        null,
                        replyToTopMsg,
                        caption,
                        entities,
                        null,
                        params,
                        notify,
                        scheduleDate,
                        0,
                        0,
                        null,
                        null,
                        false,
                        msg.hasMediaSpoilers()
                );
                AndroidUtilities.runOnUIThread(() -> SendMessagesHelper.getInstance(currentAccount).sendMessage(sendParams));
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
                        params,
                        notify,
                        scheduleDate,
                        0,
                        null,
                        false
                );
                AndroidUtilities.runOnUIThread(() -> SendMessagesHelper.getInstance(currentAccount).sendMessage(sendParams));
            }

            if (newGid == null || isFinalInGroup) {
                try {
                    Thread.sleep(180);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private static void downloadMediaSync(int currentAccount, ArrayList<MessageObject> toBeDownloaded) {
        if (toBeDownloaded == null || toBeDownloaded.isEmpty()) return;
        final CountDownLatch latch = new CountDownLatch(toBeDownloaded.size());
        final NotificationCenter.NotificationCenterDelegate delegate = new NotificationCenter.NotificationCenterDelegate() {
            @Override
            public void didReceivedNotification(int id, int account, Object... args) {
                if (id == NotificationCenter.fileLoaded || id == NotificationCenter.fileLoadFailed) {
                    latch.countDown();
                }
            }
        };

        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(currentAccount).addObserver(delegate, NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(currentAccount).addObserver(delegate, NotificationCenter.fileLoadFailed);
            for (int i = 0; i < toBeDownloaded.size(); i++) {
                MessageObject msg = toBeDownloaded.get(i);
                TLRPC.Document doc = msg.getDocument();
                TLRPC.Photo photo = MessageObject.getPhoto(msg.messageOwner);
                if (doc != null) {
                    FileLoader.getInstance(currentAccount).loadFile(doc, msg, FileLoader.PRIORITY_HIGH, 0);
                } else if (photo != null) {
                    TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize());
                    ImageLocation loc = ImageLocation.getForPhoto(size, photo);
                    if (loc != null) {
                        FileLoader.getInstance(currentAccount).loadFile(loc, msg, null, FileLoader.PRIORITY_HIGH, 0);
                    } else {
                        latch.countDown();
                    }
                } else {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await(45, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(currentAccount).removeObserver(delegate, NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(currentAccount).removeObserver(delegate, NotificationCenter.fileLoadFailed);
        });
    }

    private static TLRPC.TL_document mapDocument(int currentAccount, TLRPC.Document doc, File messagePath) {
        TLRPC.TL_document newDoc = new TLRPC.TL_document();
        newDoc.id = doc.id;
        newDoc.access_hash = doc.access_hash;
        newDoc.file_reference = new byte[0];
        newDoc.dc_id = Integer.MIN_VALUE;
        newDoc.user_id = doc.user_id;
        newDoc.mime_type = doc.mime_type;
        newDoc.file_name = doc.file_name;
        newDoc.file_name_fixed = doc.file_name_fixed;
        newDoc.date = AccountInstance.getInstance(currentAccount).getConnectionsManager().getCurrentTime();
        newDoc.size = (int) (messagePath != null && messagePath.exists() ? messagePath.length() : doc.size);
        newDoc.attributes = doc.attributes;
        if (messagePath != null) {
            newDoc.localPath = messagePath.getAbsolutePath();
        }
        return newDoc;
    }

    private static TLRPC.TL_photo mapPhoto(int currentAccount, TLRPC.Photo photo, File messagePath) {
        SendMessagesHelper helper = SendMessagesHelper.getInstance(currentAccount);
        TLRPC.TL_photo newPhoto = null;
        if (messagePath != null && messagePath.exists()) {
            newPhoto = helper.generatePhotoSizes(messagePath.getAbsolutePath(), null);
        }
        if (newPhoto == null) {
            newPhoto = new TLRPC.TL_photo();
            if (photo != null) {
                newPhoto.sizes = photo.sizes;
            }
        }
        if (photo != null) {
            newPhoto.flags = photo.flags;
            newPhoto.has_stickers = photo.has_stickers;
            newPhoto.geo = photo.geo;
            newPhoto.caption = photo.caption;
        }
        newPhoto.date = AccountInstance.getInstance(currentAccount).getConnectionsManager().getCurrentTime();
        return newPhoto;
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
        return message.isSecretMedia() ||
                message.isGif() ||
                message.isNewGif() ||
                message.isRoundVideo() ||
                message.isVideo() ||
                message.isPhoto() ||
                message.isDocument() ||
                message.isMusic() ||
                message.isVoice();
    }
}

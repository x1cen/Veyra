package org.veyra.client;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
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

    private static final int DOWNLOAD_TIMEOUT_SECONDS = 90;

    public static void cancelCurrentDownload() {
        // isCancelled flag in VeyraForwardProgress is checked at every stage
    }

    public static boolean needsRestrictedForward(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return false;
        }
        if (messageObject.isVeyraOrAntiDeleted()) {
            return true;
        }
        final TLRPC.Message message = messageObject.messageOwner;
        if (message.noforwards) {
            return true;
        }
        final long dialogId = messageObject.getDialogId();
        if (dialogId < 0) {
            final TLRPC.Chat chat = MessagesController.getInstance(messageObject.currentAccount).getChat(-dialogId);
            if (chat != null && (chat.noforwards || MessagesController.getInstance(messageObject.currentAccount).isChatNoForwards(chat))) {
                return true;
            }
            if (chat != null && chat.migrated_to != null) {
                final TLRPC.Chat migratedTo = MessagesController.getInstance(messageObject.currentAccount).getChat(chat.migrated_to.channel_id);
                if (migratedTo != null && (migratedTo.noforwards || MessagesController.getInstance(messageObject.currentAccount).isChatNoForwards(migratedTo))) {
                    return true;
                }
            }
        } else if (dialogId > 0) {
            final TLRPC.UserFull userFull = MessagesController.getInstance(messageObject.currentAccount).getUserFull(dialogId);
            if (userFull != null && (userFull.noforwards_peer_enabled || userFull.noforwards_my_enabled || MessagesController.getInstance(messageObject.currentAccount).isUserNoForwards(userFull))) {
                return true;
            }
        }
        return false;
    }

    public static boolean needsRestrictedForward(ArrayList<MessageObject> messages) {
        if (messages == null) {
            return false;
        }
        for (int a = 0; a < messages.size(); a++) {
            if (needsRestrictedForward(messages.get(a))) {
                return true;
            }
        }
        return false;
    }

    public static void forwardMessages(int currentAccount, ArrayList<MessageObject> messages, long toDialogId, MessageObject replyToTopMsg, boolean notify, int scheduleDate, boolean hideCaption, Runnable onDone) {
        if (messages == null || messages.isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }

        final ArrayList<MessageObject> list = new ArrayList<>(messages);
        final int totalCount = list.size();

        // Non-blocking in-app card & background notification
        VeyraForwardProgress.show(totalCount, () -> {
            // Cancel listener: user tapped close on the progress banner
        });

        Utilities.stageQueue.postRunnable(() -> {
            int failed = 0;
            try {
                failed = forwardInternal(currentAccount, list, toDialogId, replyToTopMsg, notify, scheduleDate, hideCaption);
            } catch (Throwable e) {
                FileLog.e(e);
                failed = totalCount;
            } finally {
                final int finalFailed = failed;
                AndroidUtilities.runOnUIThread(() -> {
                    VeyraForwardProgress.complete(totalCount, finalFailed);
                    if (onDone != null) {
                        onDone.run();
                    }
                });
            }
        });
    }

    private static int forwardInternal(int currentAccount, ArrayList<MessageObject> list, long toDialogId, MessageObject replyToTopMsg, boolean notify, int scheduleDate, boolean hideCaption) {
        int failed = 0;
        int done = 0;
        final int total = list.size();
        final ArrayList<MessageObject> nativeBatch = new ArrayList<>();

        int i = 0;
        while (i < list.size()) {
            if (VeyraForwardProgress.isCancelled()) {
                break;
            }

            final MessageObject first = list.get(i);
            final long groupedId = first.messageOwner != null ? first.messageOwner.grouped_id : 0;

            final ArrayList<MessageObject> unit = new ArrayList<>();
            unit.add(first);
            i++;
            if (groupedId != 0) {
                while (i < list.size()) {
                    final MessageObject next = list.get(i);
                    if (next.messageOwner == null || next.messageOwner.grouped_id != groupedId) {
                        break;
                    }
                    unit.add(next);
                    i++;
                }
            }

            boolean needs = false;
            for (int a = 0; a < unit.size(); a++) {
                if (needsRestrictedForward(unit.get(a))) {
                    needs = true;
                    break;
                }
            }

            if (!needs) {
                nativeBatch.addAll(unit);
                done += unit.size();
                VeyraForwardProgress.update(done, total);
                continue;
            }

            flushNativeBatch(currentAccount, nativeBatch, toDialogId, replyToTopMsg, notify, scheduleDate, hideCaption);
            final int unitFailed = sendUnit(currentAccount, unit, toDialogId, replyToTopMsg, notify, scheduleDate, hideCaption);
            failed += unitFailed;
            done += unit.size();
            VeyraForwardProgress.update(done, total);
        }

        flushNativeBatch(currentAccount, nativeBatch, toDialogId, replyToTopMsg, notify, scheduleDate, hideCaption);
        return failed;
    }

    private static void flushNativeBatch(int currentAccount, ArrayList<MessageObject> batch, long toDialogId, MessageObject replyToTopMsg, boolean notify, int scheduleDate, boolean hideCaption) {
        if (batch.isEmpty()) {
            return;
        }
        final ArrayList<MessageObject> copy = new ArrayList<>(batch);
        batch.clear();
        AndroidUtilities.runOnUIThread(() ->
                SendMessagesHelper.getInstance(currentAccount).sendMessage(copy, toDialogId, false, hideCaption, notify, scheduleDate, replyToTopMsg, -1, 0));
    }

    private static int sendUnit(int currentAccount, ArrayList<MessageObject> unit, long toDialogId, MessageObject replyToTopMsg, boolean notify, int scheduleDate, boolean hideCaption) {
        if (unit == null || unit.isEmpty() || VeyraForwardProgress.isCancelled()) {
            return 0;
        }

        final boolean album = unit.size() > 1 && !DialogObject.isEncryptedDialog(toDialogId);
        final long groupId = album ? Utilities.random.nextLong() : 0;
        final ArrayList<SendMessagesHelper.SendMessageParams> built = new ArrayList<>();
        int failed = 0;

        for (int a = 0; a < unit.size(); a++) {
            if (VeyraForwardProgress.isCancelled()) {
                break;
            }
            final MessageObject messageObject = unit.get(a);
            SendMessagesHelper.SendMessageParams params = null;
            try {
                params = build(currentAccount, messageObject, toDialogId, replyToTopMsg, notify, scheduleDate, hideCaption);
            } catch (Throwable e) {
                FileLog.e(e);
            }
            if (params == null) {
                failed++;
                continue;
            }
            built.add(params);
        }

        if (built.isEmpty()) {
            return failed;
        }

        if (groupId != 0 && built.size() > 1) {
            for (int a = 0; a < built.size(); a++) {
                final SendMessagesHelper.SendMessageParams params = built.get(a);
                if (params.params == null) {
                    params.params = new HashMap<>();
                }
                params.params.put("groupId", "" + groupId);
                if (a == built.size() - 1) {
                    params.params.put("final", "1");
                }
            }
        }

        AndroidUtilities.runOnUIThread(() -> {
            for (int a = 0; a < built.size(); a++) {
                try {
                    SendMessagesHelper.getInstance(currentAccount).sendMessage(built.get(a));
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        });
        return failed;
    }

    private static SendMessagesHelper.SendMessageParams build(int currentAccount, MessageObject messageObject, long toDialogId, MessageObject replyToTopMsg, boolean notify, int scheduleDate, boolean hideCaption) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return null;
        }

        final TLRPC.Message message = messageObject.messageOwner;
        final TLRPC.MessageMedia media = message.media;
        final HashMap<String, String> params = new HashMap<>();

        // Sticker
        if (messageObject.isSticker()) {
            final TLRPC.Document sourceDocument = messageObject.getDocument();
            if (sourceDocument != null) {
                final File local = resolveLocalFile(currentAccount, messageObject);
                return SendMessagesHelper.SendMessageParams.of((TLRPC.TL_document) sourceDocument, null, local != null ? local.getAbsolutePath() : null, toDialogId, null, replyToTopMsg, null, null, null, params, notify, scheduleDate, 0, 0, messageObject, null, false, false);
            }
        }

        // Poll
        if (media instanceof TLRPC.TL_messageMediaPoll) {
            final TLRPC.TL_messageMediaPoll poll = clonePoll((TLRPC.TL_messageMediaPoll) media);
            if (poll != null) {
                return SendMessagesHelper.SendMessageParams.of(poll, toDialogId, null, replyToTopMsg, null, params, notify, scheduleDate, 0);
            }
        }

        // Contact
        if (media instanceof TLRPC.TL_messageMediaContact) {
            final TLRPC.TL_messageMediaContact contact = (TLRPC.TL_messageMediaContact) media;
            final TLRPC.TL_user user = new TLRPC.TL_user();
            user.id = contact.user_id;
            user.phone = contact.phone_number;
            user.first_name = contact.first_name;
            user.last_name = contact.last_name;
            if (!TextUtils.isEmpty(contact.vcard)) {
                final TLRPC.TL_reastrictionReason reason = new TLRPC.TL_reastrictionReason();
                reason.platform = "";
                reason.reason = "";
                reason.text = contact.vcard;
                user.restriction_reason.add(reason);
            }
            return SendMessagesHelper.SendMessageParams.of(user, toDialogId, null, replyToTopMsg, null, params, notify, scheduleDate, 0);
        }

        // Geo / Venue
        if (media instanceof TLRPC.TL_messageMediaGeo || media instanceof TLRPC.TL_messageMediaVenue) {
            return SendMessagesHelper.SendMessageParams.of(media, toDialogId, null, replyToTopMsg, null, params, notify, scheduleDate, 0);
        }

        final String caption = hideCaption ? "" : (message.message == null ? "" : message.message);
        final ArrayList<TLRPC.MessageEntity> entities = hideCaption ? null : copyEntities(message.entities);

        // Photo
        if (media instanceof TLRPC.TL_messageMediaPhoto) {
            final File file = ensureDownloaded(currentAccount, messageObject);
            if (file == null || !file.exists()) {
                return null;
            }
            final TLRPC.TL_photo photo = SendMessagesHelper.getInstance(currentAccount).generatePhotoSizes(file.getAbsolutePath(), null);
            if (photo == null) {
                return null;
            }
            params.put("originalPath", file.getAbsolutePath() + file.length());
            final SendMessagesHelper.SendMessageParams out = SendMessagesHelper.SendMessageParams.of(
                    photo,
                    file.getAbsolutePath(),
                    toDialogId,
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
                    messageObject.hasMediaSpoilers()
            );
            if (out != null) {
                out.invert_media = message.invert_media;
            }
            return out;
        }

        // Document / Video / Audio / Voice / Round
        if (media instanceof TLRPC.TL_messageMediaDocument) {
            final TLRPC.Document sourceDocument = messageObject.getDocument();
            if (sourceDocument == null) {
                return null;
            }
            final File file = ensureDownloaded(currentAccount, messageObject);
            if (file == null || !file.exists()) {
                return null;
            }
            final TLRPC.TL_document document = cloneDocument(sourceDocument, file);
            params.put("originalPath", file.getAbsolutePath() + file.length());
            final SendMessagesHelper.SendMessageParams out = SendMessagesHelper.SendMessageParams.of(
                    document,
                    null,
                    file.getAbsolutePath(),
                    toDialogId,
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
                    messageObject.hasMediaSpoilers()
            );
            if (out != null) {
                out.invert_media = message.invert_media;
            }
            return out;
        }

        // Plain text / WebPage fallback
        CharSequence text = message.message;
        if (TextUtils.isEmpty(text)) {
            text = messageObject.messageText;
        }
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        return SendMessagesHelper.SendMessageParams.of(text.toString(), toDialogId, null, replyToTopMsg, null, true,
                entities, null, params, notify, scheduleDate, 0, null, false);
    }

    private static TLRPC.TL_document cloneDocument(TLRPC.Document source, File file) {
        final TLRPC.TL_document document = new TLRPC.TL_document();
        document.id = 0;
        document.access_hash = 0;
        document.dc_id = 0;
        document.file_reference = new byte[0];
        document.date = (int) (System.currentTimeMillis() / 1000);
        document.mime_type = source.mime_type != null ? source.mime_type : "application/octet-stream";
        document.size = file.length();
        document.attributes.addAll(source.attributes);
        boolean hasFileName = false;
        for (int a = 0; a < document.attributes.size(); a++) {
            if (document.attributes.get(a) instanceof TLRPC.TL_documentAttributeFilename) {
                hasFileName = true;
                break;
            }
        }
        if (!hasFileName) {
            final TLRPC.TL_documentAttributeFilename fileName = new TLRPC.TL_documentAttributeFilename();
            fileName.file_name = file.getName();
            document.attributes.add(fileName);
        }
        if (source.thumbs != null) {
            for (int a = 0; a < source.thumbs.size(); a++) {
                final TLRPC.PhotoSize photoSize = source.thumbs.get(a);
                if (photoSize instanceof TLRPC.TL_photoStrippedSize) {
                    document.thumbs.add(photoSize);
                    document.flags |= 1;
                }
            }
        }
        return document;
    }

    private static TLRPC.TL_messageMediaPoll clonePoll(TLRPC.TL_messageMediaPoll src) {
        if (src == null || src.poll == null) {
            return null;
        }
        final TLRPC.TL_poll poll = new TLRPC.TL_poll();
        poll.id = 0;
        poll.question = src.poll.question;
        poll.answers.addAll(src.poll.answers);
        poll.multiple_choice = src.poll.multiple_choice;
        poll.public_voters = src.poll.public_voters;
        poll.quiz = false;
        poll.closed = false;

        final TLRPC.TL_messageMediaPoll media = new TLRPC.TL_messageMediaPoll();
        media.poll = poll;
        media.results = new TLRPC.TL_pollResults();
        return media;
    }

    private static ArrayList<TLRPC.MessageEntity> copyEntities(ArrayList<TLRPC.MessageEntity> src) {
        if (src == null || src.isEmpty()) {
            return null;
        }
        return new ArrayList<>(src);
    }

    public static File resolveLocalFile(int currentAccount, MessageObject messageObject) {
        if (messageObject == null) return null;
        final TLRPC.Message message = messageObject.messageOwner;
        if (message != null && !TextUtils.isEmpty(message.attachPath)) {
            final File attach = new File(message.attachPath);
            if (attach.exists() && attach.length() > 0) {
                return attach;
            }
        }
        try {
            final File file = FileLoader.getInstance(currentAccount).getPathToMessage(message);
            if (file != null && file.exists() && file.length() > 0) {
                return file;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }

        final int[] dirs = new int[]{
                FileLoader.MEDIA_DIR_CACHE,
                FileLoader.MEDIA_DIR_DOCUMENT,
                FileLoader.MEDIA_DIR_VIDEO,
                FileLoader.MEDIA_DIR_AUDIO,
                FileLoader.MEDIA_DIR_FILES,
                FileLoader.MEDIA_DIR_IMAGE,
                FileLoader.MEDIA_DIR_VIDEO_PUBLIC,
                FileLoader.MEDIA_DIR_IMAGE_PUBLIC
        };

        // Check document
        final TLRPC.Document doc = messageObject.getDocument();
        if (doc != null) {
            try {
                if (!TextUtils.isEmpty(doc.localPath)) {
                    final File local = new File(doc.localPath);
                    if (local.exists() && local.length() > 0) {
                        return local;
                    }
                }
                File file = FileLoader.getInstance(currentAccount).getPathToAttach(doc, false);
                if (file != null && file.exists() && file.length() > 0) {
                    return file;
                }
                file = FileLoader.getInstance(currentAccount).getPathToAttach(doc, true);
                if (file != null && file.exists() && file.length() > 0) {
                    return file;
                }

                // Check FilePathDatabase
                try {
                    final int docType = MessageObject.isVoiceDocument(doc) ? FileLoader.MEDIA_DIR_AUDIO : (MessageObject.isVideoDocument(doc) ? FileLoader.MEDIA_DIR_VIDEO : FileLoader.MEDIA_DIR_DOCUMENT);
                    final String dbPath = FileLoader.getInstance(currentAccount).getFileDatabase().getPath(doc.id, doc.dc_id, docType, true);
                    if (!TextUtils.isEmpty(dbPath)) {
                        final File dbFile = new File(dbPath);
                        if (dbFile.exists() && dbFile.length() > 0) {
                            return dbFile;
                        }
                    }
                } catch (Throwable ignore) {}

                // Check dirs with attachName & originalName
                final String attachName = FileLoader.getAttachFileName(doc);
                final String originalName = FileLoader.getDocumentFileName(doc);
                for (int d : dirs) {
                    final File dir = FileLoader.getInstance(currentAccount).getDirectory(d);
                    if (dir == null) continue;
                    if (!TextUtils.isEmpty(attachName)) {
                        File candidate = new File(dir, attachName);
                        if (candidate.exists() && candidate.length() > 0) return candidate;
                        File enc = new File(dir, attachName + ".enc");
                        if (enc.exists() && enc.length() > 0) return enc;
                    }
                    if (!TextUtils.isEmpty(originalName)) {
                        File candidate = new File(dir, originalName);
                        if (candidate.exists() && candidate.length() > 0) return candidate;
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }

        // Check photo
        final TLRPC.Photo photo = MessageObject.getPhoto(message);
        if (photo != null && photo.sizes != null) {
            try {
                for (int s = photo.sizes.size() - 1; s >= 0; s--) {
                    final TLRPC.PhotoSize ps = photo.sizes.get(s);
                    if (ps == null || ps instanceof TLRPC.TL_photoStrippedSize || ps instanceof TLRPC.TL_photoPathSize) {
                        continue;
                    }
                    File file = FileLoader.getInstance(currentAccount).getPathToAttach(ps, false);
                    if (file != null && file.exists() && file.length() > 0) {
                        return file;
                    }
                    file = FileLoader.getInstance(currentAccount).getPathToAttach(ps, true);
                    if (file != null && file.exists() && file.length() > 0) {
                        return file;
                    }
                    final String psFileName = FileLoader.getAttachFileName(ps);
                    if (!TextUtils.isEmpty(psFileName)) {
                        for (int d : dirs) {
                            final File dir = FileLoader.getInstance(currentAccount).getDirectory(d);
                            if (dir == null) continue;
                            File candidate = new File(dir, psFileName);
                            if (candidate.exists() && candidate.length() > 0) return candidate;
                            File enc = new File(dir, psFileName + ".enc");
                            if (enc.exists() && enc.length() > 0) return enc;
                        }
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        return null;
    }

    private static File ensureDownloaded(int currentAccount, MessageObject messageObject) {
        if (VeyraForwardProgress.isCancelled()) {
            return null;
        }
        final File file = resolveLocalFile(currentAccount, messageObject);
        if (file != null) {
            return file;
        }

        final TLRPC.Document document = messageObject.getDocument();
        TLRPC.Photo photo = null;
        TLRPC.PhotoSize photoSize = null;
        if (document == null) {
            photo = MessageObject.getPhoto(messageObject.messageOwner);
            if (photo != null && photo.sizes != null) {
                photoSize = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize(true), false, null, true);
            }
            if (photoSize == null) {
                return null;
            }
        }

        final String fileName = document != null ? FileLoader.getAttachFileName(document) : FileLoader.getAttachFileName(photoSize);
        if (TextUtils.isEmpty(fileName)) {
            return null;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final File[] result = new File[1];
        final NotificationCenter.NotificationCenterDelegate[] observer = new NotificationCenter.NotificationCenterDelegate[1];
        observer[0] = (id, account, args) -> {
            if (account != currentAccount || args == null || args.length == 0) {
                return;
            }
            if (!(args[0] instanceof String) || !fileName.equals(args[0])) {
                return;
            }
            if (id == NotificationCenter.fileLoaded && args.length > 1 && args[1] instanceof File) {
                result[0] = (File) args[1];
            }
            latch.countDown();
        };

        final TLRPC.PhotoSize photoSizeFinal = photoSize;
        final TLRPC.Photo photoFinal = photo;
        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(currentAccount).addObserver(observer[0], NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(currentAccount).addObserver(observer[0], NotificationCenter.fileLoadFailed);
            if (document != null) {
                FileLoader.getInstance(currentAccount).loadFile(document, messageObject, FileLoader.PRIORITY_HIGH, 0);
            } else {
                FileLoader.getInstance(currentAccount).loadFile(ImageLocation.getForObject(photoSizeFinal, photoFinal), messageObject, null, FileLoader.PRIORITY_HIGH, 1);
            }
        });

        try {
            latch.await(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            FileLog.e(e);
        }

        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(currentAccount).removeObserver(observer[0], NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(currentAccount).removeObserver(observer[0], NotificationCenter.fileLoadFailed);
        });

        if (result[0] != null && result[0].exists() && result[0].length() > 0) {
            return result[0];
        }
        return resolveLocalFile(currentAccount, messageObject);
    }
}

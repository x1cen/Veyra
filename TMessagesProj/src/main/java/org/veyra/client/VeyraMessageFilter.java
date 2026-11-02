package org.veyra.client;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class VeyraMessageFilter {

    public static ArrayList<TLRPC.MessageEntity> checkBlockedEntities(MessageObject messageObject, ArrayList<TLRPC.MessageEntity> original) {
        if (shouldBlockMessage(messageObject) && messageObject.messageOwner != null && messageObject.messageOwner.message != null) {
            ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>(original != null ? original : new ArrayList<>());
            TLRPC.TL_messageEntitySpoiler spoiler = new TLRPC.TL_messageEntitySpoiler();
            spoiler.offset = 0;
            spoiler.length = messageObject.messageOwner.message.length();
            entities.add(spoiler);
            return entities;
        } else {
            return original;
        }
    }

    public static boolean shouldBlockMessage(MessageObject message) {
        if (message == null || message.messageOwner == null || message.storyItem != null) {
            return false;
        }
        if (!VeyraUIConfig.ignoreBlocked) {
            return false;
        }
        if (isUserBlocked(message.currentAccount, message.getFromChatId())) {
            return true;
        }
        if (message.messageOwner.fwd_from != null && message.messageOwner.fwd_from.from_id != null) {
            return isUserBlocked(message.currentAccount, MessageObject.getPeerId(message.messageOwner.fwd_from.from_id));
        }
        return false;
    }

    private static boolean isUserBlocked(int currentAccount, long id) {
        if (id == 0) return false;
        MessagesController messagesController = MessagesController.getInstance(currentAccount);
        if (messagesController == null) return false;
        TLRPC.UserFull userFull = messagesController.getUserFull(id);
        return (userFull != null && userFull.blocked) || messagesController.blockePeers.indexOfKey(id) >= 0;
    }
}

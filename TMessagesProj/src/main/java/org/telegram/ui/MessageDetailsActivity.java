package org.telegram.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.telegram.messenger.MessageObject;

public class MessageDetailsActivity extends JsonViewerActivity {

    public static final Gson gson = new Gson();
    public static final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();

    public MessageDetailsActivity(MessageObject messageObject) {
        super(() -> {
            if (messageObject == null) {
                return "{}";
            }
            JsonObject root = new JsonObject();
            if (messageObject.messageOwner != null) {
                root.add("message", gson.toJsonTree(messageObject.messageOwner));
            }
            root.addProperty("dialog_id", messageObject.getDialogId());
            root.addProperty("is_out", messageObject.isOut());
            root.addProperty("date", messageObject.messageOwner != null ? messageObject.messageOwner.date : 0);
            return prettyGson.toJson(root);
        }, "Message Details");
    }
}

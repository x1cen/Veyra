package org.telegram.ui;

import android.os.Bundle;
import android.view.View;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.BaseFragment;

public class MessageDetailsActivity extends BaseFragment {
    public static final Gson gson = new Gson();
    public static final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    private final MessageObject messageObject;
    public MessageDetailsActivity(MessageObject messageObject) {
        this.messageObject = messageObject;
    }
    @Override
    public View createView(android.content.Context context) {
        actionBar.setTitle("Message Details");
        fragmentView = new View(context);
        return fragmentView;
    }
}

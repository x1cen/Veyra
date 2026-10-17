package org.telegram.ui;

import android.content.Context;
import android.view.View;
import org.telegram.ui.ActionBar.BaseFragment;

public class JsonViewerActivity extends BaseFragment {
    public interface JsonProvider {
        String getJson();
    }
    private final JsonProvider provider;
    private final String title;
    public JsonViewerActivity(JsonProvider provider, String title) {
        this.provider = provider;
        this.title = title;
    }
    public JsonViewerActivity(JsonProvider provider) {
        this(provider, "JSON");
    }
    @Override
    public View createView(Context context) {
        actionBar.setTitle(title != null ? title : "JSON");
        fragmentView = new View(context);
        return fragmentView;
    }
}

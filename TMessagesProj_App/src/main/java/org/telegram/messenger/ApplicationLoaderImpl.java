package org.telegram.messenger;

//import org.telegram.messenger.regular.BuildConfig;

public class ApplicationLoaderImpl extends ApplicationLoader {
    @Override
    protected String onGetApplicationId() {
        return ApplicationLoader.applicationContext != null ? ApplicationLoader.applicationContext.getPackageName() : BuildVars.BUILD_VEYRA;
    }
}

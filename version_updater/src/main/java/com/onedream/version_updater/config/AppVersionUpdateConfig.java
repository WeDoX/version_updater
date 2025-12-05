package com.onedream.version_updater.config;

import com.onedream.version_updater.listener.AppVersionUpdateListener;
import com.onedream.version_updater.utils.log.AppVersionUpdateLogger;

public class AppVersionUpdateConfig {
    private final AppVersionUpdateListener appVersionUpdateListener;
    private final AppVersionUpdateLogger appVersionUpdateLogger;

    private AppVersionUpdateConfig(AppVersionUpdateListener appVersionUpdateListener, AppVersionUpdateLogger appVersionUpdateLogger){
        this.appVersionUpdateListener = appVersionUpdateListener;
        this.appVersionUpdateLogger = appVersionUpdateLogger;
    }

    public AppVersionUpdateListener getAppVersionUpdateListener() {
        return appVersionUpdateListener;
    }

    public AppVersionUpdateLogger getAppVersionUpdateLogger() {
        return appVersionUpdateLogger;
    }

    public static final class Builder {
        private AppVersionUpdateListener appVersionUpdateListener;
        private AppVersionUpdateLogger appVersionUpdateLogger;


        public Builder() {

        }

        public Builder setUpdateListener(AppVersionUpdateListener appVersionUpdateListener) {
            this.appVersionUpdateListener = appVersionUpdateListener;
            return this;
        }

        public Builder setUpdateLogger(AppVersionUpdateLogger appVersionUpdateLogger) {
            this.appVersionUpdateLogger = appVersionUpdateLogger;
            return this;
        }

        public AppVersionUpdateConfig build() {
            AppVersionUpdateLogger finalAppVersionUpdateLogger = appVersionUpdateLogger;
            if(null == finalAppVersionUpdateLogger){
                finalAppVersionUpdateLogger = new AppVersionUpdateLogger.Default();
            }
            return new AppVersionUpdateConfig(appVersionUpdateListener, finalAppVersionUpdateLogger);
        }
    }
}

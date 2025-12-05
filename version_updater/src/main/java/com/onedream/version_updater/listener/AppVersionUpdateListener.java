package com.onedream.version_updater.listener;


public interface AppVersionUpdateListener {
    void requestApkInstallPermission();

    void updateError(Exception e);
}

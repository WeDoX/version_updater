package com.onedream.version_updater.utils.log;

import android.util.Log;

import androidx.annotation.NonNull;

public interface AppVersionUpdateLogger {
    void printLog(@NonNull String tag, String msg);

    class Default implements AppVersionUpdateLogger {
        @Override
        public void printLog(@NonNull String tag, String msg) {
            Log.e("ATU", "VersionUpdater==>"+ "tag===>"+ msg);
        }
    }
}

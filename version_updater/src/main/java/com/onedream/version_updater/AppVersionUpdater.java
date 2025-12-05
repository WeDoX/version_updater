package com.onedream.version_updater;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.onedream.version_updater.config.AppVersionUpdateConfig;
import com.onedream.version_updater.downloader.ApkFileDownloader;
import com.onedream.version_updater.installer.ApkInstaller;
import com.onedream.version_updater.listener.ApkFileDownloadProgressListener;
import com.onedream.version_updater.utils.log.AppVersionUpdateLogUtils;
import com.onedream.version_updater.utils.FileMD5Utils;
import com.onedream.version_updater.utils.LocalApkFileUtils;

import java.io.File;

public class AppVersionUpdater {
    private static final String TAG = "AppVersionUpdater";
    //
    private static final int MSG_UPDATE_START = 1;
    private static final int MSG_UPDATE_PROGRESS = 2;
    private static final int MSG_UPDATE_FINISH = 3;
    //
    private static final String KEY_MSG_UPDATE_START_IS_REGAIN_DOWNLOAD = "key_is_regain_download";
    private static final String KEY_MSG_UPDATE_PROGRESS_SIZE = "key_size";
    private static final String KEY_MSG_UPDATE_PROGRESS_TOTAL_SIZE = "key_total_size";
    //
    private final Activity mContext;
    private final AppVersionUpdateConfig mAppVersionUpdateConfig;
    private final ApkInstaller mApkInstaller;
    //
    private ProgressDialog mProgressDialog = null;
    //
    private Handler mMainLooperHandler = null;
    //
    private String mUpdateServerApkHostUrl = "";
    private String mUpdateServerApkFileName = "";
    private String mUpdateServerApkFileMd5 = "";

    public AppVersionUpdater(@NonNull Activity context, @NonNull AppVersionUpdateConfig appVersionUpdateConfig) {
        this.mContext = context;
        this.mAppVersionUpdateConfig = appVersionUpdateConfig;
        AppVersionUpdateLogUtils.init(this.mAppVersionUpdateConfig.getAppVersionUpdateLogger());
        this.mApkInstaller = new ApkInstaller(context, () -> {
            if (null != mAppVersionUpdateConfig.getAppVersionUpdateListener()) {
                mAppVersionUpdateConfig.getAppVersionUpdateListener().requestApkInstallPermission();
            }
        });
        //
        initMainLooperHandler();
    }

    private void initMainLooperHandler() {
        mMainLooperHandler = new Handler(mContext.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_UPDATE_START:
                        doNewVersionUpdate(msg);
                        break;
                    case MSG_UPDATE_PROGRESS:
                        handleDownloadProgress(msg);
                        break;
                    case MSG_UPDATE_FINISH:
                        handleDownloadFinish();
                        break;
                }
            }
        };
    }

    public void startUpdate(String update_url, String MD5) {
        this.mUpdateServerApkHostUrl = update_url;
        this.mUpdateServerApkFileName = LocalApkFileUtils.getApkFileNameByUpdateUrl(update_url);
        this.mUpdateServerApkFileMd5 = MD5;
        //
        installOrDownloadApk(false);
    }

    private void installOrDownloadApk(boolean isRegainDownload) {
        if (checkLocalCompleteApk()) {
            requestInstallApk();
        } else {
            sendUpdateStartMsgToHandler(isRegainDownload);
        }
    }

    /**
     * 检查本地是否存在完整且正确（MD5值相等）的APK文件
     *
     * @return true-存在完整文件；false-文件不存在或损坏(损坏情况下，程序会自动删除掉）
     */
    private boolean checkLocalCompleteApk() {
        File file = new File(updateServerApkFileLocalSaveFilePath());
        // 文件存在时进行MD5校验
        if (file.exists()) {
            if (isApkFileMD5Equal(file)) {
                printLog("文件存在，MD5匹配，文件完整");
                return true;
            }
            printLog("文件存在，但MD5值不匹配，说明文件下载不全（或者损坏）之类的，执行文件删除操作");
            file.delete();
        }
        return false;
    }

    private String updateServerApkFileLocalSaveFilePath() {
        return LocalApkFileUtils.getUpdateServerApkFileLocalSaveFilePath(mContext, mUpdateServerApkFileName);
    }

    private boolean isApkFileMD5Equal(File file) {
        return mUpdateServerApkFileMd5.toUpperCase().equals(FileMD5Utils.getFileMD5(file));
    }

    public void requestInstallApk() {
        mApkInstaller.requestInstallApk(updateServerApkFileLocalSaveFilePath());
    }

    private void sendUpdateStartMsgToHandler(boolean isRegainDownload) {
        Message msg = new Message();
        msg.what = MSG_UPDATE_START;
        msg.getData().putBoolean(KEY_MSG_UPDATE_START_IS_REGAIN_DOWNLOAD, isRegainDownload);
        //
        if (null != mMainLooperHandler) {
            mMainLooperHandler.sendMessage(msg);
        }
    }

    private void doNewVersionUpdate(@NonNull Message msg) {
        boolean isRegainDownload = msg.getData().getBoolean(KEY_MSG_UPDATE_START_IS_REGAIN_DOWNLOAD);
        //
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setTitle(isRegainDownload ? mContext.getString(R.string.version_updater_download_dialog_title_again) : mContext.getString(R.string.version_updater_download_dialog_title));
        mProgressDialog.setMessage(mContext.getString(R.string.version_updater_download_dialog_content));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setProgress(0);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        //
        new Thread(() -> {
            Looper.prepare();
            downApkFile(mUpdateServerApkHostUrl);
            Looper.loop();
        }).start();
    }

    private void downApkFile(final String url) {
        try {
            ApkFileDownloader loader = new ApkFileDownloader(mContext, url, 2);

            loader.download(new ApkFileDownloadProgressListener() {
                public void onDownloadSize(int size, int total_size) {
                    sendUpdateProgressMsgToHandler(size, total_size);
                }

                @Override
                public void onDownloadFinish() {
                    sendUpdateFinishMsgToHandler();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            handleDownloadError(e);
        }
    }

    private void sendUpdateProgressMsgToHandler(int size, int total_size) {
        Message msg = new Message();
        msg.what = MSG_UPDATE_PROGRESS;
        msg.getData().putInt(KEY_MSG_UPDATE_PROGRESS_SIZE, size);
        msg.getData().putInt(KEY_MSG_UPDATE_PROGRESS_TOTAL_SIZE, total_size);
        //
        if (null != mMainLooperHandler) {
            mMainLooperHandler.sendMessage(msg);
        }
    }

    private void handleDownloadProgress(@NonNull Message msg) {
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
        //
        int size = msg.getData().getInt(KEY_MSG_UPDATE_PROGRESS_SIZE);
        int total_size = msg.getData().getInt(KEY_MSG_UPDATE_PROGRESS_TOTAL_SIZE);
        mProgressDialog.setMax(total_size / 1024);
        mProgressDialog.setProgress(size / 1024);
    }

    private void sendUpdateFinishMsgToHandler() {
        Message msg = new Message();
        msg.what = MSG_UPDATE_FINISH;
        //
        if (null != mMainLooperHandler) {
            mMainLooperHandler.sendMessage(msg);
        }
    }

    private void handleDownloadFinish() {
        //
        mProgressDialog.cancel();
        //
        installOrDownloadApk(true);
    }

    private void handleDownloadError(Exception e) {
        printLog("更新版本下载出错:" + e);
        //
        if (null != mProgressDialog) {
            mProgressDialog.dismiss();
        }
        //
        if (null != mAppVersionUpdateConfig.getAppVersionUpdateListener()) {
            mAppVersionUpdateConfig.getAppVersionUpdateListener().updateError(e);
        }
    }

    private static void printLog(String msg) {
        AppVersionUpdateLogUtils.printLog(TAG, msg);
    }
}

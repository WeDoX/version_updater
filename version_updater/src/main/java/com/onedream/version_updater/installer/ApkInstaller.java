package com.onedream.version_updater.installer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.onedream.version_updater.R;
import com.onedream.version_updater.utils.log.AppVersionUpdateLogUtils;

import java.io.File;
import java.io.IOException;

public class ApkInstaller {
    private final Context mContext;
    private final RequestApkInstallPermissionListener mRequestApkInstallPermissionListener;

    public ApkInstaller(@NonNull Context context, @NonNull RequestApkInstallPermissionListener listener) {
        this.mContext = context;
        this.mRequestApkInstallPermissionListener = listener;
    }

    public void requestInstallApk(String apkFilePath) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //先判断是否有安装未知来源应用的权限
            boolean haveInstallPermission = mContext.getPackageManager().canRequestPackageInstalls();
            if (!haveInstallPermission) {
                showNeedInstallPermissionDialog();
                return;
            }
        }
        installApk(apkFilePath);
    }

    private void showNeedInstallPermissionDialog() {
        //弹框提示用户手动打开
        Dialog dialog = new AlertDialog
                .Builder(mContext)
                .setTitle(mContext.getString(R.string.version_updater_install_permission_dialog_title))
                .setMessage(mContext.getString(R.string.version_updater_install_permission_dialog_content))
                .setCancelable(false)
                .setPositiveButton(mContext.getString(R.string.version_updater_install_permission_dialog_btn), (dialog1, which) -> {
                    //发布请求安装权限
                    mRequestApkInstallPermissionListener.requestInstallPermission();
                }).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void installApk(String apkFilePath) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            installApkForNAndAbove(apkFilePath);
        } else {
            installApkBelowN(apkFilePath);
        }

    }

    // Android N 及以上版本的安装方法
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void installApkForNAndAbove(String apkFilePath) {
        File file = new File(apkFilePath);
        printLog("file_path:" + file.getAbsolutePath() + "当前的包名:" + mContext.getPackageName());
        Uri contentUri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".file_provider", file);
        //
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    // Android N 以下版本的安装方法
    private void installApkBelowN(String apkFilePath) {
        String permission = "666";
        //
        try {
            String command = "chmod " + permission + " " + apkFilePath;
            printLog("执行更改文件权限命令:" + command);
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //
        File file = new File(apkFilePath);
        if (!file.exists()) {
            printLog("位于" + apkFilePath + "的更新包不存在");
        }
        //
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        mContext.startActivity(intent);
    }


    private static void printLog(String msg) {
        AppVersionUpdateLogUtils.printLog("AppInstaller", msg);
    }


    public interface RequestApkInstallPermissionListener {
        void requestInstallPermission();
    }
}

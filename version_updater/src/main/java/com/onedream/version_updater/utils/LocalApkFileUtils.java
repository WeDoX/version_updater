package com.onedream.version_updater.utils;

import android.content.Context;

import com.onedream.version_updater.utils.log.AppVersionUpdateLogUtils;

import java.io.File;

public final class LocalApkFileUtils {

    private LocalApkFileUtils() {

    }

    public static String getApkFileNameByUpdateUrl(String update_url){
        return update_url.substring(update_url.lastIndexOf('/') + 1);
    }

    public static String getUpdateServerApkFileLocalSaveFilePath(Context context, String updateServerApkFileName) {
        return getUpdateServerApkFileLocalSaveFileDirPath(context)+ "/" + updateServerApkFileName;
    }

    private static File getUpdateServerApkFileLocalSaveFileDirPath(Context context){
        return context.getFilesDir();
    }

    public static void ensureDirectoryExists(Context context){
        File fileSaveDir = getUpdateServerApkFileLocalSaveFileDirPath(context);
        if (!fileSaveDir.exists()){
            fileSaveDir.mkdirs();
        }
    }

    public static void clearOldApkFile(Context context, String update_url) {
        try {
            new Thread(() -> {
                try {
                    String updateServerApkFileName = getApkFileNameByUpdateUrl(update_url);
                    //
                    File file = new File(getUpdateServerApkFileLocalSaveFilePath(context, updateServerApkFileName));
                    if (file.exists()) {//文件已下载
                        printLog("删除掉旧的Apk安装包:" + file.getName());
                        //Md5值不同，删除，重新下载
                        file.delete();
                        printLog("删除掉旧的Apk安装包:安装包已删除");
                    } else {
                        printLog("删除掉旧的Apk安装包:安装包不存在");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printLog(String msg) {
        AppVersionUpdateLogUtils.printLog("OldApkFileClearUtils", msg);
    }
}

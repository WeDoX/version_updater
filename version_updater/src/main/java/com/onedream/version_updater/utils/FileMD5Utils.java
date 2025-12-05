package com.onedream.version_updater.utils;

import com.onedream.version_updater.utils.log.AppVersionUpdateLogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public final class FileMD5Utils {

    private FileMD5Utils() {

    }

    public static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        String fileMD5 = bytesToHexString(digest.digest());
        if (null == fileMD5) {
            return null;
        }
        fileMD5 = fileMD5.toUpperCase();
        printLog("文件MD5值：" + fileMD5);
        return fileMD5;
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    private static void printLog(String msg) {
        AppVersionUpdateLogUtils.printLog("FileMD5Utils", msg);
    }
}

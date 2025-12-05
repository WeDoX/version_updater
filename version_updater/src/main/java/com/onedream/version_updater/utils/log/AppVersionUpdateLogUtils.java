package com.onedream.version_updater.utils.log;

public final class AppVersionUpdateLogUtils {

    private static  AppVersionUpdateLogger mAppVersionUpdateLogger;

    private AppVersionUpdateLogUtils(){

    }

    public static void init(AppVersionUpdateLogger appVersionUpdateLogger){
        mAppVersionUpdateLogger = appVersionUpdateLogger;
    }
    public static void printLog(String tag, String msg){
        if(null != mAppVersionUpdateLogger){
            mAppVersionUpdateLogger.printLog(tag, msg);
        }
    }
}

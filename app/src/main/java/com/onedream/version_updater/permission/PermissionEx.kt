package com.onedream.version_updater.permission

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.net.toUri


/**
 * 开启安装未知来源权限
 */
@RequiresApi(api = Build.VERSION_CODES.O)
fun Activity.toInstallPermissionSettingIntent() {
    val packageURI = "package:$packageName".toUri()
    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI)
    startActivityForResult(intent, PermissionRequestCodeConstant.REQUEST_CODE_FOR_MANAGE_UNKNOWN_APP_SOURCES)
}

fun afterManageUnknownAppSourceReturn(requestCode: Int, continueAction: () -> Unit) {
    if (requestCode == PermissionRequestCodeConstant.REQUEST_CODE_FOR_MANAGE_UNKNOWN_APP_SOURCES) {
        continueAction.invoke()
    }
}
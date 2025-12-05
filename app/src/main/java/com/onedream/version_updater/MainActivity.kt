package com.onedream.version_updater

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.onedream.version_updater.config.AppVersionUpdateConfig
import com.onedream.version_updater.listener.AppVersionUpdateListener
import com.onedream.version_updater.permission.afterManageUnknownAppSourceReturn
import com.onedream.version_updater.permission.toInstallPermissionSettingIntent
import com.onedream.version_updater.ui.theme.Version_updaterTheme
import com.onedream.version_updater.utils.LocalApkFileUtils
import com.onedream.version_updater.utils.log.AppVersionUpdateLogger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Version_updaterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    Column(modifier = Modifier.padding(innerPadding)) {
                        UpdateVersionBtn(onClick = {
                            startUpdate()
                        })
                        ClearApkAfterUpdateSuccessBtn(onClick = {
                            clearOldApkFile()
                        })
                    }

                }
            }
        }
    }

    private val mUpdateListener by lazy {
        object : AppVersionUpdateListener {
            override fun requestApkInstallPermission() {
                //此方法需要API>=26才能使用
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    toInstallPermissionSettingIntent()
                }
            }

            override fun updateError(e: Exception) {
                Toast.makeText(this@MainActivity, e.message ?: e.toString(), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private val mUpdateLogger by lazy {
        AppVersionUpdateLogger { tag, msg -> Log.e("App_ATU", "$tag===>${msg?:""}") }
    }

    private val mVersionUpdateConfig by lazy {
        AppVersionUpdateConfig
            .Builder()
            .setUpdateListener(mUpdateListener)
            .setUpdateLogger(mUpdateLogger)
            .build()
    }

    private val mVersionUpdater by lazy {
        AppVersionUpdater(this@MainActivity, mVersionUpdateConfig)
    }

    private fun startUpdate() {
        //文件md5在线计算: https://tools.wedox.org/filehash/
        mVersionUpdater.startUpdate(
            "https://6261-baby-poetry-7gj0k3f7b52d8010-1314042038.tcb.qcloud.la/atuman/atuman_v1.8.0(8).apk",
            "e73928a40cedaa6b7a6e450e80acaa54"
        )
    }

    private fun clearOldApkFile(){
        //升级成功后，再次进入时，可以移除掉下载完成的apk文件
        LocalApkFileUtils.clearOldApkFile(
            this@MainActivity,
            "https://6261-baby-poetry-7gj0k3f7b52d8010-1314042038.tcb.qcloud.la/atuman/atuman_v1.8.0(8).apk"
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //
        afterManageUnknownAppSourceReturn(requestCode) {
            mVersionUpdater.requestInstallApk()
        }
    }
}

@Composable
fun UpdateVersionBtn(onClick: () -> Unit) {
    Button(onClick) {
        Text(text = "版本更新")
    }
}

@Composable
fun ClearApkAfterUpdateSuccessBtn(onClick: () -> Unit) {
    Button(onClick) {
        Text(text = "删除apk文件")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Version_updaterTheme {
        Greeting("Android")
    }
}
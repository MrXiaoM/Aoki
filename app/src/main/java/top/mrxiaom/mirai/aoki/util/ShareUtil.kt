package top.mrxiaom.mirai.aoki.util

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateUtils
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File


private const val REQUEST_CODE_SAVE_IMG = 6
    private val EXTERNAL_PERMISSION = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    /**
     * 请求读取sd卡的权限
     */
    fun <T: Activity> T.requireExternalPermission(block: T.() -> Unit) {
        if (hasPermissions(*EXTERNAL_PERMISSION)) {
            block();
        } else {
            ActivityCompat.requestPermissions(this, EXTERNAL_PERMISSION, REQUEST_CODE_SAVE_IMG)
        }
    }

    fun Context.hasPermissions(vararg perms: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        return perms.all {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun Context.saveQRCode(name: String, byteArray: ByteArray): Boolean {
        return kotlin.runCatching {
            if (Build.VERSION.SDK_INT < 29) {
                saveImageBelowAndroid10(name, byteArray)
            } else {
                saveImageAboveAndroid10(name, byteArray)
            }
        }.getOrNull() != null
    }

    private fun Context.saveImageBelowAndroid10(name: String, byteArray: ByteArray) {
        val appDir = File(Environment.getExternalStorageDirectory(), "Aoki")
        if (!appDir.exists()) {
            appDir.mkdir()
        }
        val fileName = "Aoki_qrlogin_${name}_${System.currentTimeMillis()}.png"
        val file = File(appDir, fileName)
        file.writeBytes(byteArray)

        val uri = Uri.fromFile(file)
        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
    }

    private fun Context.saveImageAboveAndroid10(name: String, byteArray: ByteArray) {
        val now = System.currentTimeMillis()
        val path = Environment.DIRECTORY_PICTURES + File.separator + "Aoki"
        val fileName = "Aoki_qrlogin_${name}_$now.png"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, path)
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.DATE_ADDED, now / 1000)
            put(MediaStore.MediaColumns.DATE_MODIFIED, now / 1000)
            put(MediaStore.MediaColumns.DATE_EXPIRES, (now + DateUtils.WEEK_IN_MILLIS) / 1000)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
        kotlin.runCatching {
            resolver.openOutputStream(uri)?.use { it.write(byteArray) }
            resolver.update(uri, values.apply {
                clear()
                put(MediaStore.MediaColumns.IS_PENDING, 0)
                putNull(MediaStore.MediaColumns.DATE_EXPIRES)
            }, null, null)
        }.onFailure {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                resolver.delete(uri, null)
        }.getOrThrow()
    }
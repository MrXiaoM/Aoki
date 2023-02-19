package top.mrxiaom.mirai.aoki

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Spinner
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.DeviceInfo
import top.mrxiaom.mirai.aoki.mirai.AokiDeviceInfoManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object U {
    /**
     * 浏览器 UA，提取自 QQ 客户端 8.4.1
     */
    val userAgent: String = arrayOf(
        "Mozilla/5.0",
        "(Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID}; wv)",
        "AppleWebKit/537.36",
        "(KHTML, like Gecko)",
        "Version/4.0",
        "Chrome/105.0.5195.136",
        "Mobile",
        "Safari/537.36",
        "V1_AND_SQ_8.4.1_1442_YYB_D",
        "QQ/8.4.1.4680",
        "NetType/4G",
        "WebP/0.4.1",
        "Pixel/720",
        "StatusBarHeight/49",
        "SimpleUISwitch/0",
        "QQTheme/2040"
    ).joinToString(" ")

    inline fun <reified T> AppCompatActivity.startActivity(
        conf: Intent.() -> Unit = {}
    ) where T : AppCompatActivity {
        this.startActivity(Intent(this, T::class.java).also {
            it.conf()
        })
    }

    @Suppress("DEPRECATION")
    inline fun <reified T> AppCompatActivity.startActivityForResult(
        requestId: Int,
        conf: Intent.() -> Unit = {}
    ) where T : AppCompatActivity {
        this.startActivityForResult(Intent(this, T::class.java).also {
            it.conf()
        }, requestId)
    }

    inline fun <reified T> Json.decodeFromStringOrNull(json: String): T? =
        try {
            decodeFromString<T>(json)
        } catch (_: Throwable) {
            null
        }

    fun Activity.needPermission(requestId: Int, vararg permissions: String) {
        val deniedPerms = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.ifEmpty { return }.toTypedArray()
        ActivityCompat.requestPermissions(this@needPermission, deniedPerms, requestId)
    }

    fun AlertDialog.Builder.buttonPositive(
        @StringRes textId: Int,
        onClick: DialogInterface.(Int) -> Unit
    ): AlertDialog.Builder =
        setPositiveButton(textId) { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.onClick(i)
        }

    fun AlertDialog.Builder.buttonPositive(text: String, onClick: DialogInterface.(Int) -> Unit = {}): AlertDialog.Builder =
        setPositiveButton(text) { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.onClick(i)
        }

    fun AlertDialog.Builder.buttonNegative(
        @StringRes textId: Int,
        onClick: DialogInterface.(Int) -> Unit = {}
    ): AlertDialog.Builder =
        setNegativeButton(textId) { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.onClick(i)
        }

    fun AlertDialog.Builder.buttonNegative(text: String, onClick: DialogInterface.(Int) -> Unit = {}): AlertDialog.Builder =
        setNegativeButton(text) { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.onClick(i)
        }

    fun AlertDialog.Builder.buttonNeutral(
        @StringRes textId: Int,
        onClick: DialogInterface.(Int) -> Unit = {}
    ): AlertDialog.Builder =
        setNeutralButton(textId) { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.onClick(i)
        }

    fun AlertDialog.Builder.buttonNeutral(text: String, onClick: DialogInterface.(Int) -> Unit = {}): AlertDialog.Builder =
        setNeutralButton(text) { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.onClick(i)
        }

    fun AlertDialog.Builder.setTitle(@StringRes messageId: Int, transform: (String) -> String): AlertDialog.Builder {
        return setTitle(transform(context.getText(messageId).toString()))
    }

    fun AlertDialog.Builder.setMessage(@StringRes messageId: Int, transform: (String) -> String): AlertDialog.Builder {
        return setMessage(transform(context.getText(messageId).toString()))
    }
    fun Context.text(@StringRes textId: Int): String = getText(textId).toString()

    fun Spinner.onItemSelected(selected: (Int) -> Unit) {
        onItemSelectedListener = object: OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) = selected(i)
            override fun onNothingSelected(p0: AdapterView<*>?) { selected(-1) }
        }
    }
    fun File.mkdirsQuietly(): Boolean =
        try {
            mkdirs()
        }catch (_: Throwable) {
            false
        }
    fun zip(srcPath: File, zipPath: File) {
        zipPath.parentFile?.mkdirsQuietly()
        val srcParent = srcPath.parentFile ?: return
        val out = ZipOutputStream(FileOutputStream(zipPath))
        zip(srcParent, srcPath, out)
        out.finish()
        out.close()
    }
    private fun zip(parent: File, path: File, out: ZipOutputStream) {
        val files = path.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                zip(parent, file, out)
                continue
            }
            val pathZip = file.toRelativeString(parent)
            val zipEntry = ZipEntry(pathZip)
            val input = FileInputStream(file)

            out.putNextEntry(zipEntry)
            out.write(input.readBytes())
            out.closeEntry()
        }
    }
}

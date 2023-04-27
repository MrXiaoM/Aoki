package top.mrxiaom.mirai.aoki.util

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateUtils
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import java.lang.IllegalStateException


private const val REQUEST_CODE_SAVE_IMG = 6
    private val EXTERNAL_PERMISSION = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    /**
     * 请求读取sd卡的权限
     */
    fun <T: Activity> T.requireExternalPermission(block: T.() -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasPermissions(*EXTERNAL_PERMISSION)) {
            ActivityCompat.requestPermissions(this, EXTERNAL_PERMISSION, REQUEST_CODE_SAVE_IMG)
        } else block()
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
        val resolver = contentResolver
        val relativePath = "Aoki"
        val fileName = "Aoki_qrlogin_${name}_${System.currentTimeMillis()}.png"
        var imageFile: File? = null
        val imageValues = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            val date = System.currentTimeMillis() / 1000
            put(MediaStore.Images.Media.DATE_ADDED, date)
            put(MediaStore.Images.Media.DATE_MODIFIED, date)
        }
        val collection: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val path = "${Environment.DIRECTORY_PICTURES}/${relativePath}"
            imageValues.apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.RELATIVE_PATH, path)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val saveDir = File(pictures, relativePath)

            if (!saveDir.exists() && !saveDir.mkdirs()) {
                throw FileNotFoundException(saveDir.absolutePath)
            }

            imageFile = File(saveDir, fileName)
            val fileNameWithoutExtension = imageFile.nameWithoutExtension
            val fileExtension = imageFile.extension

            var queryUri = resolver.queryMediaImage28(imageFile.absolutePath)
            var suffix = 1
            while (queryUri != null) {
                val newName = fileNameWithoutExtension + "(${suffix++})." + fileExtension
                imageFile = File(saveDir, newName)
                queryUri = resolver.queryMediaImage28(imageFile.absolutePath)
            }

            imageValues.apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageFile?.name)
                // 保存路径
                val imagePath = imageFile?.absolutePath
                put(MediaStore.Images.Media.DATA, imagePath)
            }
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val imageUri = resolver.insert(collection, imageValues)
        (imageUri?.outputStream(resolver) ?: throw IllegalStateException()).use { output ->
            output.write(byteArray)
            imageUri.finishPending(this, resolver, imageFile)
        }
    }.getOrNull() != null
}
private fun Uri.outputStream(resolver: ContentResolver): OutputStream? {
    return try {
        resolver.openOutputStream(this)
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
        null
    }
}
private fun Uri.finishPending(
    context: Context,
    resolver: ContentResolver,
    outputFile: File?,
) {
    val imageValues = ContentValues()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        if (outputFile != null) {
            imageValues.put(MediaStore.Images.Media.SIZE, outputFile.length())
        }
        resolver.update(this, imageValues, null, null)
        // 通知媒体库更新
        val intent = Intent(@Suppress("DEPRECATION") Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, this)
        context.sendBroadcast(intent)
    } else {
        // Android Q添加了IS_PENDING状态，为0时其他应用才可见
        imageValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(this, imageValues, null, null)
    }
}
private fun ContentResolver.queryMediaImage28(imagePath: String): Uri? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null

    val imageFile = File(imagePath)
    if (imageFile.canRead() && imageFile.exists()) {
        return Uri.fromFile(imageFile)
    }
    // 保存的位置
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    // 查询是否已经存在相同图片
    val query = this.query(
        collection,
        arrayOf(MediaStore.Images.Media._ID, @Suppress("DEPRECATION") MediaStore.Images.Media.DATA),
        "${@Suppress("DEPRECATION") MediaStore.Images.Media.DATA} == ?",
        arrayOf(imagePath), null
    )
    query?.use {
        while (it.moveToNext()) {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val id = it.getLong(idColumn)
            val existsUri = ContentUris.withAppendedId(collection, id)
            return existsUri
        }
    }
    return null
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

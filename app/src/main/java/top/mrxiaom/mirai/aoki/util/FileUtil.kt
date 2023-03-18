package top.mrxiaom.mirai.aoki.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun File.delFolder(child: String) {
    delFolder(File(this, child))
}
fun delFolder(file: File) {
    if (!file.isDirectory) {
        file.delete()
        return
    }
    for (list in file.listFiles() ?: arrayOf()) {
        if (list.isDirectory) {
            delFolder(list)
        }
        list.delete()
    }
    file.delete()
}
fun File.mkdirsQuietly(): Boolean =
    try {
        mkdirs()
    } catch (_: Throwable) {
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
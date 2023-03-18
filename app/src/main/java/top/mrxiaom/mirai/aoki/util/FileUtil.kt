package top.mrxiaom.mirai.aoki.util

import java.io.File

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
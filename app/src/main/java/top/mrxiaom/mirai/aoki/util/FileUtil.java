package top.mrxiaom.mirai.aoki.util;

import java.io.File;

public class FileUtil {
    public static void delAllFile(File file) {
        if (!file.isDirectory()) {
            file.delete();
            return;
        }
        for (File list : file.listFiles()) {
            if (list.isDirectory()) {
                delAllFile(list);
            }
            list.delete();
        }
        file.delete();
    }
}

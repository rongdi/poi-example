package com.example.utils;

import java.io.File;
import java.util.UUID;

/**
 * @author: rongdi
 * @date:
 */
public class FileUtils {

    public static File createCacheTmpFile() {
        String tempFilePrefix = System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID().toString() + File.separator;
        String cachePath = tempFilePrefix + "excache" + File.separator;
        return createDirectory(new File(cachePath + UUID.randomUUID().toString()));
    }

    private static File createDirectory(File directory) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new RuntimeException("Cannot create directory:" + directory.getAbsolutePath());
        } else {
            return directory;
        }
    }

}

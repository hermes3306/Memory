package com.jason.memory;

import android.os.Environment;

import java.io.File;

public class Config {
    private static String appname = "MEMORY";
    private static String version = "0.212";
    private static String tmp_dir  = "MEMORY.tmp";

    public static File getDownloadDir() {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), appname + version);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    public static File getTmpDir() {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), tmp_dir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

}

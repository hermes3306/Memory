package com.jason.memory;

import android.os.Environment;

import java.io.File;

public class Config {
    private static String appname = "MEMORY";
    private static String version = "0.3";
    private static String tmp_dir  = "MEMORY.temp";

    public static final String APPNAME = appname + version;

    public static final String BASE_URL = "http://58.233.69.198:8080/moment/";
    public static final String UPLOAD_URL = "http://58.233.69.198:8080/moment/upload.php";

    public static final String PREFS_NAME = "MyActivityPrefs";
    public static final String PREF_KEEP_SCREEN_ON = "keepScreenOn";
    public  static final String PREF_RUN_TYPE = "runType";
    public static final String RUN_TYPE_MEMORY = "memory";
    public static final String RUN_TYPE_MOMENT = "moment";

    public static final String PREF_ACTIVITY_ID = "activityID";
    public static final String PREF_HIDE_REASON = "hideReason";
    public static final String HIDE_REASON_BUTTON = "buttonHide";

    public static final String PREF_UPLOAD_SERVER ="uploadServer";
    public static final String PREF_UPLOAD_STRAVA ="uploadStrava";

    public static final String WEIGHT = "75.0";


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

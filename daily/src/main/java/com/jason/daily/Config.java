package com.jason.daily;

import android.os.Environment;
import java.io.File;

public class Config {
    private static String appname = "MEMORY";
    private static String version = "0.3014";
    private static String tmp_dir  = "temp";

    public static final String APPNAME = appname + version;

    public static final String BASE_URL = "http://58.233.69.198:8080/moment/";
    public static final String UPLOAD_URL = BASE_URL + "upload.php";
    public static final String DOWNLOAD_DIR = BASE_URL + "upload/";

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
    public static final String PLACE_EXT = ".place";
    public static final String MEMORY_EXT = ".memory";
    public static final String ACTIVITY_EXT = ".csv";
    public static final String DAILY_EXT = ".daily";



    public static File getDownloadDir() {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), APPNAME);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    public static File getDownloadDir4Places() {
        File directory = new File(getDownloadDir(),"json");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    public static File getDownloadDir4Memories() {
        File directory = new File(getDownloadDir(),"json");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    public static File getTmpDir() {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), APPNAME + '/' +tmp_dir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

}

package com.jason.memory;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class Config {
    private static String appname = "MEMORY";
    private static String version = "V1.1";

    public static final String APPNAME = appname + version;

    public static final String WEBSOCKET_URL ="ws://58.233.69.198:8765";
    public static final String BASE_URL = "http://58.233.69.198:8080/moment/";
    public static final String IMAGE_BASE_URL = BASE_URL + "images/";
    public static final String AUDIO_BASE_URL = BASE_URL + "audios/";
    public static final String VIDEO_BASE_URL = BASE_URL + "videos/";
    public static final String PROFILE_BASE_URL = BASE_URL + "profiles/";
    public static final String DOC_BASE_URL = BASE_URL + "documents/";

    public static final String UPLOAD_URL = BASE_URL + "upload.php";
    public static final String DOWNLOAD_DIR = BASE_URL + "upload/";

    public static final String PREFS_NAME = "MyActivityPrefs";
    public static final String PREF_KEEP_SCREEN_ON = "keepScreenOn";
    public static final String PREF_LOCATION_VALIDATION = "pref_location_validation";

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

    public static final double MIN_DISTANCE_THRESHOLD_KM = 0.0001; // 0.1 meter
    public static final double MAX_DISTANCE_THRESHOLD_KM = 1; // 13.5 meters
    public static final double MAX_SPEED_THRESHOLD_KMH = 120.0; // 120 km/h
    public static final long MIN_TIME_THRESHOLD_SECONDS = 1; // 1 second


    public static final boolean DEFAULT_LOCATION_VALIDATION = true;

    public static File getDownloadDir() {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), APPNAME);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    public static File getDownloadDir4Places() {
        return getDownloadDir();
    }

    public static File getDownloadDir4Memories() {
        return getDownloadDir();
    }

    public static File getTmpDir(Context context) {
        File tempDir = context.getCacheDir(); // Use the app's cache directory
        return tempDir;
    }

}

package com.jason.daily;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.Manifest;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LocationServiceChannel";

    public static final String ACTION_START_SERVICE = "com.jason.daily.START_SERVICE";
    public static final String ACTION_STOP_SERVICE = "com.jason.daily.STOP_SERVICE";
    public static final String ACTION_SERVICE_STATUS = "com.jason.daily.SERVICE_STATUS";
    public static final String ACTION_LOCATION_UPDATED = "com.jason.daily.LOCATION_UPDATED";
    public static final String ACTION_CHECK_STATUS = "com.jason.daily.CHECK_STATUS";
    public static final String ACTION_SEND_ALARM = "com.jason.daily.SEND_ALARM";

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseHelper dbHelper;
    private Handler handler;
    private Handler hourlyAlarmHandler;

    private static final int LOCATIONS_PER_DAY = 48; // Increased from 24 for better accuracy
    private static List<LocationData> dailyLocations = new ArrayList<>();
    private static final long INTERVAL = TimeUnit.DAYS.toMillis(1) / LOCATIONS_PER_DAY;
    private static final long HOURLY_ALARM_INTERVAL = TimeUnit.HOURS.toMillis(1);

    private boolean isFirstLocation = true;
    private static final String PREFS_NAME = "LocationServicePrefs";
    private static final String KEY_UPDATE_INTERVAL = "update_interval";
    private static final long DEFAULT_UPDATE_INTERVAL = 60000; // 1 minute in milliseconds
    private long updateInterval;

    private static final long UPLOAD_INTERVAL = 60 * 60 * 1000; // 1 hour in milliseconds
    private long lastUploadTime = 0;

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                Log.d(TAG, "--m-- Location result is null");
                return;
            }
            for (Location location : locationResult.getLocations()) {
                Log.d(TAG, "--m-- Processing location: " + location.getLatitude() + ", " + location.getLongitude());
                processLocation(location);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "--m-- LocationService onCreate");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dbHelper = new DatabaseHelper(this);
        handler = new Handler(Looper.getMainLooper());
        hourlyAlarmHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        loadUpdateInterval();
    }

    private void loadUpdateInterval() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        updateInterval = prefs.getLong(KEY_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL);
    }

    boolean isServiceRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "--m-- onStartCommand received action: " + action);
            if (ACTION_START_SERVICE.equals(action)) {
                if (!isServiceRunning && checkLocationPermissions()) {
                    startInForeground();
                    scheduleLocationUpdates();
                    scheduleHourlyAlarm();
                    isServiceRunning = true;
                } else if (!checkLocationPermissions()) {
                    Log.e(TAG, "--m-- Permissions not granted. Cannot start service.");
                    stopSelf();
                }
            }else if (ACTION_STOP_SERVICE.equals(action)) {
                stopService();
            } else if (ACTION_CHECK_STATUS.equals(action)) {
                sendBroadcast(new Intent(ACTION_SERVICE_STATUS).putExtra("isRunning", isServiceRunning));
            } else if ("UPDATE_INTERVAL".equals(action)) {
                long newInterval = intent.getLongExtra("interval", DEFAULT_UPDATE_INTERVAL);
                updateInterval(newInterval);
            }
        }
        return START_STICKY;
    }

    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void startInForeground() {
        Log.d(TAG, "--m-- Starting service in foreground");
        if (checkLocationPermissions()) {
            Log.d(TAG, "--m-- Starting service in foreground");
            try {
                // Create an intent to launch LocationMapActivity
                Intent mapIntent = new Intent(this, LocationMapActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                Notification notification = createNotification("Location Service", "Running in background", pendingIntent);
                // Send initial alarm
                sendInitialAlarm();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
                } else {
                    startForeground(NOTIFICATION_ID, notification);
                }

                requestLocationUpdates();
                sendBroadcast(new Intent(ACTION_SERVICE_STATUS).putExtra("isRunning", true));

            } catch (Exception e) {
                Log.e(TAG, "--m-- Unexpected error in startInForeground", e);
            }
        } else {
            Log.e(TAG, "--m-- Location permissions not granted. Cannot start foreground service.");
            stopSelf();
        }
    }



    public static final String ACTION_INITIAL_ALARM = "com.jason.daily.INITIAL_ALARM";

    private void sendInitialAlarm() {
        Intent alarmIntent = new Intent(this, LocationMapActivity.class);
        alarmIntent.setAction(ACTION_INITIAL_ALARM);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = createNotification(
                "Location Service Started",
                "The location service has been initialized",
                pendingIntent
        );

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(2, notification);  // Use a different ID from the service notification
        }
    }


    private Notification createNotification(String title, String content, PendingIntent pendingIntent) {
        Log.d(TAG, "--m-- Creating notification");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.memories48) // Make sure this icon exists
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
                    .setAutoCancel(true);
        }

        Log.d(TAG, "--m-- Notification built successfully");
        return builder.build();
    }

    private void stopService() {
        Log.d(TAG, "--m-- Stopping service");
        stopForeground(true);
        handler.removeCallbacksAndMessages(null);
        hourlyAlarmHandler.removeCallbacksAndMessages(null);
        fusedLocationClient.removeLocationUpdates(locationCallback);
        stopSelf();
        sendBroadcast(new Intent(ACTION_SERVICE_STATUS).putExtra("isRunning", false));
    }

    private void scheduleLocationUpdates() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "--m-- Capturing location");
                captureLocation();
                handler.postDelayed(this, updateInterval);
            }
        });
    }

    private void scheduleHourlyAlarm() {
        hourlyAlarmHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "--m-- Hourly alarm triggered");
                //sendLocationAlarm(null, "Hourly location update");
                hourlyAlarmHandler.postDelayed(this, HOURLY_ALARM_INTERVAL);
            }
        });
    }


    private void processLocation(Location location) {
        LocationData locationData = new LocationData(0, location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getTime());
        dbHelper.addLocation(location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getTime());

        if (isFirstLocation) {
            Log.d(TAG, "--m-- First location received");
            //sendLocationAlarm(location, "First location received");
            uploadLocations();
            isFirstLocation = false;
            lastUploadTime = System.currentTimeMillis();
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUploadTime >= UPLOAD_INTERVAL) {
            Log.d(TAG, "--m-- Uploading locations");
            uploadLocations();
            lastUploadTime = currentTime;
        }

        sendBroadcast(new Intent(ACTION_LOCATION_UPDATED));
    }

    private void uploadLocations() {
        Log.d(TAG, "--m-- Uploading locations");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = sdf.format(new Date()) + Config.DAILY_EXT;

        // Fetch locations from the database that haven't been uploaded yet
        List<LocationData> locationsToUpload = dbHelper.getLocationsToUpload(lastUploadTime);

        if (!locationsToUpload.isEmpty()) {
            Utility.uploadLocationsToServer(this, locationsToUpload, fileName);

            // Mark these locations as uploaded in the database
            dbHelper.markLocationsAsUploaded(locationsToUpload);
        }
    }

    private void sendLocationAlarm(Location location, String message) {
        Log.d(TAG, "--m-- Entering sendLocationAlarm method");
        Log.d(TAG, "--m-- Alarm message: " + message);

        String contentText = message != null ? message : "Tap to view your current location";
        String mapUrl = "https://www.google.com/maps/search/?api=1&query=";
        if (location != null) {
            mapUrl += location.getLatitude() + "," + location.getLongitude();
            Log.d(TAG, "--m-- Using provided location for alarm: " + location.getLatitude() + ", " + location.getLongitude());
        } else {
            LocationData lastLocation = dbHelper.getLastLocation();
            if (lastLocation != null) {
                mapUrl += lastLocation.getLatitude() + "," + lastLocation.getLongitude();
                Log.d(TAG, "--m-- Using last stored location for alarm: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
            } else {
                mapUrl += "0,0";
                Log.d(TAG, "--m-- No location available for alarm, using default coordinates");
            }
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = createNotification("Location Service Update", contentText, pendingIntent);
        showNotification(notification, (int) System.currentTimeMillis());
    }

    private Notification createNotification(String title, String content) {
        return createNotification(title, content, null);
    }




    private void showNotification(Notification notification, int notificationId) {
        Log.d(TAG, "--m-- Entering showNotification method");
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "--m-- POST_NOTIFICATIONS permission not granted");
            return;
        }

        try {
            notificationManager.notify(notificationId, notification);
            Log.d(TAG, "--m-- Notification shown successfully");
        } catch (Exception e) {
            Log.e(TAG, "--m-- Error showing notification", e);
        }
    }

    private void createNotificationChannel() {
        Log.d(TAG, "--m-- Creating notification channel");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            serviceChannel.enableVibration(true);
            serviceChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
            Log.d(TAG, "--m-- Notification channel created successfully");
        }
    }

    private void captureLocation() {
        Log.d(TAG, "--m-- Capturing location");
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            Log.d(TAG, "--m-- Location captured: " + location.getLatitude() + ", " + location.getLongitude());
                            processLocation(location);
                        } else {
                            Log.d(TAG, "--m-- Location capture returned null");
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "--m-- Error capturing location", e);
        }
    }

    // Add this method to update the interval
    public void updateInterval(long newInterval) {
        this.updateInterval = newInterval;
        // Restart location updates with new interval
        fusedLocationClient.removeLocationUpdates(locationCallback);
        requestLocationUpdates();
        scheduleLocationUpdates();
    }

    private void requestLocationUpdates() {
        Log.d(TAG, "--m-- Requesting location updates");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "--m-- Location permission not granted");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(updateInterval)
                .setMinUpdateIntervalMillis(updateInterval / 2)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
            Log.d(TAG, "--m-- Location updates requested successfully");
        } catch (SecurityException e) {
            Log.e(TAG, "--m-- Error requesting location updates", e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "--m-- LocationService onDestroy");
        stopService();
    }
}
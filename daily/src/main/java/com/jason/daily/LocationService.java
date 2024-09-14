package com.jason.daily;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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


public class LocationService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final String TAG = "LocationService";

    public static final String ACTION_START_SERVICE = "com.jason.daily.START_SERVICE";
    public static final String ACTION_STOP_SERVICE = "com.jason.daily.STOP_SERVICE";
    public static final String ACTION_SERVICE_STATUS = "com.jason.daily.SERVICE_STATUS";
    public static final String ACTION_LOCATION_UPDATED = "com.jason.daily.LOCATION_UPDATED";
    public static final String ACTION_CHECK_STATUS = "com.jason.daily.CHECK_STATUS";
    public static final String ACTION_SEND_ALARM = "com.jason.daily.SEND_ALARM";

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseHelper dbHelper;

    private static final int LOCATIONS_PER_DAY = 24 * 60; // Every minute for 24 hours
    private static List<LocationData> dailyLocations = new ArrayList<>();
    private static final long LOCATION_INTERVAL = 60 * 1000; // 1 minute
    private static final long HOURLY_UPLOAD_INTERVAL = 60 * 60 * 1000; // 1 hour

    private Handler locationHandler = new Handler();
    private Handler hourlyUploadHandler = new Handler();

    private Runnable locationRunnable = new Runnable() {
        @Override
        public void run() {
            captureLocation();
            locationHandler.postDelayed(this, LOCATION_INTERVAL);
        }
    };

    private Runnable hourlyUploadRunnable = new Runnable() {
        @Override
        public void run() {
            uploadLocations(false);
            hourlyUploadHandler.postDelayed(this, HOURLY_UPLOAD_INTERVAL);
        }
    };

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                processLocation(location);
            }
        }
    };

    private void processLocation(Location location) {
        LocationData locationData = new LocationData(0, location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getTime());
        dbHelper.addLocation(location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getTime());
        dailyLocations.add(locationData);

        if (dailyLocations.size() >= LOCATIONS_PER_DAY) {
            uploadLocations(true);
        }

        sendBroadcast(new Intent(ACTION_LOCATION_UPDATED));
    }

    private void uploadLocations(boolean isDaily) {
        SimpleDateFormat sdf = new SimpleDateFormat(isDaily ? "yyyyMMdd" : "yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = sdf.format(new Date()) + Config.DAILY_EXT;
        Utility.uploadLocationsToServer(this, dailyLocations, fileName);

        if (isDaily) {
            dailyLocations.clear();
        } else {
            // Keep only the last hour's worth of locations
            int locationsToKeep = 60;
            if (dailyLocations.size() > locationsToKeep) {
                dailyLocations = dailyLocations.subList(dailyLocations.size() - locationsToKeep, dailyLocations.size());
            }
        }

        sendLocationAlarm(null, isDaily ? "Daily location upload completed" : "Hourly location upload completed");
    }

    private void sendLocationAlarm(Location location, String message) {
        Log.d(TAG, "--m-- Sending location alarm: " + message);

        String contentText = message != null ? message : "Tap to view your current location";
        String mapUrl = "https://www.google.com/maps/search/?api=1&query=";
        if (location != null) {
            mapUrl += location.getLatitude() + "," + location.getLongitude();
        } else {
            LocationData lastLocation = dbHelper.getLastLocation();
            if (lastLocation != null) {
                mapUrl += lastLocation.getLatitude() + "," + lastLocation.getLongitude();
            } else {
                mapUrl += "0,0"; // Default to 0,0 if no location is available
            }
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.memories48)
                .setContentTitle("Location Service Update")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "--m-- POST_NOTIFICATIONS permission not granted");
            return;
        }

        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            Log.d(TAG, "--m-- Location alarm notification sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "--m-- Error sending location alarm notification", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dbHelper = new DatabaseHelper(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_SERVICE.equals(action)) {
                startInForeground();
                locationHandler.post(locationRunnable);
                hourlyUploadHandler.post(hourlyUploadRunnable);
            } else if (ACTION_STOP_SERVICE.equals(action)) {
                stopForeground(true);
                locationHandler.removeCallbacks(locationRunnable);
                hourlyUploadHandler.removeCallbacks(hourlyUploadRunnable);
                stopSelf();
                sendBroadcast(new Intent(ACTION_SERVICE_STATUS).putExtra("isRunning", false));
                return START_NOT_STICKY;
            } else if (ACTION_CHECK_STATUS.equals(action)) {
                sendBroadcast(new Intent(ACTION_SERVICE_STATUS).putExtra("isRunning", true));
            } else if (ACTION_SEND_ALARM.equals(action)) {
                String message = intent.getStringExtra("message");
                sendLocationAlarm(null, message);
            }
        }
        return START_STICKY;
    }

    private void startInForeground() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText("Running in background")
                .setSmallIcon(R.drawable.memories48)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        requestLocationUpdates();
        sendBroadcast(new Intent(ACTION_SERVICE_STATUS).putExtra("isRunning", true));
    }

    private void createNotificationChannel() {
        Log.d(TAG, "--m-- Creating notification channel");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_HIGH  // Changed from IMPORTANCE_LOW
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

    private void uploadLocations() {
        Log.d(TAG, "--m-- Uploading " + dailyLocations.size() + " locations");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = sdf.format(new Date()) + Config.DAILY_EXT;
        Utility.uploadLocationsToServer(this, dailyLocations, fileName);
        dailyLocations.clear();
    }

    private void requestLocationUpdates() {
        Log.d(TAG, "--m-- Requesting location updates");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "--m-- Location permission not granted");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(10000)
                .setMinUpdateIntervalMillis(5000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    getMainLooper());
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
}

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

    private static final int LOCATIONS_PER_DAY = 24;
    private static List<LocationData> dailyLocations = new ArrayList<>();
    private static final long INTERVAL = 24 * 60 * 60 * 1000 / LOCATIONS_PER_DAY;
    private static final long HOURLY_ALARM_INTERVAL = 60 * 60 * 1000; // 1 hour

    private Handler handler = new Handler();
    private Runnable locationRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "--m-- Capturing location");
            captureLocation();
            handler.postDelayed(this, INTERVAL);
        }
    };

    private Handler hourlyAlarmHandler = new Handler();
    private Runnable hourlyAlarmRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "--m-- Hourly alarm triggered");
            sendLocationAlarm(null, "Hourly location update");
            hourlyAlarmHandler.postDelayed(this, HOURLY_ALARM_INTERVAL);
        }
    };

    private boolean isFirstLocation = true;

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

    private void processLocation(Location location) {
        LocationData locationData = new LocationData(0, location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getTime());
        dbHelper.addLocation(location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getTime());
        dailyLocations.add(locationData);

        if (isFirstLocation) {
            Log.d(TAG, "--m-- First location received");
            sendLocationAlarm(location, "First location received");
            isFirstLocation = false;
        }

        if (dailyLocations.size() >= LOCATIONS_PER_DAY) {
            Log.d(TAG, "--m-- Uploading locations");
            uploadLocations();
        }

        sendBroadcast(new Intent(ACTION_LOCATION_UPDATED));
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
                mapUrl += "0,0"; // Default to 0,0 if no location is available
                Log.d(TAG, "--m-- No location available for alarm, using default coordinates");
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

        Log.d(TAG, "--m-- Attempting to show alarm notification");
        showNotification(builder.build(), (int) System.currentTimeMillis());
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

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "--m-- LocationService onCreate");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dbHelper = new DatabaseHelper(this);
        createNotificationChannel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "--m-- LocationService onDestroy");
        fusedLocationClient.removeLocationUpdates(locationCallback);
        handler.removeCallbacks(locationRunnable);
        hourlyAlarmHandler.removeCallbacks(hourlyAlarmRunnable);
        sendBroadcast(new Intent(ACTION_SERVICE_STATUS).putExtra("isRunning", false));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "--m-- onStartCommand received action: " + action);
            if (ACTION_START_SERVICE.equals(action)) {
                startInForeground();
                handler.post(locationRunnable);
                hourlyAlarmHandler.postDelayed(hourlyAlarmRunnable, HOURLY_ALARM_INTERVAL);
            } else if (ACTION_STOP_SERVICE.equals(action)) {
                Log.d(TAG, "--m-- Stopping service");
                stopForeground(true);
                handler.removeCallbacks(locationRunnable);
                hourlyAlarmHandler.removeCallbacks(hourlyAlarmRunnable);
                stopSelf();
                sendBroadcast(new Intent(ACTION_SERVICE_STATUS).putExtra("isRunning", false));
                return START_NOT_STICKY;
            } else if (ACTION_CHECK_STATUS.equals(action)) {
                sendBroadcast(new Intent(ACTION_SERVICE_STATUS).putExtra("isRunning", true));
            } else if (ACTION_SEND_ALARM.equals(action)) {
                String message = intent.getStringExtra("message");
                Log.d(TAG, "--m-- Received ACTION_SEND_ALARM with message: " + message);
                sendLocationAlarm(null, message);
            } else {
                Log.d(TAG, "--m-- Received unknown action: " + action);
            }
        } else {
            Log.d(TAG, "--m-- onStartCommand received null intent");
        }
        return START_STICKY;
    }

    private void startInForeground() {
        Log.d(TAG, "--m-- Starting service in foreground");
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

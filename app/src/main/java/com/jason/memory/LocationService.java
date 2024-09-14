package com.jason.memory;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import android.content.pm.ServiceInfo;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocationService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final String TAG = "LocationService";

    public static final String ACTION_START_SERVICE = "com.jason.memory.START_SERVICE";
    public static final String ACTION_STOP_SERVICE = "com.jason.memory.STOP_SERVICE";
    public static final String ACTION_SERVICE_STATUS = "com.jason.memory.SERVICE_STATUS";
    public static final String ACTION_LOCATION_UPDATED = "com.jason.memory.LOCATION_UPDATED";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseHelper dbHelper;

    private static final int LOCATIONS_PER_DAY = 24;
    private static List<LocationData> dailyLocations = new ArrayList<>();
    private static final long INTERVAL = 24 * 60 * 60 * 1000 / LOCATIONS_PER_DAY; // Interval for 24 locations per day

    private Handler handler = new Handler();
    private Runnable locationRunnable = new Runnable() {
        @Override
        public void run() {
            captureLocation();
            handler.postDelayed(this, INTERVAL);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_SERVICE.equals(action)) {
                startForegroundService();
                handler.post(locationRunnable);
            } else if (ACTION_STOP_SERVICE.equals(action)) {
                stopForeground(true);
                handler.removeCallbacks(locationRunnable);
                stopSelf();
                sendBroadcast(new Intent(ACTION_SERVICE_STATUS));
                return START_NOT_STICKY;
            }
        }
        return START_STICKY;
    }



    private void captureLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            LocationData locationData = new LocationData(0, location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getTime());
                            dailyLocations.add(locationData);
                            if (dailyLocations.size() >= LOCATIONS_PER_DAY) {
                                uploadLocations();
                            }
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Error capturing location", e);
        }
    }

    private void uploadLocations() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = sdf.format(new Date()) + ".loc";
        Utility.uploadLocationsToServer(this, dailyLocations, fileName);
        dailyLocations.clear();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dbHelper = new DatabaseHelper(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    dbHelper.addLocation(location.getLatitude(), location.getLongitude(),
                            location.getAltitude(), location.getTime());
                    sendBroadcast(new Intent(ACTION_LOCATION_UPDATED));
                }
            }
        };
    }

    private void startForegroundService() {
        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }
        requestLocationUpdates();
        sendBroadcast(new Intent(ACTION_SERVICE_STATUS));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Tracking")
                .setContentText("Tracking your location in the background")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
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
        fusedLocationClient.removeLocationUpdates(locationCallback);
        sendBroadcast(new Intent(ACTION_SERVICE_STATUS));
    }
}
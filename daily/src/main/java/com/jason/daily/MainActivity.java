package com.jason.daily;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import android.app.ActivityManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final long SERVICE_CHECK_DELAY = 5000; // 5 seconds
    private static final long SERVICE_START_TIMEOUT = 10000; // 10 seconds

    private BroadcastReceiver serviceStatusReceiver;
    private Handler handler = new Handler();
    private boolean isServiceRunning = false;

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;
    private static final int BATTERY_OPTIMIZATION_REQUEST_CODE = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "--m-- MainActivity onCreate");

        if (isLocationServiceRunning()) {
            launchMapActivityAndSendAlarm();
            Toast.makeText(this, "Location Service is already running", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            checkAndRequestPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isLocationServiceRunning()) {
            launchMapActivityAndSendAlarm();
            Toast.makeText(this, "Location Service is running in the background", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void launchMapActivityAndSendAlarm() {
        // Send initial alarm
        sendInitialAlarm();

        // Create an intent to launch LocationMapActivity
        Intent mapIntent = new Intent(this, LocationMapActivity.class);
        mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Use a Handler to delay finishing the activity and launching the map
        new Handler().postDelayed(() -> {
            startActivity(mapIntent);
            finish();
        }, 1000); // 1 second delay
    }

    private void sendInitialAlarm() {
        Intent alarmIntent = new Intent(this, LocationService.class);
        alarmIntent.setAction(LocationService.ACTION_SEND_ALARM);
        alarmIntent.putExtra("message", "Location service is running");
        startService(alarmIntent);
    }


    private void startLocationServiceAndWait() {
        if (!isLocationServiceRunning()) {
            Intent serviceIntent = new Intent(this, LocationService.class);
            serviceIntent.setAction(LocationService.ACTION_START_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "--m-- Starting foreground service");
                startForegroundService(serviceIntent);
            } else {
                Log.d(TAG, "--m-- Starting service");
                startService(serviceIntent);
            }

            registerServiceStatusReceiver();

            // Check service status after a delay
            handler.postDelayed(this::checkServiceStatus, SERVICE_CHECK_DELAY);

            // Set a timeout for service start
            handler.postDelayed(this::handleServiceStartTimeout, SERVICE_START_TIMEOUT);
        } else {
            Toast.makeText(this, "Location Service is already running", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    private void registerServiceStatusReceiver() {
        serviceStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (LocationService.ACTION_SERVICE_STATUS.equals(intent.getAction())) {
                    boolean isRunning = intent.getBooleanExtra("isRunning", false);
                    handleServiceStatus(isRunning);
                }
            }
        };
        IntentFilter filter = new IntentFilter(LocationService.ACTION_SERVICE_STATUS);
        registerReceiver(serviceStatusReceiver, filter);
    }

    private void checkServiceStatus() {
        Intent checkStatusIntent = new Intent(this, LocationService.class);
        checkStatusIntent.setAction(LocationService.ACTION_CHECK_STATUS);
        startService(checkStatusIntent);
    }

    private void handleServiceStatus(boolean isRunning) {
        isServiceRunning = isRunning;
        if (isRunning) {
            Log.d(TAG, "--m-- Location service is running successfully");
            sendAlarmMessage("Location service is running well");
            handler.removeCallbacksAndMessages(null);
            finish();
        } else {
            Log.w(TAG, "--m-- Location service is not running");
            Toast.makeText(this, "Failed to start Location Service", Toast.LENGTH_LONG).show();
        }
    }

    private void handleServiceStartTimeout() {
        if (!isServiceRunning) {
            Log.e(TAG, "--m-- Service start timeout");
            Toast.makeText(this, "Service start timeout. Please try again.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void sendAlarmMessage(String message) {
        Intent alarmIntent = new Intent(this, LocationService.class);
        alarmIntent.setAction(LocationService.ACTION_SEND_ALARM);
        alarmIntent.putExtra("message", message);
        startService(alarmIntent);
    }

    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            checkBatteryOptimization();
        }
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Log.d(TAG, "--m-- Requesting to disable battery optimization");
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST_CODE);
            } else {
                Log.d(TAG, "--m-- Battery optimization already disabled");
                startLocationServiceAndWait();
            }
        } else {
            startLocationServiceAndWait();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                checkBatteryOptimization();
            } else {
                showPermissionDeniedDialog("Required");
                // You might want to finish() the activity here if permissions are essential
            }
        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BATTERY_OPTIMIZATION_REQUEST_CODE) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Log.d(TAG, "--m-- Battery optimization disabled successfully");
                startLocationServiceAndWait();
            } else {
                Log.d(TAG, "--m-- Battery optimization still enabled");
                // You might want to show a dialog explaining why battery optimization is important
                // and give the user an option to try again or continue without it
                startLocationServiceAndWait();
            }
        }
    }


    private void showPermissionDeniedDialog(String permissionType) {
        new AlertDialog.Builder(this)
                .setTitle(permissionType + " Permissions Required")
                .setMessage("This app needs the required permissions to function properly. Without these permissions, the app cannot provide its core features.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceStatusReceiver != null) {
            unregisterReceiver(serviceStatusReceiver);
        }
        handler.removeCallbacksAndMessages(null);
    }
}
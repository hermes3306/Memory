package com.jason.daily;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;
    private static final long SERVICE_CHECK_DELAY = 5000; // 5 seconds

    private BroadcastReceiver serviceStatusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "--m-- MainActivity onCreate");

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
        } else {
            // All permissions granted, proceed with app initialization
            startLocationServiceAndFinish();
        }
    }

    private void startLocationServiceAndFinish() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_START_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "--m-- Starting foreground service");
            startForegroundService(serviceIntent);
        } else {
            Log.d(TAG, "--m-- Starting service");
            startService(serviceIntent);
        }

        // Register receiver to check if service is running
        serviceStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (LocationService.ACTION_SERVICE_STATUS.equals(intent.getAction())) {
                    boolean isRunning = intent.getBooleanExtra("isRunning", false);
                    if (isRunning) {
                        Log.d(TAG, "--m-- Location service is running successfully");
                        // Send alarm message to indicate service is running
                        Intent alarmIntent = new Intent(MainActivity.this, LocationService.class);
                        alarmIntent.setAction(LocationService.ACTION_SEND_ALARM);
                        alarmIntent.putExtra("message", "Location service is running well");
                        startService(alarmIntent);
                    } else {
                        Log.w(TAG, "--m-- Location service is not running");
                    }
                    unregisterReceiver(serviceStatusReceiver);
                    finish();
                }
            }
        };

        IntentFilter filter = new IntentFilter(LocationService.ACTION_SERVICE_STATUS);
        registerReceiver(serviceStatusReceiver, filter);

        // Check service status after a delay
        new Handler().postDelayed(() -> {
            Intent checkStatusIntent = new Intent(this, LocationService.class);
            checkStatusIntent.setAction(LocationService.ACTION_CHECK_STATUS);
            startService(checkStatusIntent);
        }, SERVICE_CHECK_DELAY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, check for notification permission
                checkAndRequestPermissions();
            } else {
                // Location permission denied, show explanation and option to open settings
                showPermissionDeniedDialog("Location");
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Notification permission granted, proceed with app initialization
                startLocationServiceAndFinish();
            } else {
                // Notification permission denied, show explanation and option to open settings
                showPermissionDeniedDialog("Notification");
            }
        }
    }

    private void showPermissionDeniedDialog(String permissionType) {
        new AlertDialog.Builder(this)
                .setTitle(permissionType + " Permission Required")
                .setMessage("This app needs " + permissionType.toLowerCase() + " access to function properly. Without this permission, the app cannot provide its core features.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Exit", (dialog, which) -> {
                    // User doesn't want to grant permissions, so we exit the app
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceStatusReceiver != null) {
            unregisterReceiver(serviceStatusReceiver);
        }
    }
}
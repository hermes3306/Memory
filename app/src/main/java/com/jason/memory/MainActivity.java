package com.jason.memory;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_LOCATION = 100;
    private Button startServiceButton;
    private Button stopServiceButton;
    private TextView statusTextView;
    private BroadcastReceiver serviceStatusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startServiceButton = findViewById(R.id.startServiceButton);
        stopServiceButton = findViewById(R.id.stopServiceButton);
        statusTextView = findViewById(R.id.statusTextView);
        Button monitorButton = findViewById(R.id.monitorButton);

        startServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionsAndStartService();
            }
        });

        stopServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationService();
            }
        });

        monitorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MonitorActivity.class));
            }
        });

        Button btnShowMap = findViewById(R.id.btnShowMap);
        btnShowMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(intent);
            }
        });

        Button btnMemoryActivity = findViewById(R.id.memoryButton);
        btnMemoryActivity.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MemoryActivity.class);
            startActivity(intent);
        });

        Button btnBehaviorAnalysis = findViewById(R.id.btnBehaviorAnalysis);
        btnBehaviorAnalysis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BehaviorAnalysisActivity.class);
                startActivity(intent);
            }
        });



        // Start MyActivity 버튼에 대한 리스너 추가
        Button btnStartMyActivity = findViewById(R.id.btnStartMyActivity);
        btnStartMyActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityWithTracking(MyActivity.class);
            }
        });

        // Start MyActivity2 버튼에 대한 리스너 추가
        Button btnStartMyActivity2 = findViewById(R.id.btnStartMyActivity2);
        btnStartMyActivity2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityWithTracking(MyActivity2.class);
            }
        });


        Button btnListActivity = findViewById(R.id.btnListActivity);
        btnListActivity.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ListActivityActivity.class);
            startActivity(intent);
        });


        Button btnListCloud = findViewById(R.id.btnListCloud);
        btnListCloud.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ListCloudActivity.class);
            startActivity(intent);
        });


        // Start the service when the app starts
        startServiceIfPermissionsGranted();

        updateUI();

        // Register BroadcastReceiver
        IntentFilter filter = new IntentFilter(LocationService.ACTION_SERVICE_STATUS);
        serviceStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUI();
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStatusReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(serviceStatusReceiver, filter);
        }

        // Add this line to check for unfinished activities when the app starts
        checkForUnfinishedActivity();
    }

    private void startServiceIfPermissionsGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationService();
        } else {
            checkPermissionsAndStartService();
        }
    }

    private void startActivityWithTracking(final Class<?> activityClass) {
        if (!isLocationServiceRunning()) {
            // If the location service is not running, start it first
            checkPermissionsAndStartService();

            // Wait for a short time to ensure the service has started
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    launchActivity(activityClass);
                }
            }, 1000); // 1 second delay
        } else {
            // If the service is already running, just start the activity
            launchActivity(activityClass);
        }
    }

    private void launchActivity(Class<?> activityClass) {
        Intent intent = new Intent(MainActivity.this, activityClass);
        startActivity(intent);
    }


    private void checkForUnfinishedActivity() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        ActivityData unfinishedActivity = dbHelper.getUnfinishedActivity();

        if (unfinishedActivity != null) {
            long currentTime = System.currentTimeMillis();
            long timeDifference = currentTime - unfinishedActivity.getStartTimestamp();
            long hoursDifference = timeDifference / (60 * 60 * 1000);

            if (hoursDifference < 24) { // Only ask if the activity started less than 24 hours ago
                showUnfinishedActivityDialog(unfinishedActivity);
            }
        }
    }

    private void showUnfinishedActivityDialog(final ActivityData activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Unfinished Activity");
        builder.setMessage("You have an unfinished activity. Do you want to resume it?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(MainActivity.this, MyActivity.class);
                intent.putExtra("ACTIVITY_ID", activity.getId());
                startActivity(intent);
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceStatusReceiver != null) {
            unregisterReceiver(serviceStatusReceiver);
        }
    }

    private void checkPermissionsAndStartService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
        } else {
            startLocationService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                // Permission denied, you might want to inform the user that the service can't start without permission
            }
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_START_SERVICE);
        startService(serviceIntent);
        updateUI();
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_STOP_SERVICE);
        startService(serviceIntent);
        // Add a small delay before updating UI to ensure service has time to stop
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        }, 200); // 200ms delay
    }

    private void updateUI() {
        boolean isRunning = isLocationServiceRunning();
        statusTextView.setText(isRunning ? "Service is running" : "Service is stopped");
        startServiceButton.setEnabled(!isRunning);
        stopServiceButton.setEnabled(isRunning);
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
}
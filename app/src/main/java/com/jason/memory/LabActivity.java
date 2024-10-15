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
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.jason.memory.lab.LabRunActivity;
import com.jason.memory.lab.StravaActivity;

public class LabActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_LOCATION = 100;
    private Button startServiceButton;
    private Button stopServiceButton;
    private TextView statusTextView;
    private BroadcastReceiver serviceStatusReceiver;
    private Spinner labSpinner;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab);
        context = this;

        if (getIntent().getBooleanExtra("START_FROM_BOOT", false)) {
            // The app was started from boot, you might want to start your service here
            startServiceIfPermissionsGranted();
        }


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
                startActivity(new Intent(LabActivity.this, MonitorActivity.class));
            }
        });

        Button btnShowMap = findViewById(R.id.btnShowMap);
        btnShowMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LabActivity.this, MapActivity.class);
                startActivity(intent);
            }
        });


        Button btnBehaviorAnalysis = findViewById(R.id.btnBehaviorAnalysis);
        btnBehaviorAnalysis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LabActivity.this, BehaviorAnalysisActivity.class);
                startActivity(intent);
            }
        });

        ImageView settingsIcon = findViewById(R.id.settingsIcon);

        // 클릭 리스너 설정
        settingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LabActivity.this, SettingActivity.class);
                // SettingActivity 시작
                startActivity(intent);
            }
        });

        Button btnListCloud = findViewById(R.id.btnListCloud);
        btnListCloud.setOnClickListener(v -> {
            Intent intent = new Intent(LabActivity.this, ListCloudActivity.class);
            startActivity(intent);
        });

        // In LabActivity.java, add this inside onCreate() method
        Button btnFileList = findViewById(R.id.btnFileList);
        btnFileList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LabActivity.this, FileActivity.class);
                startActivity(intent);
            }
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

        setupLabSpinner();
    }

    private void setupLabSpinner() {
        labSpinner = findViewById(R.id.labSpinner);

        // Create an ArrayAdapter using a simple spinner layout and lab options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.lab_options, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        labSpinner.setAdapter(adapter);

        // Set the item selected listener
        labSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = parent.getItemAtPosition(position).toString();
                if (selectedOption.equals("포스트")) {
                    startActivity(new Intent(LabActivity.this, PostActivity.class));
                } else
                if (selectedOption.equals("LabRun")) {
                    startActivity(new Intent(LabActivity.this, LabRunActivity.class));
                } else
                if (selectedOption.equals("PlaceUserJason")) {
                    try {
                        DatabaseHelper databasehelper = new DatabaseHelper(context);
                        databasehelper.updateAllPlacesUserId("Jason");
                    }catch(Exception exception){
                        Log.e("LabActivity", " --m-- " + exception.getMessage());
                    }
                }else
                if (selectedOption.equals("Strava")) {
                    startActivity(new Intent(LabActivity.this, StravaActivity.class));
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
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
        Intent intent = new Intent(LabActivity.this, activityClass);
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
                Intent intent = new Intent(LabActivity.this, MyActivity.class);
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
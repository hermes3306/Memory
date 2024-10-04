package com.jason.memory.lab;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.jason.memory.Config;
import com.jason.memory.MapActivity;
import com.jason.memory.R;
import static android.content.Context.RECEIVER_NOT_EXPORTED;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class LabRunActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private Location lastLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Button startStopButton;
    private TextView currentTimeTextView;
    private TextView distanceTextView;
    private TextView elapsedTimeTextView;
    private TextView speedTextView;
    private TextView trackingCountTextView;
    private View locationIndicator;
    private TextView hideTextView;
    private Button mapButton;

    private Handler handler = new Handler();
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss (EEEE)", Locale.getDefault());

    private KalmanFilter latitudeFilter = new KalmanFilter(0.0001, 0.0001, 0);
    private KalmanFilter longitudeFilter = new KalmanFilter(0.0001, 0.0001, 0);

    private ActivityRecognitionClient activityRecognitionClient;
    private PendingIntent activityRecognitionPendingIntent;

    // Static variables to persist data
    private static boolean isTracking = false;
    private static long startTime = 0;
    private static float totalDistance = 0f;
    private static List<Location> locationList = new ArrayList<>();
    private static int trackingCount = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_run);

        // Initialize views
        startStopButton = findViewById(R.id.startStopButton);
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        distanceTextView = findViewById(R.id.distanceTextView);
        elapsedTimeTextView = findViewById(R.id.elapsedTimeTextView);
        speedTextView = findViewById(R.id.speedTextView);
        trackingCountTextView = findViewById(R.id.trackingCountTextView);
        locationIndicator = findViewById(R.id.locationIndicator);
        hideTextView = findViewById(R.id.hideTextView);
        mapButton = findViewById(R.id.mapButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateLocation(location);
                }
            }
        };

        startStopButton = findViewById(R.id.startStopButton);
        startStopButton.setOnClickListener(v -> {
            if (isTracking) {
                stopTracking();
                saveActivityToFile();
            } else {
                startTracking();
            }
        });

        hideTextView.setOnClickListener(v -> hideActivity());
        mapButton.setOnClickListener(v -> showMap());

        updateUI();
        updateButtonState();
        startTimeUpdates();
        setupActivityRecognition();

        // If tracking was already in progress, resume location updates
        if (isTracking) {
            resumeLocationUpdates();
        }

        // Register the broadcast receiver
        IntentFilter filter = new IntentFilter("ACTION_DETECTED_ACTIVITY");
        registerReceiver(activityRecognitionReceiver, filter, RECEIVER_NOT_EXPORTED);
    }

    private void hideActivity() {
        if (isTracking) {
            Toast.makeText(this, "Tracking continues in the background", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Start tracking before hiding", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMap() {
        if (locationList.isEmpty()) {
            Toast.makeText(this, "No location data available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent mapIntent = new Intent(this, MapActivity.class);
        mapIntent.putExtra("locations", convertLocationsToLatLngList());
        startActivity(mapIntent);
    }

    private void startTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        isTracking = true;
        startTime = System.currentTimeMillis();
        locationList.clear();
        totalDistance = 0f;
        trackingCount = 0;

        resumeLocationUpdates();
        updateUI();
        updateButtonState();
    }

    private void stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isTracking = false;
        updateUI();
        updateButtonState();
    }

    private void resumeLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(1000)
                .setFastestInterval(500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(1);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void updateUI() {
        distanceTextView.setText("Distance: " + (int) totalDistance + " meters");
        trackingCountTextView.setText("Tracking Count: " + trackingCount);
        updateElapsedTime();
        updateSpeed();
        mapButton.setEnabled(!locationList.isEmpty());
    }

    private void updateButtonState() {
        if (isTracking) {
            startStopButton.setText("Stop");
        } else {
            startStopButton.setText("Start");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        updateButtonState();
    }


    private void updateLocation(Location location) {
        Location filteredLocation = filterLocation(location);
        if (!locationList.isEmpty()) {
            Location lastLocation = locationList.get(locationList.size() - 1);
            float distance = lastLocation.distanceTo(filteredLocation);
            if (distance > 0.5) { // Only update if moved more than 0.5 meters
                totalDistance += distance;
                locationList.add(filteredLocation);
                trackingCount++;
                updateUI();
                blinkLocationIndicator();
            }
        } else {
            locationList.add(filteredLocation);
            trackingCount++;
            updateUI();
            blinkLocationIndicator();
        }
    }

    private void updateElapsedTime() {
        long elapsedMillis = System.currentTimeMillis() - startTime;
        long seconds = elapsedMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        elapsedTimeTextView.setText(String.format("Elapsed Time: %02d:%02d:%02d", hours, minutes % 60, seconds % 60));
    }

    private void updateSpeed() {
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        if (elapsedSeconds > 0 && totalDistance > 0) {
            float speedMinPerKm = (elapsedSeconds / 60f) / (totalDistance / 1000f);
            int minutes = (int) speedMinPerKm;
            int seconds = (int) ((speedMinPerKm - minutes) * 60);
            speedTextView.setText(String.format("Speed: %d:%02d min/km", minutes, seconds));
        }
    }

    private void blinkLocationIndicator() {
        locationIndicator.setBackgroundResource(R.drawable.location_indicator_active);
        handler.postDelayed(() -> locationIndicator.setBackgroundResource(R.drawable.location_indicator), 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isTracking && isFinishing()) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        handler.removeCallbacksAndMessages(null);
        unregisterReceiver(activityRecognitionReceiver);
    }

    private static class KalmanFilter {
        private double processNoise;
        private double measurementNoise;
        private double estimatedValue;
        private double errorCovariance;

        public KalmanFilter(double processNoise, double measurementNoise, double estimatedValue) {
            this.processNoise = processNoise;
            this.measurementNoise = measurementNoise;
            this.estimatedValue = estimatedValue;
            this.errorCovariance = 1;
        }

        public double update(double measurement) {
            // Prediction update
            double predictedValue = estimatedValue;
            double predictedErrorCovariance = errorCovariance + processNoise;

            // Measurement update
            double kalmanGain = predictedErrorCovariance / (predictedErrorCovariance + measurementNoise);
            estimatedValue = predictedValue + kalmanGain * (measurement - predictedValue);
            errorCovariance = (1 - kalmanGain) * predictedErrorCovariance;

            return estimatedValue;
        }
    }

    private ActivityRecognitionReceiver activityRecognitionReceiver = new ActivityRecognitionReceiver();

    public void handleDetectedActivity(String activityType) {
        // Implement logic based on the detected activity
        // For example, you might want to pause location updates if the user is still
        if (activityType.equals("Still")) {
            // Consider pausing location updates
        } else if (activityType.equals("Running") || activityType.equals("Walking")) {
            // Ensure location updates are active
        }
    }


    private Location filterLocation(Location location) {
        if (lastLocation == null) {
            latitudeFilter = new KalmanFilter(0.0001, 0.0001, location.getLatitude());
            longitudeFilter = new KalmanFilter(0.0001, 0.0001, location.getLongitude());
            return location;
        }

        double filteredLat = latitudeFilter.update(location.getLatitude());
        double filteredLon = longitudeFilter.update(location.getLongitude());

        Location filteredLocation = new Location(location);
        filteredLocation.setLatitude(filteredLat);
        filteredLocation.setLongitude(filteredLon);
        return filteredLocation;
    }

    private void saveActivityToFile() {
        if (locationList.isEmpty()) {
            Toast.makeText(this, "No activity data to save", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = fileNameFormat.format(new Date(startTime)) + ".csv";

        File directory = Config.getDownloadDir(this);
        if (!directory.exists()) {
            boolean dirCreated = directory.mkdirs();
            if (!dirCreated) {
                Toast.makeText(this, "Failed to create directory", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        File file = new File(directory, fileName);

        try (FileWriter writer = new FileWriter(file)) {
            writer.append("x,y,z,t\n");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());

            for (Location location : locationList) {
                writer.append(String.format(Locale.US, "%.8f,%.8f,%s\n",
                        location.getLatitude(),
                        location.getLongitude(),
                        dateFormat.format(new Date(location.getTime()))));
            }

            Toast.makeText(this, "Activity saved to " + fileName, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to save activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupActivityRecognition() {
        activityRecognitionClient = ActivityRecognition.getClient(this);
        Intent intent = new Intent(this, ActivityRecognitionIntentService.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        activityRecognitionPendingIntent = PendingIntent.getService(this, 0, intent, flags);

        Task<Void> task = activityRecognitionClient.requestActivityUpdates(
                5000, // Detection interval in milliseconds
                activityRecognitionPendingIntent);

        task.addOnSuccessListener(result -> {
            // Successfully registered for activity recognition updates
        });

        task.addOnFailureListener(e -> {
            // Failed to register for activity recognition updates
        });
    }

    private ArrayList<LatLng> convertLocationsToLatLngList() {
        ArrayList<LatLng> latLngList = new ArrayList<>();
        for (Location location : locationList) {
            latLngList.add(new LatLng(location.getLatitude(), location.getLongitude()));
        }
        return latLngList;
    }

    private void startTimeUpdates() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                currentTimeTextView.setText("Current Time: " + timeFormat.format(new Date()));
                if (isTracking) {
                    updateElapsedTime();
                }
                handler.postDelayed(this, 1000);
            }
        });
    }

}
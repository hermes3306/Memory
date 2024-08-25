package com.jason.memory;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private long activityId;
    private long startTimestamp;
    private Handler handler = new Handler();
    private Runnable updateRunnable;

    private TextView tvTime, tvPace, tvDistance;
    private Button btnMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        dbHelper = new DatabaseHelper(this);

        tvTime = findViewById(R.id.tvTime);
        tvPace = findViewById(R.id.tvPace);
        tvDistance = findViewById(R.id.tvDistance);
        btnMap = findViewById(R.id.btnMap);

        Button btnStopActivity = findViewById(R.id.btnStopActivity);
        btnStopActivity.setOnClickListener(v -> stopActivity());

        btnMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(MyActivity.this, RunningMapActivity.class);
            mapIntent.putExtra("startTimestamp", startTimestamp);
            startActivity(mapIntent);
        });

        activityId = getIntent().getLongExtra("ACTIVITY_ID", -1);
        if (activityId != -1) {
            resumeActivity();
        } else {
            startNewActivity();
        }
    }

    private void resumeActivity() {
        ActivityData activity = dbHelper.getActivity(activityId);
        if (activity != null) {
            startTimestamp = activity.getStartTimestamp();
            // Initialize other fields based on the resumed activity
            updateUI();
            startUpdates();
        } else {
            // Handle error: activity not found
            Toast.makeText(this, "Error: Activity not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startNewActivity() {
        startTimestamp = System.currentTimeMillis();
        String activityType = "run";
        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());
        String timeOfDay = getTimeOfDay();
        String activityName = dayOfWeek + " " + timeOfDay + " " + activityType;

        LocationData currentLocation = dbHelper.getLatestLocation();
        if (currentLocation != null) {
            activityId = dbHelper.insertActivity(activityType, activityName, startTimestamp, currentLocation.getId());
            startUpdates();
        } else {
            Toast.makeText(this, "Unable to start activity: No location data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String getTimeOfDay() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Morning";
        else if (hour < 17) return "Afternoon";
        else return "Evening";
    }

    private void startUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUI();
                handler.postDelayed(this, 1000); // Update every second
            }
        };
        handler.post(updateRunnable);
    }

    private void updateUI() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTimestamp;
        double distance = calculateDistance(startTimestamp, currentTime);

        // Update TIME
        String timeString = formatTime(elapsedTime);
        tvTime.setText("TIME: " + timeString);

        // Update DISTANCE
        String distanceString = String.format(Locale.getDefault(), "%.2f", distance);
        tvDistance.setText("DISTANCE: " + distanceString + " km");

        // Update PACE
        String paceString = calculatePace(elapsedTime, distance);
        tvPace.setText("PACE: " + paceString + " /km");
    }

    private String formatTime(long millis) {
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        int hours = (int) ((millis / (1000 * 60 * 60)) % 24);
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String calculatePace(long elapsedTime, double distance) {
        if (distance < 0.01) {
            return "--:--";
        }
        long paceSeconds = (long) (elapsedTime / 1000 / distance);
        int paceMinutes = (int) (paceSeconds / 60);
        int paceSecondsRemainder = (int) (paceSeconds % 60);
        return String.format(Locale.getDefault(), "%02d:%02d", paceMinutes, paceSecondsRemainder);
    }

    private void stopActivity() {
        showStopActivityDialog();
    }

    private void showStopActivityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Stop Activity");
        builder.setMessage("Do you want to stop and save the activity?");
        builder.setPositiveButton("Stop and Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finalizeActivity();
            }
        });
        builder.setNegativeButton("Resume", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Just dismiss the dialog and continue the activity
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void finalizeActivity() {
        handler.removeCallbacks(updateRunnable);
        long endTimestamp = System.currentTimeMillis();
        LocationData startLocation = dbHelper.getFirstLocationAfterTimestamp(startTimestamp);
        LocationData endLocation = dbHelper.getLatestLocation();

        if (startLocation != null && endLocation != null) {
            double distance = calculateDistance(startTimestamp, endTimestamp);
            long elapsedTime = endTimestamp - startTimestamp;
            dbHelper.updateActivity(activityId, endTimestamp, startLocation.getId(), endLocation.getId(), distance, elapsedTime);
        } else {
            Toast.makeText(this, "Unable to save activity: location data missing", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private double calculateDistance(long startTimestamp, long endTimestamp) {
        List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(startTimestamp, endTimestamp);
        double totalDistance = 0;

        if (locations.size() < 2) {
            return 0;
        }

        for (int i = 0; i < locations.size() - 1; i++) {
            LocationData start = locations.get(i);
            LocationData end = locations.get(i + 1);
            totalDistance += calculateDistanceBetweenPoints(start, end);
        }

        return totalDistance;
    }

    private double calculateDistanceBetweenPoints(LocationData start, LocationData end) {
        double earthRadius = 6371; // in kilometers

        double dLat = Math.toRadians(end.getLatitude() - start.getLatitude());
        double dLon = Math.toRadians(end.getLongitude() - start.getLongitude());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(start.getLatitude())) * Math.cos(Math.toRadians(end.getLatitude())) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c; // Distance in kilometers
    }

    @Override
    public void onBackPressed() {
        showStopActivityDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
    }
}
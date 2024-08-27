package com.jason.memory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class MyActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private static final long UPDATE_INTERVAL = 1000; // 1 second
    private DatabaseHelper dbHelper;
    private long activityId;
    private long startTimestamp;
    private Handler handler = new Handler();
    private Runnable updateRunnable;
    private boolean hideButtonClicked = false;

    private TextView tvTime, tvPace, tvDistance;
    private Button btnMap;
    private Button btnMonitorTracking;

    private static final String PREFS_NAME = "MyActivityPrefs";
    private static final String PREF_ACTIVITY_ID = "activityID";
    private static final String PREF_HIDE_REASON = "hideReason";
    private static final String HIDE_REASON_BUTTON = "buttonHide";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        dbHelper = new DatabaseHelper(this);

        tvTime = findViewById(R.id.tvTime);
        tvPace = findViewById(R.id.tvPace);
        tvDistance = findViewById(R.id.tvDistance);
        btnMap = findViewById(R.id.btnMap);

        btnMonitorTracking = findViewById(R.id.btnMonitorTracking);
        btnMonitorTracking.setOnClickListener(v -> openMonitorActivity());

        Button btnStopActivity = findViewById(R.id.btnStopActivity);
        btnStopActivity.setOnClickListener(v -> stopActivity());

        btnMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(MyActivity.this, RunningMapActivity.class);
            mapIntent.putExtra("startTimestamp", startTimestamp);
            startActivity(mapIntent);
        });

        TextView btnHide = findViewById(R.id.tvHide);
        btnHide.setOnClickListener(v -> hideActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        activityId = getIntent().getLongExtra("ACTIVITY_ID", -1);
        if (activityId != -1) {
            resumeActivity();
        } else if(wasActivityHiddenByButton(this)) {
            activityId = getHiddenActivityId(this);
            if(activityId == -1) {
                startNewActivity();
            } else {
                resumeActivity();
            }
        } else {
            startNewActivity();
        }

        // Ensure updates are started
        startUpdates();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // You can set initial camera position here if needed
    }

    private void openMonitorActivity() {
        Intent monitorIntent = new Intent(MyActivity.this, MonitorActivity.class);
        monitorIntent.putExtra("ACTIVITY_ID", activityId);
        monitorIntent.putExtra("START_TIMESTAMP", startTimestamp);
        startActivity(monitorIntent);
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
            Toast.makeText(this, "Error: Activity("+ activityId+") not found\n New Activity started!", Toast.LENGTH_SHORT).show();
            startNewActivity();
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
                updateMap();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        handler.post(updateRunnable);
    }

    private void updateMap() {
        if (mMap == null) return;

        LocationData latestLocation = dbHelper.getLatestLocation();
        if (latestLocation != null) {
            LatLng latLng = new LatLng(latestLocation.getLatitude(), latestLocation.getLongitude());

            // Move camera to the latest location
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

            // Optionally, you can draw a polyline of the entire track
            List<LocationData> allLocations = dbHelper.getLocationsBetweenTimestamps(startTimestamp, System.currentTimeMillis());

            if(allLocations == null) return;
            // Add start marker
            LocationData startLocation = allLocations.get(0);
            LatLng startPoint = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(startPoint)
                    .title("Start")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            // Add end marker
            LocationData endLocation = allLocations.get(allLocations.size() - 1);
            LatLng endPoint = new LatLng(endLocation.getLatitude(), endLocation.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(endPoint)
                    .title("Current")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

            List<LatLng> points = new ArrayList<>();
            for (LocationData location : allLocations) {
                LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                points.add(point);
                boundsBuilder.include(point);
            }
            // 폴리라인을 빨간색으로 설정하고 너비를 3으로 지정
            mMap.addPolyline(new PolylineOptions()
                    .addAll(points)
                    .color(0xFFFF0000)
                    .width(3));

            LatLngBounds bounds = boundsBuilder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }
    }

    private void updateUI() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTimestamp;
        double distance = calculateDistance(startTimestamp, currentTime);

        // Update TIME
        String timeString = formatTime(elapsedTime);
        tvTime.setText("" + timeString);

        // Update DISTANCE
        String distanceString = String.format(Locale.getDefault(), "%.2f", distance);
        tvDistance.setText("" + distanceString + " km");

        // Update PACE
        String paceString = calculatePace(elapsedTime, distance);
        tvPace.setText("" + paceString + " /km");
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
        builder.setMessage("What would you like to do with this activity?");
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finalizeActivity();
            }
        });
        builder.setNeutralButton("Resume", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Just dismiss the dialog and continue the activity
            }
        });
        builder.setNegativeButton("Discard", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                discardActivity();
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
        clearHideFlags();  // Clear flags when finalizing
        finish();
    }

    private void discardActivity() {
        handler.removeCallbacks(updateRunnable);
        dbHelper.deleteActivity(activityId);
        Toast.makeText(this, "Activity discarded", Toast.LENGTH_SHORT).show();
        clearHideFlags();  // Clear flags when discarding
        finish();
    }


    private double calculateDistance(long startTimestamp, long endTimestamp) {
        List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(startTimestamp, endTimestamp);
        double totalDistance = 0;
        if(locations == null) return 0;
        if (locations.size() < 2) return 0;

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

    private void hideActivity() {
        // Stop all background processes
        handler.removeCallbacks(updateRunnable);

        // Set a flag to indicate that the activity was hidden
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_HIDE_REASON, HIDE_REASON_BUTTON);
        editor.putLong(PREF_ACTIVITY_ID, activityId);
        editor.apply();

        hideButtonClicked = true;  // Set the flag

        // Finish the activity
        finish();
    }

    // Method for other activities to check if MyActivity was hidden
    public static boolean wasActivityHiddenByButton(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String hideReason = prefs.getString(PREF_HIDE_REASON, null);
        return HIDE_REASON_BUTTON.equals(hideReason);
    }

    // Method to get the hidden activity ID
    public static long getHiddenActivityId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getLong(PREF_ACTIVITY_ID, -1);
    }

    private void clearHideFlags() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREF_HIDE_REASON);
        editor.remove(PREF_ACTIVITY_ID);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);

        if (!hideButtonClicked) {
            clearHideFlags();
        }
    }
}
package com.jason.memory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.Manifest;

import java.io.File;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MyActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;

    private static final String PREFS_NAME = "MyActivityPrefs";
    private static final String PREF_KEEP_SCREEN_ON = "keepScreenOn";
    private static final String PREF_ACTIVITY_ID = "activityID";
    private static final String PREF_HIDE_REASON = "hideReason";
    private static final String HIDE_REASON_BUTTON = "buttonHide";

    private static final long UI_UPDATE_INTERVAL = 1000; // 1 second
    private static final long MAP_UPDATE_INTERVAL = 10000; // 10 seconds

    private DatabaseHelper dbHelper;
    private long activityId;
    private long startTimestamp;
    private Handler handler = new Handler();
    private Runnable updateRunnable;
    private boolean hideButtonClicked = false;

    private TextView tvTime, tvPace, tvDistance;
    private Button btnMap;




    private StravaUploader stravaUploader;

    private List<LatLng> mLastPoints = new ArrayList<>();
    private LatLngBounds mLastBounds;
    private Marker mStartMarker;
    private Marker mCurrentMarker;
    private Polyline mPathPolyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        dbHelper = new DatabaseHelper(this);

        tvTime = findViewById(R.id.tvTime);
        tvPace = findViewById(R.id.tvPace);
        tvDistance = findViewById(R.id.tvDistance);
        btnMap = findViewById(R.id.btnMap);

        stravaUploader = new StravaUploader(this);


        Button btnStopActivity = findViewById(R.id.btnStopActivity);
        btnStopActivity.setOnClickListener(v -> stopActivity());

        Button btnStrava = findViewById(R.id.btnStrava);
        btnStrava.setOnClickListener(v-> strava());

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
        applyKeepScreenOnSetting();
    }

    private void applyKeepScreenOnSetting() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean keepScreenOn = prefs.getBoolean(PREF_KEEP_SCREEN_ON, false);
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyKeepScreenOnSetting();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Enable compass
        mMap.getUiSettings().setCompassEnabled(true);

        // Enable my location button
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Enable map toolbar
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Set initial map type
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Enable all gestures
        mMap.getUiSettings().setAllGesturesEnabled(true);

        startUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            }
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
        String simpleAddress = currentLocation.getSimpleAddress(this);
        if (currentLocation != null) {
            activityId = dbHelper.insertActivity(activityType, activityName, startTimestamp, currentLocation.getId(),simpleAddress);
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
                handler.postDelayed(this, UI_UPDATE_INTERVAL);
            }
        };
        handler.post(updateRunnable);

        // Start map updates separately
        startMapUpdates();
    }

    private void startMapUpdates() {
        final Runnable mapUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateMap();
                handler.postDelayed(this, MAP_UPDATE_INTERVAL);
            }
        };
        handler.postDelayed(mapUpdateRunnable, MAP_UPDATE_INTERVAL);
    }

    private void updateMap_orig() {
        if (mMap == null || dbHelper == null) return;

        List<LocationData> allLocations = dbHelper.getLocationsBetweenTimestamps(startTimestamp, System.currentTimeMillis());
        if (allLocations == null || allLocations.isEmpty()) return;

        mMap.clear();  // Clear the map before redrawing

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        List<LatLng> points = new ArrayList<>();

        // Add start marker
        LocationData startLocation = allLocations.get(0);
        LatLng startPoint = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(startPoint)
                .title("Start")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Add end marker (current location)
        LocationData endLocation = allLocations.get(allLocations.size() - 1);
        LatLng endPoint = new LatLng(endLocation.getLatitude(), endLocation.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(endPoint)
                .title("Current")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        for (LocationData location : allLocations) {
            LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
            points.add(point);
            boundsBuilder.include(point);
        }

        // Draw the polyline
        mMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .color(0xFFFF0000)
                .width(3));

        LatLngBounds bounds = boundsBuilder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private void updateMap() {
        if (mMap == null) return;

        List<LocationData> allLocations = dbHelper.getLocationsBetweenTimestamps(startTimestamp, System.currentTimeMillis());

        if (allLocations == null || allLocations.isEmpty()) return;

        List<LatLng> newPoints = new ArrayList<>();
        for (LocationData location : allLocations) {
            newPoints.add(new LatLng(location.getLatitude(), location.getLongitude()));
        }

        // Update start marker if needed
        if (mStartMarker == null) {
            LatLng startPoint = newPoints.get(0);
            mStartMarker = mMap.addMarker(new MarkerOptions()
                    .position(startPoint)
                    .title("Start")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        // Update current marker
        LatLng currentPoint = newPoints.get(newPoints.size() - 1);
        if (mCurrentMarker == null) {
            mCurrentMarker = mMap.addMarker(new MarkerOptions()
                    .position(currentPoint)
                    .title("Current")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        } else {
            mCurrentMarker.setPosition(currentPoint);
        }

        // Update polyline
        if (mPathPolyline == null) {
            mPathPolyline = mMap.addPolyline(new PolylineOptions()
                    .addAll(newPoints)
                    .color(0xFFFF0000)
                    .width(3));
        } else {
            mPathPolyline.setPoints(newPoints);
        }

        // Update camera only if new points are outside the current bounds
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng point : newPoints) {
            boundsBuilder.include(point);
        }
        LatLngBounds newBounds = boundsBuilder.build();

        if (mLastBounds == null || !mLastBounds.contains(newBounds.northeast) || !mLastBounds.contains(newBounds.southwest)) {
            mLastBounds = newBounds;
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mLastBounds, 100));
        }

        mLastPoints = newPoints;
    }

    private void updateUI() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTimestamp;
        double distance = calculateDistance(startTimestamp, currentTime);

        // Update TIME
        String timeString = formatTime(elapsedTime);
        tvTime.setText(timeString);

        // Update DISTANCE
        String distanceString = String.format(Locale.getDefault(), "%.2f", distance);
        tvDistance.setText(distanceString);

        // Update PACE
        String paceString = calculatePace(elapsedTime, distance);
        tvPace.setText(paceString);
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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == StravaUploader.AUTH_REQUEST_CODE) {
            stravaUploader.handleAuthResult(resultCode, data);
        }
    }

    private void strava() {
        Toast.makeText(this, "Preparing to upload to Strava...", Toast.LENGTH_SHORT).show();

        // Generate GPX file from the current activity
        List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(startTimestamp, System.currentTimeMillis());
        File gpxFile = stravaUploader.generateGpxFile(locations);

        if (gpxFile != null) {
            Toast.makeText(this, "GPX file generated successfully", Toast.LENGTH_SHORT).show();

            // Get activity details
            ActivityData activity = dbHelper.getActivity(activityId);

            if (activity != null) {
                Toast.makeText(this, "Starting Strava authentication...", Toast.LENGTH_SHORT).show();

                // Start the authentication and upload process
                stravaUploader.authenticate(gpxFile, activity.getName(),
                        "Activity recorded using MyActivity app", activity.getType());
            } else {
                Toast.makeText(this, "Unable to upload: Activity data not found", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Unable to upload: GPX file generation failed", Toast.LENGTH_LONG).show();
        }

        Toast.makeText(this, "Finalizing activity...", Toast.LENGTH_SHORT).show();
        //finalizeActivity();
    }



    private void finalizeActivity() {
        handler.removeCallbacks(updateRunnable);
        long endTimestamp = System.currentTimeMillis();
        LocationData startLocation = dbHelper.getFirstLocationAfterTimestamp(startTimestamp);
        LocationData endLocation = dbHelper.getLatestLocation();

        if (startLocation != null && endLocation != null) {
            double distance = calculateDistance(startTimestamp, endTimestamp);
            long elapsedTime = endTimestamp - startTimestamp;

            // Get the current activity to retrieve the address
            ActivityData currentActivity = dbHelper.getActivity(activityId);
            String address = "";

            if (currentActivity != null) {
                // If the activity has an address, use it
                address = currentActivity.getAddress();
            }

            // If the address is empty, try to get it from the end location
            if (address.isEmpty()) {
                address = endLocation.getSimpleAddress(this);
            }

            // Update the activity with all the information, including the address
            dbHelper.updateActivity(activityId, endTimestamp, startLocation.getId(), endLocation.getId(), distance, elapsedTime, address);
        } else {
            Toast.makeText(this, "Unable to save activity(" + activityId + "): location data missing", Toast.LENGTH_SHORT).show();
        }
        clearHideFlags();  // Clear flags when finalizing
        finish();
    }


    private void discardActivity() {
        handler.removeCallbacks(updateRunnable);
        dbHelper.deleteActivity(activityId);
        Toast.makeText(this, "Activity(" + activityId + ") discarded", Toast.LENGTH_SHORT).show();
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
        handler.removeCallbacksAndMessages(null);  // This removes all callbacks

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
        handler.removeCallbacksAndMessages(null);  // This removes all callbacks

        if (!hideButtonClicked) {
            clearHideFlags();
        }
    }
}
package com.jason.memory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.google.maps.android.SphericalUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.google.maps.android.SphericalUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


public class MyActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;

    private static final long UI_UPDATE_INTERVAL = 1000; // 1 second
    private static final long MAP_UPDATE_INTERVAL = 10000; // 10 seconds

    private DatabaseHelper dbHelper;
    private long activityId;
    private long startTimestamp;
    private Handler handler = new Handler();
    private Runnable updateRunnable;
    private boolean hideButtonClicked = false;

    private TextView tvTime, tvPace, tvCalories, tvDistance;
    private TextView tvDateStr;

    private StravaUploader stravaUploader;

    private List<LatLng> mLastPoints = new ArrayList<>();
    private LatLngBounds mLastBounds;
    private Marker mStartMarker;
    private Marker mCurrentMarker;
    private Polyline mPathPolyline;
    private TextView tvSetting;
    private CheckBox checkBoxServer;
    private CheckBox checkBoxStrava;

    private ExecutorService executorService;

    LinearLayout statKM;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the current run type from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        String currentRunType = prefs.getString(Config.PREF_RUN_TYPE, Config.RUN_TYPE_MEMORY);

        // Set the layout based on the run type
        if (currentRunType.equals(Config.RUN_TYPE_MEMORY)) {
            setContentView(R.layout.activity_my);
        } else {
            setContentView(R.layout.activity_my2);
        }

        executorService = Executors.newSingleThreadExecutor();

        dbHelper = new DatabaseHelper(this);

        initializeViews();

        stravaUploader = new StravaUploader(this);

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

    private void initializeViews() {
        tvDateStr = findViewById(R.id.idnew_date_str);
        tvTime = findViewById(R.id.tvTime);
        tvPace = findViewById(R.id.tvPace);
        tvCalories = findViewById(R.id.tvCalories);
        tvDistance = findViewById(R.id.tvDistance);
        tvSetting = findViewById(R.id.tvSetting);
        checkBoxServer = findViewById(R.id.idnew_save_server);
        checkBoxStrava = findViewById(R.id.idnew_save_Strava);
        statKM = findViewById(R.id.statKM);

        TextView btnHide = findViewById(R.id.tvHide);
        btnHide.setOnClickListener(v -> hideActivity());
        tvSetting.setOnClickListener(v -> openSettingActivity());
        statKM.setOnClickListener(v -> showPlacesDialog());
    }

    private Marker mGoalMarker;

    private void setGoalMarker(Place place) {
        if (mMap != null) {
            LatLng goalPosition = new LatLng(place.getLat(), place.getLon());
            if (mGoalMarker != null) {
                mGoalMarker.remove();
            }
            mGoalMarker = mMap.addMarker(new MarkerOptions()
                    .position(goalPosition)
                    .title("Goal: " + place.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));

            updateMap();
        }
    }

    private void showPlacesDialog() {
        List<Place> allPlaces = dbHelper.getAllPlaces();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose a Goal");

        String[] placeNames = new String[allPlaces.size()];
        for (int i = 0; i < allPlaces.size(); i++) {
            placeNames[i] = allPlaces.get(i).getName();
        }

        builder.setItems(placeNames, (dialog, which) -> {
            Place selectedPlace = allPlaces.get(which);
            setGoalMarker(selectedPlace);
        });

        builder.show();
    }

    private static final int SETTINGS_REQUEST_CODE = 1001;

    private void openSettingActivity() {
        Intent settingIntent = new Intent(this, SettingActivity.class);
        settingIntent.putExtra("ACTIVITY_ID", activityId);
        settingIntent.putExtra("START_TIMESTAMP", startTimestamp);
        startActivityForResult(settingIntent, SETTINGS_REQUEST_CODE);
        Log.d("MyActivity", "--m-- Starting SettingActivity for result");
    }

    private void applyKeepScreenOnSetting() {
        SharedPreferences prefs = getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        boolean keepScreenOn = prefs.getBoolean(Config.PREF_KEEP_SCREEN_ON, false);
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
        Log.d("MyActivity", "--m-- onResume called");
        SharedPreferences prefs = getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        boolean settingsChanged = prefs.getBoolean("SETTINGS_CHANGED", false);
        if (settingsChanged) {
            Log.d("MyActivity", "--m-- Settings changed, updating layout and resuming");
            updateLayoutBasedOnRunType();
            resumeActivityAfterSettings();
            prefs.edit().putBoolean("SETTINGS_CHANGED", false).apply();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("MyActivity", "--m-- onActivityResult called with requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == SETTINGS_REQUEST_CODE) {
            Log.d("MyActivity", "--m-- Returned from SettingActivity");
            if (resultCode == RESULT_OK) {
                Log.d("MyActivity", "--m-- SettingActivity result OK, updating layout and resuming");
                updateLayoutBasedOnRunType();
                resumeActivityAfterSettings();
            } else {
                Log.d("MyActivity", "--m-- SettingActivity result not OK");
            }
        } else if (requestCode == StravaUploader.AUTH_REQUEST_CODE) {
            Log.d("MyActivity", "--m-- Handling Strava auth result");
            stravaUploader.handleAuthResult(resultCode, data);
        } else {
            Log.d("MyActivity", "--m-- Unhandled requestCode: " + requestCode);
        }
    }

    private void updateLayoutBasedOnRunType() {
        SharedPreferences prefs = getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        String currentRunType = prefs.getString(Config.PREF_RUN_TYPE, Config.RUN_TYPE_MEMORY);

        // Remove existing fragments before changing the layout
        removeExistingMapFragment();

        // Set the layout based on the run type
        if (currentRunType.equals(Config.RUN_TYPE_MEMORY)) {
            setContentView(R.layout.activity_my);
        } else {
            setContentView(R.layout.activity_my2);
        }

        // Reinitialize views and other necessary components
        initializeViews();
        initializeMapFragment();
    }

    private void initializeMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.map, mapFragment)
                    .commitNow();
        }
        mapFragment.getMapAsync(this);
    }

    private void removeExistingMapFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.map);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commitNow();
        }
    }

    private void resumeActivityAfterSettings() {
        ActivityData activity = dbHelper.getActivity(activityId);
        if (activity != null) {
            startTimestamp = activity.getStartTimestamp();
            mStartMarker = null;
            mCurrentMarker = null;
            mPathPolyline = null;
            mLastBounds = null;
            updateUI();
            startUpdates();
        } else {
            Toast.makeText(this, "Error: Activity not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void resumeActivity() {
        ActivityData activity = dbHelper.getActivity(activityId);
        if (activity != null) {
            startTimestamp = activity.getStartTimestamp();
            updateUI();
            startUpdates();
        } else {
            Toast.makeText(this, "Error: Activity(" + activityId + ") not found\n New Activity started!", Toast.LENGTH_SHORT).show();
            startNewActivity();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        if (!hideButtonClicked) {
            clearHideFlags();
        }
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

    private Bitmap createDistanceMarker(String distance) {
        Paint paint = new Paint();
        paint.setTextSize(50);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.CENTER);

        int width = (int) (paint.measureText(distance) + 20);
        int height = 70;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        paint.setColor(Color.CYAN);
        canvas.drawRect(0, 0, width, height, paint);

        paint.setColor(Color.BLACK);
        canvas.drawText(distance, width / 2, height - 20, paint);

        return bitmap;
    }

    private void updateMap() {
        if (mMap == null) return;

        List<LocationData> allLocations = dbHelper.getLocationsBetweenTimestamps(startTimestamp, System.currentTimeMillis());

        if (allLocations == null || allLocations.isEmpty()) return;

        List<LatLng> newPoints = new ArrayList<>();
        for (LocationData location : allLocations) {
            newPoints.add(new LatLng(location.getLatitude(), location.getLongitude()));
        }

        mMap.clear(); // Clear all markers and polylines

        // Add start marker
        LatLng startPoint = newPoints.get(0);
        mStartMarker = mMap.addMarker(new MarkerOptions()
                .position(startPoint)
                .title("Start")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Add current marker
        LatLng currentPoint = newPoints.get(newPoints.size() - 1);
        mCurrentMarker = mMap.addMarker(new MarkerOptions()
                .position(currentPoint)
                .title("Current")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Add polyline
        mPathPolyline = mMap.addPolyline(new PolylineOptions()
                .addAll(newPoints)
                .color(0xFFFF0000)
                .width(5));

        // Add goal marker and line if exists
        if (mGoalMarker != null) {
            mGoalMarker = mMap.addMarker(new MarkerOptions()
                    .position(mGoalMarker.getPosition())
                    .title(mGoalMarker.getTitle())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));

            // Draw line to goal
            PolylineOptions goalLineOptions = new PolylineOptions()
                    .add(currentPoint, mGoalMarker.getPosition())
                    .color(Color.CYAN)
                    .width(5);
            Polyline goalLine = mMap.addPolyline(goalLineOptions);

            // Add distance text
            double distance = SphericalUtil.computeDistanceBetween(currentPoint, mGoalMarker.getPosition()) / 1000; // in km
            LatLng midPoint = SphericalUtil.interpolate(currentPoint, mGoalMarker.getPosition(), 0.5);
            MarkerOptions distanceMarkerOptions = new MarkerOptions()
                    .position(midPoint)
                    .icon(BitmapDescriptorFactory.fromBitmap(createDistanceMarker(String.format("%.2f km", distance))))
                    .anchor(0.5f, 0.5f);
            mMap.addMarker(distanceMarkerOptions);
        }

        // Update camera to show all points
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng point : newPoints) {
            boundsBuilder.include(point);
        }
        if (mGoalMarker != null) {
            boundsBuilder.include(mGoalMarker.getPosition());
        }
        LatLngBounds bounds = boundsBuilder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        mLastPoints = newPoints;
        mLastBounds = bounds;
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
        // Update DATE STRING
        String dateString = formatDateString(currentTime);
        tvDateStr.setText(dateString);

        String caloriesString = Utility.calculateCalories(this,distance);
        tvCalories.setText(caloriesString);
    }

    private String formatDateString(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년MM월dd일 HH:mm:ss", Locale.KOREA);
        String formattedDate = sdf.format(new Date(timestamp));

        // Replace AM/PM with 오전/오후
        formattedDate = formattedDate.replace("AM", "오전").replace("PM", "오후");

        return formattedDate;
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

        Utility.finalizeActivity(this, dbHelper, stravaUploader, activityId, startTimestamp, endTimestamp,
                () -> {
                    // This is called when the activity is finalized, but before Strava upload completes
                    clearHideFlags();
                    // Don't finish the activity here
                },
                success -> {
                    // This is called when the Strava upload is complete
                    runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(this, "Activity uploaded to Strava successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to upload activity to Strava", Toast.LENGTH_SHORT).show();
                        }
                        finish(); // Now we can finish the activity
                    });
                }
        );
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
        SharedPreferences prefs = getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Config.PREF_HIDE_REASON, Config.HIDE_REASON_BUTTON);
        editor.putLong(Config.PREF_ACTIVITY_ID, activityId);
        editor.apply();

        hideButtonClicked = true;  // Set the flag
        // Finish the activity
        finish();
    }


    // Method for other activities to check if MyActivity was hidden
    public static boolean wasActivityHiddenByButton(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        String hideReason = prefs.getString(Config.PREF_HIDE_REASON, null);
        return Config.HIDE_REASON_BUTTON.equals(hideReason);
    }

    // Method to get the hidden activity ID
    public static long getHiddenActivityId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        return prefs.getLong(Config.PREF_ACTIVITY_ID, -1);
    }

    private void clearHideFlags() {
        SharedPreferences prefs = getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(Config.PREF_HIDE_REASON);
        editor.remove(Config.PREF_ACTIVITY_ID);
        editor.apply();
    }

}
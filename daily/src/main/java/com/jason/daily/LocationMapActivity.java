package com.jason.daily;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocationMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseHelper dbHelper;
    private DatePicker datePicker;
    private List<LatLng> locations;
    private static final String PREFS_NAME = "LocationServicePrefs";
    private static final String KEY_UPDATE_INTERVAL = "update_interval";
    private static final long DEFAULT_UPDATE_INTERVAL = 60000; // 1 minute in milliseconds

    private Spinner intervalSpinner;
    private Button dayButton, weekButton, monthButton;
    private ImageButton settingsButton;
    private static final int VIEW_DAY = 0;
    private static final int VIEW_WEEK = 1;
    private static final int VIEW_MONTH = 2;
    private int currentView = VIEW_DAY;
    private TextView logTextView;
    private StringBuilder logBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_map);

        dbHelper = new DatabaseHelper(this);
        datePicker = findViewById(R.id.datePicker);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        datePicker.setOnDateChangedListener((view, year, monthOfYear, dayOfMonth) -> {
            updateMap(year, monthOfYear, dayOfMonth);
        });

        dayButton = findViewById(R.id.dayButton);
        weekButton = findViewById(R.id.weekButton);
        monthButton = findViewById(R.id.monthButton);
        settingsButton = findViewById(R.id.settingsButton);

        dayButton.setOnClickListener(v -> updateView(VIEW_DAY));
        weekButton.setOnClickListener(v -> updateView(VIEW_WEEK));
        monthButton.setOnClickListener(v -> updateView(VIEW_MONTH));
        settingsButton.setOnClickListener(v -> openSettings());
        logTextView = findViewById(R.id.logTextView);

        updateView(currentView);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && LocationService.ACTION_INITIAL_ALARM.equals(intent.getAction())) {
            // Handle the initial alarm
            Toast.makeText(this, "Location Service has been initialized", Toast.LENGTH_LONG).show();
            // You can add more logic here, like updating UI or logging
        }
    }


    private void updateView(int viewType) {
        currentView = viewType;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(datePicker.getCalendarView().getDate());
        long selectedTimestamp = calendar.getTimeInMillis();

        List<LocationData> locationDataList;

        switch (viewType) {
            case VIEW_DAY:
                locationDataList = dbHelper.getLocationDataForDay(selectedTimestamp);
                break;
            case VIEW_WEEK:
                locationDataList = dbHelper.getLocationDataForWeek(selectedTimestamp);
                break;
            case VIEW_MONTH:
                locationDataList = dbHelper.getLocationDataForMonth(selectedTimestamp);
                break;
            default:
                locationDataList = new ArrayList<>();
        }

        updateMapWithLocations(locationDataList);
        logMessage("Fetched " + locationDataList.size() + " locations for " + getViewTypeString(viewType));
    }


    private void updateMapWithLocations(List<LocationData> locationDataList) {
        if (mMap == null) {
            return;
        }

        mMap.clear();
        List<LatLng> locations = new ArrayList<>();

        for (LocationData locationData : locationDataList) {
            LatLng latLng = new LatLng(locationData.getLatitude(), locationData.getLongitude());
            locations.add(latLng);
        }

        if (!locations.isEmpty()) {
            LatLng firstLocation = locations.get(0);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12));

            // Add markers for start and end locations
            mMap.addMarker(new MarkerOptions().position(locations.get(0)).title("Start"));
            mMap.addMarker(new MarkerOptions().position(locations.get(locations.size() - 1)).title("End"));

            // Add heatmap
            HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                    .data(locations)
                    .build();
            TileOverlay overlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));

            // Optionally, you can add a polyline to show the path
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(locations)
                    .color(Color.BLUE)
                    .width(5);
            mMap.addPolyline(polylineOptions);
            logMessage("Displayed " + locations.size() + " locations on the map");
            logMessage("Start: " + formatLatLng(locations.get(0)));
            logMessage("End: " + formatLatLng(locations.get(locations.size() - 1)));
        } else {
            logMessage("No locations found for the selected period");
        }

    }

    private void logMessage(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String logMessage = timestamp + ": " + message + "\n";
        logBuilder.append(logMessage);
        logTextView.setText(logBuilder.toString());

        // Scroll to the bottom of the log
        final ScrollView scrollView = findViewById(R.id.logScrollView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private String getViewTypeString(int viewType) {
        switch (viewType) {
            case VIEW_DAY:
                return "Day";
            case VIEW_WEEK:
                return "Week";
            case VIEW_MONTH:
                return "Month";
            default:
                return "Unknown";
        }
    }

    private String formatLatLng(LatLng latLng) {
        return String.format(Locale.getDefault(), "%.6f, %.6f", latLng.latitude, latLng.longitude);
    }

    private void openSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View settingsView = getLayoutInflater().inflate(R.layout.layout_settings_menu, null);
        builder.setView(settingsView);

        Spinner settingsIntervalSpinner = settingsView.findViewById(R.id.settingsIntervalSpinner);
        setupSettingsIntervalSpinner(settingsIntervalSpinner);

        builder.setTitle("Settings")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Save settings
                    int position = settingsIntervalSpinner.getSelectedItemPosition();
                    long newInterval = getIntervalForPosition(position);
                    saveUpdateInterval(newInterval);
                    updateServiceInterval(newInterval);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setupSettingsIntervalSpinner(Spinner spinner) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.update_intervals, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Load the current interval
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long currentInterval = prefs.getLong(KEY_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL);
        int position = getPositionForInterval(currentInterval);
        spinner.setSelection(position);
    }


    private void setupIntervalSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.update_intervals, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        intervalSpinner.setAdapter(adapter);


        // Load the current interval
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long currentInterval = prefs.getLong(KEY_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL);
        int position = getPositionForInterval(currentInterval);
        intervalSpinner.setSelection(position);

        intervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                long newInterval = getIntervalForPosition(position);
                saveUpdateInterval(newInterval);
                updateServiceInterval(newInterval);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void saveUpdateInterval(long interval) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putLong(KEY_UPDATE_INTERVAL, interval);
        editor.apply();
    }

    private void updateServiceInterval(long newInterval) {
        Intent intent = new Intent(this, LocationService.class);
        intent.setAction("UPDATE_INTERVAL");
        intent.putExtra("interval", newInterval);
        startService(intent);
    }

    private int getPositionForInterval(long interval) {
        if (interval <= 60000) return 0; // 1 minute
        if (interval <= 300000) return 1; // 5 minutes
        if (interval <= 900000) return 2; // 15 minutes
        return 3; // 30 minutes
    }

    private long getIntervalForPosition(int position) {
        switch (position) {
            case 0: return 60000; // 1 minute
            case 1: return 300000; // 5 minutes
            case 2: return 900000; // 15 minutes
            case 3: return 1800000; // 30 minutes
            default: return DEFAULT_UPDATE_INTERVAL;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        updateMap(Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
    }

    private void updateMap(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        long startOfDay = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long endOfDay = calendar.getTimeInMillis();

        List<LocationData> locationDataList = dbHelper.getLocationDataForDateRange(startOfDay, endOfDay);
        locations = new ArrayList<>();

        for (LocationData locationData : locationDataList) {
            LatLng latLng = new LatLng(locationData.getLatitude(), locationData.getLongitude());
            locations.add(latLng);
        }

        mMap.clear();

        if (!locations.isEmpty()) {
            LatLng firstLocation = locations.get(0);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12));

            // Add markers for start and end locations
            mMap.addMarker(new MarkerOptions().position(locations.get(0)).title("Start"));
            mMap.addMarker(new MarkerOptions().position(locations.get(locations.size() - 1)).title("End"));

            // Add heatmap
            HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                    .data(locations)
                    .build();
            TileOverlay overlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
        }
    }
}
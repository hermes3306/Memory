package com.jason.memory;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityCloudDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "ActivityCloudDetailActivity";
    private TextView tvName, tvStartTime, tvDistance, tvElapsedTime, tvAveragePace, tvCalories;
    private GoogleMap mMap;
    private ActivityData activity;
    private Button btnBack;
    private static final String UPLOAD_URL = "http://58.233.69.198/moment/upload.php";
    private static final String BASE_URL = "http://58.233.69.198/moment/upload/";
    private String activityFilename;
    private List<LocationData> locations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail2);
        Log.d(TAG, "--m-- onCreate: Starting ActivityCloudDetailActivity");

        initializeViews();

        activityFilename = getIntent().getStringExtra("ACTIVITY_FILENAME");
        if (activityFilename == null) {
            Log.e(TAG, "--m-- onCreate: No activity filename provided");
            Toast.makeText(this, "No activity filename provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Log.d(TAG, "--m-- onCreate: Activity filename: " + activityFilename);

        new FetchActivityDataTask().execute(activityFilename);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void initializeViews() {
        Log.d(TAG, "--m-- initializeViews: Initializing views");
        tvName = findViewById(R.id.tvName);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvDistance = findViewById(R.id.tvDistance);
        tvElapsedTime = findViewById(R.id.tvElapsedTime);
        tvAveragePace = findViewById(R.id.tvAveragePace);
        tvCalories = findViewById(R.id.tvCalories);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private class FetchActivityDataTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... filenames) {
            Log.d(TAG, "--m-- FetchActivityDataTask: Fetching data for file: " + filenames[0]);
            try {
                URL url = new URL(BASE_URL + filenames[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                reader.close();
                connection.disconnect();
                Log.d(TAG, "--m-- FetchActivityDataTask: Data fetched successfully");
                return content.toString();
            } catch (IOException e) {
                Log.e(TAG, "--m-- FetchActivityDataTask: Error fetching activity data", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                parseActivityData(result);
            } else {
                Log.e(TAG, "--m-- onPostExecute: Failed to fetch activity data");
                Toast.makeText(ActivityCloudDetailActivity.this, "Failed to fetch activity data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void parseActivityData(String data) {
        Log.d(TAG, "--m-- parseActivityData: Starting to parse activity data");
        String[] lines = data.split("\n");
        if (lines.length < 2) {
            Log.e(TAG, "--m-- parseActivityData: Invalid activity data");
            Toast.makeText(this, "Invalid activity data", Toast.LENGTH_SHORT).show();
            return;
        }

        locations = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());
        try {
            String[] firstLine = lines[1].split(",");
            String[] lastLine = lines[lines.length - 1].split(",");

            Date startDate = sdf.parse(firstLine[2] + "," + firstLine[3]);
            Date endDate = sdf.parse(lastLine[2] + "," + lastLine[3]);

            long startTimestamp = startDate.getTime();
            long endTimestamp = endDate.getTime();

            for (int i = 1; i < lines.length; i++) {
                String[] parts = lines[i].split(",");
                if (parts.length >= 4) {
                    double lat = Double.parseDouble(parts[0]);
                    double lon = Double.parseDouble(parts[1]);
                    Date timestamp = sdf.parse(parts[2] + "," + parts[3]);
                    locations.add(new LocationData(0, lat, lon, 0, timestamp.getTime()));
                }
            }

            double distance = calculateTotalDistance(locations);
            long elapsedTime = endTimestamp - startTimestamp;
            String name = activityFilename.replace(".csv", "");

            activity = new ActivityData(
                    0,
                    "run",
                    name,
                    startTimestamp,
                    endTimestamp,
                    0,
                    0,
                    distance,
                    elapsedTime,
                    "Address not available"
            );

            Log.d(TAG, "--m-- parseActivityData: Activity parsed successfully");
            displayActivityDetails();
            if (mMap != null) {
                drawActivityTrack();
            }
        } catch (ParseException e) {
            Log.e(TAG, "--m-- parseActivityData: Error parsing activity data", e);
            Toast.makeText(this, "Error parsing activity data", Toast.LENGTH_SHORT).show();
        }
    }

    private double calculateTotalDistance(List<LocationData> locations) {
        Log.d(TAG, "--m-- calculateTotalDistance: Calculating total distance");
        double distance = 0;
        for (int i = 0; i < locations.size() - 1; i++) {
            LocationData start = locations.get(i);
            LocationData end = locations.get(i + 1);
            distance += calculateDistance(start.getLatitude(), start.getLongitude(),
                    end.getLatitude(), end.getLongitude());
        }
        Log.d(TAG, "--m-- calculateTotalDistance: Total distance: " + distance);
        return distance;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371; // kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private void displayActivityDetails() {
        Log.d(TAG, "--m-- displayActivityDetails: Displaying activity details");
        tvName.setText(activity.getName());
        tvStartTime.setText(formatTimestamp(activity.getStartTimestamp()));
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", activity.getDistance()));
        tvElapsedTime.setText(formatElapsedTime(activity.getElapsedTime()));
        tvAveragePace.setText(calculateAveragePace(activity.getElapsedTime(), activity.getDistance()));
        tvCalories.setText(String.format(Locale.getDefault(), "%d KCal", estimateCaloriesBurned(activity.getElapsedTime(), activity.getDistance())));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "--m-- onMapReady: Google Map is ready");
        mMap = googleMap;
        if (locations != null && !locations.isEmpty()) {
            drawActivityTrack();
        }
    }

    private void drawActivityTrack() {
        Log.d(TAG, "--m-- drawActivityTrack: Drawing activity track on map");
        if (locations.size() < 2) {
            Log.w(TAG, "--m-- drawActivityTrack: Not enough location data to draw track");
            Toast.makeText(this, "Not enough location data to draw track", Toast.LENGTH_SHORT).show();
            return;
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .color(Color.RED)
                .width(3);
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (LocationData location : locations) {
            LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
            polylineOptions.add(point);
            boundsBuilder.include(point);
        }

        mMap.addPolyline(polylineOptions);
        LatLngBounds bounds = boundsBuilder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        addMarker(locations.get(0), "Start", BitmapDescriptorFactory.HUE_GREEN);
        addMarker(locations.get(locations.size() - 1), "End", BitmapDescriptorFactory.HUE_RED);
        Log.d(TAG, "--m-- drawActivityTrack: Activity track drawn successfully");
    }

    private void addMarker(LocationData location, String title, float markerColor) {
        LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(point)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
        Log.d(TAG, "--m-- addMarker: Added marker - " + title);
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String formatElapsedTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }

    private String calculateAveragePace(long elapsedTime, double distance) {
        if (distance < 0.01) {
            return "--:--";
        }
        long averagePaceSeconds = (long) (elapsedTime / 1000 / distance);
        int averagePaceMinutes = (int) (averagePaceSeconds / 60);
        int averagePaceSecondsRemainder = (int) (averagePaceSeconds % 60);
        return String.format(Locale.getDefault(), "%02d:%02d", averagePaceMinutes, averagePaceSecondsRemainder);
    }

    private int estimateCaloriesBurned(long elapsedTime, double distance) {
        return (int) (distance * 60);
    }
}
package com.jason.memory;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.app.AlertDialog;
import android.widget.Button;

public class ActivityCloudDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "ActivityCloudDetailActivity";
    private TextView tvName, tvStartTime, tvDistance, tvElapsedTime, tvAveragePace, tvCalories;
    private GoogleMap mMap;
    private ActivityData activity;
    private Button btnBack;
    private DatabaseHelper dbHelper;

    private static final String BASE_URL = "http://58.233.69.198/moment/";
    private static final String UPLOAD_DIR = "upload/";

    private String activityFilename;
    private List<LocationData> locations;
    private TextView tvSaveDatabase;
    private TextView tvSaveDatabaseInfo;
    private TextView tvStrava;
    private TextView tvStravaInfo;

    private Button btnDelete;
    private static final int STRAVA_AUTH_REQUEST_CODE = 1001;
    private StravaUploader stravaUploader;
    private static final String ACTIVITY_FOLDER = "memory_activity";
    private TextView tvSaveActivity;
    private TextView tvSaveActivityInfo;
    private TextView tvCloud;
    private TextView tvCloudInfo;
    private static final String UPLOAD_URL = "http://58.233.69.198/moment/upload.php";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail2);
        Log.d(TAG, "--m-- onCreate: Starting ActivityCloudDetailActivity");

        initializeViews();
        setupClickListeners();
        stravaUploader = new StravaUploader(this);
        dbHelper = new DatabaseHelper(this);

        activityFilename = getIntent().getStringExtra("ACTIVITY_FILENAME");
        if (activityFilename == null || activityFilename.isEmpty()) {
            Log.e(TAG, "--m-- onCreate: No activity filename provided");
            Toast.makeText(this, "No activity filename provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Log.d(TAG, "--m-- onCreate: Activity filename: " + activityFilename);

        new FetchActivityDataTask().execute(activityFilename);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "--m-- onCreate: Map fragment not found");
        }

        // Set up delete button
        Button btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> {
            // Show a confirmation dialog before deleting
            new AlertDialog.Builder(this)
                    .setTitle("Delete Activity")
                    .setMessage("Are you sure you want to delete this activity?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteActivityFile())
                    .setNegativeButton("No", null)
                    .show();
        });


    }

    private void initializeViews() {
        tvName = findViewById(R.id.tvName);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvDistance = findViewById(R.id.tvDistance);
        tvElapsedTime = findViewById(R.id.tvElapsedTime);
        tvAveragePace = findViewById(R.id.tvAveragePace);
        tvCalories = findViewById(R.id.tvCalories);

        btnDelete = findViewById(R.id.btnDelete);
        btnBack = findViewById(R.id.btnBack);

        tvSaveActivity = findViewById(R.id.tv_save_activity);
        tvSaveActivityInfo = findViewById(R.id.tv_save_activity_info);

        tvCloud = findViewById(R.id.tv_cloud);
        tvCloudInfo = findViewById(R.id.tv_cloud_info);

        tvSaveDatabase = findViewById(R.id.tv_save_database);
        tvSaveDatabaseInfo = findViewById(R.id.tv_save_database_info);

        tvStrava = findViewById(R.id.tv_strava);
        tvStravaInfo = findViewById(R.id.tv_strava_info);
    }

    private void setupClickListeners() {
        btnDelete.setOnClickListener(v -> deleteActivityFile());
        btnBack.setOnClickListener(v -> finish());

        tvSaveActivity.setOnClickListener(v -> saveActivityToFile());
        tvSaveActivityInfo.setOnClickListener(v -> saveActivityToFile());

        tvCloud.setOnClickListener(v -> saveAndUploadActivity());
        tvCloudInfo.setOnClickListener(v -> saveAndUploadActivity());

        View.OnClickListener saveToDbListener = v -> saveToDatabase();
        tvSaveDatabase.setOnClickListener(saveToDbListener);
        tvSaveDatabaseInfo.setOnClickListener(saveToDbListener);

        tvStrava.setOnClickListener(v -> uploadToStrava());
        tvStravaInfo.setOnClickListener(v -> uploadToStrava());
    }


    private void saveAndUploadActivity() {
        File savedFile = saveActivityToFile();
        if (savedFile != null) {
            uploadFile(savedFile);
        }
    }

    private void saveToDatabase() {
        if (activity != null) {
            if (locations != null && !locations.isEmpty()) {
                long activityId = dbHelper.insertOrUpdateActivityWithLocations(activity, locations);

                if (activityId > 0) {
                    Toast.makeText(this, "Activity and locations saved to database", Toast.LENGTH_SHORT).show();
                    // Update the activity's ID if it was a new insert
                    if (activity.getId() <= 0) {
                        activity.setId(activityId);
                    }
                } else {
                    Toast.makeText(this, "Failed to save activity and locations", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No location data available for this activity", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No activity data to save", Toast.LENGTH_SHORT).show();
        }
    }

    private void goBack() {
        finish();
    }


    private File saveActivityToFile() {
        if (activity == null) {
            Toast.makeText(this, "No activity data to save", Toast.LENGTH_SHORT).show();
            return null;
        }

        SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = fileNameFormat.format(new Date(activity.getStartTimestamp())) + ".csv";

        File directory = new File(getExternalFilesDir(null), ACTIVITY_FOLDER);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, fileName);

        try (FileWriter writer = new FileWriter(file)) {
            writer.append("x,y,d,t\n");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());

            for (LocationData location : locations) {
                writer.append(String.format(Locale.US, "%.8f,%.8f,%s\n",
                        location.getLatitude(),
                        location.getLongitude(),
                        dateFormat.format(new Date(location.getTimestamp()))));
            }

            Toast.makeText(this, "Activity saved to " + fileName, Toast.LENGTH_SHORT).show();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }



    private void uploadFile(File file) {
        final String serverUrl = UPLOAD_URL;  // Assuming Config._uploadURL is defined somewhere

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                HttpURLConnection httpUrlConnection = null;
                try {
                    URL url = new URL(serverUrl);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                    httpUrlConnection.setUseCaches(false);
                    httpUrlConnection.setDoInput(true);
                    httpUrlConnection.setDoOutput(true);
                    httpUrlConnection.setConnectTimeout(15000);

                    httpUrlConnection.setRequestMethod("POST");
                    httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
                    httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");

                    String boundary = "*****";
                    String crlf = "\r\n";
                    String twoHyphens = "--";

                    httpUrlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                    DataOutputStream request = new DataOutputStream(httpUrlConnection.getOutputStream());

                    request.writeBytes(twoHyphens + boundary + crlf);
                    request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + crlf);
                    request.writeBytes("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName()) + crlf);
                    request.writeBytes("Content-Transfer-Encoding: binary" + crlf);
                    request.writeBytes(crlf);
                    request.flush();

                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    long totalBytesRead = 0;
                    long fileSize = file.length();

                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        request.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        int progress = (int) ((totalBytesRead * 100) / fileSize);
                        Log.d("Upload", "Progress: " + progress + "%");
                        // You can use publishProgress(progress) here if you want to update UI
                    }

                    request.writeBytes(crlf);
                    request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

                    request.flush();
                    request.close();

                    int responseCode = httpUrlConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(httpUrlConnection.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        return "Upload successful. Server response: " + response.toString();
                    } else {
                        return "Upload failed. Server returned: " + responseCode;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return "Upload failed: " + e.getMessage();
                } finally {
                    if (httpUrlConnection != null) {
                        httpUrlConnection.disconnect();
                    }
                }
            }

            @Override
            protected void onPostExecute(String result) {
                Toast.makeText(ActivityCloudDetailActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }





    private void uploadToStrava() {
        File gpxFile = stravaUploader.generateGpxFile(locations);

        if (gpxFile != null) {
            stravaUploader.authenticate(gpxFile, activity.getName(),
                    "Activity recorded using MyActivity app", activity.getType());
        } else {
            Toast.makeText(this, "Unable to upload: GPX file generation failed", Toast.LENGTH_SHORT).show();
        }
    }

    private class FetchActivityDataTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... filenames) {
            Log.d(TAG, "--m-- FetchActivityDataTask: Fetching data for file: " + filenames[0]);
            try {
                URL url = new URL(BASE_URL + UPLOAD_DIR + filenames[0]);
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
                finish();
            }
        }
    }

    private void parseActivityData(String data) {
        Log.d(TAG, "--m-- parseActivityData: Starting to parse activity data");
        String[] lines = data.split("\n");
        if (lines.length < 2) {
            Log.e(TAG, "--m-- parseActivityData: Invalid activity data");
            Toast.makeText(this, "Invalid activity data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        locations = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());

        try {
            for (int i = 1; i < lines.length; i++) {
                String[] parts = lines[i].split(",");
                if (parts.length >= 4) {
                    double lat = Double.parseDouble(parts[0]);
                    double lon = Double.parseDouble(parts[1]);
                    Date timestamp = sdf.parse(parts[2] + "," + parts[3]);
                    locations.add(new LocationData(0, lat, lon, 0, timestamp.getTime()));
                }
            }

            if (locations.size() < 2) {
                Log.e(TAG, "--m-- parseActivityData: Not enough valid location data");
                Toast.makeText(this, "Not enough valid location data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            long startTimestamp = locations.get(0).getTimestamp();
            long endTimestamp = locations.get(locations.size() - 1).getTimestamp();
            double distance = calculateDistance(locations);
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
            runOnUiThread(() -> {
                displayActivityDetails();
                if (mMap != null) {
                    drawActivityTrack();
                }
            });
        } catch (ParseException | NumberFormatException e) {
            Log.e(TAG, "--m-- parseActivityData: Error parsing activity data", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Error parsing activity data", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }



    private double calculateDistance(List<LocationData> locations) {
        double totalDistance = 0;
        if (locations == null || locations.size() < 2) return 0;
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
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        if (locations != null && !locations.isEmpty()) {
            drawActivityTrack();
        } else {
            Log.w(TAG, "--m-- onMapReady: No location data available");
            Toast.makeText(this, "No location data available to display", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawActivityTrack() {
        Log.d(TAG, "--m-- drawActivityTrack: Drawing activity track on map");
        if (locations == null || locations.size() < 2) {
            Log.w(TAG, "--m-- drawActivityTrack: Not enough location data to draw track");
            Toast.makeText(this, "Not enough location data to draw track", Toast.LENGTH_SHORT).show();
            return;
        }

        new AsyncTask<Void, Void, PolylineOptions>() {
            @Override
            protected PolylineOptions doInBackground(Void... voids) {
                PolylineOptions polylineOptions = new PolylineOptions()
                        .color(Color.RED)
                        .width(5);

                List<LatLng> smoothedPoints = smoothLocations(locations);
                for (LatLng point : smoothedPoints) {
                    polylineOptions.add(point);
                }

                return polylineOptions;
            }

            @Override
            protected void onPostExecute(PolylineOptions polylineOptions) {
                mMap.clear();
                mMap.addPolyline(polylineOptions);

                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                for (LatLng point : polylineOptions.getPoints()) {
                    boundsBuilder.include(point);
                }

                addMarker(locations.get(0), "Start", BitmapDescriptorFactory.HUE_GREEN);
                addMarker(locations.get(locations.size() - 1), "End", BitmapDescriptorFactory.HUE_RED);

                LatLngBounds bounds = boundsBuilder.build();
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

                Log.d(TAG, "--m-- drawActivityTrack: Activity track drawn successfully");
            }
        }.execute();
    }

    private List<LatLng> smoothLocations(List<LocationData> rawLocations) {
        List<LatLng> smoothedPoints = new ArrayList<>();
        int windowSize = 5; // Adjust this value to change the smoothing level

        for (int i = 0; i < rawLocations.size(); i++) {
            double latSum = 0, lonSum = 0;
            int count = 0;

            for (int j = Math.max(0, i - windowSize / 2); j < Math.min(rawLocations.size(), i + windowSize / 2 + 1); j++) {
                latSum += rawLocations.get(j).getLatitude();
                lonSum += rawLocations.get(j).getLongitude();
                count++;
            }

            double smoothedLat = latSum / count;
            double smoothedLon = lonSum / count;
            smoothedPoints.add(new LatLng(smoothedLat, smoothedLon));
        }

        return smoothedPoints;
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

    private void deleteActivityFile() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    URL url = new URL(BASE_URL + "delete.php?filename=" + activityFilename);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        // Check if the server response indicates successful deletion
                        return response.toString().trim().equalsIgnoreCase("success");
                    }
                    return false;
                } catch (IOException e) {
                    Log.e(TAG, "Error deleting file: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Toast.makeText(ActivityCloudDetailActivity.this, "Activity deleted successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity after successful deletion
                } else {
                    Toast.makeText(ActivityCloudDetailActivity.this, "Failed to delete activity", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

}
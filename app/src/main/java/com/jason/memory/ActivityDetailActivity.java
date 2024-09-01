package com.jason.memory;

import android.app.AlertDialog;
import android.content.Intent;
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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.io.FileWriter;
import java.io.IOException;

public class ActivityDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private TextView tvName, tvStartTime, tvDistance, tvElapsedTime, tvAveragePace, tvCalories;
    private GoogleMap mMap;
    private DatabaseHelper dbHelper;
    private ActivityData activity;
    private Button btnDelete;
    private Button btnBack;
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

        initializeViews();
        setupClickListeners();

        stravaUploader = new StravaUploader(this);
        dbHelper = new DatabaseHelper(this);

        long activityId = getIntent().getLongExtra("ACTIVITY_ID", -1);
        String activityFilename = getIntent().getStringExtra("ACTIVITY_FILENAME");

        activity = dbHelper.getActivity(activityId);

        if (activity != null) {
            displayActivityDetails();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
    }

    private void setupClickListeners() {
        TextView tvStrava = findViewById(R.id.tv_strava);
        TextView tvStravaInfo = findViewById(R.id.tv_strava_info);

        tvStrava.setOnClickListener(v -> uploadToStrava());
        tvStravaInfo.setOnClickListener(v -> uploadToStrava());

        btnDelete.setOnClickListener(v -> deleteActivity());
        btnBack.setOnClickListener(v -> goBack());

        tvSaveActivity.setOnClickListener(v -> saveActivityToFile());
        tvSaveActivityInfo.setOnClickListener(v -> saveActivityToFile());

        tvCloud.setOnClickListener(v -> saveAndUploadActivity());
        tvCloudInfo.setOnClickListener(v -> saveAndUploadActivity());
    }

    private void saveAndUploadActivity() {
        File savedFile = saveActivityToFile();
        if (savedFile != null) {
            uploadFile(savedFile);
        }
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

            List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(activity.getStartTimestamp(), activity.getEndTimestamp());
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
                Toast.makeText(ActivityDetailActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }


    private void goBack() {
        finish();
    }

    private void deleteActivity() {
        if (activity != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Activity")
                    .setMessage("Are you sure you want to delete this activity?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dbHelper.deleteActivity(activity.getId());
                        Toast.makeText(this, "Activity deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else {
            Toast.makeText(this, "No activity to delete", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == StravaUploader.AUTH_REQUEST_CODE) {
            stravaUploader.handleAuthResult(resultCode, data);
        }
    }

    private void uploadToStrava() {
        List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(activity.getStartTimestamp(), activity.getEndTimestamp());
        File gpxFile = stravaUploader.generateGpxFile(locations);

        if (gpxFile != null) {
            stravaUploader.authenticate(gpxFile, activity.getName(),
                    "Activity recorded using MyActivity app", activity.getType());
        } else {
            Toast.makeText(this, "Unable to upload: GPX file generation failed", Toast.LENGTH_SHORT).show();
        }
    }



    private void displayActivityDetails() {
        tvName.setText(activity.getName());
        tvStartTime.setText(formatTimestamp(activity.getStartTimestamp()));
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f", activity.getDistance()));
        tvElapsedTime.setText(formatElapsedTime(activity.getElapsedTime()));

        // Add average pace
        String averagePace = calculateAveragePace(activity.getElapsedTime(), activity.getDistance());
        tvAveragePace.setText(averagePace);

        // Add calories
        int calories = estimateCaloriesBurned(activity.getElapsedTime(), activity.getDistance());
        tvCalories.setText("  "+calories + " KCal");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        drawActivityTrack();
    }

    private void drawActivityTrack() {
        List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(activity.getStartTimestamp(), activity.getEndTimestamp());
        if (locations.size() < 2) {
            Toast.makeText(this, "Not enough location data to draw track", Toast.LENGTH_SHORT).show();
            return;
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .color(Color.RED)  // Set the line color to red
                .width(3);  // Set the line width to 3
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (LocationData location : locations) {
            LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
            polylineOptions.add(point);
            boundsBuilder.include(point);
        }

        LocationData startLocation = locations.get(0);
        LatLng startPoint = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(startPoint)
                .title("Start")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        mMap.addPolyline(polylineOptions);
        LatLngBounds bounds = boundsBuilder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        LocationData endLocation = locations.get(locations.size() - 1);
        LatLng endPoint = new LatLng(endLocation.getLatitude(), endLocation.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(endPoint)
                .title("End")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
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
        // This is a very rough estimation. For more accurate results, you'd need to consider
        // the user's weight, gender, age, and other factors.
        // Here we assume an average person burns about 60 calories per km when running.
        return (int) (distance * 60);
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

}
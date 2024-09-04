package com.jason.memory;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.IOException;

public class FileListActivity extends AppCompatActivity {
    private static final String TAG = "FileListActivity";
    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private List<ActivityData> activityList;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);
        Log.d(TAG, "--m-- onCreate: Initializing FileListActivity");

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        activityList = new ArrayList<>();
        adapter = new FileAdapter(activityList, activity -> {
            Log.d(TAG, "--m-- onItemClick: Starting ActivityDetailActivity for " + activity.getFilename());
            Intent intent = new Intent(FileListActivity.this, ActivityDetailActivity.class);
            intent.putExtra("ACTIVITY_FILENAME", activity.getFilename());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadFileList();
    }

    private void showNoFilesMessage() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("No Files Found")
                .setMessage("There are no CSV files in the directory.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
        Log.d(TAG, "--m-- showNoFilesMessage: Displayed no files message");
    }


    private static final int BATCH_SIZE = 50;
    private int currentFileIndex = 0;

    private void loadFileList() {
        Log.d(TAG, "--m-- loadFileList: Starting to load file list");
        showProgressBar(true);
        new Thread(() -> {
            File directory = Config.getDownloadDir();
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

            if (files == null || files.length == 0) {
                runOnUiThread(() -> {
                    showProgressBar(false);
                    showNoFilesMessage();
                });
                return;
            }

            loadNextBatch(files);
        }).start();
    }

    private void loadNextBatch(File[] files) {
        int endIndex = Math.min(currentFileIndex + BATCH_SIZE, files.length);
        for (int i = currentFileIndex; i < endIndex; i++) {
            ActivityData activity = parseActivityDataFromFile(files[i]);
            if (activity != null) {
                activityList.add(activity);
            }
        }

        sortActivityList();

        runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
            if (endIndex < files.length) {
                currentFileIndex = endIndex;
                loadNextBatch(files);
            } else {
                showProgressBar(false);
            }
            Log.d(TAG, "--m-- loadFileList: Loaded batch. Total activities: " + activityList.size());
        });
    }


    private ActivityData parseActivityDataFromFile(File file) {
        Log.d(TAG, "--m-- parseActivityDataFromFile: Parsing file: " + file.getName());
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {
            String line;
            reader.readLine(); // Skip header


            String firstLine = reader.readLine();
            if (firstLine == null) {
                Log.e(TAG, "--m-- parseActivityDataFromFile: Empty file: " + file.getName());
                return null;
            }

            String[] firstParts = firstLine.split(",");
            if (firstParts.length < 4) {
                Log.e(TAG, "--m-- parseActivityDataFromFile: Invalid CSV format in file: " + file.getName());
                return null;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());
            Date startDate = sdf.parse(firstParts[2] + "," + firstParts[3]);
            long startTimestamp = startDate.getTime();

            // Read the last line to get the end timestamp
            String lastLine = firstLine;
            String previousLine = null;


            while ((line = reader.readLine()) != null) {
                previousLine = lastLine;
                lastLine = line;
            }


            String[] lastParts = lastLine.split(",");
            if (lastParts.length < 4) {
                if (previousLine != null) {
                    lastParts = previousLine.split(",");
                } else {
                    Log.e(TAG, "--m-- parseActivityDataFromFile: Invalid CSV format in last line of file: " + file.getName());
                    return null;
                }
            }

            Date endDate = sdf.parse(lastParts[2] + "," + lastParts[3]);
            long endTimestamp = endDate.getTime();

            String name = file.getName().replace(".csv", "");
            double distance = calculateDistance(firstParts, lastParts);
            long elapsedTime = endTimestamp - startTimestamp;
            String address = "";
            try{
                address = getAddress(Double.parseDouble(firstParts[0]), Double.parseDouble(firstParts[1]));
            }catch(Exception e) {
                Log.e(TAG, "--m-- " + e.getMessage());
            }

            Log.d(TAG, "--m-- parseActivityDataFromFile: Successfully parsed activity: " + name);
            return new ActivityData(0, file.getName(), "run", name, startTimestamp, endTimestamp, 0, 0, distance, elapsedTime, address);
        } catch (IOException | ParseException e) {
            Log.e(TAG, "--m-- parseActivityDataFromFile: Error parsing file: " + file.getName(), e);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "--m-- parseActivityDataFromFile: Invalid CSV format in file: " + file.getName(), e);
        }
        return null;
    }

    private double calculateDistance(String[] start, String[] end) {
        try {
            double lat1 = Double.parseDouble(start[0]);
            double lon1 = Double.parseDouble(start[1]);
            double lat2 = Double.parseDouble(end[0]);
            double lon2 = Double.parseDouble(end[1]);

            // Implement distance calculation using Haversine formula
            // This is a simplified version, you may want to use a more accurate method
            final int R = 6371; // Radius of the earth in km
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        }catch(Exception e) {
            Log.e(TAG, "--m-- " + e.getMessage());
            return 0;
        }
    }

    private String getAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting address", e);
        }
        return "Address not available";
    }


    private void sortActivityList() {
        Log.d(TAG, "--m-- sortActivityList: Sorting activity list");
        Collections.sort(activityList, new ActivityTimestampComparator());
    }

    private void showProgressBar(boolean show) {
        runOnUiThread(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            Log.d(TAG, "--m-- showProgressBar: Progress bar visibility set to " + (show ? "visible" : "gone"));
        });
    }

    private class ActivityTimestampComparator implements Comparator<ActivityData> {
        @Override
        public int compare(ActivityData a1, ActivityData a2) {
            return Long.compare(a2.getStartTimestamp(), a1.getStartTimestamp());
        }
    }
}
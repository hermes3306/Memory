package com.jason.memory;

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




    private void loadFileList() {
        Log.d(TAG, "--m-- loadFileList: Starting to load file list");
        showProgressBar(true);
        new Thread(() -> {
            File directory = Config.getDownloadDir();
            Log.d(TAG, "--m-- loadFileList: Searching directory: " + directory.getAbsolutePath());
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

            activityList.clear();
            if (files == null || files.length == 0) {
                Log.d(TAG, "--m-- loadFileList: No CSV files found or directory does not exist");
                runOnUiThread(() -> {
                    showProgressBar(false);
                    showNoFilesMessage();
                });
                return;
            }

            Log.d(TAG, "--m-- loadFileList: Found " + files.length + " CSV files");
            for (File file : files) {
                ActivityData activity = parseActivityDataFromFile(file);
                if (activity != null) {
                    activityList.add(activity);
                }
            }

            sortActivityList();

            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                showProgressBar(false);
                Log.d(TAG, "--m-- loadFileList: Finished loading. Total activities: " + activityList.size());
            });
        }).start();
    }



    private ActivityData parseActivityDataFromFile(File file) {
        Log.d(TAG, "--m-- parseActivityDataFromFile: Parsing file: " + file.getName());
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {
            String line;
            reader.readLine(); // Skip header
            if ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 4) {
                    Log.e(TAG, "--m-- parseActivityDataFromFile: Invalid CSV format in file: " + file.getName());
                    return null;
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());
                Date startDate = sdf.parse(parts[2] + "," + parts[3]);
                long startTimestamp = startDate.getTime();

                // Read the last line to get the end timestamp
                String lastLine = line;
                while ((line = reader.readLine()) != null) {
                    lastLine = line;
                }
                parts = lastLine.split(",");
                if (parts.length < 4) {
                    Log.e(TAG, "--m-- parseActivityDataFromFile: Invalid CSV format in last line of file: " + file.getName());
                    return null;
                }
                Date endDate = sdf.parse(parts[2] + "," + parts[3]);
                long endTimestamp = endDate.getTime();

                String name = file.getName().replace(".csv", "");
                Log.d(TAG, "--m-- parseActivityDataFromFile: Successfully parsed activity: " + name);
                return new ActivityData(0, file.getName(), "run", name, startTimestamp, endTimestamp, 0, 0, 0, endTimestamp - startTimestamp, "Address not available");
            }
        } catch (IOException | ParseException e) {
            Log.e(TAG, "--m-- parseActivityDataFromFile: Error parsing file: " + file.getName(), e);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "--m-- parseActivityDataFromFile: Invalid CSV format in file: " + file.getName(), e);
        }
        return null;
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
package com.jason.memory;

import android.os.Bundle;
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
    private ActivityAdapter adapter;
    private List<ActivityData> activityList;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        activityList = new ArrayList<>();
        adapter = new ActivityAdapter(activityList, activity -> {
            Intent intent = new Intent(FileListActivity.this, ActivityDetailActivity.class);
            intent.putExtra("ACTIVITY_FILENAME", activity.getFilename());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadFileList();
    }

    private void loadFileList() {
        showProgressBar(true);
        new Thread(() -> {
            File directory = new File(getExternalFilesDir(null), "memory_activity");
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

            activityList.clear();
            if (files != null) {
                for (File file : files) {
                    ActivityData activity = parseActivityDataFromFile(file);
                    if (activity != null) {
                        activityList.add(activity);
                    }
                }
            }

            sortActivityList();

            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                showProgressBar(false);
            });
        }).start();
    }

    private ActivityData parseActivityDataFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {
            String line;
            reader.readLine(); // Skip header
            if ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());
                Date startDate = sdf.parse(parts[2] + "," + parts[3]);
                long startTimestamp = startDate.getTime();

                // Read the last line to get the end timestamp
                String lastLine = line;
                while ((line = reader.readLine()) != null) {
                    lastLine = line;
                }
                parts = lastLine.split(",");
                Date endDate = sdf.parse(parts[2] + "," + parts[3]);
                long endTimestamp = endDate.getTime();

                String name = file.getName().replace(".csv", "");
                return new ActivityData(0, file.getName(), "run", name, startTimestamp, endTimestamp, 0, 0, 0, endTimestamp - startTimestamp, "Address not available");
            }
        } catch (IOException | ParseException e) {
            Log.e(TAG, "Error parsing file: " + file.getName(), e);
        }
        return null;
    }

    private void sortActivityList() {
        Collections.sort(activityList, new ActivityTimestampComparator());
    }

    private void showProgressBar(boolean show) {
        runOnUiThread(() -> progressBar.setVisibility(show ? View.VISIBLE : View.GONE));
    }

    private class ActivityTimestampComparator implements Comparator<ActivityData> {
        @Override
        public int compare(ActivityData a1, ActivityData a2) {
            return Long.compare(a2.getStartTimestamp(), a1.getStartTimestamp());
        }
    }
}
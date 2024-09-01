package com.jason.memory;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileListActivity extends AppCompatActivity {
    private static final String TAG = "FileListActivity";
    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<File> files = getCSVFiles();
        fileAdapter = new FileAdapter(files);
        recyclerView.setAdapter(fileAdapter);
    }

    private List<File> getCSVFiles() {
        List<File> csvFiles = new ArrayList<>();
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File appDir = new File(downloadDir, "MemoryApp");

        Log.d(TAG, "Searching for CSV files in: " + appDir.getAbsolutePath());

        if (appDir.exists() && appDir.isDirectory()) {
            File[] files = appDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
            if (files != null) {
                csvFiles.addAll(Arrays.asList(files));
                Log.d(TAG, "Found " + csvFiles.size() + " CSV files");
            } else {
                Log.d(TAG, "No files found or unable to list files");
            }
        } else {
            Log.d(TAG, "App directory does not exist or is not a directory");
        }

        return csvFiles;
    }
}
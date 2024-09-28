package com.jason.memory;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MemoryActivity extends AppCompatActivity implements MemoryAdapter.OnMemoryClickListener {
    private static final String TAG = "MemoryActivity";
    private ZoomableRecyclerView memoriesRecyclerView;
    private MemoryAdapter memoryAdapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "--m-- onCreate: Starting MemoryActivity");
        setContentView(R.layout.activity_memory);

        dbHelper = new DatabaseHelper(this);

        memoriesRecyclerView = findViewById(R.id.memoriesRecyclerView);
        if (memoriesRecyclerView == null) {
            Log.e(TAG, "--m-- onCreate: memoriesRecyclerView is null");
        } else {
            Log.d(TAG, "--m-- onCreate: memoriesRecyclerView found");
            memoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            memoriesRecyclerView.setHasFixedSize(true);
        }

        updateMemoryList();

        ImageButton searchButton = findViewById(R.id.searchButton);
        ImageButton addButton = findViewById(R.id.addButton);
        ImageButton saveButton = findViewById(R.id.saveButton);
        ImageButton syncButton = findViewById(R.id.syncButton);

        searchButton.setOnClickListener(v -> searchMemories());
        addButton.setOnClickListener(v -> openAddEditMemoryActivity());
        saveButton.setOnClickListener(v -> saveMemories());
        syncButton.setOnClickListener(v -> syncMemories());

        Log.d(TAG, "--m-- onCreate: MemoryActivity setup complete");
    }

    @Override
    public void onEditMemory(long memoryId) {
        openAddEditMemoryActivity(memoryId);
    }

    private void openAddEditMemoryActivity(long memoryId) {
        Intent intent = new Intent(this, AddEditMemoryActivity.class);
        intent.putExtra("MEMORY_ID", memoryId);
        intent.putExtra("IS_EDIT", true);
        startActivity(intent);
    }

    private void updateMemoryList() {
        Log.d(TAG, "--m-- updateMemoryList: Fetching memories from database");
        List<MemoryItem> memoryItems = dbHelper.getAllMemories();
        Log.d(TAG, "--m-- updateMemoryList: Retrieved " + memoryItems.size() + " memories");
        memoryAdapter = new MemoryAdapter(memoryItems, this, this);
        if (memoriesRecyclerView != null) {
            memoriesRecyclerView.setAdapter(memoryAdapter);
            Log.d(TAG, "--m-- updateMemoryList: Adapter set on RecyclerView");
        } else {
            Log.e(TAG, "--m-- updateMemoryList: memoriesRecyclerView is null");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "--m-- onResume: Updating memory list");
        updateMemoryList();
    }

    @Override
    public void onMemoryClick(long memoryId) {
        Log.d(TAG, "--m-- onMemoryClick: Opening edit activity for memory ID: " + memoryId);
        openAddEditMemoryActivity(memoryId);
    }

    @Override
    public void onMemoryLongClick(String content) {
        Log.d(TAG, "--m-- onMemoryLongClick: Copying content to clipboard");
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Memory Content", content);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Memory content copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void openAddEditMemoryActivity() {
        Log.d(TAG, "--m-- openAddEditMemoryActivity: Opening activity to add new memory");
        Intent intent = new Intent(this, AddEditMemoryActivity.class);
        startActivity(intent);
    }


    private void searchMemories() {
        Log.d(TAG, "--m-- searchMemories: Search function not implemented yet");
        Toast.makeText(this, "Search function not implemented yet", Toast.LENGTH_SHORT).show();
    }

    private void saveMemories() {
        Log.d(TAG, "--m-- saveMemories: Starting save process");
        try {
            List<MemoryItem> allMemories = dbHelper.getAllMemories();
            Gson gson = new Gson();
            String jsonMemories = gson.toJson(allMemories);

            File directory = Config.getDownloadDir4Memories();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String fileName = System.currentTimeMillis() + Config.MEMORY_EXT;
            File file = new File(directory, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(jsonMemories);
            writer.close();

            Utility.uploadFile(this, file, new Utility.UploadCallback() {
                @Override
                public void onUploadComplete(boolean success, String serverResponse) {
                    runOnUiThread(() -> {
                        if (success) {
                            Log.d(TAG, "--m-- saveMemories: Memories saved and uploaded successfully");
                            Toast.makeText(MemoryActivity.this, "Memories saved and uploaded successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "--m-- saveMemories: Error uploading memories: " + serverResponse);
                            Toast.makeText(MemoryActivity.this, "Error uploading memories: " + serverResponse, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            Log.d(TAG, "--m-- saveMemories: Memories saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "--m-- saveMemories: Error saving memories", e);
            Toast.makeText(this, "Error saving memories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void syncMemories() {
        Log.d(TAG, "--m-- syncMemories: Showing sync confirmation dialog");
        new AlertDialog.Builder(this)
                .setTitle("Sync with Server")
                .setMessage("Do you want to download and merge the latest data from the server?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Log.d(TAG, "--m-- syncMemories: Starting sync process");
                    Utility.downloadJsonAndMergeServerData(this, Config.MEMORY_EXT, dbHelper, this::onSyncComplete);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void onSyncComplete(boolean success) {
        runOnUiThread(() -> {
            if (success) {
                Log.d(TAG, "--m-- onSyncComplete: Sync completed successfully");
                Toast.makeText(this, "Sync completed successfully", Toast.LENGTH_SHORT).show();
                List<MemoryItem> updatedMemories = dbHelper.getAllMemories();
                updateMemoryList();
            } else {
                Log.e(TAG, "--m-- onSyncComplete: Sync failed");
                Toast.makeText(this, "Sync failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
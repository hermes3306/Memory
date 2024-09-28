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


    private void updateMemoryList() {
        Log.d(TAG, "--m-- updateMemoryList: Fetching memories from database");
        List<MemoryItem> memoryItems = dbHelper.getAllMemories();
        Log.d(TAG, "--m-- updateMemoryList: Retrieved " + memoryItems.size() + " memories");

        if (memoryAdapter == null) {
            memoryAdapter = new MemoryAdapter(memoryItems, this, this);
            if (memoriesRecyclerView != null) {
                memoriesRecyclerView.setAdapter(memoryAdapter);
                Log.d(TAG, "--m-- updateMemoryList: Adapter set on RecyclerView");
            } else {
                Log.e(TAG, "--m-- updateMemoryList: memoriesRecyclerView is null");
            }
        } else {
            memoryAdapter.updateMemories(memoryItems);
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

    private void openAddEditMemoryActivity(long memoryId) {
        Log.d(TAG, "--m-- openAddEditMemoryActivity: Opening activity to edit memory ID: " + memoryId);
        Intent intent = new Intent(this, AddEditMemoryActivity.class);
        intent.putExtra("MEMORY_ID", memoryId);
        intent.putExtra("IS_EDIT", true);
        startActivity(intent);
    }

    private void searchMemories() {
        Log.d(TAG, "--m-- searchMemories: Search function not implemented yet");
        Toast.makeText(this, "Search function not implemented yet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLikeCountClick(long memoryId) {
        // Implement what should happen when like count is clicked
        // For example, show a list of users who liked the memory
    }

    @Override
    public void onCommentCountClick(long memoryId) {
        // Implement what should happen when comment count is clicked
        // For example, expand/collapse the comments section
    }

    @Override
    public void onLikeClick(long memoryId, String userId) {
        Log.d(TAG, "--m-- onLikeClick: Attempting to increment like count for memory ID: " + memoryId + " by user: " + userId);
        boolean likeAdded = dbHelper.incrementLikeCount(memoryId, userId);
        if (likeAdded) {
            MemoryItem updatedItem = dbHelper.getMemory(memoryId);
            memoryAdapter.updateItem(memoryId, updatedItem);
        }
    }

    @Override
    public void onCommentSend(long memoryId, String comment) {
        Log.d(TAG, "--m-- onCommentSend: Adding comment for memory ID: " + memoryId);
        MemoryItem updatedItem = dbHelper.addComment(memoryId, comment, this);


        // Update only this item in the adapter
        memoryAdapter.updateItem(memoryId, updatedItem);

    }

    @Override
    public boolean hasUserLikedMemory(long memoryId, String userId) {
        return dbHelper.hasUserLikedMemory(memoryId, userId);
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
            String fileName =System.currentTimeMillis() + Config.MEMORY_EXT;
            File file = new File(directory, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(jsonMemories);
            writer.close();

            Utility.uploadFile(this, file);

            Log.d(TAG, "--m-- saveMemories: Memories saved to " + file.getAbsolutePath());
            Toast.makeText(this, "Memories saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "--m-- saveMemories: Error saving memories", e);
            Toast.makeText(this, "Error saving memories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void syncMemories() {
        Log.d(TAG, "--m-- syncMemories: Showing sync confirmation dialog");
        boolean noalert=true;
        if (noalert) {
            Log.d(TAG, "--m-- syncMemories: Starting sync process");
            Utility.downloadJsonAndMergeServerData(this, Config.MEMORY_EXT, dbHelper, this::onSyncComplete);
        } else {
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
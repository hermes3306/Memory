package com.jason.memory;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
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
    private String currentUserId;


    @Override
    public void onUserIdClick(String userId) {
        Toast.makeText(this, "User ID: " + userId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTitleClick(String title) {
        Toast.makeText(this, "Title: " + title, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDateClick(String date) {
        Toast.makeText(this, "Date: " + date, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onContentClick(String content) {
        // Truncate the content if it's too long for the toast
        String truncatedContent = content.length() > 50 ? content.substring(0, 47) + "..." : content;
        Toast.makeText(this, "Content: " + truncatedContent, Toast.LENGTH_SHORT).show();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "--m-- onCreate: Starting MemoryActivity");
        setContentView(R.layout.activity_memory);

        dbHelper = new DatabaseHelper(this);
        currentUserId = Utility.getCurrentUser(this);

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
        //saveButton.setOnClickListener(v -> saveMemories());
        syncButton.setOnClickListener(v -> syncMemories());



        // Add click listeners for bottom navigation icons
        findViewById(R.id.iconChat).setOnClickListener(v -> openChatActivity());
        findViewById(R.id.iconRun).setOnClickListener(v -> openListActivityActivity());
        findViewById(R.id.iconMemory).setOnClickListener(v -> {/* Already in MemoryActivity */});
        findViewById(R.id.iconPlace).setOnClickListener(v -> openPlacesActivity());
        findViewById(R.id.iconMe).setOnClickListener(v -> openSettingActivity());


        Log.d(TAG, "--m-- onCreate: MemoryActivity setup complete");
    }


    private void openChatActivity() {
        Intent intent = new Intent(this, ChatActivity.class);
        startActivity(intent);
    }

    private void openListActivityActivity() {
        Intent intent = new Intent(this, ListActivityActivity.class);
        startActivity(intent);
    }

    private void openPlacesActivity() {
        Intent intent = new Intent(this, PlacesActivity.class);
        startActivity(intent);
    }

    private void openSettingActivity() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
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
            Log.d(TAG, "--m-- Like added successfully. New like count: " + updatedItem.getLikes() + ", Who likes: " + updatedItem.getWhoLikes());
        } else {
            Log.d(TAG, "--m-- Like not added. User may have already liked this memory.");
        }
    }

    @Override
    public void onUnlikeClick(long memoryId, String userId) {
        Log.d(TAG, "--m-- onUnlikeClick: Attempting to decrement like count for memory ID: " + memoryId + " by user: " + userId);
        boolean likeRemoved = dbHelper.decrementLikeCount(memoryId, userId);
        if (likeRemoved) {
            MemoryItem updatedItem = dbHelper.getMemory(memoryId);
            memoryAdapter.updateItem(memoryId, updatedItem);
            Log.d(TAG, "--m-- Like removed successfully. New like count: " + updatedItem.getLikes() + ", Who likes: " + updatedItem.getWhoLikes());
        } else {
            Log.d(TAG, "--m-- Like not removed. User may not have liked this memory.");
        }
    }


    @Override
    public boolean hasUserLikedMemory(long memoryId, String userId) {
        return dbHelper.hasUserLikedMemory(memoryId, currentUserId);
    }

    @Override
    public void onCommentSend(long memoryId, String comment) {
        Log.d(TAG, "--m-- onCommentSend: Adding comment for memory ID: " + memoryId);
        MemoryItem updatedItem = dbHelper.addComment(memoryId, comment, this);


        // Update only this item in the adapter
        memoryAdapter.updateItem(memoryId, updatedItem);

    }


    private void uploadMemories() {
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

            Log.d(TAG, "--m-- uploadMemories: Memories saved to " + file.getAbsolutePath());
            Toast.makeText(this, "Memories saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "--m-- uploadMemories: Error saving memories", e);
            Toast.makeText(this, "Error upload memories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void syncMemories() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sync Options")
                .setItems(new CharSequence[]{"Download", "Upload", "Both"}, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // Download
                                downloadMemories();
                                break;
                            case 1: // Upload
                                uploadMemories();
                                break;
                            case 2: // Both
                                uploadMemories();
                                downloadMemories();
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    private void downloadMemories() {
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
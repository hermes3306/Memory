package com.jason.memory;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MemoryActivity extends AppCompatActivity implements MemoryAdapter.OnMemoryClickListener {
    private RecyclerView memoriesRecyclerView;
    private MemoryAdapter memoryAdapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory);

        dbHelper = new DatabaseHelper(this);

        memoriesRecyclerView = findViewById(R.id.memoriesRecyclerView);
        memoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        updateMemoryList();

        ImageButton searchButton = findViewById(R.id.searchButton);
        ImageButton addButton = findViewById(R.id.addButton);
        ImageButton saveButton = findViewById(R.id.saveButton);
        ImageButton syncButton = findViewById(R.id.syncButton);

        searchButton.setOnClickListener(v -> searchMemories());
        addButton.setOnClickListener(v -> openAddEditMemoryActivity());
        saveButton.setOnClickListener(v -> saveMemories());
        syncButton.setOnClickListener(v -> syncMemories());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMemoryList();
    }

    private void updateMemoryList() {
        List<MemoryItem> memoryItems = dbHelper.getAllMemories();
        memoryAdapter = new MemoryAdapter(memoryItems, this, this);
        memoriesRecyclerView.setAdapter(memoryAdapter);
    }

    @Override
    public void onMemoryClick(long memoryId) {
        openAddEditMemoryActivity(memoryId);
    }

    @Override
    public void onMemoryLongClick(String content) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Memory Content", content);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Memory content copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void openAddEditMemoryActivity() {
        Intent intent = new Intent(this, AddEditMemoryActivity.class);
        startActivity(intent);
    }

    private void openAddEditMemoryActivity(long memoryId) {
        Intent intent = new Intent(this, AddEditMemoryActivity.class);
        intent.putExtra("MEMORY_ID", memoryId);
        intent.putExtra("IS_EDIT", true);
        startActivity(intent);
    }

    private void searchMemories() {
        // TODO: Implement search functionality
        Toast.makeText(this, "Search function not implemented yet", Toast.LENGTH_SHORT).show();
    }


    private void saveMemories() {
        try {
            List<MemoryItem> allMemories = dbHelper.getAllMemories();
            Gson gson = new Gson();
            String jsonMemories = gson.toJson(allMemories);

            File directory = new File(Config.getDownloadDir(), "json");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String fileName = "memories_" + System.currentTimeMillis() + ".jsn";
            File file = new File(directory, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(jsonMemories);
            writer.close();

            Utility.uploadFile(this, file);

            Toast.makeText(this, "Memories saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving memories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void syncMemories() {
        new AlertDialog.Builder(this)
                .setTitle("Sync with Server")
                .setMessage("Do you want to download and merge the latest data from the server?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Utility.downloadJsonAndMergeServerData(this, "jsn",dbHelper, this::onSyncComplete);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void onSyncComplete(boolean success) {
        runOnUiThread(() -> {
            if (success) {
                Toast.makeText(this, "Sync completed successfully", Toast.LENGTH_SHORT).show();
                // Refresh the list of places
                List<MemoryItem> updatedMemories = dbHelper.getAllMemories();
                //memoryAdapter.submitList(updatedMemories);
                updateMemoryList();
            } else {
                Toast.makeText(this, "Sync failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

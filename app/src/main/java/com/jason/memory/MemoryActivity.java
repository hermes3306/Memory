package com.jason.memory;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
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

        FloatingActionButton fab = findViewById(R.id.fabAddMemory);
        fab.setOnClickListener(v -> openAddEditMemoryActivity());
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
}
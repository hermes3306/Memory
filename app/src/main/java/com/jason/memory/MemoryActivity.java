package com.jason.memory;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MemoryActivity extends AppCompatActivity {

    private RecyclerView memoriesRecyclerView;
    private MemoryAdapter memoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory);

        memoriesRecyclerView = findViewById(R.id.memoriesRecyclerView);
        memoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<MemoryItem> memoryItems = getMemoryItems(); // You'll need to implement this method
        memoryAdapter = new MemoryAdapter(memoryItems, this);  // Pass 'this' as the Context
        memoriesRecyclerView.setAdapter(memoryAdapter);
    }

    private List<MemoryItem> getMemoryItems() {
        // TODO: Implement this method to fetch memory items from your database
        // For now, we'll return a dummy list
        List<MemoryItem> items = new ArrayList<>();
        items.add(new MemoryItem("First Memory", "2023-08-27", "This is my first memory..."));
        items.add(new MemoryItem("Second Memory", "2023-08-28", "Another memorable day..."));
        return items;
    }
}

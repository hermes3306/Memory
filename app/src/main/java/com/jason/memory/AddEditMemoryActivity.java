package com.jason.memory;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class AddEditMemoryActivity extends AppCompatActivity {
    private EditText titleEditText;
    private TextView dateTextView;
    private EditText memoryEditText;
    private DatabaseHelper dbHelper;
    private long editMemoryId = -1;
    private Button deleteButton;
    private Button addPlaceInfoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_memory);

        dbHelper = new DatabaseHelper(this);

        titleEditText = findViewById(R.id.titleEditText);
        dateTextView = findViewById(R.id.dateTextView);
        memoryEditText = findViewById(R.id.memoryEditText);
        deleteButton = findViewById(R.id.deleteButton);
        addPlaceInfoButton = findViewById(R.id.addPlaceInfoButton);
        addPlaceInfoButton.setOnClickListener(v -> showPlaceInfoDialog());

        // Set the current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        dateTextView.setText(currentDate);

        // Check if we're editing an existing memory
        if (getIntent().getBooleanExtra("IS_EDIT", false)) {
            long memoryId = getIntent().getLongExtra("MEMORY_ID", -1);
            if (memoryId != -1) {
                editMemoryId = memoryId;
                MemoryItem memory = dbHelper.getMemory(memoryId);
                titleEditText.setText(memory.getTitle());
                dateTextView.setText(memory.getDate());
                memoryEditText.setText(memory.getMemoryText());

                // Show delete button for existing memories
                deleteButton.setVisibility(View.VISIBLE);
            }
        }

        // Set up save button
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> saveMemory());

        // Set up delete button
        deleteButton.setOnClickListener(v -> deleteMemory());
    }

    private void showPlaceInfoDialog() {
        // Assuming you have a PlacesManager class to handle places data
        List<Place> places = dbHelper.getAllPlaces();

        String[] placeNames = new String[places.size()];
        for (int i = 0; i < places.size(); i++) {
            placeNames[i] = places.get(i).getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a place")
                .setItems(placeNames, (dialog, which) -> {
                    Place selectedPlace = places.get(which);
                    updateMemoryWithPlaceInfo(selectedPlace);
                });
        builder.create().show();
    }

    private void updateMemoryWithPlaceInfo(Place place) {
        String currentText = memoryEditText.getText().toString();
        String placeInfo = String.format(Locale.getDefault(),
                "\n\n-- The place ID(%d).\n" +
                        "Name: %s\n" +
                        "Address: %s\n" +
                        "Visits: %d\n" +
                        "Last visited: %s\n" +
                        "Location: (%.6f, %.6f)\n" +
                        "URL: %s\n",
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getNumberOfVisits(),
                place.getLastVisited(),
                place.getLat(),
                place.getLon(),
                place.getMapUrl());

        String updatedText = currentText + placeInfo;
        memoryEditText.setText(updatedText);
    }


    private void saveMemory() {
        String title = titleEditText.getText().toString();
        String memoryText = memoryEditText.getText().toString();
        String date = dateTextView.getText().toString();

        if (title.isEmpty() || memoryText.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        MemoryItem memory = new MemoryItem(editMemoryId, title, date, memoryText);
        if (editMemoryId == -1) {
            long newId = dbHelper.addMemory(memory);
            if (newId != -1) {
                Toast.makeText(this, "Memory saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error saving memory", Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsAffected = dbHelper.updateMemory(memory);
            if (rowsAffected > 0) {
                Toast.makeText(this, "Memory updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error updating memory", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void deleteMemory() {
        if (editMemoryId != -1) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Memory")
                    .setMessage("Are you sure you want to delete this memory?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        int rowsDeleted = dbHelper.deleteMemory(editMemoryId);
                        if (rowsDeleted > 0) {
                            Toast.makeText(this, "Memory deleted successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Error deleting memory", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }
}

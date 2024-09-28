package com.jason.memory;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddEditMemoryActivity extends AppCompatActivity {
    private static final String TAG = "AddEditMemoryActivity";
    private EditText titleEditText;
    private EditText memoryEditText;
    private DatabaseHelper dbHelper;
    private long editMemoryId = -1;
    private ImageButton closeButton;
    private Button saveButton;
    private ImageButton addPlaceInfoImage;
    private ImageButton deleteImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_memory);

        dbHelper = new DatabaseHelper(this);

        titleEditText = findViewById(R.id.titleEditText);

        memoryEditText = findViewById(R.id.memoryEditText);
        closeButton = findViewById(R.id.closeButton);
        saveButton = findViewById(R.id.saveButton); // This should now work correctly
        addPlaceInfoImage = findViewById(R.id.addPlaceInfoImage);
        deleteImage = findViewById(R.id.deleteImage);

        closeButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveMemory());
        addPlaceInfoImage.setOnClickListener(v -> showPlaceInfoDialog());
        deleteImage.setOnClickListener(v -> deleteMemory());

        // Check if we're editing an existing memory
        if (getIntent().getBooleanExtra("IS_EDIT", false)) {
            long memoryId = getIntent().getLongExtra("MEMORY_ID", -1);
            if (memoryId != -1) {
                editMemoryId = memoryId;
                MemoryItem memory = dbHelper.getMemory(memoryId);
                titleEditText.setText(memory.getTitle());
                memoryEditText.setText(memory.getMemoryText());

                // Show delete image for existing memories
                deleteImage.setVisibility(View.VISIBLE);
            }
        } else {
            deleteImage.setVisibility(View.GONE);
        }
    }




    private void showPlaceInfoDialog() {
        List<Place> places = dbHelper.getAllPlaces();
        if (places == null || places.isEmpty()) {
            Toast.makeText(this, "No places found", Toast.LENGTH_SHORT).show();
            return;
        }

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
        String url = String.format(Locale.US, "https://www.google.com/maps?q=%f,%f",
                place.getLat(), place.getLon());
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
                formatDate(place.getLastVisited()),
                place.getLat(),
                place.getLon(),
                url);

        String updatedText = currentText + placeInfo;
        memoryEditText.setText(updatedText);

        Log.d(TAG, "Place info added: " + placeInfo);
        Toast.makeText(this, "Place info added", Toast.LENGTH_SHORT).show();
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }


    private void saveMemory() {
        String title = titleEditText.getText().toString();
        String memoryText = memoryEditText.getText().toString();
        long timestamp = System.currentTimeMillis();

        if (title.isEmpty() || memoryText.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        MemoryItem memory = new MemoryItem(editMemoryId, title, timestamp, memoryText);
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
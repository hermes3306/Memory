package com.jason.memory;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddEditMemoryActivity extends AppCompatActivity {
    private static final String TAG = "AddEditMemoryActivity";
    private EditText titleEditText;
    private TextView dateTextView;
    private EditText memoryEditText;
    private DatabaseHelper dbHelper;
    private long editMemoryId = -1;
    private Button deleteButton;
    private Button addPlaceInfoButton;
    private Button addPictureButton;
    private String[] pictures;
    private String place;

    private static final int PICK_IMAGE_REQUEST = 1;
    private ArrayList<Uri> selectedImageUris = new ArrayList<>();
    private ArrayList<String> uploadedImageUrls = new ArrayList<>();

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
        addPictureButton = findViewById(R.id.addPictureButton);
        addPictureButton.setOnClickListener(v -> openImageChooser());



        pictures = new String[9];
        place = "";

        addPlaceInfoButton.setOnClickListener(v -> showPlaceInfoDialog());
        addPictureButton.setOnClickListener(v -> openImageChooser());

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
                pictures = memory.getPictures();
                place = memory.getPlace();

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

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    selectedImageUris.add(imageUri);
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                selectedImageUris.add(imageUri);
            }

            uploadImages();
        }
    }

    private void uploadImages() {
        for (Uri imageUri : selectedImageUris) {
            File imageFile = new File(getRealPathFromURI(imageUri));
            Utility.uploadFile(this, imageFile, new Utility.UploadCallback() {
                @Override
                public void onUploadComplete(boolean success, String serverResponse) {
                    if (success) {
                        String imageUrl = Config.IMAGE_BASE_URL + imageFile.getName();
                        uploadedImageUrls.add(imageUrl);
                        updatePicturesInMemory();
                    } else {
                        Toast.makeText(AddEditMemoryActivity.this, "Upload failed: " + serverResponse, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void updatePicturesInMemory() {
        pictures = uploadedImageUrls.toArray(new String[0]);
        Toast.makeText(this, "Pictures updated", Toast.LENGTH_SHORT).show();
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    private void showPlaceInfoDialog() {
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

    private void showAddPictureDialog() {
        EditText input = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("Add Picture URL")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String url = input.getText().toString();
                    addPictureUrl(url);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addPictureUrl(String url) {
        for (int i = 0; i < pictures.length; i++) {
            if (pictures[i] == null || pictures[i].isEmpty()) {
                pictures[i] = url;
                Toast.makeText(this, "Picture added", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(this, "Cannot add more pictures. Maximum limit reached.", Toast.LENGTH_SHORT).show();
    }

    private void updateMemoryWithPlaceInfo(Place selectedPlace) {
        place = selectedPlace.getName();
        String placeInfo = String.format(Locale.getDefault(),
                "\n\nPlace: %s\nAddress: %s\n",
                selectedPlace.getName(),
                selectedPlace.getAddress());
        String currentText = memoryEditText.getText().toString();
        memoryEditText.setText(currentText + placeInfo);
    }

    private void saveMemory() {
        String title = titleEditText.getText().toString();
        String memoryText = memoryEditText.getText().toString();
        String date = dateTextView.getText().toString();

        if (title.isEmpty() || memoryText.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        MemoryItem memory = new MemoryItem(editMemoryId, title, date, memoryText, pictures, place);
        // ... rest of the save logic ...
        if (editMemoryId == -1) {
            long newId = dbHelper.addMemoryItem(memory);
            if (newId != -1) {
                Toast.makeText(this, "Memory saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error saving memory", Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsAffected = dbHelper.updateMemoryItem(memory);
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
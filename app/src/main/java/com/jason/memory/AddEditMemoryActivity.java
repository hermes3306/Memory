package com.jason.memory;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private String currentUserId;
    private ImageButton addImageButton;
    private static final int PICK_IMAGES_REQUEST = 1;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private LinearLayout imagePreviewContainer;

    private void openGallery() {
        Log.d(TAG, "--m-- Opening gallery for image selection");
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            Log.d(TAG, "--m-- Images selected from gallery");
            if (data.getClipData() != null) {
                int count = Math.min(data.getClipData().getItemCount(), 9 - selectedImageUris.size());
                Log.d(TAG, "--m-- Multiple images selected: " + count);
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    selectedImageUris.add(imageUri);
                    Log.d(TAG, "--m-- Added image URI: " + imageUri);
                }
            } else if (data.getData() != null && selectedImageUris.size() < 9) {
                Uri imageUri = data.getData();
                selectedImageUris.add(imageUri);
                Log.d(TAG, "--m-- Added single image URI: " + imageUri);
            }
            updateImagePreview();
            if (selectedImageUris.size() >= 9) {
                Log.d(TAG, "--m-- Maximum image limit reached: " + selectedImageUris.size());
                Toast.makeText(this, "Maximum 9 images allowed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateImagePreview() {
        Log.d(TAG, "--m-- Updating image preview");
        imagePreviewContainer.removeAllViews();
        for (Uri uri : selectedImageUris) {
            View imageView = LayoutInflater.from(this).inflate(R.layout.item_image_preview, imagePreviewContainer, false);
            ImageView preview = imageView.findViewById(R.id.previewImage);
            ImageButton deleteButton = imageView.findViewById(R.id.deleteImageButton);

            Glide.with(this).load(uri).into(preview);
            deleteButton.setOnClickListener(v -> {
                selectedImageUris.remove(uri);
                Log.d(TAG, "--m-- Removed image from preview: " + uri);
                updateImagePreview();
            });

            imagePreviewContainer.addView(imageView);
        }
        Log.d(TAG, "--m-- Image preview updated with " + selectedImageUris.size() + " images");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_memory);

        Log.d(TAG, "--m-- onCreate: Initializing AddEditMemoryActivity");

        dbHelper = new DatabaseHelper(this);
        currentUserId = Utility.getUserName(this);
        Log.d(TAG, "--m-- Current User ID: " + currentUserId);

        titleEditText = findViewById(R.id.titleEditText);
        memoryEditText = findViewById(R.id.memoryEditText);
        closeButton = findViewById(R.id.closeButton);
        saveButton = findViewById(R.id.saveButton);
        addPlaceInfoImage = findViewById(R.id.addPlaceInfoImage);
        deleteImage = findViewById(R.id.deleteImage);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);

        closeButton.setOnClickListener(v -> {
            Log.d(TAG, "--m-- Close button clicked");
            finish();
        });
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "--m-- Save button clicked");
            saveMemory();
        });
        addPlaceInfoImage.setOnClickListener(v -> {
            Log.d(TAG, "--m-- Add place info button clicked");
            showPlaceInfoDialog();
        });
        deleteImage.setOnClickListener(v -> {
            Log.d(TAG, "--m-- Delete image button clicked");
            deleteMemory();
        });

        addImageButton = findViewById(R.id.imageButton);
        addImageButton.setOnClickListener(v -> {
            Log.d(TAG, "--m-- Add image button clicked");
            openGallery();
        });

        if (getIntent().getBooleanExtra("IS_EDIT", false)) {
            editMemoryId = getIntent().getLongExtra("MEMORY_ID", -1);
            Log.d(TAG, "--m-- Editing existing memory with ID: " + editMemoryId);
            if (editMemoryId != -1) {
                MemoryItem memory = dbHelper.getMemory(editMemoryId);
                titleEditText.setText(memory.getTitle());
                memoryEditText.setText(memory.getMemoryText());

                // Load existing images
                List<String> existingImages = memory.getPictures();
                for (String imageUrl : existingImages) {
                    selectedImageUris.add(Uri.parse(imageUrl));
                    Log.d(TAG, "--m-- Loaded existing image: " + imageUrl);
                }
                updateImagePreview();

                deleteImage.setVisibility(View.VISIBLE);
            }
        } else {
            Log.d(TAG, "--m-- Creating new memory");
            deleteImage.setVisibility(View.GONE);
        }
    }

    private String getCurrentUserId() {
        return Utility.getUserName(this);
    }

    private void showPlaceInfoDialog() {
        Log.d(TAG, "--m-- Showing place info dialog");
        List<Place> places = dbHelper.getAllPlaces();
        if (places == null || places.isEmpty()) {
            Log.d(TAG, "--m-- No places found");
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
                    Log.d(TAG, "--m-- Place selected: " + selectedPlace.getName());
                    updateMemoryWithPlaceInfo(selectedPlace);
                });
        builder.create().show();
    }

    private void updateMemoryWithPlaceInfo(Place place) {
        Log.d(TAG, "--m-- Updating memory with place info: " + place.getName());
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

        Log.d(TAG, "--m-- Place info added: " + placeInfo);
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

        Log.d(TAG, "--m-- Saving memory. Title: " + title);

        if (title.isEmpty() || memoryText.isEmpty()) {
            Log.d(TAG, "--m-- Title or memory text is empty");
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        MemoryItem memory = new MemoryItem(editMemoryId, title, timestamp, memoryText);
        memory.setUserId(currentUserId);

        if (editMemoryId == -1) {
            long newId = dbHelper.addMemory(memory);
            Log.d(TAG, "--m-- New memory added with ID: " + newId);
            if (newId != -1) {
                if (!selectedImageUris.isEmpty()) {
                    uploadImages(newId);
                }
                Toast.makeText(this, "Memory saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Log.e(TAG, "--m-- Error saving memory");
                Toast.makeText(this, "Error saving memory", Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsAffected = dbHelper.updateMemory(memory);
            Log.d(TAG, "--m-- Memory updated. Rows affected: " + rowsAffected);
            if (rowsAffected > 0) {
                if (!selectedImageUris.isEmpty()) {
                    uploadImages(editMemoryId);
                }
                Toast.makeText(this, "Memory updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Log.e(TAG, "--m-- Error updating memory");
                Toast.makeText(this, "Error updating memory", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap rotateImageIfRequired(Bitmap bitmap, Uri imageUri) throws IOException {
        Log.d(TAG, "--m-- Rotating image if required");
        InputStream input = getContentResolver().openInputStream(imageUri);
        if (input == null) {
            return bitmap;
        }
        ExifInterface ei = new ExifInterface(input);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        input.close();

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxDimension) {
        Log.d(TAG, "--m-- Resizing bitmap");
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleFactor = Math.min((float) maxDimension / width, (float) maxDimension / height);
        return Bitmap.createScaledBitmap(bitmap, Math.round(width * scaleFactor), Math.round(height * scaleFactor), true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        Log.d(TAG, "--m-- Converting bitmap to base64");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }


    private void uploadImages(long memoryId) {
        Log.d(TAG, "--m-- Starting image upload process for memory ID: " + memoryId);
        final int[] successCount = {0};
        final int totalImages = selectedImageUris.size();
        Log.d(TAG, "--m-- Total images to upload: " + totalImages);

        for (Uri imageUri : selectedImageUris) {
            new Thread(() -> {
                try {
                    Log.d(TAG, "--m-- Processing image: " + imageUri);
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    if (inputStream == null) {
                        throw new IOException("Failed to open input stream for URI: " + imageUri);
                    }
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();

                    if (bitmap == null) {
                        throw new IOException("Failed to decode bitmap for URI: " + imageUri);
                    }

                    bitmap = rotateImageIfRequired(bitmap, imageUri);
                    bitmap = resizeBitmap(bitmap, 1024);
                    String base64Image = bitmapToBase64(bitmap);
                    Log.d(TAG, "--m-- Image processed and converted to base64");

                    Utility.uploadImage(this, base64Image, memoryId, new Utility.UploadImageCallback() {
                        @Override
                        public void onUploadComplete(String imageUrl) {
                            Log.d(TAG, "--m-- Image upload complete: " + imageUrl);
                            dbHelper.addImageToMemory(memoryId, imageUrl);
                            successCount[0]++;
                            Log.d(TAG, "--m-- Successful uploads: " + successCount[0] + "/" + totalImages);
                            checkUploadCompletion();
                        }

                        @Override
                        public void onUploadFailed(Exception e) {
                            Log.e(TAG, "--m-- Image upload failed", e);
                            checkUploadCompletion();
                        }

                        private void checkUploadCompletion() {
                            if (successCount[0] + (totalImages - successCount[0]) == totalImages) {
                                Log.d(TAG, "--m-- All image uploads completed");
                                runOnUiThread(() -> {
                                    Toast.makeText(AddEditMemoryActivity.this,
                                            successCount[0] + " out of " + totalImages + " images uploaded successfully",
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                            }
                        }
                    });
                } catch (SecurityException e) {
                    Log.e(TAG, "--m-- Security exception when processing image: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditMemoryActivity.this, "Permission denied to access image", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "--m-- Error processing image: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditMemoryActivity.this, "Error processing image", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
    }


    private void deleteMemory() {
        if (editMemoryId != -1) {
            Log.d(TAG, "--m-- Attempting to delete memory with ID: " + editMemoryId);
            new AlertDialog.Builder(this)
                    .setTitle("Delete Memory")
                    .setMessage("Are you sure you want to delete this memory?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        int rowsDeleted = dbHelper.deleteMemory(editMemoryId);
                        if (rowsDeleted > 0) {
                            Log.d(TAG, "--m-- Memory deleted successfully");
                            Toast.makeText(this, "Memory deleted successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Log.e(TAG, "--m-- Error deleting memory");
                            Toast.makeText(this, "Error deleting memory", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        Log.d(TAG, "--m-- Memory deletion cancelled");
                    })
                    .show();
        } else {
            Log.e(TAG, "--m-- Attempted to delete memory with invalid ID");
        }
    }
}
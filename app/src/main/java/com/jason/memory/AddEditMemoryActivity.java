package com.jason.memory;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddEditMemoryActivity extends AppCompatActivity {
    private EditText titleEditText;
    private TextView dateTextView;
    private EditText memoryEditText;
    private DatabaseHelper dbHelper;
    private long editMemoryId = -1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_memory);

        dbHelper = new DatabaseHelper(this);

        titleEditText = findViewById(R.id.titleEditText);
        dateTextView = findViewById(R.id.dateTextView);
        memoryEditText = findViewById(R.id.memoryEditText);

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
            }
        }

        // Set up save button
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> saveMemory());
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
}
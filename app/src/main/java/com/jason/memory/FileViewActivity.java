package com.jason.memory;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class FileViewActivity extends AppCompatActivity implements FileViewListAdapter.OnFileClickListener {
    private static final String[] FILE_TYPES = {"Activity", "Memory", "Place", "Daily"};
    private Spinner fileTypeSpinner;
    private TextView fileCountTextView;
    private RecyclerView fileListRecyclerView;
    private FileViewListAdapter fileListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_view);

        fileTypeSpinner = findViewById(R.id.fileTypeSpinner);
        fileCountTextView = findViewById(R.id.fileCountTextView);
        fileListRecyclerView = findViewById(R.id.fileListRecyclerView);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, FILE_TYPES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fileTypeSpinner.setAdapter(adapter);

        fileTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateFileList(FILE_TYPES[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        fileListAdapter = new FileViewListAdapter(this);
        fileListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        fileListRecyclerView.setAdapter(fileListAdapter);

        updateFileList(FILE_TYPES[0]);
    }

    private void updateFileList(String fileType) {
        File directory = Config.getDownloadDir();
        File[] files;
        switch (fileType) {
            case "Activity":
                files = Config.getDownloadDir().listFiles((dir, name) -> name.toLowerCase().endsWith(Config.ACTIVITY_EXT));
                break;
            case "Memory":
                files = Config.getDownloadDir4Memories().listFiles((dir, name) -> name.toLowerCase().endsWith(Config.MEMORY_EXT));
                break;
            case "Place":
                files = Config.getDownloadDir4Places().listFiles((dir, name) -> name.toLowerCase().endsWith(Config.PLACE_EXT));
                break;
            case "Daily":
                files = Config.getDownloadDir4Places().listFiles((dir, name) -> name.toLowerCase().endsWith(Config.DAILY_EXT));
                break;

            default:
                files = new File[0];
        }

        fileCountTextView.setText("Total files: " + (files != null ? files.length : 0));
        fileListAdapter.setFiles(files != null ? Arrays.asList(files) : new ArrayList<>());
    }

    @Override
    public void onFileClick(File file) {
        showFileContent(file);
    }

    private void showFileContent(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;
            boolean isFirstLine = true;
            String[] headers = null;

            if (file.getName().toLowerCase().endsWith(".csv")) {
                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        headers = line.split(",");
                        isFirstLine = false;
                    } else {
                        String[] values = line.split(",");
                        for (int i = 0; i < headers.length && i < values.length; i++) {
                            content.append(headers[i]).append(": ").append(values[i]).append("\n");
                        }
                        content.append("\n");
                    }
                }
            } else {
                // For JSON files, just read the content as is
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            reader.close();

            showContentDialog(file.getName(), content.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
    }

    private void showContentDialog(String fileName, String content) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_file_content, null);
        TextView contentTextView = dialogView.findViewById(R.id.contentTextView);
        contentTextView.setText(content);

        new AlertDialog.Builder(this)
                .setTitle(fileName)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }
}
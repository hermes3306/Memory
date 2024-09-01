package com.jason.memory;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingActivity extends AppCompatActivity {

    private static final String TAG = "SettingActivity";
    private static final String BASE_URL = "http://58.233.69.198/moment/";
    private static final String UPLOAD_DIR = "upload/";
    private static final String FILE_LIST_URL = BASE_URL + "list.php?ext=csv";

    private Button fileListButton;
    private Button fileDownloadButton;
    private ProgressBar downloadProgressBar;
    private TextView downloadStatusTextView;
    private List<String> fileList = new ArrayList<>();
    private int currentFileIndex = 0;
    private long currentDownloadId;
    private static final int MAX_FILENAME_LENGTH = 128;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Log.d(TAG, "--m-- SettingActivity onCreate");

        fileDownloadButton = findViewById(R.id.fileDownloadButton);
        fileListButton = findViewById(R.id.fileListButton);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);
        downloadStatusTextView = findViewById(R.id.downloadStatusTextView);

        fileDownloadButton.setOnClickListener(v -> {
            Log.d(TAG, "--m-- Download button clicked");
            fetchFileList();
        });

        fileListButton.setOnClickListener(v -> {
            Log.d(TAG, "--m-- List button clicked");
            listFiles();
        });

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED);
        Log.d(TAG, "--m-- BroadcastReceiver registered");
    }

    private void parseCSVFile(File file) {
        Log.d(TAG, "--m-- Starting to parse CSV file: " + file.getName());
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    Log.d(TAG, "--m-- CSV header: " + line);
                    continue;  // Skip the header line
                }
                String[] values = line.split(",");
                if (values.length == 4) {
                    double latitude = Double.parseDouble(values[0]);
                    double longitude = Double.parseDouble(values[1]);
                    String date = values[2];
                    String time = values[3];
                    Log.d(TAG, "--m-- Parsed data: " + latitude + ", " + longitude + ", " + date + ", " + time);
                    lineCount++;
                } else {
                    Log.w(TAG, "--m-- Invalid line format: " + line);
                }
            }
            Log.d(TAG, "--m-- Finished parsing CSV. Total lines processed: " + lineCount);
        } catch (IOException e) {
            Log.e(TAG, "--m-- Error parsing CSV file: " + e.getMessage());
        }
    }

    private void listFiles(){

    }

    private void fetchFileList() {
        Log.d(TAG, "--m-- Fetching file list from: " + FILE_LIST_URL);
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, FILE_LIST_URL,
                response -> {
                    Log.d(TAG, "--m-- File list response received: " + response);
                    fileList.clear();
                    String[] files = response.split("\n");
                    for (String file : files) {
                        if (!file.trim().isEmpty()) {
                            fileList.add(file.trim());
                        }
                    }
                    Log.d(TAG, "--m-- Number of files in list: " + fileList.size());
                    if (!fileList.isEmpty()) {
                        currentFileIndex = 0;
                        downloadFile(fileList.get(currentFileIndex));
                    } else {
                        Log.d(TAG, "--m-- No files to download");
                        Toast.makeText(this, "다운로드할 파일이 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "--m-- Error fetching file list: " + error.toString());
                    Toast.makeText(this, "파일 목록을 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
        );
        queue.add(stringRequest);
    }

    private void updateDownloadProgress() {
        new Thread(() -> {
            boolean downloading = true;
            while (downloading && currentFileIndex < fileList.size()) {
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(currentDownloadId);
                try (Cursor cursor = ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).query(q)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false;
                        }
                        final int dlProgress = bytesTotal > 0 ? (int) ((bytesDownloaded * 100L) / bytesTotal) : 0;
                        final int overallProgress = (int) ((currentFileIndex * 100.0 + dlProgress) / fileList.size());
                        runOnUiThread(() -> {
                            downloadProgressBar.setProgress(overallProgress);
                            if (currentFileIndex < fileList.size()) {
                                downloadStatusTextView.setText(String.format("다운로드 중: %s (%d/%d)",
                                        fileList.get(currentFileIndex), currentFileIndex + 1, fileList.size()));
                            }
                        });
                        Log.d(TAG, "--m-- Download progress: " + dlProgress + "%, Overall progress: " + overallProgress + "%");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "--m-- Error updating download progress: " + e.getMessage());
                    downloading = false;
                }

                try {
                    Thread.sleep(1000); // Update every second
                } catch (InterruptedException e) {
                    Log.e(TAG, "--m-- Download progress thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }).start();
    }

    private void downloadFile(String fileName) {
        if (fileName.length() > MAX_FILENAME_LENGTH) {
            fileName = fileName.substring(0, MAX_FILENAME_LENGTH);
        }
        String fileUrl = BASE_URL + UPLOAD_DIR + fileName;
        Log.d(TAG, "--m-- Downloading file from: " + fileUrl);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setTitle("파일 다운로드");
        request.setDescription("다운로드 중: " + fileName);

        File destinationDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Memory");
        if (!destinationDir.exists()) {
            boolean dirCreated = destinationDir.mkdirs();
            Log.d(TAG, "--m-- Destination directory created: " + dirCreated);
        }
        File destinationFile = new File(destinationDir, fileName);
        request.setDestinationUri(Uri.fromFile(destinationFile));
        Log.d(TAG, "--m-- Download destination: " + destinationFile.getPath());

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        try {
            currentDownloadId = downloadManager.enqueue(request);
            Log.d(TAG, "--m-- Download enqueued with ID: " + currentDownloadId);

            fileDownloadButton.setEnabled(false);
            downloadProgressBar.setVisibility(View.VISIBLE);
            downloadStatusTextView.setVisibility(View.VISIBLE);
            updateDownloadProgress();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "--m-- Error enqueueing download: " + e.getMessage());
            handleDownloadError();
        }
    }

    private void handleDownloadError() {
        currentFileIndex++;
        if (currentFileIndex < fileList.size()) {
            downloadFile(fileList.get(currentFileIndex));
        } else {
            runOnUiThread(() -> {
                downloadStatusTextView.setText("다운로드 완료 (일부 파일 오류)");
                fileDownloadButton.setEnabled(true);
            });
        }
    }


    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == currentDownloadId) {
                Log.d(TAG, "--m-- Download completed for file: " + fileList.get(currentFileIndex));
                File downloadedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                        "Memory/" + fileList.get(currentFileIndex));
                if (downloadedFile.exists()) {
                    Log.d(TAG, "--m-- File exists, parsing CSV: " + downloadedFile.getPath());
                    parseCSVFile(downloadedFile);
                } else {
                    Log.e(TAG, "--m-- Downloaded file does not exist: " + downloadedFile.getPath());
                }

                currentFileIndex++;
                if (currentFileIndex < fileList.size()) {
                    Log.d(TAG, "--m-- Moving to next file: " + fileList.get(currentFileIndex));
                    downloadFile(fileList.get(currentFileIndex));
                } else {
                    Log.d(TAG, "--m-- All files downloaded");
                    runOnUiThread(() -> {
                        downloadStatusTextView.setText("모든 파일 다운로드 완료");
                        fileDownloadButton.setEnabled(true);
                    });
                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(downloadReceiver);
        Log.d(TAG, "--m-- SettingActivity onDestroy");
    }
}
package com.jason.memory;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingActivity extends AppCompatActivity {



    private Switch switchKeepScreenOn;

    private static final String TAG = "SettingActivity";
    private static final String BASE_URL = Config.BASE_URL;
    private static final String UPLOAD_DIR = "upload/";
    private static final String FILE_LIST_URL = BASE_URL + "listM.php?ext=csv";

    private Button fileDownloadButton;
    private Button fileUploadButton;
    private Button listFilesButton;
    private Button deleteFilesButton;
    private ProgressBar progressBar;
    private Button listServerFilesButton;
    private Button listActivitiesButton;
    private DatabaseHelper dbHelper;

    private List<String> fileList = new ArrayList<>();
    private List<Long> downloadIds = new ArrayList<>();
    private int totalFiles = 0;
    private int processedFiles = 0;

    private TextView statusTextView;
    private Button initActivityButton;
    private Button migrateButton;
    private boolean isInitialized = false;
    private Button fileViewButton;

    private RadioGroup runTypeRadioGroup;
    private RadioButton memoryRadioButton;
    private RadioButton momentRadioButton;

    private CheckBox checkBoxServer;
    private CheckBox checkBoxStrava;


    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            handleDownloadComplete(id);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Log.d(TAG, "--m-- SettingActivity onCreate");

        dbHelper = new DatabaseHelper(this);
        initializeViews();

        setClickListeners();
        registerDownloadReceiver();
    }

    private void initializeViews() {
        fileDownloadButton = findViewById(R.id.fileDownloadButton);
        fileUploadButton = findViewById(R.id.fileUploadButton);
        listFilesButton = findViewById(R.id.listFilesButton);
        deleteFilesButton = findViewById(R.id.deleteFilesButton);
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);
        listServerFilesButton = findViewById(R.id.listServerFilesButton);
        listActivitiesButton = findViewById(R.id.listActivitiesButton);
        initActivityButton = findViewById(R.id.initActivityButton);
        migrateButton = findViewById(R.id.migrateButton);
        switchKeepScreenOn = findViewById(R.id.switchKeepScreenOn);

        runTypeRadioGroup = findViewById(R.id.idnew_runTypeRadioGroup);
        memoryRadioButton = findViewById(R.id.idnew_memoryRadioButton);
        momentRadioButton = findViewById(R.id.idnew_momentRadioButton);

        checkBoxServer = findViewById(R.id.idnew_save_server);
        checkBoxStrava = findViewById(R.id.idnew_save_Strava);
        fileViewButton = findViewById(R.id.fileViewButton);


    }

    private void setClickListeners() {
        fileDownloadButton.setOnClickListener(v -> fetchFileList());
        fileUploadButton.setOnClickListener(v -> uploadFiles());
        listFilesButton.setOnClickListener(v -> listFiles());
        deleteFilesButton.setOnClickListener(v -> deleteFiles());
        listServerFilesButton.setOnClickListener(v -> listServerFiles());
        listActivitiesButton.setOnClickListener(v -> listActivities());
        initActivityButton.setOnClickListener(v -> initializeActivities());
        migrateButton.setOnClickListener(v -> migrateFiles());

        SharedPreferences prefs = getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        boolean keepScreenOn = prefs.getBoolean(Config.PREF_KEEP_SCREEN_ON, false);
        switchKeepScreenOn.setChecked(keepScreenOn);
        switchKeepScreenOn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(Config.PREF_KEEP_SCREEN_ON, isChecked);
            editor.apply();

            if (isChecked) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });

        String savedRunType = prefs.getString(Config.PREF_RUN_TYPE, Config.RUN_TYPE_MEMORY);

        if (savedRunType.equals(Config.RUN_TYPE_MEMORY)) {
            memoryRadioButton.setChecked(true);
        } else {
            momentRadioButton.setChecked(true);
        }

        runTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = prefs.edit();
            if (checkedId == R.id.idnew_memoryRadioButton) {
                editor.putString(Config.PREF_RUN_TYPE, Config.RUN_TYPE_MEMORY);
            } else if (checkedId == R.id.idnew_momentRadioButton) {
                editor.putString(Config.PREF_RUN_TYPE, Config.RUN_TYPE_MOMENT);
            }
            editor.apply();

            String selectedType = (checkedId == R.id.idnew_memoryRadioButton) ? "Memory" : "Moment";
            Toast.makeText(this, "Run type set to: " + selectedType, Toast.LENGTH_SHORT).show();
        });

        fileViewButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingActivity.this, FileViewActivity.class);
            startActivity(intent);
        });

        checkBoxServer.setChecked(prefs.getBoolean(Config.PREF_UPLOAD_SERVER, false));
        checkBoxStrava.setChecked(prefs.getBoolean(Config.PREF_UPLOAD_STRAVA, false));

        checkBoxServer.setOnCheckedChangeListener((buttonView, isChecked) -> savePreferences());
        checkBoxStrava.setOnCheckedChangeListener((buttonView, isChecked) -> savePreferences());
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Config.PREF_UPLOAD_SERVER, checkBoxServer.isChecked());
        editor.putBoolean(Config.PREF_UPLOAD_STRAVA, checkBoxStrava.isChecked());
        editor.apply();
    }

    private void listServerFiles() {
        new Thread(() -> {
            try {
                URL url = new URL(FILE_LIST_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                    reader.close();

                    String[] files = response.toString().split("\n");
                    runOnUiThread(() -> showFileListDialog(files));
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch file list", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void initializeActivities() {
        new AlertDialog.Builder(this)
                .setTitle("Initialize Activities")
                .setMessage("This will delete all existing activities. Are you sure?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dbHelper.deleteAllActivities();
                    isInitialized = true;
                    Toast.makeText(this, "All activities deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }


    private void migrateFiles() {
        // Disable buttons
        initActivityButton.setEnabled(false);
        listActivitiesButton.setEnabled(false);

        // Stop location service
        Intent stopServiceIntent = new Intent(this, LocationService.class);
        stopServiceIntent.setAction(LocationService.ACTION_STOP_SERVICE);
        startService(stopServiceIntent);

        File appDir = Config.getDownloadDir();
        File[] files = appDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (files == null || files.length == 0) {
            Toast.makeText(this, "No files to migrate", Toast.LENGTH_SHORT).show();
            startLocationService();
            // Re-enable buttons
            initActivityButton.setEnabled(true);
            listActivitiesButton.setEnabled(true);
            return;
        }

        progressBar.setMax(files.length);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText("Migrating 0/" + files.length + " files");

        new Thread(() -> {
            int processedFiles = 0;
            int successfulMigrations = 0;
            int skippedFiles = 0;

            for (File file : files) {
                String activityName = file.getName().replace(".csv", "");
                if (dbHelper.isActivityExist(activityName)) {
                    skippedFiles++;
                    processedFiles++;
                    int finalProcessedFiles = processedFiles;
                    int finalSkippedFiles = skippedFiles;
                    runOnUiThread(() -> {
                        progressBar.setProgress(finalProcessedFiles);
                        statusTextView.setText("Migrating " + finalProcessedFiles + "/" + files.length +
                                " files: " + file.getName() + " (Skipped: already exists)");
                    });
                    continue;
                }

                boolean success = Utility.SaveActivityToDB(this, file, dbHelper);
                if (success) {
                    successfulMigrations++;
                }
                processedFiles++;
                int finalProcessedFiles = processedFiles;
                int finalSuccessfulMigrations = successfulMigrations;
                int finalSkippedFiles = skippedFiles;
                runOnUiThread(() -> {
                    progressBar.setProgress(finalProcessedFiles);
                    statusTextView.setText("Migrating " + finalProcessedFiles + "/" + files.length +
                            " files: " + file.getName() +
                            " (OK: " + finalSuccessfulMigrations + ", Skipped: " + finalSkippedFiles + ")");
                });
            }
            int finalSuccessfulMigrations = successfulMigrations;
            int finalSkippedFiles = skippedFiles;
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                statusTextView.setText("Migration completed. " + finalSuccessfulMigrations +
                        " out of " + files.length + " files migrated successfully. " +
                        finalSkippedFiles + " files skipped.");
                Toast.makeText(this, "Migration completed", Toast.LENGTH_SHORT).show();
                startLocationService();

                // Re-enable buttons
                initActivityButton.setEnabled(true);
                listActivitiesButton.setEnabled(true);
            });
        }).start();
    }

    private void startLocationService() {
        Intent startServiceIntent = new Intent(this, LocationService.class);
        startServiceIntent.setAction(LocationService.ACTION_START_SERVICE);
        startService(startServiceIntent);
    }

    private void showFileListDialog(String[] files) {
        StringBuilder fileList = new StringBuilder();
        for (String file : files) {
            fileList.append(file).append("\n");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Server Files")
                .setMessage(String.format("Total files: %d\n\nFiles:\n%s", files.length, fileList.toString()))
                .setPositiveButton("OK", null)
                .show();
    }

    private void listActivities() {
        List<ActivityData> activities = dbHelper.getAllActivities();
        if (activities.isEmpty()) {
            Toast.makeText(this, "No activities found", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder activityList = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss", Locale.getDefault());

        for (ActivityData activity : activities) {
            String activityString = String.format("%s",
                    activity.getName());
            activityList.append(activityString).append("\n");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Activities")
                .setMessage(String.format("Total activities: %d\n\nActivities:\n%s", activities.size(), activityList.toString()))
                .setPositiveButton("OK", null)
                .show();
    }

    private void registerDownloadReceiver() {
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, filter);
        }
        Log.d(TAG, "--m-- BroadcastReceiver registered");
    }

    private void handleFetchError(String errorMessage) {
        Log.e(TAG, "--m-- " + errorMessage);
        runOnUiThread(() -> Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show());
    }

    private void handleDownloadComplete(long id) {
        Log.d(TAG, "--m-- Download completed, id: " + id);
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);
        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                String fileName = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    Log.d(TAG, "--m-- Download successful: " + fileName);
                    processedFiles++;
                    updateProgressUI(fileName);
                } else if (status == DownloadManager.STATUS_FAILED) {
                    int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                    Log.e(TAG, "--m-- Download failed: " + fileName + ", reason: " + reason);
                }
            }
        }
    }

    private void updateProgressUI(String fileName) {
        runOnUiThread(() -> {
            statusTextView.setText("Downloading " + processedFiles + "/" + totalFiles + " files: " + fileName);
            progressBar.setProgress(processedFiles);
            if (processedFiles == totalFiles) {
                Toast.makeText(this, "All files downloaded", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void uploadFiles() {
        File appDir = Config.getDownloadDir();
        File[] files = appDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (files != null && files.length > 0) {
            totalFiles = files.length;
            processedFiles = 0;
            runOnUiThread(() -> {
                statusTextView.setText("Uploading 0/" + totalFiles + " files");
                progressBar.setMax(totalFiles);
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
                statusTextView.setText(""); // Reset the upload status
            });

            for (File file : files) {
                new Thread(() -> uploadFile(file)).start();
            }
        } else {
            Toast.makeText(this, "No files to upload", Toast.LENGTH_SHORT).show();
        }
    }


    private void uploadFile(File file) {
        String serverUrl = BASE_URL + "upload.php"; // Adjust this to your server's upload endpoint
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=*****");

            try (OutputStream os = connection.getOutputStream();
                 FileInputStream fileInputStream = new FileInputStream(file)) {

                String boundary = "*****";
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                String fileParameterName = "uploadedFile"; // Adjust this to match your server's expected parameter name

                // Write file metadata
                os.write((twoHyphens + boundary + lineEnd).getBytes());
                os.write(("Content-Disposition: form-data; name=\"" + fileParameterName + "\";filename=\"" + file.getName() + "\"" + lineEnd).getBytes());
                os.write(lineEnd.getBytes());

                // Write file content
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                os.write(lineEnd.getBytes());
                os.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "--m-- File uploaded successfully: " + file.getName());
                processedFiles++;
                updateUploadProgressUI(file.getName(), true);
            } else {
                Log.e(TAG, "--m-- File upload failed: " + file.getName() + ", response code: " + responseCode);
                updateUploadProgressUI(file.getName(), false);
            }
        } catch (Exception e) {
            Log.e(TAG, "--m-- Error uploading file: " + file.getName(), e);
            updateUploadProgressUI(file.getName(), false);
        }
    }

    private void updateUploadProgressUI(String fileName, boolean success) {
        runOnUiThread(() -> {
            statusTextView.setText("Uploading " + processedFiles + "/" + totalFiles + " files");
            progressBar.setProgress(processedFiles);

            String status = fileName + " upload " + (success ? "success" : "failed");
            if(!success) statusTextView.setText(status);

            if (processedFiles == totalFiles) {
                Toast.makeText(this, "All files uploaded", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void listFiles() {
        File appDir = Config.getDownloadDir();
        File[] files = appDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (files != null && files.length > 0) {
            StringBuilder fileList = new StringBuilder();
            for (File file : files) {
                fileList.append(file.getName()).append("\n");
            }

            new AlertDialog.Builder(this)
                    .setTitle("Downloaded Files")
                    .setMessage(String.format("Total files: %d\n\nFiles:\n%s", files.length, fileList.toString()))
                    .setPositiveButton("OK", null)
                    .show();

            Log.d(TAG, "--m-- Files listed: " + files.length);
        } else {
            Toast.makeText(this, "No files found", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "--m-- No files found");
        }
    }

    private void deleteFiles() {
        File appDir = Config.getDownloadDir();
        File[] files = appDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (files != null && files.length > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Files")
                    .setMessage("Are you sure you want to delete all CSV files?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        int deletedCount = 0;
                        for (File file : files) {
                            if (file.delete()) {
                                deletedCount++;
                            }
                        }
                        Toast.makeText(this, deletedCount + " files deleted", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "--m-- Files deleted: " + deletedCount);
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else {
            Toast.makeText(this, "No files to delete", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "--m-- No files to delete");
        }
    }

    private void fetchFileList() {
        Log.d(TAG, "--m-- Fetching file list from: " + FILE_LIST_URL);
        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(FILE_LIST_URL).openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    processFileList(connection);
                } else {
                    handleFetchError("Error fetching file list, response code: " + responseCode);
                }
            } catch (Exception e) {
                handleFetchError("Error fetching file list: " + e.getMessage());
            }
        }).start();
    }

    private void processFileList(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
        }

        Log.d(TAG, "--m-- File list received, response: " + response.toString());
        fileList.clear();
        String[] files = response.toString().split("\n");
        for (String file : files) {
            if (!file.trim().isEmpty()) {
                fileList.add(file.trim());
                Log.d(TAG, "--m-- Added file to list: " + file.trim());
            }
        }

        if (!fileList.isEmpty()) {
            totalFiles = fileList.size();
            runOnUiThread(() -> {
                statusTextView.setText("Downloading 0/" + totalFiles + " files");
                progressBar.setMax(totalFiles);
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
            });
            downloadAllFiles();
        } else {
            runOnUiThread(() -> Toast.makeText(this, "No files to download", Toast.LENGTH_SHORT).show());
        }
    }

    private void downloadAllFiles() {
        processedFiles = 0;
        for (String fileName : fileList) {
            new Thread(() -> downloadFile(fileName)).start();
        }
    }

    private void downloadFile(String fileName) {
        String fileUrl = BASE_URL + UPLOAD_DIR + fileName;
        Log.d(TAG, "--m-- Downloading file from: " + fileUrl);


        File appDir = Config.getDownloadDir();

        if (!appDir.exists()) {
            boolean created = appDir.mkdirs();
            Log.d(TAG, "--m-- App directory created: " + created);
        }
        File destinationFile = new File(appDir, fileName);

        // Check if file already exists
        if (destinationFile.exists()) {
            Log.d(TAG, "--m-- File already exists: " + fileName);
            processedFiles++;
            updateDownloadProgressUI(fileName, true, true);
            return;
        }

        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                updateDownloadProgressUI(fileName, false, false);
                return;
            }

            try (java.io.InputStream input = connection.getInputStream();
                 java.io.OutputStream output = new java.io.FileOutputStream(destinationFile)) {

                byte[] data = new byte[4096];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }
            }

            Log.d(TAG, "--m-- File downloaded successfully: " + fileName);
            processedFiles++;
            updateDownloadProgressUI(fileName, true, false);
        } catch (Exception e) {
            Log.e(TAG, "--m-- Error downloading file: " + fileName, e);
            updateDownloadProgressUI(fileName, false, false);
        }
    }

    private void updateDownloadProgressUI(String fileName, boolean success, boolean alreadyExists) {
        runOnUiThread(() -> {
            statusTextView.setText("Downloading " + processedFiles + "/" + totalFiles + " files");
            progressBar.setProgress(processedFiles);

            String status;
            if (alreadyExists) {
                status = fileName + " exists already!";
                statusTextView.setText(status);
            } else if (!success) {
                status = fileName + " download failed";
                statusTextView.setText(status);
            }

            if (processedFiles == totalFiles) {
                Toast.makeText(this, "All files processed", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(downloadReceiver);
        Log.d(TAG, "--m-- SettingActivity onDestroy, BroadcastReceiver unregistered");
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void finish() {
        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);
        super.finish();
    }
}
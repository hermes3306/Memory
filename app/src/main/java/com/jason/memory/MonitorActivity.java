package com.jason.memory;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import android.os.Build;
import android.app.AlertDialog;
import android.widget.Toast;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MonitorActivity extends AppCompatActivity {
    private TextView locationCountTextView;
    private RecyclerView locationRecyclerView;
    private LocationAdapter locationAdapter;
    private List<LocationData> locationList = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private BroadcastReceiver locationUpdateReceiver;

    private static final String BACKUP_DIRECTORY = "MemoryBackup";
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyMMdd", Locale.getDefault());
    private static final SimpleDateFormat CSV_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());

    private static final int PAGE_SIZE = 100;
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    private Button btnBackup, btnRestore, btnInitialize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        locationCountTextView = findViewById(R.id.locationCountTextView);
        locationRecyclerView = findViewById(R.id.locationRecyclerView);

        dbHelper = new DatabaseHelper(this);

        locationAdapter = new LocationAdapter(locationList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        locationRecyclerView.setLayoutManager(layoutManager);
        locationRecyclerView.setAdapter(locationAdapter);

        btnBackup = findViewById(R.id.btnBackup);
        btnRestore = findViewById(R.id.btnRestore);
        btnInitialize = findViewById(R.id.btnInitialize);

        btnBackup.setOnClickListener(v -> showBackupDialog());
        btnRestore.setOnClickListener(v -> showRestoreDialog());
        btnInitialize.setOnClickListener(v -> showInitializeDialog());

        updateLocationCount();
        loadLocations();

        // Register receiver for location updates
        locationUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadLatestLocation();
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationUpdateReceiver,
                    new IntentFilter(LocationService.ACTION_LOCATION_UPDATED),
                    Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(locationUpdateReceiver,
                    new IntentFilter(LocationService.ACTION_LOCATION_UPDATED));
        }

        setupRecyclerView();
        loadMoreLocations();
    }

    private void setupRecyclerView() {
        locationAdapter = new LocationAdapter(locationList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        locationRecyclerView.setLayoutManager(layoutManager);
        locationRecyclerView.setAdapter(locationAdapter);

        locationRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!isLoading && hasMoreData) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadMoreLocations();
                    }
                }
            }
        });
    }

    private void loadMoreLocations() {
        if (isLoading) return;
        isLoading = true;

        new Thread(() -> {
            List<LocationData> newLocations = dbHelper.getLocationsPage(currentPage, PAGE_SIZE);
            runOnUiThread(() -> {
                if (newLocations.size() > 0) {
                    int startPosition = locationList.size();
                    locationList.addAll(newLocations);
                    locationAdapter.notifyItemRangeInserted(startPosition, newLocations.size());
                    currentPage++;
                } else {
                    hasMoreData = false;
                }
                isLoading = false;
                updateLocationCount();
            });
        }).start();
    }

    private void loadLatestLocation() {
        new Thread(() -> {
            LocationData latestLocation = dbHelper.getLatestLocation();
            if (latestLocation != null) {
                runOnUiThread(() -> {
                    locationList.add(0, latestLocation);
                    locationAdapter.notifyItemInserted(0);
                    locationRecyclerView.smoothScrollToPosition(0);
                    updateLocationCount();

                    View firstItem = locationRecyclerView.getLayoutManager().findViewByPosition(0);
                    if (firstItem != null) {
                        firstItem.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_animation_fall_down));
                    }
                });
            }
        }).start();
    }

    private void showBackupDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Backup")
                .setMessage("Choose backup type:")
                .setPositiveButton("Today's Data", (dialog, which) -> backupData(true))
                .setNegativeButton("All Data", (dialog, which) -> backupData(false))
                .show();
    }

    private void showInitializeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Initialize")
                .setMessage("Initialize all data?")
                .setPositiveButton("No", (dialog, which) -> initializeData(false))
                .setNegativeButton("Yes", (dialog, which) -> initializeData(true))
                .show();
    }

    private void showRestoreDialog() {
        File backupDir = new File(getExternalFilesDir(null), BACKUP_DIRECTORY);
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.endsWith(".csv"));

        if (backupFiles == null || backupFiles.length == 0) {
            Toast.makeText(this, "No backup files found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] fileNames = new String[backupFiles.length];
        for (int i = 0; i < backupFiles.length; i++) {
            fileNames[i] = backupFiles[i].getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Restore")
                .setItems(fileNames, (dialog, which) -> restoreData(backupFiles[which]))
                .show();
    }

    private void initializeData(boolean init) {
        if (init) {
            try {
                dbHelper.clearAllData();
                Toast.makeText(this, "Database cleared successfully", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to clear database: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                loadLocations();
                updateLocationCount();
            }
        }
    }

    private void backupData(boolean todayOnly) {
        try {
            File backupDir = new File(getExternalFilesDir(null), BACKUP_DIRECTORY);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            String fileName = "memory_backup_" + FILE_DATE_FORMAT.format(new Date()) + ".csv";
            File backupFile = new File(backupDir, fileName);

            FileWriter fw = new FileWriter(backupFile);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write("lat,lon,alt,d,t\n");

            List<LocationData> locations;
            if (todayOnly) {
                long startOfDay = getStartOfDay();
                locations = dbHelper.getLocationDataForDateRange(startOfDay, System.currentTimeMillis());
            } else {
                locations = dbHelper.getAllLocations();
            }

            for (LocationData location : locations) {
                String line = String.format(Locale.US, "%.8f,%.8f,%.1f,%s\n",
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getAltitude(),
                        CSV_DATE_FORMAT.format(new Date(location.getTimestamp())));
                bw.write(line);
            }

            bw.close();
            fw.close();

            Toast.makeText(this, "Backup completed: " + fileName, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void restoreData(File backupFile) {


        SQLiteDatabase db = null;
        BufferedReader br = null;
        try {
            db = dbHelper.getWritableDatabase();


            br = new BufferedReader(new FileReader(backupFile));
            String line;
            br.readLine(); // Skip header

            db.beginTransaction();
            try {
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(",");
                    if (data.length == 5) {
                        double lat = Double.parseDouble(data[0]);
                        double lon = Double.parseDouble(data[1]);
                        double alt = Double.parseDouble(data[2]);
                        long timestamp = CSV_DATE_FORMAT.parse(data[3] + "," + data[4]).getTime();

                        ContentValues values = new ContentValues();
                        values.put(DatabaseHelper.COLUMN_LATITUDE, lat);
                        values.put(DatabaseHelper.COLUMN_LONGITUDE, lon);
                        values.put(DatabaseHelper.COLUMN_ALTITUDE, alt);
                        values.put(DatabaseHelper.COLUMN_TIMESTAMP, timestamp);

                        db.insert(DatabaseHelper.TABLE_LOCATIONS, null, values);
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            Toast.makeText(this, "Restore completed", Toast.LENGTH_SHORT).show();
            loadLocations();
            updateLocationCount();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private long getStartOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationUpdateReceiver != null) {
            unregisterReceiver(locationUpdateReceiver);
        }
    }

    private void updateLocationCount() {
        int count = dbHelper.getLocationCount();
        locationCountTextView.setText("Total Locations: " + count);
        locationCountTextView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
    }

    private void loadLocations() {
        locationList.clear();
        locationList.addAll(dbHelper.getAllLocationsDesc());
        locationAdapter.notifyDataSetChanged();
        locationRecyclerView.scrollToPosition(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_monitor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_show_map) {
            Intent mapIntent = new Intent(this, MapActivity.class);
            startActivity(mapIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void backupData() {
        // 백업 로직 구현
        // 예: 데이터베이스를 파일로 내보내기
        Toast.makeText(this, "Backup started", Toast.LENGTH_SHORT).show();
        // TODO: 실제 백업 로직 구현
    }

    private void restoreData() {
        // 복원 로직 구현
        // 예: 백업 파일에서 데이터베이스 복원
        Toast.makeText(this, "Restore started", Toast.LENGTH_SHORT).show();
        // TODO: 실제 복원 로직 구현
    }


}
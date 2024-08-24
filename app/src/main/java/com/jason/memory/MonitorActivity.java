package com.jason.memory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import android.os.Build;

public class MonitorActivity extends AppCompatActivity {
    private TextView locationCountTextView;
    private RecyclerView locationRecyclerView;
    private LocationAdapter locationAdapter;
    private List<LocationData> locationList = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private BroadcastReceiver locationUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        locationCountTextView = findViewById(R.id.locationCountTextView);
        locationRecyclerView = findViewById(R.id.locationRecyclerView);

        dbHelper = new DatabaseHelper(this);

        locationAdapter = new LocationAdapter(locationList);
        locationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        locationRecyclerView.setAdapter(locationAdapter);

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
        }    }

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
    }

    private void loadLocations() {
        locationList.clear();
        locationList.addAll(dbHelper.getAllLocations());
        locationAdapter.notifyDataSetChanged();
        locationRecyclerView.scrollToPosition(locationList.size() - 1);
    }

    private void loadLatestLocation() {
        LocationData latestLocation = dbHelper.getLatestLocation();
        if (latestLocation != null) {
            locationList.add(0, latestLocation); // Add to the beginning of the list
            locationAdapter.notifyItemInserted(0);
            locationRecyclerView.scrollToPosition(0);
            updateLocationCount();
        }
    }

}
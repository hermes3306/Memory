package com.jason.memory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
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
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        locationRecyclerView.setLayoutManager(layoutManager);
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
        }
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



    private void loadLatestLocation() {
        LocationData latestLocation = dbHelper.getLatestLocation();
        if (latestLocation != null) {
            locationList.add(0, latestLocation);
            locationAdapter.notifyItemInserted(0);
            locationRecyclerView.smoothScrollToPosition(0);
            updateLocationCount();

            // Apply animation to the new item
            locationRecyclerView.getLayoutManager().findViewByPosition(0)
                    .startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_animation_fall_down));
        }
    }

}
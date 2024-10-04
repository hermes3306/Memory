package com.jason.memory;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ListActivityActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ActivityAdapter adapter;
    private List<ActivityData> activityList;
    private DatabaseHelper dbHelper;
    private EditText searchEditText;
    private static final String TAG = "ListActivityActivity";
    private BroadcastReceiver serviceStatusReceiver;
    private static final int PERMISSIONS_REQUEST_LOCATION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_activity);

        if (getIntent().getBooleanExtra("START_FROM_BOOT", false)) {
            startServiceIfPermissionsGranted();
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DatabaseHelper(this);
        activityList = dbHelper.getAllActivities();
        loadActivitiesFromDatabase();

        adapter = new ActivityAdapter(activityList, activity -> {
            Intent intent = new Intent(ListActivityActivity.this, ActivityDetailActivity.class);
            intent.putExtra("ACTIVITY_ID", activity.getId());
            startActivity(intent);
        }, dbHelper);

        recyclerView.setAdapter(adapter);


        setupAddButton();
        searchEditText = findViewById(R.id.searchEditText);
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                performSearch();
                return true;
            }
            return false;
        });

        // Start the service when the app starts
        startServiceIfPermissionsGranted();

        // Register BroadcastReceiver
        IntentFilter filter = new IntentFilter(LocationService.ACTION_SERVICE_STATUS);
        serviceStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Handle service status updates if needed
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(serviceStatusReceiver, filter);
        }
        setupBottomNavigation();

    }


    private void setupBottomNavigation() {
        // Find layout views
        View chatLayout = findViewById(R.id.chatLayout);
        View runLayout = findViewById(R.id.runLayout);
        View memoryLayout = findViewById(R.id.memoryLayout);
        View placeLayout = findViewById(R.id.placeLayout);
        View meLayout = findViewById(R.id.meLayout);

        // Find icon views
        ImageView chatIcon = findViewById(R.id.iconChat);
        ImageView runIcon = findViewById(R.id.iconRun);
        ImageView memoryIcon = findViewById(R.id.iconMemory);
        ImageView placeIcon = findViewById(R.id.iconPlace);
        ImageView meIcon = findViewById(R.id.iconMe);

        // Set default icon colors
        chatIcon.setImageResource(R.drawable.ht_chat);
        runIcon.setImageResource(R.drawable.ht_run_blue);
        memoryIcon.setImageResource(R.drawable.ht_memory);
        placeIcon.setImageResource(R.drawable.ht_place);
        meIcon.setImageResource(R.drawable.ht_my);

        // Add click listeners for bottom navigation layouts
        chatLayout.setOnClickListener(v -> openChatActivity());
        //runLayout.setOnClickListener(v -> openListActivityActivity());
        memoryLayout.setOnClickListener(v -> openMemoryActivity());
        placeLayout.setOnClickListener(v -> openPlacesActivity());
        meLayout.setOnClickListener(v -> openSettingActivity());
    }



    private void startServiceIfPermissionsGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationService();
        } else {
            checkPermissionsAndStartService();
        }
    }

    private void checkPermissionsAndStartService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
        } else {
            startLocationService();
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_START_SERVICE);
        startService(serviceIntent);
    }

    private void setupAddButton() {
        ImageButton addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> startMyActivity());
    }

    private void startMyActivity() {
        ActivityData unfinishedActivity = dbHelper.getUnfinishedActivity();

        if (unfinishedActivity != null) {
            showUnfinishedActivityDialog(unfinishedActivity);
        } else {
            startNewActivity();
        }
    }

    private void showUnfinishedActivityDialog(final ActivityData activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Unfinished Activity");
        builder.setMessage("You have an unfinished activity. Do you want to resume it?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            Intent intent = new Intent(ListActivityActivity.this, MyActivity.class);
            intent.putExtra("ACTIVITY_ID", activity.getId());
            startActivity(intent);
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            dbHelper.deleteActivity(activity.getId());
            startNewActivity();
        });
        builder.show();
    }

    private void startNewActivity() {
        Intent intent = new Intent(ListActivityActivity.this, MyActivity.class);
        startActivity(intent);
    }

    private void performSearch() {
        String searchText = searchEditText.getText().toString().trim();
        Log.d(TAG, "Performing search with text: " + searchText);
        if (!searchText.isEmpty()) {
            List<ActivityData> searchResults = searchActivities(searchText);
            Log.d(TAG, "Search results count: " + searchResults.size());
            adapter.updateActivities(searchResults);
            Toast.makeText(this, searchResults.size() + " activities found", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Search text is empty, showing all activities");
            loadActivitiesFromDatabase();
        }
    }

    private List<ActivityData> searchActivities(String searchText) {
        List<ActivityData> searchResults = new ArrayList<>();
        for (ActivityData activity : activityList) {
            if (activity.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                    activity.getType().toLowerCase().contains(searchText.toLowerCase()) ||
                    activity.getAddress().toLowerCase().contains(searchText.toLowerCase())) {
                searchResults.add(activity);
            }
        }
        return searchResults;
    }


    private void loadActivitiesFromDatabase() {
        showProgressBar(true);
        new Thread(() -> {
            activityList = dbHelper.getAllActivities();
            runOnUiThread(() -> {
                if (activityList.isEmpty()) {
                    showNoActivitiesMessage();
                } else {
                    sortActivityList();
                }
                updateUIWithActivities();
                showProgressBar(false);
            });
        }).start();
    }

    private void showNoActivitiesMessage() {
        View noActivitiesView = findViewById(R.id.noActivitiesMessage);
        if (noActivitiesView != null) {
            noActivitiesView.setVisibility(View.VISIBLE);
        } else {
            Log.w(TAG, "No view found for displaying 'No Activities' message");
        }
        recyclerView.setVisibility(View.GONE);
    }

    private void updateUIWithActivities() {
        if (activityList.isEmpty()) {
            showNoActivitiesMessage();
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            if (findViewById(R.id.noActivitiesMessage) != null) {
                findViewById(R.id.noActivitiesMessage).setVisibility(View.GONE);
            }
            if (adapter == null) {
                adapter = new ActivityAdapter(activityList, this::onItemClick, dbHelper);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateActivities(activityList);
            }
        }
    }


    private void onItemClick(ActivityData activity) {
        Intent intent = new Intent(ListActivityActivity.this, ActivityDetailActivity.class);
        intent.putExtra("ACTIVITY_ID", activity.getId());
        startActivity(intent);
    }

    private void openChatActivity() {
        Intent intent = new Intent(this, ChatActivity.class);
        startActivity(intent);
    }

    private void openMemoryActivity() {
        Intent intent = new Intent(this, MemoryActivity.class);
        startActivity(intent);
    }

    private void openPlacesActivity() {
        Intent intent = new Intent(this, PlacesActivity.class);
        startActivity(intent);
    }

    private void openSettingActivity() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }


    interface OnItemClickListener {
        void onItemClick(ActivityData activity);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (recyclerView != null) {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
                if (holder instanceof ActivityAdapter.ViewHolder) {
                    ((ActivityAdapter.ViewHolder) holder).onResume();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        if (recyclerView != null) {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
                if (holder instanceof ActivityAdapter.ViewHolder) {
                    ((ActivityAdapter.ViewHolder) holder).onPause();
                }
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (recyclerView != null) {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
                if (holder instanceof ActivityAdapter.ViewHolder) {
                    ((ActivityAdapter.ViewHolder) holder).onDestroy();
                }
            }
        }
        super.onDestroy();
        if (serviceStatusReceiver != null) {
            unregisterReceiver(serviceStatusReceiver);
        }
    }

    private class ActivityTimestampComparator implements Comparator<ActivityData> {
        private boolean ascending = false; // Default to descending order

        public ActivityTimestampComparator() {}

        public ActivityTimestampComparator(boolean ascending) {
            this.ascending = ascending;
        }

        @Override
        public int compare(ActivityData a1, ActivityData a2) {
            return ascending
                    ? Long.compare(a1.getStartTimestamp(), a2.getStartTimestamp())
                    : Long.compare(a2.getStartTimestamp(), a1.getStartTimestamp());
        }
    }

    private void sortActivityList() {
        Collections.sort(activityList, new ActivityTimestampComparator());
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    private void showProgressBar(boolean show) {
        runOnUiThread(() -> {
            findViewById(R.id.progressBar).setVisibility(show ? View.VISIBLE : View.GONE);
        });
    }

    private void showNoFilesMessage() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("No Activities Found")
                .setMessage("There are no activities in the database.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
        Log.d(TAG, "--m-- showNoFilesMessage: Displayed no activities message");
    }

}
package com.jason.memory;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ListActivityActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ActivityAdapter adapter;
    private List<ActivityData> activityList;
    private DatabaseHelper dbHelper;
    private static final String TAG = "ListActivityActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_activity);

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

    private void loadActivitiesFromDatabase() {
        showProgressBar(true);
        new Thread(() -> {
            // Load activities from database
            activityList = dbHelper.getAllActivities();

            runOnUiThread(() -> {
                if (activityList.isEmpty()) {
                    showNoFilesMessage();
                } else {
                    sortActivityList();
                    adapter = new ActivityAdapter(activityList, activity -> {
                        Intent intent = new Intent(ListActivityActivity.this, ActivityDetailActivity.class);
                        intent.putExtra("ACTIVITY_ID", activity.getId());
                        startActivity(intent);
                    }, dbHelper);
                    recyclerView.setAdapter(adapter);
                }
                showProgressBar(false);
            });
        }).start();
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
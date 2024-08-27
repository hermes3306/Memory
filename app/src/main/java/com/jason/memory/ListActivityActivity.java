package com.jason.memory;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ListActivityActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ActivityAdapter adapter;
    private List<ActivityData> activityList;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_activity);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DatabaseHelper(this);
        activityList = dbHelper.getAllActivities();

        adapter = new ActivityAdapter(activityList, activity -> {
            Intent intent = new Intent(ListActivityActivity.this, ActivityDetailActivity.class);
            intent.putExtra("ACTIVITY_ID", activity.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    private class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
        private List<ActivityData> activities;
        private OnItemClickListener listener;

        public ActivityAdapter(List<ActivityData> activities, OnItemClickListener listener) {
            this.activities = activities;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ActivityData activity = activities.get(position);
            holder.bind(activity, listener);
        }

        @Override
        public int getItemCount() {
            return activities.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDate, tvDistance, tvAveragePace, tvTime, tvAddress, tvCalories;
            MapView mapView;
            GoogleMap map;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvDistance = itemView.findViewById(R.id.tvDistance);
                tvAveragePace = itemView.findViewById(R.id.tvAveragePace);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvAddress = itemView.findViewById(R.id.tvAddress);
                tvCalories = itemView.findViewById(R.id.tvCalories);
                mapView = itemView.findViewById(R.id.mapView);

                mapView.onCreate(null);
                mapView.getMapAsync(googleMap -> {
                    MapsInitializer.initialize(itemView.getContext());
                    map = googleMap;
                    map.getUiSettings().setAllGesturesEnabled(false);
                });
            }

            void bind(final ActivityData activity, final OnItemClickListener listener) {
                tvName.setText(activity.getName());
                tvDate.setText(formatDate(activity.getStartTimestamp()));
                tvDistance.setText(String.format(Locale.getDefault(), "%.2f", activity.getDistance()));
                tvAveragePace.setText(String.format(Locale.getDefault(), "%s", calculateAveragePace(activity.getElapsedTime(), activity.getDistance())));
                tvTime.setText(formatElapsedTime(activity.getElapsedTime()));
                tvAddress.setText(activity.getAddress());
                tvCalories.setText(String.format(Locale.getDefault(), "%d", calculateCalories(activity)));
                itemView.setOnClickListener(v -> listener.onItemClick(activity));

                if (map != null) {
                    map.clear();
                    drawActivityTrack(activity);
                }
            }

            private void drawActivityTrack(ActivityData activity) {
                List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(activity.getStartTimestamp(), activity.getEndTimestamp());
                if (locations.size() < 2) return;

                PolylineOptions polylineOptions = new PolylineOptions()
                        .color(Color.RED)
                        .width(3f);
                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

                for (LocationData location : locations) {
                    LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                    polylineOptions.add(point);
                    boundsBuilder.include(point);
                }

                map.addPolyline(polylineOptions);

                // Add start marker
                LatLng startPoint = new LatLng(locations.get(0).getLatitude(), locations.get(0).getLongitude());
                map.addMarker(new MarkerOptions()
                        .position(startPoint)
                        .title("Start")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                // Add end marker
                LatLng endPoint = new LatLng(locations.get(locations.size() - 1).getLatitude(), locations.get(locations.size() - 1).getLongitude());
                map.addMarker(new MarkerOptions()
                        .position(endPoint)
                        .title("End")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                LatLngBounds bounds = boundsBuilder.build();
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
            }
        }
    }

    interface OnItemClickListener {
        void onItemClick(ActivityData activity);
    }

    private String formatElapsedTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }

    private String calculateAveragePace(long elapsedTime, double distance) {
        if (distance < 0.01) {
            return "--:--";
        }
        long averagePaceSeconds = (long) (elapsedTime / 1000 / distance);
        int averagePaceMinutes = (int) (averagePaceSeconds / 60);
        int averagePaceSecondsRemainder = (int) (averagePaceSeconds % 60);
        return String.format(Locale.getDefault(), "%02d:%02d", averagePaceMinutes, averagePaceSecondsRemainder);
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("M월d일, yyyy h:mm a", Locale.KOREAN);
        return sdf.format(new Date(timestamp));
    }

    private int calculateCalories(ActivityData activity) {
        // Implement your calorie calculation logic here
        // This is a placeholder calculation
        return (int) (activity.getDistance() * 60);
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (holder instanceof ActivityAdapter.ViewHolder) {
                ((ActivityAdapter.ViewHolder) holder).mapView.onResume();
            }
        }
    }

    @Override
    protected void onPause() {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (holder instanceof ActivityAdapter.ViewHolder) {
                ((ActivityAdapter.ViewHolder) holder).mapView.onPause();
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (holder instanceof ActivityAdapter.ViewHolder) {
                ((ActivityAdapter.ViewHolder) holder).mapView.onDestroy();
            }
        }
        super.onDestroy();
    }
}
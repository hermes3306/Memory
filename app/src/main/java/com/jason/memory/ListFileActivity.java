package com.jason.memory;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ListFileActivity extends AppCompatActivity {
    private static final String TAG = ListFileActivity.class.getSimpleName();
    private RecyclerView recyclerView;
    private List<ActivityData> activityList;
    private FileHelper fileHelper;
    private ActivityAdapter adapter;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private int currentPage = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_file);
        Log.d(TAG, "--m-- onCreate: Initializing ListFileActivity");

        recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView == null) {
            Log.e(TAG, "--m-- onCreate: RecyclerView not found in layout");
            Toast.makeText(this, "Error initializing UI", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fileHelper = new FileHelper();
        fileHelper.clearCache();

        activityList = new ArrayList<>();

        adapter = new ActivityAdapter(activityList, activity -> {
            Intent intent = new Intent(ListFileActivity.this, ActivityCloudDetailActivity.class);
            intent.putExtra("ACTIVITY_FILENAME", activity.getFilename());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        Log.d(TAG, "--m-- onCreate: RecyclerView and Adapter set up");

        fetchActivityList();

        setupScrollListener();

        Log.d(TAG, "--m-- onCreate: Activity list fetch initiated");
    }

    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && hasMoreData) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0) {
                            loadMoreActivities();
                        }
                    }
                }
            }
        });
    }

    private void fetchActivityList() {
        hasMoreData = false;
        isLoading = true;
        new Thread(() -> {
            List<String> fileList = fileHelper.getFileList(currentPage);
            runOnUiThread(() -> {
                for (String fileName : fileList) {
                    activityList.add(new ActivityData(fileName));
                }
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void loadMoreActivities() {
        if (!isLoading && hasMoreData) {
            currentPage++;
            fetchActivityList();
        }
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
            boolean isMapReady = false;
            private boolean isMapDataLoaded = false;

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
                    isMapReady = true;
                });
            }

            void bind(final ActivityData activity, final OnItemClickListener listener) {
                tvName.setText(activity.getName());
                if (activity.getStartTimestamp() == 0) {
                    loadActivityDetails(activity);
                } else {
                    displayActivityDetails(activity);
                }
                itemView.setOnClickListener(v -> listener.onItemClick(activity));

                if (isMapReady && !isMapDataLoaded && itemView.isShown()) {
                    loadMapData(activity);
                }
            }

            private void loadActivityDetails(ActivityData activity) {
                new Thread(() -> {
                    ActivityData detailedActivity = fileHelper.getActivityDataFromFile(activity.getFilename());
                    if (detailedActivity != null) {
                        activity.updateFrom(detailedActivity);
                        itemView.post(() -> displayActivityDetails(activity));
                    }
                }).start();
            }

            private void displayActivityDetails(ActivityData activity) {
                tvDate.setText(formatDate(activity.getStartTimestamp()));
                tvDistance.setText(String.format(Locale.getDefault(), "%.2f", activity.getDistance()));
                tvAveragePace.setText(String.format(Locale.getDefault(), "%s", calculateAveragePace(activity.getElapsedTime(), activity.getDistance())));
                tvTime.setText(formatElapsedTime(activity.getElapsedTime()));
                tvAddress.setText(activity.getAddress());
                tvCalories.setText(String.format(Locale.getDefault(), "%d", calculateCalories(activity)));

                if (isMapReady && !isMapDataLoaded) {
                    loadMapData(activity);
                }
            }

            private void loadMapData(ActivityData activity) {
                if (map == null || activity == null) return;

                new Thread(() -> {
                    try {
                        List<LocationData> locations = fileHelper.getLocationDataFromFile(activity.getFilename());
                        if (locations != null && !locations.isEmpty()) {
                            itemView.post(() -> drawActivityTrack(locations));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading map data for " + activity.getFilename(), e);
                    }
                    isMapDataLoaded = true;
                }).start();
            }

            private void drawActivityTrack(List<LocationData> locations) {
                if (map == null || locations.size() < 2) return;

                map.clear();
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

                addMarker(locations.get(0), "Start", BitmapDescriptorFactory.HUE_GREEN);
                addMarker(locations.get(locations.size() - 1), "End", BitmapDescriptorFactory.HUE_RED);

                LatLngBounds bounds = boundsBuilder.build();
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
            }

            private void addMarker(LocationData location, String title, float markerColor) {
                LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                map.addMarker(new MarkerOptions()
                        .position(point)
                        .title(title)
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
            }

            void onResume() {
                mapView.onResume();
            }

            void onPause() {
                mapView.onPause();
            }

            void onDestroy() {
                mapView.onDestroy();
            }
        }
    }

    private static class FileHelper {
        private static final String BASE_URL = "http://58.233.69.198/moment/";
        private Map<String, String> contentCache = new HashMap<>();

        public List<String> getFileList(int page) {
            String url = BASE_URL + "list.php?ext=csv";
            String result = fetchContent(url);
            if (result != null) {
                return Arrays.asList(result.split("<br>"));
            }
            return new ArrayList<>();
        }

        public void clearCache() {
            contentCache.clear();
            Log.d(TAG, "--m-- clearCache: Content cache cleared");
        }

        public ActivityData getActivityDataFromFile(String fileName) {
            try {
                Log.d(TAG, "--m-- getActivityDataFromFile: Fetching data for file: " + fileName);
                String fileContent = fetchContent(getFullUrl(fileName));
                if (fileContent != null && !fileContent.isEmpty()) {
                    // Parse file content and create ActivityData object
                    // This is a simplified version, you'll need to implement the actual parsing logic
                    return new ActivityData(fileName); // Placeholder
                }
            } catch (Exception e) {
                Log.e(TAG, "--m-- getActivityDataFromFile: Error processing file: " + fileName, e);
            }
            return null;
        }

        public List<LocationData> getLocationDataFromFile(String fileName) {
            String fullUrl = getFullUrl(fileName);
            Log.d(TAG, "--m-- getLocationDataFromFile: Fetching location data from URL: " + fullUrl);
            List<LocationData> locations = new ArrayList<>();
            String fileContent = fetchContent(fullUrl);
            if (fileContent != null) {
                // Parse file content and create LocationData objects
                // This is a simplified version, you'll need to implement the actual parsing logic
            }
            return locations;
        }

        private String getFullUrl(String fileName) {
            return BASE_URL + fileName;
        }

        public String fetchContent(String urlString) {
            if (contentCache.containsKey(urlString)) {
                Log.d(TAG, "--m-- fetchContent: Returning cached content for URL: " + urlString);
                return contentCache.get(urlString);
            }

            Log.d(TAG, "--m-- fetchContent: Fetching content from URL: " + urlString);
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000); // 15 seconds
                connection.setReadTimeout(15000); // 15 seconds

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "--m-- fetchContent: Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder result = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line).append("\n");
                        }
                    }
                    String content = result.toString();
                    Log.d(TAG, "--m-- fetchContent: Successfully fetched content from URL: " + urlString);

                    contentCache.put(urlString, content);

                    return content;
                } else {
                    Log.e(TAG, "--m-- fetchContent: HTTP error code: " + responseCode);
                    return null;
                }
            } catch (IOException e) {
                Log.e(TAG, "--m-- fetchContent: Error fetching content from URL: " + urlString, e);
                return null;
            }
        }
    }

    // Utility methods
    private String formatDate(long timestamp) {
        // Implement date formatting logic
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timestamp));
    }

    private String calculateAveragePace(long elapsedTime, double distance) {
        // Implement average pace calculation logic
        return "0'00\""; // Placeholder
    }

    private String formatElapsedTime(long elapsedTime) {
        // Implement elapsed time formatting logic
        return "00:00:00"; // Placeholder
    }

    private int calculateCalories(ActivityData activity) {
        // Implement calorie calculation logic
        return 0; // Placeholder
    }

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(ActivityData activity);
    }
}
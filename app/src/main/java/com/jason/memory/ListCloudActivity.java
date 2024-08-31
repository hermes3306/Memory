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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ListCloudActivity extends AppCompatActivity {
    private static final String TAG = ListCloudActivity.class.getSimpleName();

    private RecyclerView recyclerView;
    private ActivityAdapter adapter;
    private List<ActivityData> activityList;
    private CloudHelper cloudHelper;
    private static final String BASE_URL = "http://58.233.69.198/moment/";
    private static final String UPLOAD_DIR = "upload/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_activity);
        Log.d(TAG, "--m-- onCreate: Initializing ListCloudActivity");

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        cloudHelper = new CloudHelper();
        activityList = new ArrayList<>();

        fetchActivityList();

        adapter = new ActivityAdapter(activityList, activity -> {
            Intent intent = new Intent(ListCloudActivity.this, ActivityCloudDetailActivity.class);
            intent.putExtra("ACTIVITY_FILENAME", activity.getFilename());
            startActivity(intent);
        });


        recyclerView.setAdapter(adapter);
        Log.d(TAG, "--m-- onCreate: Activity list fetch initiated");
    }

    private void fetchActivityList() {
        new Thread(() -> {
            Log.d(TAG, "--m-- fetchActivityList: Starting to fetch file list");
            String fileList = cloudHelper.getFileList();
            if (fileList != null) {
                Log.d(TAG, "--m-- fetchActivityList: File list retrieved successfully");
                parseFileList(fileList);
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "--m-- fetchActivityList: UI updated with new data");
                });
            } else {
                Log.e(TAG, "--m-- fetchActivityList: Failed to retrieve file list");
            }
        }).start();
    }

    private void parseFileList(String fileList) {
        Log.d(TAG, "--m-- parseFileList: Raw file list: " + fileList);
        String[] files = fileList.split("<br>");
        for (String file : files) {
            file = file.trim();
            if (!file.isEmpty()) {  // Add this check
                Log.d(TAG, "--m-- parseFileList: Processing file: " + file);
                if (file.endsWith(".csv")) {
                    ActivityData activity = cloudHelper.getActivityDataFromFile(file);
                    if (activity != null) {
                        activity.setId(activityList.size() + 1);
                        activity.setFilename(file);
                        activityList.add(activity);
                        Log.d(TAG, "--m-- parseFileList: Added activity from file: " + file);
                    } else {
                        Log.w(TAG, "--m-- parseFileList: Failed to parse activity from file: " + file);
                    }
                } else {
                    Log.d(TAG, "--m-- parseFileList: Skipping non-CSV file: " + file);
                }
            }
        }
        Log.d(TAG, "--m-- parseFileList: Finished parsing file list. Total activities: " + activityList.size());
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
                    if (itemView.isShown() && !isMapDataLoaded) {
                        loadMapData();
                    }
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

                if (isMapReady && itemView.isShown() && !isMapDataLoaded) {
                    loadMapData();
                }
            }

            private void loadMapData() {
                ActivityData activity = activityList.get(getAdapterPosition());
                new Thread(() -> {
                    try {
                        List<LocationData> locations = cloudHelper.getLocationDataFromFile(activity.getFilename());
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

                // Add start and end markers
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
        Log.d(TAG, "--m-- onResume: Resuming ListCloudActivity");
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
        Log.d(TAG, "--m-- onPause: Pausing ListCloudActivity");
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
        Log.d(TAG, "--m-- onDestroy: Destroying ListCloudActivity");
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

    private class CloudHelper {
        private static final String BASE_URL = "http://58.233.69.198/moment/";

        public String getFileList() {
            Log.d(TAG, "--m-- getFileList: Fetching file list from server");
            String result = fetchContent(BASE_URL + "list.php?ext=csv");
            if (result != null) {
                Log.d(TAG, "--m-- getFileList: File list retrieved successfully");
            } else {
                Log.e(TAG, "--m-- getFileList: Failed to retrieve file list");
            }
            return result;
        }

        public ActivityData getActivityDataFromFile(String fileName) {
            Log.d(TAG, "--m-- getActivityDataFromFile: Fetching data for file: " + fileName);
            String fileContent = fetchContent(getFullUrl(fileName));
            if (fileContent != null) {
                Log.d(TAG, "--m-- getActivityDataFromFile: Content fetched, length: " + fileContent.length());

                // Extract name from fileName (remove .csv extension)
                String name = fileName.substring(0, fileName.lastIndexOf('.'));

                // Parse the content
                String[] lines = fileContent.split("\n");
                if (lines.length > 1) {
                    String[] firstDataLine = lines[1].split(",");
                    String[] lastDataLine = lines[lines.length - 1].split(",");

                    if (firstDataLine.length >= 4 && lastDataLine.length >= 4) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());
                            Date startDate = sdf.parse(firstDataLine[2] + "," + firstDataLine[3]);
                            Date endDate = sdf.parse(lastDataLine[2] + "," + lastDataLine[3]);

                            long startTimestamp = startDate.getTime();
                            long endTimestamp = endDate.getTime();
                            long elapsedTime = endTimestamp - startTimestamp;

                            double startLat = Double.parseDouble(firstDataLine[0]);
                            double startLon = Double.parseDouble(firstDataLine[1]);
                            double endLat = Double.parseDouble(lastDataLine[0]);
                            double endLon = Double.parseDouble(lastDataLine[1]);

                            double distance = calculateDistance(startLat, startLon, endLat, endLon);

                            return new ActivityData(0, fileName, "run", name, startTimestamp, endTimestamp, 0, 0, distance, elapsedTime, "Address not available");
                        } catch (ParseException | NumberFormatException e) {
                            Log.e(TAG, "--m-- getActivityDataFromFile: Error parsing data from file: " + fileName, e);
                        }
                    } else {
                        Log.e(TAG, "--m-- getActivityDataFromFile: Invalid data format in file: " + fileName);
                    }
                } else {
                    Log.e(TAG, "--m-- getActivityDataFromFile: Not enough data in file: " + fileName);
                }
            } else {
                Log.e(TAG, "--m-- getActivityDataFromFile: Failed to fetch content for file: " + fileName);
            }
            return null;
        }

        private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
            // Implement distance calculation (e.g., using Haversine formula)
            // This is a placeholder implementation
            return Math.sqrt(Math.pow(lat2 - lat1, 2) + Math.pow(lon2 - lon1, 2)) * 111.32;
        }


        private String getFullUrl(String fileName) {
            return BASE_URL + UPLOAD_DIR + fileName;
        }


        public List<LocationData> getLocationDataFromFile(String fileName) {
            String fullUrl = getFullUrl(fileName);
            Log.d(TAG, "--m-- getLocationDataFromFile: Fetching location data from URL: " + fullUrl);
            List<LocationData> locations = new ArrayList<>();
            String fileContent = fetchContent(fullUrl);
            if (fileContent != null) {
                String[] lines = fileContent.split("\n");
                for (int i = 1; i < lines.length; i++) {
                    try {
                        String[] data = lines[i].split(",");
                        if (data.length >= 4) {
                            double latitude = Double.parseDouble(data[0]);
                            double longitude = Double.parseDouble(data[1]);
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());
                            Date date = sdf.parse(data[2] + "," + data[3]);
                            locations.add(new LocationData(0, latitude, longitude, 0, date.getTime()));
                        }
                    } catch (ParseException | NumberFormatException e) {
                        Log.e(TAG, "--m-- Error parsing line: " + lines[i], e);
                        // Continue to next line instead of breaking the whole process
                    }
                }
                Log.d(TAG, "--m-- getLocationDataFromFile: Parsed " + locations.size() + " locations from file: " + fileName);
            } else {
                Log.e(TAG, "--m-- getLocationDataFromFile: Failed to fetch content for file: " + fileName);
            }
            return locations;
        }

        private String fetchContent(String urlString) {
            Log.d(TAG, "--m-- fetchContent: Fetching content from URL: " + urlString);
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

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
                    Log.d(TAG, "--m-- fetchContent: Successfully fetched content from URL: " + urlString);
                    return result.toString();
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
}
package com.jason.memory;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ListCloudActivity extends AppCompatActivity {
    private boolean isAscendingOrder = true;
    private static final String TAG = ListCloudActivity.class.getSimpleName();
    private RecyclerView recyclerView;
    private ActivityAdapter adapter;
    private List<ActivityData> activityList;
    private CloudHelper cloudHelper;
    private static final String BASE_URL = Config.BASE_URL;
    private static final String UPLOAD_DIR = "upload/";
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private Set<String> processedFiles = new HashSet<>();
    private static final int ACTIVITY_DETAIL_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_activity);
        Log.d(TAG, "--m-- onCreate: Initializing ListCloudActivity");

        recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView == null) {
            Log.e(TAG, "--m-- onCreate: RecyclerView not found in layout");
            Toast.makeText(this, "Error initializing UI", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        cloudHelper = new CloudHelper();
        cloudHelper.clearCache();

        activityList = new ArrayList<>();
        processedFiles = new HashSet<>();

        adapter = new ActivityAdapter(activityList, activity -> {
            Intent intent = new Intent(ListCloudActivity.this, ActivityCloudDetailActivity.class);
            intent.putExtra("ACTIVITY_FILENAME", activity.getName() + ".csv");
            startActivityForResult(intent, ACTIVITY_DETAIL_REQUEST);
        });

        recyclerView.setAdapter(adapter);
        Log.d(TAG, "--m-- onCreate: RecyclerView and Adapter set up");

        fetchActivityList();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
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
        });

        Log.d(TAG, "--m-- onCreate: Activity list fetch initiated");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_DETAIL_REQUEST && resultCode == RESULT_OK) {
            ActivityData updatedActivity = (ActivityData) data.getSerializableExtra("UPDATED_ACTIVITY");
            if (updatedActivity != null) {
                updateActivityInList(updatedActivity);
            }
        }
    }

    private void updateActivityInList(ActivityData updatedActivity) {
        for (int i = 0; i < activityList.size(); i++) {
            if (activityList.get(i).getName().equals(updatedActivity.getName())) {
                activityList.set(i, updatedActivity);
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }


    private void toggleSortOrder() {
        isAscendingOrder = !isAscendingOrder;
        sortActivityList();
    }

    private void sortActivityList() {
        Collections.sort(activityList, new ActivityTimestampComparator());
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    private void fetchActivityList() {
        Log.d(TAG, "--m-- fetchActivityList: Starting to fetch file list");
        showProgressBar(true);
        new Thread(() -> {
            try {
                List<String> files = cloudHelper.getFileList(currentPage);
                for (String file : files) {
                    if (file.endsWith(".csv") && !processedFiles.contains(file)) {
                        processedFiles.add(file);
                        ActivityData activity = cloudHelper.getActivityDataFromFile(file);
                        if (activity != null) {
                            activity.setId(activityList.size() + 1);
                            activity.setFilename(file);
                            activityList.add(activity);
                        }
                    }
                }
                currentPage++;

                // Sort the list after adding all activities
                sortActivityList();

                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    if (activityList.isEmpty()) {
                        Toast.makeText(this, "No activities found", Toast.LENGTH_LONG).show();
                    }
                    showProgressBar(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "--m-- fetchActivityList: Error fetching file list", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showProgressBar(false);
                });
            }
        }).start();
    }

    private void loadMoreActivities() {
        if (isLoading) return;
        isLoading = true;
        showProgressBar(true);
        new Thread(() -> {
            List<String> newFiles = cloudHelper.getFileList(currentPage);
            if (newFiles.isEmpty()) {
                hasMoreData = false;
            } else {
                for (String file : newFiles) {
                    if (file.endsWith(".csv") && !processedFiles.contains(file)) {
                        processedFiles.add(file);
                        ActivityData activity = cloudHelper.getActivityDataFromFile(file);
                        if (activity != null) {
                            activity.setId(activityList.size() + 1);
                            activity.setFilename(file);
                            activityList.add(activity);
                        }
                    }
                }
                currentPage++;

                // Sort the list after adding new activities
                sortActivityList();
            }
            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                isLoading = false;
                showProgressBar(false);
            });
        }).start();
    }


    private void showProgressBar(boolean show) {
        runOnUiThread(() -> {
            findViewById(R.id.progressBar).setVisibility(show ? View.VISIBLE : View.GONE);
        });
    }

    private void parseFileList(String fileList) {
        Log.d(TAG, "--m-- parseFileList: Raw file list: " + fileList);
        String[] files = fileList.split("<br>");
        for (String file : files) {
            file = file.trim();
            if (!file.isEmpty() && !processedFiles.contains(file) && file.endsWith(".csv")) {
                Log.d(TAG, "--m-- parseFileList: Processing file: " + file);
                processedFiles.add(file);
                ActivityData activity = cloudHelper.getActivityDataFromFile(file);
                if (activity != null) {
                    activity.setId(activityList.size() + 1);
                    activity.setFilename(file);
                    activityList.add(activity);
                    Log.d(TAG, "--m-- parseFileList: Added activity: " + activity.toString());
                } else {
                    Log.e(TAG, "--m-- parseFileList: Failed to parse activity from file: " + file);
                }
            } else {
                Log.d(TAG, "--m-- parseFileList: Skipping file: " + file);
            }
        }
        Log.d(TAG, "--m-- parseFileList: Finished parsing. Total activities: " + activityList.size());
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
            private MapView mapView;
            private GoogleMap map;
            private boolean isMapReady = false;
            private ActivityData boundActivity;
            private String boundActivityFilename;
            private OnItemClickListener listener;

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
                mapView.getMapAsync(this::onMapReady);
            }

            private void onMapReady(GoogleMap googleMap) {
                this.map = googleMap;
                isMapReady = true;
                if (boundActivity != null) {
                    loadMapData();
                }
            }

            void bind(final ActivityData activity, final OnItemClickListener listener) {
                this.boundActivity = activity;
                this.boundActivityFilename = activity.getFilename();
                this.listener = listener;

                tvName.setText(activity.getName());
                if (activity.getStartTimestamp() == 0) {
                    loadActivityDetails(activity);
                } else {
                    displayActivityDetails(activity);
                }

                if (isMapReady) {
                    loadMapData();
                }

                itemView.setOnClickListener(v -> {
                    if (this.listener != null) {
                        this.listener.onItemClick(activity);
                    }
                });
            }

            private void loadActivityDetails(ActivityData activity) {
                new Thread(() -> {
                    ActivityData detailedActivity = cloudHelper.getActivityDataFromFile(activity.getFilename());
                    if (detailedActivity != null) {
                        // Update the activity with the detailed information
                        activity.setStartTimestamp(detailedActivity.getStartTimestamp());
                        activity.setEndTimestamp(detailedActivity.getEndTimestamp());
                        activity.setDistance(detailedActivity.getDistance());
                        activity.setElapsedTime(detailedActivity.getElapsedTime());
                        activity.setAddress(detailedActivity.getAddress());
                        // Update any other fields that might be in your ActivityData class

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
            }

            private void loadMapData() {
                if (map == null || boundActivity == null) return;

                map.clear();
                new AsyncTask<Void, Void, List<LocationData>>() {
                    @Override
                    protected List<LocationData> doInBackground(Void... voids) {
                        return cloudHelper.getLocationDataFromFile(boundActivityFilename);
                    }

                    @Override
                    protected void onPostExecute(List<LocationData> locations) {
                        if (locations != null && !locations.isEmpty() && boundActivityFilename.equals(boundActivity.getFilename())) {
                            drawActivityTrack(locations);
                        }
                    }
                }.execute();
            }

            private void drawActivityTrack(List<LocationData> locations) {
                map.clear();
                PolylineOptions polylineOptions = new PolylineOptions().color(Color.RED).width(5);
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
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
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

            private String formatDate(long timestamp) {
                SimpleDateFormat sdf = new SimpleDateFormat("M월d일, yyyy h:mm a", Locale.KOREAN);
                return sdf.format(new Date(timestamp));
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

            private int calculateCalories(ActivityData activity) {
                return (int) (activity.getDistance() * 60);
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
        return (int) (activity.getDistance() * 60);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "--m-- onResume: Resuming ListCloudActivity");
        if (recyclerView != null) {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
                if (viewHolder instanceof ActivityAdapter.ViewHolder) {
                    ((ActivityAdapter.ViewHolder) viewHolder).onResume();
                }
            }
        }
    }


    @Override
    protected void onPause() {
        Log.d(TAG, "--m-- onPause: Pausing ListCloudActivity");
        if (recyclerView != null) {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
                if (viewHolder instanceof ActivityAdapter.ViewHolder) {
                    ((ActivityAdapter.ViewHolder) viewHolder).onPause();
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
                RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
                if (viewHolder instanceof ActivityAdapter.ViewHolder) {
                    ((ActivityAdapter.ViewHolder) viewHolder).onDestroy();
                }
            }
        }
        super.onDestroy();
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "--m-- onRestart: Restarting ListCloudActivity");

        cloudHelper.clearCache();
        cloudHelper.clearLocationCache();
        activityList.clear();
        processedFiles.clear();
        currentPage = 1;
        hasMoreData = true;

        runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
        });

        fetchActivityList();
    }


    private static class CloudHelper {
        private static final String BASE_URL = Config.BASE_URL;
        private Map<String, String> contentCache = new HashMap<>();
        private Map<String, List<LocationData>> locationCache = new HashMap<>();
        private static final int PAGE_SIZE = 20;

        public List<String> getFileList(int page) {
            String url = BASE_URL + "list.php?ext=csv&page=" + page + "&limit=" + PAGE_SIZE;
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

        public String getFileList() {
            Log.d(TAG, "--m-- getFileList: Fetching file list from server");
            String result = fetchContent(BASE_URL + "list.php?ext=csv");
            if (result != null) {
                Log.d(TAG, "--m-- getFileList: File list retrieved successfully. Raw response: " + result);
            } else {
                Log.e(TAG, "--m-- getFileList: Failed to retrieve file list");
            }
            return result;
        }


        public ActivityData getActivityDataFromFile(String fileName) {
            try {
                Log.d(TAG, "--m-- getActivityDataFromFile: Fetching data for file: " + fileName);
                String fileContent = fetchContent(getFullUrl(fileName));
                if (fileContent != null && !fileContent.isEmpty()) {
                    Log.d(TAG, "--m-- getActivityDataFromFile: Content fetched, length: " + fileContent.length());
                    Log.d(TAG, "--m-- getActivityDataFromFile: First 100 characters: " + fileContent.substring(0, Math.min(fileContent.length(), 100)));

                    String[] lines = fileContent.split("\n");
                    Log.d(TAG, "--m-- getActivityDataFromFile: Number of lines: " + lines.length);

                    if (lines.length > 1) {
                        String[] headerLine = lines[0].split(",");
                        String[] firstDataLine = lines[1].split(",");
                        String[] lastDataLine = lines[lines.length - 1].split(",");

                        Log.d(TAG, "--m-- getActivityDataFromFile: Header: " + Arrays.toString(headerLine));
                        Log.d(TAG, "--m-- getActivityDataFromFile: First data line: " + Arrays.toString(firstDataLine));
                        Log.d(TAG, "--m-- getActivityDataFromFile: Last data line: " + Arrays.toString(lastDataLine));

                        // Find the indices for latitude, longitude, date, and time
                        int latIndex = findIndex(headerLine, "latitude", 0);
                        int lonIndex = findIndex(headerLine, "longitude", 1);
                        int dateIndex = findIndex(headerLine, "date", 2);
                        int timeIndex = findIndex(headerLine, "time", 3);

                        if (latIndex != -1 && lonIndex != -1 && dateIndex != -1 && timeIndex != -1) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());
                                Date startDate = sdf.parse(firstDataLine[dateIndex] + "," + firstDataLine[timeIndex]);
                                Date endDate = sdf.parse(lastDataLine[dateIndex] + "," + lastDataLine[timeIndex]);

                                long startTimestamp = startDate.getTime();
                                long endTimestamp = endDate.getTime();
                                long elapsedTime = endTimestamp - startTimestamp;

                                List<LocationData> locations = getLocationDataFromFile(fileName);
                                double distance = calculateDistance(locations);

                                String name = fileName.substring(0, fileName.lastIndexOf('.'));
                                ActivityData activityData = new ActivityData(
                                        0L,  // id
                                        "run",  // type
                                        name,  // name
                                        startTimestamp,
                                        endTimestamp,
                                        0L,  // startLocation
                                        0L,  // endLocation
                                        distance,
                                        elapsedTime,
                                        "Address not available"
                                );
                                activityData.setFilename(fileName);  // Set the filename

                                Log.d(TAG, "--m-- getActivityDataFromFile: Parsed ActivityData: " + activityData.toString());
                                return activityData;
                            } catch (ParseException | NumberFormatException e) {
                                Log.e(TAG, "--m-- getActivityDataFromFile: Error parsing data from file: " + fileName, e);
                            }
                        } else {
                            Log.e(TAG, "--m-- getActivityDataFromFile: Required columns not found in file: " + fileName);
                        }
                    } else {
                        Log.e(TAG, "--m-- getActivityDataFromFile: Not enough data in file: " + fileName);
                    }
                } else {
                    Log.e(TAG, "--m-- getActivityDataFromFile: Failed to fetch content for file: " + fileName);
                }
            } catch (Exception e) {
                Log.e(TAG, "--m-- getActivityDataFromFile: Error processing file: " + fileName, e);
            }
            return null;
        }

        private int findIndex(String[] array, String target, int defaultIndex) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].toLowerCase().contains(target)) {
                    return i;
                }
            }
            return defaultIndex;
        }



        private double calculateDistance(List<LocationData> locations) {
            double totalDistance = 0;
            if (locations == null || locations.size() < 2) return 0;
            for (int i = 0; i < locations.size() - 1; i++) {
                LocationData start = locations.get(i);
                LocationData end = locations.get(i + 1);
                totalDistance += calculateDistanceBetweenPoints(start, end);
            }
            return totalDistance;
        }

        private double calculateDistanceBetweenPoints(LocationData start, LocationData end) {
            double earthRadius = 6371; // in kilometers
            double dLat = Math.toRadians(end.getLatitude() - start.getLatitude());
            double dLon = Math.toRadians(end.getLongitude() - start.getLongitude());
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(start.getLatitude())) * Math.cos(Math.toRadians(end.getLatitude())) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return earthRadius * c; // Distance in kilometers
        }

        private String getFullUrl(String fileName) {
            return BASE_URL + UPLOAD_DIR + fileName;
        }


        public List<LocationData> getLocationDataFromFile(String fileName) {
            if (locationCache.containsKey(fileName)) {
                return locationCache.get(fileName);
            }

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
                    }
                }
                Log.d(TAG, "--m-- getLocationDataFromFile: Parsed " + locations.size() + " locations from file: " + fileName);
                locationCache.put(fileName, locations);
            } else {
                Log.e(TAG, "--m-- getLocationDataFromFile: Failed to fetch content for file: " + fileName);
            }
            return locations;
        }

        public void clearLocationCache() {
            locationCache.clear();
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

        private String fetchContent_without_cache(String urlString) {
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

    private class ActivityTimestampComparator implements Comparator<ActivityData> {
        @Override
        public int compare(ActivityData a1, ActivityData a2) {
            return Long.compare(a2.getStartTimestamp(), a1.getStartTimestamp());
        }
    }

}
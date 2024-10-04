package com.jason.memory;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    private static final String TAG = "FileAdapter";
    private List<ActivityData> activities;
    private ListActivityActivity.OnItemClickListener listener;
    private Context context;
    private Map<String, List<LocationData>> locationCache = new HashMap<>();

    public FileAdapter(Context context, List<ActivityData> activities, ListActivityActivity.OnItemClickListener listener) {
        this.context = context;
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

        void bind(final ActivityData activity, final ListActivityActivity.OnItemClickListener listener) {
            tvName.setText(activity.getName());
            tvDate.setText(formatDate(activity.getStartTimestamp()));
            tvDistance.setText(String.format(Locale.getDefault(), "%.2f", activity.getDistance()));
            tvAveragePace.setText(String.format(Locale.getDefault(), "%s", calculateAveragePace(activity.getElapsedTime(), activity.getDistance())));
            tvTime.setText(formatElapsedTime(activity.getElapsedTime()));
            tvAddress.setText(activity.getAddress());
            tvCalories.setText(String.format(Locale.getDefault(), "%d", calculateCalories(activity)));
            itemView.setOnClickListener(v -> listener.onItemClick(activity));

            mapView.setTag(activity.getName());
            mapView.getMapAsync(googleMap -> {
                if (mapView.getTag().equals(activity.getName())) {
                    map = googleMap;
                    map.getUiSettings().setAllGesturesEnabled(false);
                    isMapReady = true;
                    drawActivityTrack(activity);
                }
            });
        }


        private void drawActivityTrack(ActivityData activity) {
            if (!isMapReady) return;
            map.clear();
            String filename = activity.getName() + ".csv";
            new Thread(() -> {
                List<LocationData> locations = getLocationDataFromFile(filename);
                if (locations.size() < 2) return;

                PolylineOptions polylineOptions = new PolylineOptions()
                        .color(Color.RED)
                        .width(3f);
                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

                double totalDistance = 0;
                LocationData prevLocation = null;
                int sampleRate = Math.max(1, locations.size() / 100); // Sample at most 100 points

                for (int i = 0; i < locations.size(); i += sampleRate) {
                    LocationData location = locations.get(i);
                    LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                    polylineOptions.add(point);
                    boundsBuilder.include(point);

                    if (prevLocation != null) {
                        totalDistance += calculateDistance(prevLocation, location);
                    }
                    prevLocation = location;
                }

                // Include the last point if it wasn't included due to sampling
                if (locations.size() - 1 % sampleRate != 0) {
                    LocationData lastLocation = locations.get(locations.size() - 1);
                    LatLng lastPoint = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    polylineOptions.add(lastPoint);
                    boundsBuilder.include(lastPoint);
                    totalDistance += calculateDistance(prevLocation, lastLocation);
                }

                final double finalDistance = totalDistance;

                itemView.post(() -> {
                    if (!mapView.getTag().equals(activity.getName())) return;
                    map.addPolyline(polylineOptions);
                    addMarker(locations.get(0), "Start", BitmapDescriptorFactory.HUE_GREEN);
                    addMarker(locations.get(locations.size() - 1), "End", BitmapDescriptorFactory.HUE_RED);
                    LatLngBounds bounds = boundsBuilder.build();
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

                    // Update the distance in the ActivityData object
                    activity.setDistance(finalDistance);
                    tvDistance.setText(String.format(Locale.getDefault(), "%.2f", finalDistance));
                    tvAveragePace.setText(calculateAveragePace(activity.getElapsedTime(), finalDistance));
                    tvCalories.setText(String.format(Locale.getDefault(), "%d", calculateCalories(activity)));
                });
            }).start();
        }

        private double calculateDistance(LocationData start, LocationData end) {
            double earthRadius = 6371; // in kilometers
            double dLat = Math.toRadians(end.getLatitude() - start.getLatitude());
            double dLon = Math.toRadians(end.getLongitude() - start.getLongitude());
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(start.getLatitude())) * Math.cos(Math.toRadians(end.getLatitude())) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return earthRadius * c; // Distance in kilometers
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

    private List<LocationData> getLocationDataFromFile(String fileName) {
        if (locationCache.containsKey(fileName)) {
            return locationCache.get(fileName);
        }

        List<LocationData> locations = new ArrayList<>();
        File file = new File(Config.getDownloadDir(context), fileName);

        if (!file.exists()) {
            Log.e(TAG, "File does not exist: " + file.getAbsolutePath());
            return locations;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;  // Skip the header line
                }
                String[] data = line.split(",");
                if (data.length >= 4) {
                    try {
                        double latitude = Double.parseDouble(data[0]);
                        double longitude = Double.parseDouble(data[1]);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault());
                        Date date = sdf.parse(data[2] + "," + data[3]);
                        locations.add(new LocationData(0, latitude, longitude, 0, date.getTime()));
                    } catch (ParseException | NumberFormatException e) {
                        Log.e(TAG, "Error parsing line: " + line, e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + file.getAbsolutePath(), e);
        }

        Log.d(TAG, "Parsed " + locations.size() + " locations from file: " + fileName);
        locationCache.put(fileName, locations);
        return locations;
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
        // Implement your calorie calculation logic here
        // This is a placeholder calculation
        return (int) (activity.getDistance() * 60);
    }


}
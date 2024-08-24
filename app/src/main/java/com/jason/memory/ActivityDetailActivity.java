package com.jason.memory;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private TextView tvName, tvStartTime, tvEndTime, tvDistance, tvElapsedTime;
    private GoogleMap mMap;
    private DatabaseHelper dbHelper;
    private ActivityData activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        tvName = findViewById(R.id.tvName);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime = findViewById(R.id.tvEndTime);
        tvDistance = findViewById(R.id.tvDistance);
        tvElapsedTime = findViewById(R.id.tvElapsedTime);

        dbHelper = new DatabaseHelper(this);
        long activityId = getIntent().getLongExtra("ACTIVITY_ID", -1);
        activity = dbHelper.getActivity(activityId);

        if (activity != null) {
            displayActivityDetails();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void displayActivityDetails() {
        tvName.setText(activity.getName());
        tvStartTime.setText("Start: " + formatTimestamp(activity.getStartTimestamp()));
        tvEndTime.setText("End: " + formatTimestamp(activity.getEndTimestamp()));
        tvDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f km", activity.getDistance()));
        tvElapsedTime.setText("Time: " + formatElapsedTime(activity.getElapsedTime()));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        drawActivityTrack();
    }

    private void drawActivityTrack() {
        List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(activity.getStartTimestamp(), activity.getEndTimestamp());
        if (locations.size() < 2) {
            Toast.makeText(this, "Not enough location data to draw track", Toast.LENGTH_SHORT).show();
            return;
        }

        PolylineOptions polylineOptions = new PolylineOptions();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (LocationData location : locations) {
            LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
            polylineOptions.add(point);
            boundsBuilder.include(point);
        }

        mMap.addPolyline(polylineOptions);
        LatLngBounds bounds = boundsBuilder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String formatElapsedTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }
}

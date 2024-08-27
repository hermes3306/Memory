package com.jason.memory;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class RunningMapActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private long startTimestamp;
    private DatabaseHelper dbHelper;
    private Handler handler = new Handler();
    private Runnable updateRunnable;
    private static final int UPDATE_INTERVAL = 5000; // 5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        dbHelper = new DatabaseHelper(this);
        startTimestamp = getIntent().getLongExtra("startTimestamp", 0);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        startTrackUpdates();
    }

    private void startTrackUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                drawTrack();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        handler.post(updateRunnable);
    }

    private void drawTrack() {
        //updateMap is the same as that of MyActivity
        updateMap();
    }

    private void updateMap() {
        if (mMap == null) return;

        // Clear the previous end marker
        mMap.clear();

        LocationData latestLocation = dbHelper.getLatestLocation();
        if (latestLocation != null) {
            LatLng latLng = new LatLng(latestLocation.getLatitude(), latestLocation.getLongitude());

            // Move camera to the latest location
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

            // Optionally, you can draw a polyline of the entire track
            List<LocationData> allLocations = dbHelper.getLocationsBetweenTimestamps(startTimestamp, System.currentTimeMillis());

            if(allLocations == null || allLocations.isEmpty()) return;

            // Add start marker
            LocationData startLocation = allLocations.get(0);
            LatLng startPoint = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(startPoint)
                    .title("Start")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

            List<LatLng> points = new ArrayList<>();
            for (LocationData location : allLocations) {
                LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                points.add(point);
                boundsBuilder.include(point);
            }

            // Add polyline
            mMap.addPolyline(new PolylineOptions()
                    .addAll(points)
                    .color(0xFFFF0000)
                    .width(3));

            LatLngBounds bounds = boundsBuilder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

            // Add end marker after everything else
            LocationData endLocation = allLocations.get(allLocations.size() - 1);
            LatLng endPoint = new LatLng(endLocation.getLatitude(), endLocation.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(endPoint)
                    .title("Current")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }
}
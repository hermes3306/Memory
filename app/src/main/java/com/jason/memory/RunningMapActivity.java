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
        List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(startTimestamp, System.currentTimeMillis());
        if (locations.size() < 2) {
            Toast.makeText(this, "Not enough location data to draw track", Toast.LENGTH_SHORT).show();
            return;
        }

        mMap.clear(); // Clear previous polylines and markers

        PolylineOptions polylineOptions = new PolylineOptions();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (LocationData location : locations) {
            LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
            polylineOptions.add(point);
            boundsBuilder.include(point);
        }

        mMap.addPolyline(polylineOptions);

        // Add start marker
        LocationData startLocation = locations.get(0);
        LatLng startPoint = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(startPoint)
                .title("Start")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Add end marker
        LocationData endLocation = locations.get(locations.size() - 1);
        LatLng endPoint = new LatLng(endLocation.getLatitude(), endLocation.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(endPoint)
                .title("Current")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        LatLngBounds bounds = boundsBuilder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
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
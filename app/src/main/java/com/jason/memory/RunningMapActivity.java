package com.jason.memory;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class RunningMapActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private long startTimestamp;
    private DatabaseHelper dbHelper;

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
        drawTrack();
    }

    private void drawTrack() {
        List<LocationData> locations = dbHelper.getLocationsBetweenTimestamps(startTimestamp, System.currentTimeMillis());
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

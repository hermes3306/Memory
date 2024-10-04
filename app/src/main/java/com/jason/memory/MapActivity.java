package com.jason.memory;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import android.graphics.Color;
import java.util.ArrayList;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private ArrayList<LatLng> receivedLocations;
    private static final float DEFAULT_ZOOM = 15f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Retrieve the locations from the intent
        receivedLocations = getIntent().getParcelableArrayListExtra("locations");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);

        if (receivedLocations != null && !receivedLocations.isEmpty()) {
            displayReceivedLocations();
        }
    }

    private void displayReceivedLocations() {
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(receivedLocations)
                .color(Color.RED)
                .width(3f);
        mMap.addPolyline(polylineOptions);

        // Move camera to show the entire track
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : receivedLocations) {
            builder.include(latLng);
        }
        LatLngBounds bounds = builder.build();
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
    }
}
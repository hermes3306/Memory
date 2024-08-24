package com.jason.memory;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        dbHelper = new DatabaseHelper(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        List<LocationData> locations = dbHelper.getAllLocations();

        if (!locations.isEmpty()) {
            PolylineOptions polylineOptions = new PolylineOptions();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());



            for (LocationData location : locations) {
                LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                String timeString = sdf.format(new Date(location.getTime()));
                mMap.addMarker(new MarkerOptions().position(point).title(timeString));
                polylineOptions.add(point);
            }



            mMap.addPolyline(polylineOptions);

            // Move camera to the last known location
            LocationData lastLocation = locations.get(locations.size() - 1);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 15));
        }
    }
}
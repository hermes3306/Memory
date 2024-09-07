package com.jason.memory;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.Date;
import java.util.List;

public class PlacesActivity extends AppCompatActivity implements OnMapReadyCallback {
    private RecyclerView recyclerView;
    private PlacesAdapter adapter;
    private List<Place> places;
    private DatabaseHelper dbHelper;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);

        dbHelper = new DatabaseHelper(this);
        dbHelper.createPlacesTable();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        places = dbHelper.getAllPlaces();
        adapter = new PlacesAdapter(places, this::onPlaceClick);
        recyclerView.setAdapter(adapter);

        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> addNewPlace());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        updateMap();
    }

    private void updateMap() {
        if (mMap == null) return;
        mMap.clear();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Place place : places) {
            LatLng latLng = new LatLng(place.getLat(), place.getLon());
            mMap.addMarker(new MarkerOptions().position(latLng).title(place.getName()));
            builder.include(latLng);
        }
        if (!places.isEmpty()) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        }
    }

    private void onPlaceClick(Place place) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(place.getName())
                .setItems(new CharSequence[]{"Update", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        updatePlace(place);
                    } else {
                        deletePlace(place);
                    }
                });
        builder.create().show();
    }

    private void addNewPlace() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Place");

        // Get the last known location from DatabaseHelper
        LatLng lastLocation = dbHelper.getLastKnownLocation();

        // Create the dialog layout
        final EditText nameInput = new EditText(this);
        nameInput.setHint("Place Name");

        builder.setView(nameInput);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = nameInput.getText().toString();
            if (!name.isEmpty() && lastLocation != null) {
                long currentTime = System.currentTimeMillis();
                Place newPlace = new Place(
                        0, // id will be set by the database
                        "Current Country", // You might want to get this dynamically
                        "place",
                        name,
                        "", // address (you might want to get this using Geocoder)
                        currentTime, // firstVisited
                        1, // numberOfVisits
                        currentTime, // lastVisited
                        lastLocation.latitude,
                        lastLocation.longitude,
                        0.0, // altitude (you might want to get this from your location data)
                        "" // memo
                );
                long id = dbHelper.addPlace(newPlace);
                newPlace.setId(id);
                places.add(newPlace);
                adapter.notifyDataSetChanged();
                updateMap();
            }
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }



    private void updatePlace(Place place) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Place");

        // Create the dialog layout
        final EditText nameInput = new EditText(this);
        nameInput.setText(place.getName());
        nameInput.setHint("Place Name");

        builder.setView(nameInput);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = nameInput.getText().toString();
            if (!newName.isEmpty()) {
                place.setName(newName);
                place.setNumberOfVisits(place.getNumberOfVisits() + 1);
                place.setLastVisited(System.currentTimeMillis());

                // Get the last known location from DatabaseHelper
                LatLng lastLocation = dbHelper.getLastKnownLocation();
                if (lastLocation != null) {
                    place.setLat(lastLocation.latitude);
                    place.setLon(lastLocation.longitude);
                }

                dbHelper.updatePlace(place);
                adapter.notifyDataSetChanged();
                updateMap();
            }
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void deletePlace(Place place) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Place")
                .setMessage("Are you sure you want to delete this place?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dbHelper.deletePlace(place.getId());
                    places.remove(place);
                    adapter.notifyDataSetChanged();
                    updateMap();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
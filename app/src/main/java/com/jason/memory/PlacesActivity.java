package com.jason.memory;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;
import android.location.Address;
import android.location.Geocoder;
import java.util.List;
import java.util.Locale;
import java.io.IOException;

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
        showUpdateDialog(place);
    }

    private void addNewPlace() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Place");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_place, null);
        builder.setView(dialogView);

        final EditText nameInput = dialogView.findViewById(R.id.nameInput);
        final EditText addressInput = dialogView.findViewById(R.id.addressInput);
        final Spinner typeSpinner = dialogView.findViewById(R.id.typeSpinner);
        final EditText countryInput = dialogView.findViewById(R.id.countryInput);

        // Set up the spinner with place types
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.place_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        // Get the last known location from DatabaseHelper
        LatLng lastLocation = dbHelper.getLastKnownLocation();

        // Get address information using Geocoder
        if (lastLocation != null) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(lastLocation.latitude, lastLocation.longitude, 1);
                if (!addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String shortName = address.getFeatureName() != null ? address.getFeatureName() : address.getSubThoroughfare();
                    String fullAddress = address.getAddressLine(0);
                    String country = address.getCountryName();

                    nameInput.setText(shortName);
                    addressInput.setText(fullAddress);
                    countryInput.setText(country);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Unable to get address information", Toast.LENGTH_SHORT).show();
            }
        }

        builder.setPositiveButton("Save", null); // We'll set the listener later

        final AlertDialog dialog = builder.create();
        dialog.show();

        // Set the click listener for the positive button
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String address = addressInput.getText().toString().trim();
            String type = typeSpinner.getSelectedItem() != null ? typeSpinner.getSelectedItem().toString() : "";
            String country = countryInput.getText().toString().trim();

            if (name.isEmpty() || address.isEmpty() || type.isEmpty() || country.isEmpty()) {
                Toast.makeText(PlacesActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (lastLocation != null) {
                long currentTime = System.currentTimeMillis();
                Place newPlace = new Place(
                        0, // id will be set by the database
                        country,
                        type,
                        name,
                        address,
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
                dialog.dismiss();
            } else {
                Toast.makeText(PlacesActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUpdateDialog(Place place) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Place");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_update_place, null);
        builder.setView(dialogView);

        final EditText nameInput = dialogView.findViewById(R.id.nameInput);
        final EditText addressInput = dialogView.findViewById(R.id.addressInput);
        final Spinner typeSpinner = dialogView.findViewById(R.id.typeSpinner);
        final EditText countryInput = dialogView.findViewById(R.id.countryInput);
        final TextView visitsTextView = dialogView.findViewById(R.id.visitsTextView);

        nameInput.setText(place.getName());
        addressInput.setText(place.getAddress());
        countryInput.setText(place.getCountry());
        visitsTextView.setText(String.format("Visits: %d", place.getNumberOfVisits()));

        // Set up the type spinner
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.place_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        // Set the current type in the spinner
        int typePosition = typeAdapter.getPosition(place.getType());
        if (typePosition != -1) {
            typeSpinner.setSelection(typePosition);
        }

        builder.setPositiveButton("Update", null);
        builder.setNegativeButton("Delete", null);
        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());

        final AlertDialog dialog = builder.create();
        dialog.show();

        // Set the click listener for the positive button
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();
            String newAddress = addressInput.getText().toString().trim();
            String newType = typeSpinner.getSelectedItem() != null ? typeSpinner.getSelectedItem().toString() : "";
            String newCountry = countryInput.getText().toString().trim();

            if (newName.isEmpty() || newAddress.isEmpty() || newType.isEmpty() || newCountry.isEmpty()) {
                Toast.makeText(PlacesActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            place.setName(newName);
            place.setAddress(newAddress);
            place.setType(newType);
            place.setCountry(newCountry);
            place.setNumberOfVisits(place.getNumberOfVisits() + 1);
            place.setLastVisited(System.currentTimeMillis());

            // Get the last known location from DatabaseHelper
            LatLng lastLocation = dbHelper.getLastKnownLocation();
            if (lastLocation != null) {
                place.setLat(lastLocation.latitude);
                place.setLon(lastLocation.longitude);
            }

            if (dbHelper.updatePlace(place) > 0) {
                adapter.notifyDataSetChanged();
                updateMap();
                dialog.dismiss();
            } else {
                Toast.makeText(PlacesActivity.this, "Failed to update place", Toast.LENGTH_SHORT).show();
            }
        });

        // Set the click listener for the negative button
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            new AlertDialog.Builder(PlacesActivity.this)
                    .setTitle("Delete Place")
                    .setMessage("Are you sure you want to delete this place?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        dbHelper.deletePlace(place.getId());
                        places.remove(place);
                        adapter.notifyDataSetChanged();
                        updateMap();
                        dialog.dismiss();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void showUpdateDialog_orig(Place place) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Place");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_update_place, null);
        builder.setView(dialogView);

        final Spinner nameSpinner = dialogView.findViewById(R.id.nameSpinner);
        final EditText addressInput = dialogView.findViewById(R.id.addressInput);
        final Spinner typeSpinner = dialogView.findViewById(R.id.typeSpinner);
        final EditText countryInput = dialogView.findViewById(R.id.countryInput);
        final TextView visitsTextView = dialogView.findViewById(R.id.visitsTextView);

        addressInput.setText(place.getAddress());
        countryInput.setText(place.getCountry());
        visitsTextView.setText(String.format("Visits: %d", place.getNumberOfVisits()));

        // Get more detailed address information
        List<String> nameOptions = getAddressOptions(place.getLat(), place.getLon());
        ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nameOptions);
        nameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        nameSpinner.setAdapter(nameAdapter);

        // Set the current name in the spinner
        int namePosition = nameOptions.indexOf(place.getName());
        if (namePosition != -1) {
            nameSpinner.setSelection(namePosition);
        }

        // Set up the type spinner
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.place_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        // Set the current type in the spinner
        int typePosition = typeAdapter.getPosition(place.getType());
        if (typePosition != -1) {
            typeSpinner.setSelection(typePosition);
        }

        builder.setPositiveButton("Update", null);
        builder.setNegativeButton("Delete", null);
        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());

        final AlertDialog dialog = builder.create();
        dialog.show();

        // Set the click listener for the positive button
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = nameSpinner.getSelectedItem() != null ? nameSpinner.getSelectedItem().toString() : "";
            String newAddress = addressInput.getText().toString().trim();
            String newType = typeSpinner.getSelectedItem() != null ? typeSpinner.getSelectedItem().toString() : "";
            String newCountry = countryInput.getText().toString().trim();

            if (newName.isEmpty() || newAddress.isEmpty() || newType.isEmpty() || newCountry.isEmpty()) {
                Toast.makeText(PlacesActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            place.setName(newName);
            place.setAddress(newAddress);
            place.setType(newType);
            place.setCountry(newCountry);
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
            dialog.dismiss();
        });

        // Set the click listener for the negative button
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            new AlertDialog.Builder(PlacesActivity.this)
                    .setTitle("Delete Place")
                    .setMessage("Are you sure you want to delete this place?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        dbHelper.deletePlace(place.getId());
                        places.remove(place);
                        adapter.notifyDataSetChanged();
                        updateMap();
                        dialog.dismiss();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private List<String> getAddressOptions(double latitude, double longitude) {
        List<String> options = new ArrayList<>();
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Short form
                String shortForm = address.getFeatureName();
                if (shortForm != null && !shortForm.isEmpty()) {
                    options.add(shortForm);
                }

                // Medium form
                String mediumForm = address.getThoroughfare();
                if (mediumForm != null && !mediumForm.isEmpty()) {
                    options.add(mediumForm);
                }

                // Long form
                String longForm = address.getAddressLine(0);
                if (longForm != null && !longForm.isEmpty()) {
                    options.add(longForm);
                }

                // Add more options if needed
                String locality = address.getLocality();
                if (locality != null && !locality.isEmpty()) {
                    options.add(locality);
                }

                String subLocality = address.getSubLocality();
                if (subLocality != null && !subLocality.isEmpty()) {
                    options.add(subLocality);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // If no options were added, add a default option
        if (options.isEmpty()) {
            options.add("Unknown Location");
        }

        return options;
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
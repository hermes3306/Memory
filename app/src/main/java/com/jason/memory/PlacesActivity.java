package com.jason.memory;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import android.location.Address;
import android.location.Geocoder;
import java.util.List;
import java.util.Locale;
import java.io.IOException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdate;
import android.view.ViewGroup;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.graphics.Typeface;

public class PlacesActivity extends AppCompatActivity implements OnMapReadyCallback {
    private RecyclerView recyclerView;
    private PlacesAdapter adapter;
    private List<Place> places;
    private DatabaseHelper dbHelper;
    private GoogleMap mMap;
    private ImageButton syncButton;
    private List<Locale> supportedLocales;

    private static final int DEFAULT_ZOOM = 12;
    private static final int MIN_ZOOM = 3;
    private static final int MAX_ZOOM = 21;
    private int currentZoom = DEFAULT_ZOOM;
    private boolean isMapSizeReduced = false;

    private List<Locale> getSupportedLocales() {
        List<Locale> locales = new ArrayList<>();
        locales.add(Locale.ENGLISH);
        locales.add(Locale.KOREAN);
        locales.add(Locale.CHINESE);
        locales.add(Locale.JAPANESE);
        return locales;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);

        dbHelper = new DatabaseHelper(this);
        dbHelper.createPlacesTable();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PlacesAdapter(this::onPlaceClick, dbHelper);
        recyclerView.setAdapter(adapter);

        // Initialize supported locales
        supportedLocales = Arrays.asList(Locale.ENGLISH, Locale.KOREAN, Locale.CHINESE, Locale.JAPANESE);


        // Initialize the places list
        places = dbHelper.getAllPlaces();
        adapter.submitList(places);

        syncButton = findViewById(R.id.syncButton);
        syncButton.setOnClickListener(v -> syncWithServer());

        ImageButton addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> addNewPlace());

        ImageButton searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> searchPlace());

        ImageButton saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> savePlace());


        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        updateMap(); // Call this to set up initial map state
    }

    private void onPlaceClick(Place place) {
        showUpdateDialog(place);
    }

    private void syncWithServer() {
        new AlertDialog.Builder(this)
                .setTitle("Sync with Server")
                .setMessage("Do you want to download and merge the latest data from the server?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Utility.downloadJsonAndMergeServerData(this, "json", dbHelper, this::onSyncComplete);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void onSyncComplete(boolean success) {
        runOnUiThread(() -> {
            if (success) {
                Toast.makeText(this, "Sync completed successfully", Toast.LENGTH_SHORT).show();
                // Refresh the list of places
                List<Place> updatedPlaces = dbHelper.getAllPlaces();
                adapter.submitList(updatedPlaces);
                updateMap();
            } else {
                Toast.makeText(this, "Sync failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePlace() {
        try {
            // 1. Save JSON format for all places
            List<Place> allPlaces = dbHelper.getAllPlaces();
            Gson gson = new Gson();
            String jsonPlaces = gson.toJson(allPlaces);

            // Create file in the download directory
            File directory = new File(Config.getDownloadDir(),"json");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String fileName = "places_" + System.currentTimeMillis() + ".json";
            File file = new File(directory, fileName);

            // Write JSON to file
            FileWriter writer = new FileWriter(file);
            writer.write(jsonPlaces);
            writer.close();

            // 2. Upload file to server
            Utility.uploadFile(this, file);

            // 3. Show toast for save result
            Toast.makeText(this, "Places saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving places: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void searchPlace() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search Places");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_search_place, null);
        builder.setView(dialogView);

        final EditText nameInput = dialogView.findViewById(R.id.searchNameInput);
        final EditText addressInput = dialogView.findViewById(R.id.searchAddressInput);
        final Spinner typeSpinner = dialogView.findViewById(R.id.searchTypeSpinner);
        final EditText memoInput = dialogView.findViewById(R.id.searchMemoInput);

        // Set up the type spinner
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.place_types, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(spinnerAdapter);
        typeSpinner.setSelection(0); // Set default selection to first item (which could be "All" or "")

        builder.setPositiveButton("Search", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String address = addressInput.getText().toString().trim();
            String type = typeSpinner.getSelectedItemPosition() == 0 ? "" : typeSpinner.getSelectedItem().toString();
            String memo = memoInput.getText().toString().trim();

            try {
                // Perform the search
                List<Place> searchResults = dbHelper.searchPlaces(name, address, type, memo);

                // Update the RecyclerView with search results
                adapter.submitList(searchResults);

                // Update the map
                updateMap();

                dialog.dismiss();

                // Show a toast with the number of results
                Toast.makeText(PlacesActivity.this,
                        searchResults.size() + " places found",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(PlacesActivity.this,
                        "Error updating places: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }


    private void updateMap() {
        if (mMap == null) return;
        mMap.clear();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        List<Place> currentList = adapter.getCurrentList();
        for (Place place : currentList) {
            LatLng latLng = new LatLng(place.getLat(), place.getLon());
            mMap.addMarker(new MarkerOptions().position(latLng).title(place.getName()));
            builder.include(latLng);
        }
        if (!currentList.isEmpty()) {
            LatLngBounds bounds = builder.build();
            int padding = 100;
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.animateCamera(cu);

            // Adjust zoom level based on the number of places
            int placesCount = currentList.size();
            if (placesCount > 100) {
                currentZoom = 10;
            } else if (placesCount > 50) {
                currentZoom = 11;
            } else if (placesCount > 20) {
                currentZoom = 12;
            } else if (placesCount > 10) {
                currentZoom = 13;
            } else {
                currentZoom = 14;
            }

            mMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoom));
        }
    }

    private void zoomIn() {
        if (mMap != null && currentZoom < MAX_ZOOM) {
            currentZoom++;
            mMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoom));
        }
    }

    private void zoomOut() {
        if (mMap != null && currentZoom > MIN_ZOOM) {
            currentZoom--;
            mMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoom));
        }
    }

    private void toggleMapSize() {
        View mapFragment = findViewById(R.id.map);
        ViewGroup.LayoutParams params = mapFragment.getLayoutParams();

        if (isMapSizeReduced) {
            // Restore original size
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            isMapSizeReduced = false;
        } else {
            // Reduce size
            params.height = mapFragment.getHeight() / 2;
            isMapSizeReduced = true;
        }

        mapFragment.setLayoutParams(params);
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
        final Spinner localeSpinner = dialogView.findViewById(R.id.localeSpinner);

        // Set up the spinner with place types
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.place_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        // Set up the locale spinner
        ArrayAdapter<String> localeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        localeAdapter.add("-");
        for (Locale locale : supportedLocales) {
            localeAdapter.add(locale.getDisplayLanguage(Locale.ENGLISH));
        }
        localeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        localeSpinner.setAdapter(localeAdapter);

        // Get the last known location from DatabaseHelper
        LatLng lastLocation = dbHelper.getLastKnownLocation();

        // Get address information using Geocoder
        if (lastLocation != null) {
            updateAddressInfo(lastLocation, nameInput, addressInput, countryInput, localeSpinner);
        }

        // Set up listener for localeSpinner
        localeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && lastLocation != null) {
                    Locale selectedLocale = supportedLocales.get(position - 1);
                    updateAddressInfo(lastLocation, nameInput, addressInput, countryInput, selectedLocale);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setPositiveButton("Save", null);

        final AlertDialog dialog = builder.create();
        dialog.show();

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

                // Update the adapter with the new list
                List<Place> updatedPlaces = dbHelper.getAllPlaces();
                this.adapter.submitList(updatedPlaces);  // Use 'this.adapter' to refer to the class member

                updateMap();
                dialog.dismiss();

                // Show a confirmation message
                Toast.makeText(PlacesActivity.this, "New place added successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PlacesActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAddressInfo(LatLng location, EditText nameInput, EditText addressInput,
                                   EditText countryInput, Locale locale) {
        Geocoder geocoder = new Geocoder(this, locale);
        try {
            List<Address> addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                String shortName = address.getFeatureName() != null ? address.getFeatureName() : address.getSubThoroughfare();
                String fullAddress = address.getAddressLine(0);
                String country = address.getCountryName();

                if (nameInput != null) nameInput.setText(shortName);
                addressInput.setText(fullAddress);
                countryInput.setText(country);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to get address information", Toast.LENGTH_SHORT).show();
        }
    }


    private void updateAddressInfo(LatLng location, EditText nameInput, EditText addressInput,
                                   EditText countryInput, Spinner localeSpinner) {
        Locale locale = getSupportedLocales().get(localeSpinner.getSelectedItemPosition());
        updateAddressInfo(location, nameInput, addressInput, countryInput, locale);
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
        final Spinner localeSpinner = dialogView.findViewById(R.id.localeSpinner);
        final TextView visitedDateView = dialogView.findViewById(R.id.vistedDateView);
        final EditText memoInput = dialogView.findViewById(R.id.memoInput);  // Add this line

        // Set initial values
        nameInput.setText(place.getName());
        addressInput.setText(place.getAddress());
        countryInput.setText(place.getCountry());
        visitsTextView.setText(String.format("Visits: %d", place.getNumberOfVisits()));
        memoInput.setText(place.getMemo());  // Add this line

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String visitedDates = String.format("First: %s\nLast: %s",
                sdf.format(new Date(place.getFirstVisited())),
                sdf.format(new Date(place.getLastVisited())));
        visitedDateView.setText(visitedDates);

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

        // Set up listener for localeSpinner
        localeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Locale selectedLocale = supportedLocales.get(position - 1);
                    updateAddressInfo(new LatLng(place.getLat(), place.getLon()), addressInput, countryInput, selectedLocale);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setPositiveButton("Update", null);
        builder.setNegativeButton("Delete", null);
        builder.setNeutralButton("Visit", null);

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();
            String newAddress = addressInput.getText().toString().trim();
            String newType = typeSpinner.getSelectedItem() != null ? typeSpinner.getSelectedItem().toString() : "";
            String newCountry = countryInput.getText().toString().trim();
            String newMemo = memoInput.getText().toString().trim();  // Add this line

            if (newName.isEmpty() || newAddress.isEmpty() || newType.isEmpty() || newCountry.isEmpty()) {
                Toast.makeText(PlacesActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            place.setName(newName);
            place.setAddress(newAddress);
            place.setType(newType);
            place.setCountry(newCountry);
            place.setMemo(newMemo);  // Add this line

            if (dbHelper.updatePlace(place) > 0) {
                adapter.notifyDataSetChanged();
                updateMap();
                dialog.dismiss();
            } else {
                Toast.makeText(PlacesActivity.this, "Failed to update place", Toast.LENGTH_SHORT).show();
            }
        });


        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            new AlertDialog.Builder(PlacesActivity.this)
                    .setTitle("Delete Place")
                    .setMessage("Are you sure you want to delete this place?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        dbHelper.deletePlace(place.getId());
                        List<Place> currentList = new ArrayList<>(adapter.getCurrentList());
                        currentList.remove(place);
                        adapter.submitList(currentList);
                        updateMap();
                        dialog.dismiss();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            place.setNumberOfVisits(place.getNumberOfVisits() + 1);
            place.setLastVisited(System.currentTimeMillis());
            if (dbHelper.updatePlace(place) > 0) {
                visitsTextView.setText(String.format("Visits: %d", place.getNumberOfVisits()));
                String updatedDates = String.format("First: %s\nLast: %s",
                        sdf.format(new Date(place.getFirstVisited())),
                        sdf.format(new Date(place.getLastVisited())));
                visitedDateView.setText(updatedDates);
                Toast.makeText(PlacesActivity.this, "Visit recorded", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PlacesActivity.this, "Failed to record visit", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAddressInfo(LatLng location, EditText addressInput, EditText countryInput, Locale locale) {
        Geocoder geocoder = new Geocoder(this, locale);
        try {
            List<Address> addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);
                String country = address.getCountryName();

                addressInput.setText(fullAddress);
                countryInput.setText(country);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to get address information", Toast.LENGTH_SHORT).show();
        }
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


}
package com.jason.memory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
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
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;

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
    private EditText searchEditText;
    private static String TAG = "PlacesActivity";
    private Context context;

    private List<Locale> getSupportedLocales() {
        List<Locale> locales = new ArrayList<>();
        locales.add(Locale.ENGLISH);
        locales.add(Locale.KOREAN);
        locales.add(Locale.CHINESE);
        locales.add(Locale.JAPANESE);
        return locales;
    }

    private void onUserIdClick(String userId) {
        // Handle user ID click, e.g., open a user profile page
        Toast.makeText(this, "User ID clicked: " + userId, Toast.LENGTH_SHORT).show();
    }

    private void showUserProfileDialog(String userId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_profile, null);
        builder.setView(dialogView);

        CircleImageView profileImageView = dialogView.findViewById(R.id.profileImageView);
        TextView userIdTextView = dialogView.findViewById(R.id.userIdTextView);
        TextView userInfoTextView = dialogView.findViewById(R.id.userInfoTextView);

        userIdTextView.setText(userId);

        // Load user profile image
        String profileImageUrl = getUserProfileImageUrl(userId);
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.default_profile);
        }

        // Load user info
        String userInfo = getUserInfo(userId);
        userInfoTextView.setText(userInfo);

        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String getUserProfileImageUrl(String userId) {
        // Implement this method to return the correct image URL for the user
        // For example: return "https://your-api.com/user-images/" + userId + ".jpg";
        return Config.PROFILE_BASE_URL + userId + ".jpg";
    }

    private String getUserInfo(String userId) {
        // Implement this method to return user information
        // For now, return a placeholder text
        return "User information for " + userId;
    }

    private void openFullScreenImage(ArrayList<String> imageUrls, int position, boolean isProfileImage) {
        Log.d(TAG, "--m-- Opening full screen image: position " + position + ", isProfileImage: " + isProfileImage);
        Intent intent = new Intent(this, FullScreenImageActivity.class);
        intent.putStringArrayListExtra("IMAGE_URLS", imageUrls);
        intent.putExtra("POSITION", position);
        intent.putExtra("IS_PROFILE_IMAGE", isProfileImage);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);

        dbHelper = new DatabaseHelper(this);
        dbHelper.createPlacesTable();

        context = this.getApplicationContext();
        Log.d(TAG, "--m-- Current user ID: " + getUserId());

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PlacesAdapter(new PlacesAdapter.OnPlaceClickListener() {
            @Override
            public void onPlaceClick(Place place) {
                PlacesActivity.this.onPlaceClick(place);
            }

            @Override
            public void onUserIdClick(String userId) {
                PlacesActivity.this.showUserProfileDialog(userId);
            }

            @Override
            public void onProfileImageClick(String imageUrl) {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    ArrayList<String> imageUrls = new ArrayList<>();
                    imageUrls.add(imageUrl);
                    PlacesActivity.this.openFullScreenImage(imageUrls, 0, true);
                } else {
                    Toast.makeText(PlacesActivity.this, "No profile image available", Toast.LENGTH_SHORT).show();
                }
            }
        }, dbHelper, this);

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

        searchEditText = findViewById(R.id.searchEditText);
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                performSearch();
                return true;
            }
            return false;
        });



        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupBottomNavigation();
        updatePlacesList();

    }

    private void updatePlacesList() {
        places = dbHelper.getAllPlaces();
        Log.d(TAG, "--m-- Fetched " + places.size() + " places from database");
        for (Place place : places) {
            Log.d(TAG, "--m-- Place: " + place.getName() + ", User ID: " + place.getUserId());
        }
        adapter.submitList(places);
    }


    private void setupBottomNavigation() {
        // Find layout views
        View chatLayout = findViewById(R.id.chatLayout);
        View runLayout = findViewById(R.id.runLayout);
        View memoryLayout = findViewById(R.id.memoryLayout);
        View placeLayout = findViewById(R.id.placeLayout);
        View meLayout = findViewById(R.id.meLayout);

        // Find icon views
        ImageView chatIcon = findViewById(R.id.iconChat);
        ImageView runIcon = findViewById(R.id.iconRun);
        ImageView memoryIcon = findViewById(R.id.iconMemory);
        ImageView placeIcon = findViewById(R.id.iconPlace);
        ImageView meIcon = findViewById(R.id.iconMe);

        // Set default icon colors
        chatIcon.setImageResource(R.drawable.ht_chat);
        runIcon.setImageResource(R.drawable.ht_run);
        memoryIcon.setImageResource(R.drawable.ht_memory);
        placeIcon.setImageResource(R.drawable.ht_place_blue);
        meIcon.setImageResource(R.drawable.ht_my);

        // Add click listeners for bottom navigation layouts
        chatLayout.setOnClickListener(v -> openChatActivity());
        runLayout.setOnClickListener(v -> openListActivityActivity());
        memoryLayout.setOnClickListener(v -> openMemoryActivity());
        //placeLayout.setOnClickListener(v -> openPlacesActivity());
        meLayout.setOnClickListener(v -> openSettingActivity());
    }

    private void performSearch() {
        String searchText = searchEditText.getText().toString().trim();
        if (!searchText.isEmpty()) {
            try {
                List<Place> searchResults = dbHelper.searchPlacesByText(searchText);
                adapter.submitList(searchResults);
                updateMap();
                Toast.makeText(this, searchResults.size() + " places found", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error searching places: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openChatActivity() {
        Intent intent = new Intent(this, ChatActivity.class);
        startActivity(intent);
    }

    private void openListActivityActivity() {
        Intent intent = new Intent(this, ListActivityActivity.class);
        startActivity(intent);
    }

    private void openMemoryActivity() {
        Intent intent = new Intent(this, MemoryActivity.class);
        startActivity(intent);
    }

    private void openSettingActivity() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sync with Server")
                .setItems(new CharSequence[]{"Upload", "Download", "Both"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Upload
                            savePlace();
                            break;
                        case 1: // Download
                            downloadFromServer();
                            break;
                        case 2: // Both
                            savePlace();
                            downloadFromServer();
                            break;
                    }
                });
        builder.create().show();
    }

    private void downloadFromServer() {
        Utility.downloadJsonAndMergeServerData(this, Config.PLACE_EXT, dbHelper, this::onSyncComplete);
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
            List<Place> allPlaces = dbHelper.getAllPlaces();
            for (Place place : allPlaces) {
                if (place.getUserId() == null || place.getUserId().isEmpty()) {
                    place.setUserId(Utility.getCurrentUser(this));
                }
            }
            Gson gson = new Gson();
            String jsonPlaces = gson.toJson(allPlaces);

            // Create file in the download directory
            File directory = Config.getDownloadDir4Places(this);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String fileName = System.currentTimeMillis() + Config.PLACE_EXT;
            File file = new File(directory, fileName);

            // Write JSON to file
            FileWriter writer = new FileWriter(file);
            writer.write(jsonPlaces);
            writer.close();

            // Upload file to server
            Utility.uploadFile(this, file);

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

        final Switch searchTypeSwitch = dialogView.findViewById(R.id.searchTypeSwitch);
        final LinearLayout contentSearchLayout = dialogView.findViewById(R.id.contentSearchLayout);
        final LinearLayout distanceSearchLayout = dialogView.findViewById(R.id.distanceSearchLayout);

        final EditText nameInput = dialogView.findViewById(R.id.searchNameInput);
        final EditText addressInput = dialogView.findViewById(R.id.searchAddressInput);
        final Spinner distanceSpinner = dialogView.findViewById(R.id.searchDistanceSpinner);
        final Spinner typeSpinner = dialogView.findViewById(R.id.searchTypeSpinner);
        final EditText memoInput = dialogView.findViewById(R.id.searchMemoInput);

        // Set up the distance spinner
        ArrayAdapter<CharSequence> distanceAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"1km", "2km", "5km", "10km", "20km", "+20km"});
        distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        distanceSpinner.setAdapter(distanceAdapter);

        // Set up the type spinner
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.place_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        searchTypeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            contentSearchLayout.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            distanceSearchLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        builder.setPositiveButton("Search", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean isDistanceSearch = searchTypeSwitch.isChecked();

            try {
                List<Place> searchResults;
                if (isDistanceSearch) {
                    String distanceStr = distanceSpinner.getSelectedItem().toString();
                    int distance = distanceStr.equals("+20km") ? Integer.MAX_VALUE :
                            Integer.parseInt(distanceStr.replace("km", ""));

                    LocationData locationData = dbHelper.getLatestLocation();
                    LatLng currentLocation = new LatLng(locationData.getLatitude(), locationData.getLongitude());
                    searchResults = dbHelper.searchPlacesByDistance(currentLocation, distance);
                } else {
                    String name = nameInput.getText().toString().trim();
                    String address = addressInput.getText().toString().trim();
                    String type = typeSpinner.getSelectedItem().toString();
                    String memo = memoInput.getText().toString().trim();

                    searchResults = dbHelper.searchPlaces(name, address, type, memo);
                }

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
                        "Error searching places: " + e.getMessage(),
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


                String userId = getUserId();
                Log.d(TAG, "--m-- Adding new place with user ID: " + userId);
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
                        0.0, // altitude
                        "", // memo
                        userId // Set the user ID
                );

                long id = dbHelper.addPlace(newPlace);
                newPlace.setId(id);

                // Update the adapter with the new list
                List<Place> updatedPlaces = dbHelper.getAllPlaces();
                this.adapter.submitList(updatedPlaces);

                updateMap();
                dialog.dismiss();

                // Show a confirmation message
                Toast.makeText(PlacesActivity.this, "New place added successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PlacesActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getUserId() {
        return Utility.getCurrentUser(context);
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
        final TextView addressInput = dialogView.findViewById(R.id.addressInput);
        final Spinner typeSpinner = dialogView.findViewById(R.id.typeSpinner);
        final TextView countryInput = dialogView.findViewById(R.id.countryInput);
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
            place.setMemo(newMemo);
            place.setUserId(getUserId());

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

    private void updateAddressInfo(LatLng location, TextView addressInput, TextView countryInput, Locale locale) {
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
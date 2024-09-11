package com.jason.memory;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import android.graphics.Color;
import android.location.Location;

public class PlacesAdapter extends ListAdapter<Place, PlacesAdapter.PlaceViewHolder> {
    private OnPlaceClickListener listener;
    private DatabaseHelper dbHelper;

    // Define the OnPlaceClickListener interface
    public interface OnPlaceClickListener {
        void onPlaceClick(Place place);
    }

    public PlacesAdapter(OnPlaceClickListener listener, DatabaseHelper dbHelper) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.dbHelper = dbHelper;
    }

    private static final DiffUtil.ItemCallback<Place> DIFF_CALLBACK = new DiffUtil.ItemCallback<Place>() {
        @Override
        public boolean areItemsTheSame(@NonNull Place oldItem, @NonNull Place newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Place oldItem, @NonNull Place newItem) {
            return oldItem.equals(newItem);
        }
    };

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        Place place = getItem(position);
        holder.bind(place, listener);
    }


    public class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvDate;
        TextView tvAddress;
        TextView tvVisits;
        TextView tvMemo;  // Add this line
        MapView mapView;

        public PlaceViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvVisits = itemView.findViewById(R.id.tvVisits);
            tvMemo = itemView.findViewById(R.id.tvMemo);  // Add this line
            mapView = itemView.findViewById(R.id.mapView);

            if (mapView != null) {
                mapView.onCreate(null);
                mapView.onResume();
            }
        }

        public void bind(final Place place, final OnPlaceClickListener listener) {
            if (tvName != null) tvName.setText(place.getName());
            if (tvDate != null) {
                String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date(place.getLastVisited()));
                tvDate.setText(formattedDate);
            }
            if (tvAddress != null) tvAddress.setText(place.getAddress());
            if (tvVisits != null) tvVisits.setText("Visits: " + place.getNumberOfVisits());
            if (tvMemo != null) tvMemo.setText(place.getMemo());  // Add this line

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaceClick(place);
                }
            });

            if (mapView != null) {
                mapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        MapsInitializer.initialize(mapView.getContext());
                        LatLng placeLocation = new LatLng(place.getLat(), place.getLon());
                        LatLng currentLocation = dbHelper.getLastKnownLocation();

                        // Clear any existing markers
                        googleMap.clear();

                        // Add marker for the place (most recently updated place)
                        googleMap.addMarker(new MarkerOptions()
                                .position(placeLocation)
                                .title(place.getName())
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                        // Add marker for current location
                        googleMap.addMarker(new MarkerOptions()
                                .position(currentLocation)
                                .title("Current Location")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                        // Draw line between current location and place
                        PolylineOptions lineOptions = new PolylineOptions()
                                .add(currentLocation, placeLocation)
                                .width(3)
                                .color(Color.BLUE);
                        googleMap.addPolyline(lineOptions);

                        // Calculate distance
                        float[] results = new float[1];
                        Location.distanceBetween(currentLocation.latitude, currentLocation.longitude,
                                placeLocation.latitude, placeLocation.longitude, results);
                        float distanceInMeters = results[0];
                        double distanceInKm = distanceInMeters / 1000.0;

                        // Add distance information as a marker
                        LatLng midPoint = new LatLng(
                                (currentLocation.latitude + placeLocation.latitude) / 2,
                                (currentLocation.longitude + placeLocation.longitude) / 2
                        );

                        // Create a custom view for the distance information
                        TextView distanceView = new TextView(mapView.getContext());
                        distanceView.setText(String.format("%.2f km", distanceInKm));
                        distanceView.setTextColor(Color.BLACK);
                        distanceView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                        distanceView.setBackgroundColor(Color.CYAN);
                        distanceView.setPadding(8, 4, 8, 4);

                        // Convert the view to a bitmap
                        distanceView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                        Bitmap distanceBitmap = Bitmap.createBitmap(distanceView.getMeasuredWidth(), distanceView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(distanceBitmap);
                        distanceView.layout(0, 0, distanceView.getMeasuredWidth(), distanceView.getMeasuredHeight());
                        distanceView.draw(canvas);

                        // Add the distance bitmap as a marker
                        googleMap.addMarker(new MarkerOptions()
                                .position(midPoint)
                                .icon(BitmapDescriptorFactory.fromBitmap(distanceBitmap))
                                .anchor(0.5f, 0.5f));

                        // Calculate bounds with extra padding
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(placeLocation);
                        builder.include(currentLocation);
                        LatLngBounds bounds = builder.build();

                        // Move camera to show both locations with extra padding
                        int padding = 200; // Adjust this value to increase or decrease the padding
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

                        // Enable zoom controls
                        googleMap.getUiSettings().setZoomControlsEnabled(true);

                        // Enable and force show compass
                        googleMap.getUiSettings().setCompassEnabled(true);
                        googleMap.setOnCameraMoveStartedListener(reason -> {
                            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                                googleMap.getUiSettings().setCompassEnabled(false);
                                googleMap.getUiSettings().setCompassEnabled(true);
                            }
                        });

                        // Enable gestures for zooming and panning
                        googleMap.getUiSettings().setAllGesturesEnabled(true);

                        // Set the map type to NORMAL
                        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    }
                });
            }
        }
    }
}
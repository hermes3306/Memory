package com.jason.memory;

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
        MapView mapView;

        public PlaceViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvVisits = itemView.findViewById(R.id.tvVisits);
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

                        // Move camera to show both locations
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(placeLocation);
                        builder.include(currentLocation);
                        LatLngBounds bounds = builder.build();
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

                        // Add marker for the place
                        googleMap.addMarker(new MarkerOptions().position(placeLocation).title(place.getName()));

                        // Add marker for current location with green color
                        googleMap.addMarker(new MarkerOptions()
                                .position(currentLocation)
                                .title("Current Location")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));

                        // Draw line between current location and place
                        PolylineOptions lineOptions = new PolylineOptions()
                                .add(currentLocation, placeLocation)
                                .width(3)
                                .color(Color.BLUE);
                        Polyline polyline = googleMap.addPolyline(lineOptions);

                        // Calculate distance
                        float[] results = new float[1];
                        Location.distanceBetween(currentLocation.latitude, currentLocation.longitude,
                                placeLocation.latitude, placeLocation.longitude, results);
                        float distanceInMeters = results[0];
                        double distanceInKm = distanceInMeters / 1000.0;

                        // Add marker with distance information
                        LatLng midPoint = new LatLng(
                                (currentLocation.latitude + placeLocation.latitude) / 2,
                                (currentLocation.longitude + placeLocation.longitude) / 2
                        );
                        googleMap.addMarker(new MarkerOptions()
                                .position(midPoint)
                                .title(String.format("%.2f km", distanceInKm))
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                        googleMap.getUiSettings().setAllGesturesEnabled(false);
                    }
                });
            }
        }
    }
}
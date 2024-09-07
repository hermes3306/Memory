package com.jason.memory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
import java.util.List;
import java.util.Locale;

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {
    private List<Place> places;
    private OnPlaceClickListener listener;

    public PlacesAdapter(List<Place> places, OnPlaceClickListener listener) {
        this.places = places;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        Place place = places.get(position);
        if (holder.tvName != null) holder.tvName.setText(place.getName());
        if (holder.tvDate != null) {
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date(place.getLastVisited()));
            holder.tvDate.setText(formattedDate);
        }
        if (holder.tvAddress != null) holder.tvAddress.setText(place.getAddress());
        if (holder.tvVisits != null) holder.tvVisits.setText("Visits: " + place.getNumberOfVisits());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaceClick(place);
            }
        });

        // Setup MapView
        if (holder.mapView != null) {
            holder.mapView.onCreate(null);
            holder.mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    MapsInitializer.initialize(holder.mapView.getContext());
                    LatLng placeLocation = new LatLng(place.getLat(), place.getLon());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(placeLocation, 15f));
                    googleMap.addMarker(new MarkerOptions().position(placeLocation).title(place.getName()));
                    googleMap.getUiSettings().setAllGesturesEnabled(false);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return places.size();
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
        }
    }

    public interface OnPlaceClickListener {
        void onPlaceClick(Place place);
    }
}
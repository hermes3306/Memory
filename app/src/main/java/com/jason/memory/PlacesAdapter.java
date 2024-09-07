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

public class PlacesAdapter extends ListAdapter<Place, PlacesAdapter.PlaceViewHolder> {
    private OnPlaceClickListener listener;

    public PlacesAdapter(OnPlaceClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
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

            // Setup MapView
            if (mapView != null) {
                mapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        MapsInitializer.initialize(mapView.getContext());
                        LatLng placeLocation = new LatLng(place.getLat(), place.getLon());
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(placeLocation, 15f));
                        googleMap.addMarker(new MarkerOptions().position(placeLocation).title(place.getName()));
                        googleMap.getUiSettings().setAllGesturesEnabled(false);
                    }
                });
            }
        }
    }

    public interface OnPlaceClickListener {
        void onPlaceClick(Place place);
    }
}
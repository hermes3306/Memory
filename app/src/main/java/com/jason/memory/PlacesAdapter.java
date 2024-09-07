package com.jason.memory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

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
        holder.bind(place, listener);
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, addressTextView, visitsTextView;

        PlaceViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            addressTextView = itemView.findViewById(R.id.addressTextView);
            visitsTextView = itemView.findViewById(R.id.visitsTextView);
        }

        void bind(final Place place, final OnPlaceClickListener listener) {
            nameTextView.setText(place.getName());
            addressTextView.setText(place.getAddress());
            visitsTextView.setText("Visits: " + place.getNumberOfVisits());
            itemView.setOnClickListener(v -> listener.onPlaceClick(place));
        }
    }

    public interface OnPlaceClickListener {
        void onPlaceClick(Place place);
    }
}

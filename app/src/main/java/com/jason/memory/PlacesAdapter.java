
package com.jason.memory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.MapView;
import de.hdodenhof.circleimageview.CircleImageView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.util.TypedValue;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.graphics.Color;
import android.location.Location;
import androidx.core.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.ViewGroup;

public class PlacesAdapter extends ListAdapter<Place, PlacesAdapter.PlaceViewHolder> {
    private OnPlaceClickListener listener;
    private DatabaseHelper dbHelper;
    private Context context;


    public interface OnPlaceClickListener {
        void onPlaceClick(Place place);
        void onUserIdClick(String userId);
        void onProfileImageClick(String imageUrl);
        void onLikeClick(Place place);
        void onCommentClick(Place place);
        void onShowWhoLikes(Place place);
    }

    public PlacesAdapter(OnPlaceClickListener listener, DatabaseHelper dbHelper, Context context) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.dbHelper = dbHelper;
        this.context = context;
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
        holder.likeCount.setText(String.valueOf(place.getLikeCount()));
        holder.commentCount.setText(String.valueOf(place.getCommentCount()));

        int likeCount = place.getLikeCount();
        holder.likeCount.setText(String.valueOf(likeCount));

        String currentUserId = Utility.getCurrentUser(holder.itemView.getContext());
        boolean isLiked = place.getWhoLikes() != null && place.getWhoLikes().contains(currentUserId);

        int color;
        if (isLiked) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.liked_color);
        } else {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.unliked_color);
        }
        holder.likeIcon.setColorFilter(color);

        holder.likeIcon.setOnClickListener(v -> {
            if (listener != null) {  // Change this line
                listener.onLikeClick(place);  // Change this line
            }
        });

        holder.bind(place, listener);
    }

    public class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserId;
        TextView tvName;
        TextView tvDate;
        TextView tvAddress;
        TextView tvVisits;
        TextView tvMemo;
        MapView mapView;
        CircleImageView profileImageView;
        ImageView countryFlagIcon;
        ImageView placeTypeIcon;
        LinearLayout commentsContainer;

        ImageView likeIcon;
        TextView likeCount;
        ImageView commentIcon;
        TextView commentCount;
        TextView commentsTextView;


        public PlaceViewHolder(View itemView) {
            super(itemView);
            commentsContainer = itemView.findViewById(R.id.commentsContainer);
            tvUserId = itemView.findViewById(R.id.tvUserId);
            tvName = itemView.findViewById(R.id.tvName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvVisits = itemView.findViewById(R.id.tvVisits);
            tvMemo = itemView.findViewById(R.id.tvMemo);
            countryFlagIcon = itemView.findViewById(R.id.countryFlagIcon);
            placeTypeIcon = itemView.findViewById(R.id.placeTypeIcon);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            likeCount = itemView.findViewById(R.id.likeCount);
            commentIcon = itemView.findViewById(R.id.commentIcon);
            commentCount = itemView.findViewById(R.id.commentCount);

            mapView = itemView.findViewById(R.id.mapView);
            profileImageView = itemView.findViewById(R.id.profileImageView);

            if (mapView != null) {
                mapView.onCreate(null);
                mapView.onResume();
            }
        }

        private void setCountryFlag(String country) {
            // Default to Korean flag
            int flagResourceId = R.drawable.flag_korea;
             if ("China".equalsIgnoreCase(country)) {
                 flagResourceId = R.drawable.flag_china;
             } else if ("Japan".equalsIgnoreCase(country)) {
                 flagResourceId = R.drawable.flag_japan;
             }

            countryFlagIcon.setImageResource(flagResourceId);
        }

        private void setPlaceTypeIcon(String type) {
            int iconResourceId;
            switch (type.toLowerCase()) {
                case "restaurant":
                    iconResourceId = R.drawable.ic_restaurant;
                    break;
                case "office":
                    iconResourceId = R.drawable.ic_office;
                    break;
                case "airport":
                    iconResourceId = R.drawable.ic_airport;
                    break;
                case "hotel":
                    iconResourceId = R.drawable.ic_hotel;
                    break;
                case "street":
                    iconResourceId = R.drawable.ic_street;
                    break;
                case "house":
                    iconResourceId = R.drawable.ic_house;
                    break;
                case "department":
                    iconResourceId = R.drawable.ic_department;
                    break;
                case "shop":
                    iconResourceId = R.drawable.ic_shop;
                    break;
                case "park":
                    iconResourceId = R.drawable.ic_park;
                    break;
                case "other business":
                    iconResourceId = R.drawable.ic_otherbusiness;
                    break;
                case "other":
                default:
                    iconResourceId = R.drawable.ic_place;
                    break;
            }
            placeTypeIcon.setImageResource(iconResourceId);
        }


        public void bind(final Place place, final OnPlaceClickListener listener) {
            final String userId = place.getUserId() != null && !place.getUserId().isEmpty()
                    ? place.getUserId()
                    : "Unknown";

            Log.d("PlacesAdapter", "--m-- Binding place with user ID: " + userId);

            tvUserId.setText(userId);
            tvUserId.setOnClickListener(v -> listener.onUserIdClick(userId));

            if (tvName != null) tvName.setText(place.getName());
            setCountryFlag(place.getCountry());
            setPlaceTypeIcon(place.getType());

            if (tvDate != null) {
                String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date(place.getLastVisited()));
                tvDate.setText(formattedDate);
            }
            if (tvAddress != null) tvAddress.setText(place.getAddress());
            if (tvVisits != null) tvVisits.setText("Visits: " + place.getNumberOfVisits());
            if (tvMemo != null) {
                if (place.getMemo() != null && !place.getMemo().isEmpty()) {
                    tvMemo.setText(place.getMemo());
                    tvMemo.setVisibility(View.VISIBLE);
                } else {
                    tvMemo.setVisibility(View.GONE);
                }
            }

            // Handle likes
            String whoLikes = place.getWhoLikes();
            String[] likes = (whoLikes != null && !whoLikes.isEmpty()) ? whoLikes.split(",") : new String[0];
            int likeCountValue = likes.length;

            likeCount.setText(String.valueOf(likeCountValue));

            // Create a "Liked by:" text
            if (likeCountValue > 0) {
                String likedByText = "Liked by: " + String.join(", ", likes);
                TextView likedByTextView = new TextView(itemView.getContext());
                likedByTextView.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                likedByTextView.setText(likedByText);
                likedByTextView.setTextColor(Color.GRAY);
                likedByTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                likedByTextView.setPadding(0, 2, 0, 2);
            }

            String currentUserId = Utility.getCurrentUser(context);
            updateLikeUI(this, place, currentUserId);
            likeCount.setText(String.valueOf(likeCountValue));

            // Handle comments
            String comments = place.getComments();
            if (comments != null && !comments.isEmpty()) {
                String[] commentArray = comments.split("\n");
                if (commentArray.length > 1) {
                    commentsContainer.removeAllViews(); // Clear previous comments
                    for (int i = 0; i < Math.min(commentArray.length, 3); i++) { // Show up to 3 comments
                        String comment = commentArray[i];
                        TextView commentView = new TextView(itemView.getContext());
                        commentView.setLayoutParams(new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));

                        String[] parts = comment.split(":", 2);
                        if (parts.length == 2) {
                            SpannableString ss = new SpannableString(comment);
                            ss.setSpan(new ForegroundColorSpan(Color.BLUE), 0, parts[0].length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            ss.setSpan(new ForegroundColorSpan(Color.GRAY), parts[0].length() + 1, comment.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            commentView.setText(ss);
                        } else {
                            commentView.setText(comment);
                            commentView.setTextColor(Color.GRAY);
                        }

                        commentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                        commentView.setPadding(0, 2, 0, 2);
                        commentsContainer.addView(commentView);
                    }
                    if (commentArray.length > 3) {
                        TextView moreCommentsView = new TextView(itemView.getContext());
                        moreCommentsView.setLayoutParams(new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                        moreCommentsView.setText("...");
                        moreCommentsView.setTextColor(Color.GRAY);
                        moreCommentsView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                        moreCommentsView.setPadding(0, 2, 0, 2);
                        commentsContainer.addView(moreCommentsView);
                    }
                    commentsContainer.setVisibility(View.VISIBLE);
                } else {
                    commentsContainer.setVisibility(View.GONE);
                }
            } else {
                commentsContainer.setVisibility(View.GONE);
            }

            commentCount.setText(String.valueOf(place.getCommentCount()));

            String profileImageUrl = place.getProfileImageUrl();
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                Glide.with(context)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(profileImageView);
            } else {
                profileImageView.setImageResource(R.drawable.default_profile);
            }

            profileImageView.setOnClickListener(v -> {
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    listener.onProfileImageClick(profileImageUrl);
                } else {
                    listener.onUserIdClick(userId);
                }
            });


            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaceClick(place);
                }
            });


            if (tvMemo != null) {
                if (place.getMemo() != null && !place.getMemo().isEmpty()) {
                    tvMemo.setText(place.getMemo());
                    tvMemo.setVisibility(View.VISIBLE);
                } else {
                    tvMemo.setVisibility(View.GONE);
                }
            }

            if (mapView != null) {
                mapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        MapsInitializer.initialize(mapView.getContext());
                        LatLng placeLocation = new LatLng(place.getLat(), place.getLon());
                        LatLng currentLocation = dbHelper.getLastKnownLocation();

                        // Clear any existing markers
                        googleMap.clear();

                        // Check if placeLocation is valid
                        if (placeLocation.latitude != 0 && placeLocation.longitude != 0) {
                            // Add marker for the place (most recently updated place)
                            googleMap.addMarker(new MarkerOptions()
                                    .position(placeLocation)
                                    .title(place.getName())
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                        } else {
                            Log.e("PlacesAdapter", "--m-- Invalid place location for: " + place.getName());
                        }

                        // Check if currentLocation is valid
                        if (currentLocation != null) {
                            // Add marker for current location
                            googleMap.addMarker(new MarkerOptions()
                                    .position(currentLocation)
                                    .title("Current Location")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                            // Only draw line and calculate distance if both locations are valid
                            if (placeLocation.latitude != 0 && placeLocation.longitude != 0) {
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
                            }
                        } else {
                            Log.e("PlacesAdapter", "--m-- Current location is null");
                        }

                        // Calculate bounds and move camera
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        if (placeLocation.latitude != 0 && placeLocation.longitude != 0) {
                            builder.include(placeLocation);
                        }
                        if (currentLocation != null) {
                            builder.include(currentLocation);
                        }

                        if (builder.build().getCenter().latitude != 0 && builder.build().getCenter().longitude != 0) {
                            LatLngBounds bounds = builder.build();
                            int padding = 200;
                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                            googleMap.moveCamera(cu);

                            // Set zoom level
                            googleMap.animateCamera(CameraUpdateFactory.zoomTo(12));
                        } else {
                            // If no valid locations, show a default location or handle accordingly
                            LatLng defaultLocation = new LatLng(0, 0); // Or any other default location
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 2));
                        }

                        // Enable zoom controls and compass
                        googleMap.getUiSettings().setZoomControlsEnabled(true);
                        googleMap.getUiSettings().setCompassEnabled(true);

                        // Enable gestures for zooming and panning
                        googleMap.getUiSettings().setAllGesturesEnabled(true);

                        // Set the map type to NORMAL
                        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    }
                });
            }


            likeIcon.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(place);
                    listener.onShowWhoLikes(place);
                }
            });

            commentIcon.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentClick(place);
                }
            });
        }

        private void updateLikeUI(PlaceViewHolder holder, Place place, String currentUserId) {
            boolean isLiked = place.getWhoLikes() != null && place.getWhoLikes().contains(currentUserId);
            int color = isLiked ? context.getResources().getColor(R.color.Red) : context.getResources().getColor(R.color.Gray);
            holder.likeIcon.setColorFilter(color);
            holder.likeCount.setTextColor(color);

            String[] likes = place.getWhoLikes() != null ? place.getWhoLikes().split(",") : new String[0];
            int likeCountValue = likes.length;
            holder.likeCount.setText(String.valueOf(likeCountValue));

            if (isLiked) {
                holder.likeIcon.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(200)
                        .withEndAction(() -> holder.likeIcon.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .start())
                        .start();
            }
        }
    }
}
package com.jason.memory;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder> {
    private List<MemoryItem> memoryItems;
    private OnMemoryClickListener listener;
    private Context context;

    public interface OnMemoryClickListener {
        void onMemoryClick(long memoryId);
        void onMemoryLongClick(String content);
        void onEditMemory(long memoryId);  // Add this new method
    }

    public MemoryAdapter(List<MemoryItem> memoryItems, OnMemoryClickListener listener, Context context) {
        this.memoryItems = memoryItems;
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public MemoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_memory, parent, false);
        return new MemoryViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull MemoryViewHolder holder, int position) {
        MemoryItem memory = memoryItems.get(position);
        Log.d("MemoryAdapter", "Binding view for memory ID: " + memory.getId());

        holder.titleTextView.setText(memory.getTitle());
        holder.dateTextView.setText(memory.getDate());
        holder.memoryTextView.setText(memory.getMemoryText());
        holder.placeTextView.setText(memory.getPlace());

        Log.d("MemoryAdapter", "Memory details - Title: " + memory.getTitle() + ", Date: " + memory.getDate() + ", Place: " + memory.getPlace());

        // Clear previous images
        holder.imageGridLayout.removeAllViews();
        Log.d("MemoryAdapter", "Cleared previous images from GridLayout");

        if (memory.getPictures() != null && memory.getPictures().length > 0) {
            Log.d("MemoryAdapter", "Number of pictures for memory " + memory.getId() + ": " + memory.getPictures().length);

            for (String imageUrl : memory.getPictures()) {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Log.d("MemoryAdapter", "Processing image URL: " + imageUrl);

                    ImageView imageView = new ImageView(holder.itemView.getContext());
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = (int) (80 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
                    params.height = (int) (80 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
                    params.setMargins(4, 4, 4, 4);
                    imageView.setLayoutParams(params);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    Log.d("MemoryAdapter", "Created ImageView for URL: " + imageUrl);

                    Glide.with(holder.itemView.getContext())
                            .load(imageUrl)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    Log.e("MemoryAdapter", "Failed to load image: " + imageUrl, e);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    Log.d("MemoryAdapter", "Successfully loaded image: " + imageUrl);
                                    return false;
                                }
                            })
                            .into(imageView);

                    holder.imageGridLayout.addView(imageView);
                    Log.d("MemoryAdapter", "Added ImageView to GridLayout for URL: " + imageUrl);
                } else {
                    Log.w("MemoryAdapter", "Skipped null or empty image URL");
                }
            }
        } else {
            Log.d("MemoryAdapter", "No pictures for memory " + memory.getId());
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMemoryClick(memory.getId());
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onMemoryLongClick(memory.getMemoryText());
            }
            return true;
        });

        // Add this new click listener for editing
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditMemory(memory.getId());
            }
        });


        // Show or hide the GridLayout based on whether there are images
        boolean hasImages = holder.imageGridLayout.getChildCount() > 0;
        holder.imageGridLayout.setVisibility(hasImages ? View.VISIBLE : View.GONE);
        Log.d("MemoryAdapter", "GridLayout visibility set to: " + (hasImages ? "VISIBLE" : "GONE"));
    }

    @Override
    public int getItemCount() {
        return memoryItems.size();
    }

    public class MemoryViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView dateTextView;
        TextView memoryTextView;
        TextView placeTextView;
        GridLayout imageGridLayout;
        TextView editButton;

        public MemoryViewHolder(View itemView) {
            super(itemView);
            editButton = itemView.findViewById(R.id.editButton);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            memoryTextView = itemView.findViewById(R.id.memoryTextView);
            placeTextView = itemView.findViewById(R.id.placeTextView);
            imageGridLayout = itemView.findViewById(R.id.imageGridLayout);
        }
    }
}
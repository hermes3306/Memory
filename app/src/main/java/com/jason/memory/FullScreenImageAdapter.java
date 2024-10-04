package com.jason.memory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import java.util.ArrayList;

public class FullScreenImageAdapter extends RecyclerView.Adapter<FullScreenImageAdapter.ImageViewHolder> {

    private Context context;
    private ArrayList<String> imageUrls;
    private boolean isProfileImage;

    public FullScreenImageAdapter(Context context, ArrayList<String> imageUrls, boolean isProfileImage) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.isProfileImage = isProfileImage;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_full_screen_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        if (isProfileImage) {
            Glide.with(context)
                    .load(imageUrl)
                    .transform(new CircleCrop())
                    .into(holder.imageView);
        } else {
            Glide.with(context)
                    .load(imageUrl)
                    .fitCenter()
                    .into(holder.imageView);
        }
        holder.imageView.setTag("image_" + position);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.fullScreenImageView);
        }
    }
}
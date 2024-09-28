package com.jason.memory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder> {
    private List<MemoryItem> memoryItems;
    private OnMemoryClickListener listener;
    private Context context;
    CircleImageView profileImageView;

    public interface OnMemoryClickListener {
        void onMemoryClick(long memoryId);
        void onMemoryLongClick(String content);
        void onLikeClick(long memoryId, String userId);
        void onCommentSend(long memoryId, String comment);
        boolean hasUserLikedMemory(long memoryId, String userId);
        void onLikeCountClick(long memoryId);
        void onCommentCountClick(long memoryId);
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

    public void updateItem(long memoryId, MemoryItem updatedItem) {
        for (int i = 0; i < memoryItems.size(); i++) {
            if (memoryItems.get(i).getId() == memoryId) {
                memoryItems.set(i, updatedItem);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void updateMemories(List<MemoryItem> newMemories) {
        this.memoryItems = newMemories;
        notifyDataSetChanged();
    }



    @Override
    public void onBindViewHolder(@NonNull MemoryViewHolder holder, int position) {
        MemoryItem item = memoryItems.get(position);
        holder.titleTextView.setText(item.getTitle());
        holder.dateTextView.setText(item.getFormattedDate());
        holder.memoryTextView.setText(item.getMemoryText());

        // Set up pictures RecyclerView
        List<String> pictures = item.getPictures();
        if (pictures != null && !pictures.isEmpty()) {
            holder.picturesRecyclerView.setVisibility(View.VISIBLE);
            MemoryPictureAdapter pictureAdapter = new MemoryPictureAdapter(pictures, context);
            holder.picturesRecyclerView.setAdapter(pictureAdapter);
            holder.picturesRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        } else {
            holder.picturesRecyclerView.setVisibility(View.GONE);
        }



        String currentUserId = getCurrentUserId();
        boolean isLiked = listener.hasUserLikedMemory(item.getId(), currentUserId);

        // Using Glide or your preferred image loading library
        Glide.with(context)
                .load(item.getUserProfilePictureUrl())
                .placeholder(R.drawable.default_profile_image)
                .into(holder.profileImageView);
        updateLikeUI(holder, isLiked);

        holder.likeIcon.setOnClickListener(v -> {
            if (!isLiked) {
                listener.onLikeClick(item.getId(), currentUserId);
            }
        });

        holder.likeCountTextView.setText(String.valueOf(item.getLikes()));

        // Set comment count and show comments
        List<String> comments = item.getComments();
        int commentCount = comments != null ? comments.size() : 0;
        holder.commentCountTextView.setText(String.valueOf(commentCount));

        // Set up comments RecyclerView
        if (comments != null && !comments.isEmpty()) {
            holder.commentsRecyclerView.setVisibility(View.VISIBLE);
            CommentAdapter commentAdapter = new CommentAdapter(comments);
            holder.commentsRecyclerView.setAdapter(commentAdapter);
            holder.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            holder.commentsRecyclerView.setVisibility(View.GONE);
        }

        holder.likeIcon.setOnClickListener(v -> {
            if (!isLiked) {
                //String currentUserId = getCurrentUserId(); // Make sure this method exists and returns the current user's ID
                listener.onLikeClick(item.getId(), currentUserId);
                int newLikeCount = item.getLikes() + 1;
                item.setLikes(newLikeCount);
                holder.likeCountTextView.setText(String.valueOf(newLikeCount));
                updateLikeUI(holder, true);
            }
        });

        // Handle comment click
        holder.commentIcon.setOnClickListener(v -> {
            holder.commentInputLayout.setVisibility(View.VISIBLE);
        });

        holder.likeCountTextView.setOnClickListener(v -> {
            listener.onLikeCountClick(item.getId());
        });

        holder.commentCountTextView.setOnClickListener(v -> {
            listener.onCommentCountClick(item.getId());
        });

        // Handle comment send
        holder.commentSendButton.setOnClickListener(v -> {
            String newComment = holder.commentEditText.getText().toString().trim();
            if (!newComment.isEmpty()) {
                listener.onCommentSend(item.getId(), newComment);
                if (item.getComments() == null) {
                    item.setComments(new ArrayList<>());
                }
                item.addComment(newComment);
                holder.commentEditText.setText("");
                holder.commentInputLayout.setVisibility(View.GONE);
                notifyItemChanged(position);
            }
        });

        holder.itemView.setOnClickListener(v -> listener.onMemoryClick(item.getId()));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onMemoryLongClick(item.getMemoryText());
            return true;
        });
    }

    private void updateLikeUI(MemoryViewHolder holder, boolean isLiked) {
        int color = isLiked ? context.getResources().getColor(R.color.Red) : context.getResources().getColor(R.color.Gray);
        holder.likeIcon.setColorFilter(color);
        holder.likeCountTextView.setTextColor(color);
    }

    private String getCurrentUserId() {
        // Implement this method to get the current user's ID
        // This could be stored in SharedPreferences or retrieved from a User object
        return Utility.getUserName(context);
    }


    @Override
    public int getItemCount() {
        return memoryItems.size();
    }

    static class MemoryViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView dateTextView;
        TextView memoryTextView;
        ImageView likeIcon;
        TextView likeCountTextView;
        ImageView commentIcon;
        TextView commentCountTextView;
        RecyclerView commentsRecyclerView;
        LinearLayout commentInputLayout;
        EditText commentEditText;
        ImageButton commentSendButton;
        CircleImageView profileImageView;
        RecyclerView picturesRecyclerView; // Add this line

        MemoryViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            memoryTextView = itemView.findViewById(R.id.memoryTextView);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            likeCountTextView = itemView.findViewById(R.id.likeCountTextView);
            commentIcon = itemView.findViewById(R.id.commentIcon);
            commentCountTextView = itemView.findViewById(R.id.commentCountTextView);
            commentsRecyclerView = itemView.findViewById(R.id.commentsRecyclerView);
            commentInputLayout = itemView.findViewById(R.id.commentInputLayout);
            commentEditText = itemView.findViewById(R.id.commentEditText);
            commentSendButton = itemView.findViewById(R.id.commentSendButton);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            picturesRecyclerView = itemView.findViewById(R.id.picturesRecyclerView); // Add this line
        }
    }

    private static class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
        private List<String> comments;

        CommentAdapter(List<String> comments) {
            this.comments = comments;
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            String comment = comments.get(position);
            String[] parts = comment.split(":", 2);
            if (parts.length == 2) {
                holder.usernameTextView.setText(parts[0].trim() + ":");
                holder.commentTextView.setText(parts[1].trim());
            } else {
                holder.usernameTextView.setText("");
                holder.commentTextView.setText(comment);
            }
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        static class CommentViewHolder extends RecyclerView.ViewHolder {
            TextView usernameTextView;
            TextView commentTextView;

            CommentViewHolder(View itemView) {
                super(itemView);
                usernameTextView = itemView.findViewById(R.id.usernameTextView);
                commentTextView = itemView.findViewById(R.id.commentTextView);
            }
        }
    }

    public class MemoryPictureAdapter extends RecyclerView.Adapter<MemoryPictureAdapter.PictureViewHolder> {
        private List<String> pictureUrls;
        private Context context;

        public MemoryPictureAdapter(List<String> pictureUrls, Context context) {
            this.pictureUrls = pictureUrls;
            this.context = context;
        }

        @NonNull
        @Override
        public PictureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_memory_picture, parent, false);
            return new PictureViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PictureViewHolder holder, int position) {
            String pictureUrl = pictureUrls.get(position);
            Glide.with(context)
                    .load(pictureUrl)
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return pictureUrls.size();
        }

        static class PictureViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            PictureViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.memoryPictureView);
            }
        }
    }

}
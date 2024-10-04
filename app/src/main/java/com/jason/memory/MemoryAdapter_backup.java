package com.jason.memory;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class MemoryAdapter_backup extends RecyclerView.Adapter<MemoryAdapter_backup.MemoryViewHolder> {
    private List<MemoryItem> memoryItems;
    private OnMemoryClickListener listener;
    private Context context;
    CircleImageView profileImageView;
    private String TAG = "MemoryAdapter";

    public interface OnMemoryClickListener {
        void onMemoryClick(long memoryId);
        void onMemoryLongClick(String content);
        void onLikeClick(long memoryId, String userId);
        void onUnlikeClick(long memoryId, String userId);
        void onCommentSend(long memoryId, String comment);
        boolean hasUserLikedMemory(long memoryId, String userId);
        void onLikeCountClick(long memoryId);
        void onCommentCountClick(long memoryId);

        void onUserIdClick(String userId);
        void onTitleClick(String title);
        void onDateClick(String date);
        void onContentClick(String content);
    }

    public MemoryAdapter_backup(List<MemoryItem> memoryItems, OnMemoryClickListener listener, Context context) {
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


    private void openFullScreenImage(ArrayList<String> imageUrls, int position, boolean isProfileImage) {
        Log.d(TAG, "--m-- Opening full screen image: position " + position + ", isProfileImage: " + isProfileImage);
        Intent intent = new Intent(context, FullScreenImageActivity.class);
        intent.putStringArrayListExtra("IMAGE_URLS", imageUrls);
        intent.putExtra("POSITION", position);
        intent.putExtra("IS_PROFILE_IMAGE", isProfileImage);
        context.startActivity(intent);
    }

    public void refreshData() {
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull MemoryViewHolder holder, int position) {
        MemoryItem item = memoryItems.get(position);

        String currentUserId = getCurrentUserId();

        holder.usernameTextView.setText(item.getUserId());
        holder.usernameTextView.setOnClickListener(v -> {
            listener.onUserIdClick(item.getUserId());
        });

        holder.titleTextView.setText(item.getTitle());
        holder.titleTextView.setOnClickListener(v -> {
            listener.onTitleClick(item.getTitle());
        });

        holder.dateTextView.setText(item.getFormattedDate());
        holder.dateTextView.setOnClickListener(v -> {
            listener.onDateClick(item.getFormattedDate());
        });

        holder.memoryTextView.setText(item.getMemoryText());
        holder.memoryTextView.setOnClickListener(v -> {
            listener.onContentClick(item.getMemoryText());
        });
        holder.memoryTextView.post(() -> {
            if (holder.memoryTextView.getLineCount() > 10) {
                holder.continueTextView.setVisibility(View.VISIBLE);
                holder.isExpanded = false;
                holder.memoryTextView.setMaxLines(10);
            } else {
                holder.continueTextView.setVisibility(View.GONE);
            }
        });

        holder.continueTextView.setOnClickListener(v -> {
            if (holder.isExpanded) {
                holder.memoryTextView.setMaxLines(10);
                holder.continueTextView.setText("(continue)");
            } else {
                holder.memoryTextView.setMaxLines(Integer.MAX_VALUE);
                holder.continueTextView.setText("(collapse)");
            }
            holder.isExpanded = !holder.isExpanded;
        });


        // Load the user's profile picture
        String profileImageUrl = item.getUserProfileImageUrl();
        Log.d(TAG, "--m-- Loading profile image: " + profileImageUrl);

        Glide.with(context)
                .load(profileImageUrl)
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(holder.profileImageView);


        // Reset visibility of picturesRecyclerView
        holder.picturesRecyclerView.setVisibility(View.VISIBLE);

        // Set up pictures RecyclerView
        List<String> pictures = item.getPictures();
        if (pictures != null && !pictures.isEmpty()) {
            Log.d(TAG, "--m-- Memory has " + pictures.size() + " pictures");
            MemoryPictureAdapter pictureAdapter = new MemoryPictureAdapter(pictures, context);
            holder.picturesRecyclerView.setAdapter(pictureAdapter);
            holder.picturesRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            pictureAdapter.notifyDataSetChanged(); // Add this line
        } else {
            Log.d(TAG, "--m-- Memory has no pictures");
            holder.picturesRecyclerView.setVisibility(View.GONE);
        }

        boolean isLiked = listener.hasUserLikedMemory(item.getId(), currentUserId);

        updateLikeUI(holder, isLiked, item.getLikes());

        holder.likeCountTextView.setText(String.valueOf(item.getLikes()));

        holder.profileImageView.setOnClickListener(v -> {
            Log.d(TAG, "--m-- Profile image clicked: " + item.getUserProfileImageUrl());
            ArrayList<String> profileImageList = new ArrayList<>();
            profileImageList.add(item.getUserProfileImageUrl());
            openFullScreenImage(profileImageList, 0, true);
        });

        holder.likeIcon.setOnClickListener(v -> {
            if (isLiked) {
                // Unlike
                listener.onUnlikeClick(item.getId(), currentUserId);
                int newLikeCount = Math.max(0, item.getLikes() - 1);
                item.setLikes(newLikeCount);
                updateLikeUI(holder, false, newLikeCount);
            } else {
                // Like
                listener.onLikeClick(item.getId(), currentUserId);
                int newLikeCount = item.getLikes() + 1;
                item.setLikes(newLikeCount);
                updateLikeUI(holder, true, newLikeCount);
            }
            String postUserId = item.getUserId();
            String whoLikes = item.getWhoLikes();
            Log.d(TAG, "--m-- Like clicked - Post User ID: " + postUserId +
                    ", Who likes: " + whoLikes +
                    ", Current user: " + currentUserId +
                    ", Is liked: " + !isLiked);
            Toast.makeText(context,
                    "Post User ID: " + postUserId +
                            ", Who likes: " + whoLikes +
                            ", Current user: " + currentUserId,
                    Toast.LENGTH_LONG).show();
        });

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

    private void updateLikeUI(MemoryViewHolder holder, boolean isLiked, int likeCount) {
        int color = isLiked ? context.getResources().getColor(R.color.Red) : context.getResources().getColor(R.color.Gray);
        holder.likeIcon.setColorFilter(color);
        holder.likeCountTextView.setTextColor(color);
        holder.likeCountTextView.setText(String.valueOf(likeCount));

        // Add animation when liking
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


    private String getCurrentUserId() {
        return Utility.getCurrentUser(context);
    }

    @Override
    public int getItemCount() {
        return memoryItems.size();
    }

    static class MemoryViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView dateTextView;
        TextView memoryTextView;
        TextView usernameTextView;

        ImageView likeIcon;
        TextView likeCountTextView;
        ImageView commentIcon;
        TextView commentCountTextView;
        RecyclerView commentsRecyclerView;
        LinearLayout commentInputLayout;
        EditText commentEditText;
        ImageButton commentSendButton;
        CircleImageView profileImageView;
        RecyclerView picturesRecyclerView;
        TextView continueTextView;
        boolean isExpanded = false;// Add this line

        MemoryViewHolder(View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
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
            continueTextView = itemView.findViewById(R.id.continueTextView);
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

        public void refreshData(List<String> newPictureUrls) {
            this.pictureUrls = newPictureUrls;
            notifyDataSetChanged();
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
            Log.d(TAG, "--m-- Loading memory picture: " + pictureUrl);
            Glide.with(context)
                    .load(pictureUrl)
                    .into(holder.imageView);

            holder.imageView.setOnClickListener(v -> {
                Log.d(TAG, "--m-- Memory picture clicked: " + pictureUrl);
                openFullScreenImage(new ArrayList<>(pictureUrls), position, false);
            });

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
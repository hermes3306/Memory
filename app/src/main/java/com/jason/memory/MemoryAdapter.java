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

import java.util.ArrayList;
import java.util.List;

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder> {
    private List<MemoryItem> memoryItems;
    private OnMemoryClickListener listener;
    private Context context;

    public interface OnMemoryClickListener {
        void onMemoryClick(long memoryId);
        void onMemoryLongClick(String content);
        void onLikeClick(long memoryId);
        void onCommentSend(long memoryId, String comment);
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
        MemoryItem item = memoryItems.get(position);
        holder.titleTextView.setText(item.getTitle());
        holder.dateTextView.setText(item.getDate());
        holder.memoryTextView.setText(item.getMemoryText());

        // Set like count
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

        // Handle like click
        holder.likeIcon.setOnClickListener(v -> {
            listener.onLikeClick(item.getId());
            int newLikeCount = item.getLikes() + 1;
            item.setLikes(newLikeCount);
            holder.likeCountTextView.setText(String.valueOf(newLikeCount));
        });

        // Handle comment click
        holder.commentIcon.setOnClickListener(v -> {
            holder.commentInputLayout.setVisibility(View.VISIBLE);
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

}
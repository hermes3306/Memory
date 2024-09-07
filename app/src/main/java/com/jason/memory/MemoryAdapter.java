package com.jason.memory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder> {
    private List<MemoryItem> memoryItems;
    private OnMemoryClickListener listener;
    private Context context;

    public interface OnMemoryClickListener {
        void onMemoryClick(long memoryId);
        void onMemoryLongClick(String content);
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

        MemoryViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            memoryTextView = itemView.findViewById(R.id.memoryTextView);
        }
    }
}
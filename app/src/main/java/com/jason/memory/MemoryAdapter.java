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
    private Context context;

    public MemoryAdapter(List<MemoryItem> memoryItems, Context context) {
        this.memoryItems = memoryItems;
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
        holder.titleEditText.setText(item.getTitle());
        holder.dateValueTextView.setText(item.getDate());
        holder.memoryEditText.setText(item.getMemoryText());

        // TODO: Implement picture loading into the RecyclerView
        // TODO: Implement audio recording and playback functionality
    }

    @Override
    public int getItemCount() {
        return memoryItems.size();
    }

    static class MemoryViewHolder extends RecyclerView.ViewHolder {
        EditText titleEditText;
        TextView dateValueTextView;
        EditText memoryEditText;
        RecyclerView picturesRecyclerView;
        Button addPictureButton;
        Button recordAudioButton;
        Button playAudioButton;

        MemoryViewHolder(View itemView) {
            super(itemView);
            titleEditText = itemView.findViewById(R.id.titleEditText);
            dateValueTextView = itemView.findViewById(R.id.dateValueTextView);
            memoryEditText = itemView.findViewById(R.id.memoryEditText);
            picturesRecyclerView = itemView.findViewById(R.id.picturesRecyclerView);
            addPictureButton = itemView.findViewById(R.id.addPictureButton);
            recordAudioButton = itemView.findViewById(R.id.recordAudioButton);
            playAudioButton = itemView.findViewById(R.id.playAudioButton);

            // TODO: Implement click listeners for buttons
        }
    }
}

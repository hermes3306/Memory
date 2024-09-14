package com.jason.memory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileViewListAdapter extends RecyclerView.Adapter<FileViewListAdapter.FileViewHolder> {
    private List<File> files;
    private OnFileClickListener listener;

    public FileViewListAdapter(OnFileClickListener listener) {
        this.files = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fileview, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = files.get(position);
        holder.fileNameTextView.setText(file.getName());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileClick(file);
            }
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public void setFiles(List<File> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView;

        FileViewHolder(View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
        }
    }

    public interface OnFileClickListener {
        void onFileClick(File file);
    }
}
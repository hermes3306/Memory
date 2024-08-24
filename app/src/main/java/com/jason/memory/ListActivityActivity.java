package com.jason.memory;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class ListActivityActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ActivityAdapter adapter;
    private List<ActivityData> activityList;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_activity);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DatabaseHelper(this);
        activityList = dbHelper.getAllActivities();

        adapter = new ActivityAdapter(activityList, activity -> {
            Intent intent = new Intent(ListActivityActivity.this, ActivityDetailActivity.class);
            intent.putExtra("ACTIVITY_ID", activity.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    private class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
        private List<ActivityData> activities;
        private OnItemClickListener listener;

        public ActivityAdapter(List<ActivityData> activities, OnItemClickListener listener) {
            this.activities = activities;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ActivityData activity = activities.get(position);
            holder.bind(activity, listener);
        }

        @Override
        public int getItemCount() {
            return activities.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDistance, tvTime;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvDistance = itemView.findViewById(R.id.tvDistance);
                tvTime = itemView.findViewById(R.id.tvTime);
            }

            void bind(final ActivityData activity, final OnItemClickListener listener) {
                tvName.setText(activity.getName());
                tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", activity.getDistance()));
                tvTime.setText(formatElapsedTime(activity.getElapsedTime()));
                itemView.setOnClickListener(v -> listener.onItemClick(activity));
            }
        }
    }

    interface OnItemClickListener {
        void onItemClick(ActivityData activity);
    }

    private String formatElapsedTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }
}

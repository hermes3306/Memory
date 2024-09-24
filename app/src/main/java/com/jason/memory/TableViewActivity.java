package com.jason.memory;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jason.memory.databinding.ActivityTableViewBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableViewActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TableAdapter adapter;
    private List<Map<String, String>> data = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private String tableName;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_view);

        Button deleteAllButton = findViewById(R.id.deleteAllButton);
        deleteAllButton.setOnClickListener(v -> showDeleteConfirmation());

        tableName = getIntent().getStringExtra("TABLE_NAME");
        setTitle(tableName + " Table");

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadData();

        Button prevButton = findViewById(R.id.prevButton);
        Button nextButton = findViewById(R.id.nextButton);

        prevButton.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                loadData();
            }
        });

        nextButton.setOnClickListener(v -> {
            currentPage++;
            loadData();
        });
    }


    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Data")
                .setMessage("Are you sure you want to delete all data from this table? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> deleteAllData())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteAllData() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(tableName, null, null);
        Toast.makeText(this, "All data has been deleted from " + tableName, Toast.LENGTH_SHORT).show();
        loadData(); // Reload the data to reflect the changes
    }


    private void loadData() {
        data.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + tableName + " LIMIT " + ITEMS_PER_PAGE + " OFFSET " + (currentPage * ITEMS_PER_PAGE);
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    row.put(cursor.getColumnName(i), cursor.getString(i));
                }
                data.add(row);
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (adapter == null) {
            adapter = new TableAdapter(data);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }
}
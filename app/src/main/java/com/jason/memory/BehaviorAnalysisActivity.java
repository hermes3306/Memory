package com.jason.memory;

import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BehaviorAnalysisActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private DatePicker datePicker;
    private Button analyzeButton;
    private TextView resultView;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_behavior_analysis);

        datePicker = findViewById(R.id.datePicker);
        analyzeButton = findViewById(R.id.analyzeButton);
        resultView = findViewById(R.id.resultView);
        dbHelper = new DatabaseHelper(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyzeMovement();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    private void analyzeMovement() {
        int year = datePicker.getYear();
        int month = datePicker.getMonth();
        int day = datePicker.getDayOfMonth();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        long startTime = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long endTime = calendar.getTimeInMillis();

        List<LocationData> gpsDataList = dbHelper.getLocationDataForDateRange(startTime, endTime);

        if (gpsDataList.isEmpty()) {
            resultView.setText("No data available for the selected date.");
            return;
        }

        mMap.clear();
        StringBuilder result = new StringBuilder();
        LocationData startPoint = gpsDataList.get(0);
        result.append("Start point: ").append(startPoint.toString()).append("\n\n");

        LatLng lastSignificantPoint = new LatLng(startPoint.getLatitude(), startPoint.getLongitude());
        mMap.addMarker(new MarkerOptions().position(lastSignificantPoint).title("Start"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastSignificantPoint, 15));

        for (int i = 1; i < gpsDataList.size(); i++) {
            LocationData currentPoint = gpsDataList.get(i);
            LatLng currentLatLng = new LatLng(currentPoint.getLatitude(), currentPoint.getLongitude());
            float[] results = new float[1];
            Location.distanceBetween(lastSignificantPoint.latitude, lastSignificantPoint.longitude,
                    currentLatLng.latitude, currentLatLng.longitude, results);
            float distance = results[0];

            if (distance >= 100) {
                String timeString = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(new Date(currentPoint.getTimestamp()));
                result.append("Significant movement detected:\n")
                        .append("Time: ").append(timeString).append("\n")
                        .append("Location: ").append(currentPoint.toString()).append("\n")
                        .append("Distance moved: ").append(String.format("%.2f", distance)).append(" meters\n\n");

                mMap.addMarker(new MarkerOptions().position(currentLatLng).title(timeString));
                lastSignificantPoint = currentLatLng;
            }
        }

        resultView.setText(result.toString());
    }
}
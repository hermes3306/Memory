package com.jason.memory;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity_v2 extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private DatabaseHelper dbHelper;
    private int circleSizeInSquareMeters = 1000; // Default size, can be changed in settings
    private Map<LatLng, Circle> drawnCircles = new HashMap<>();
    private Map<LatLng, Integer> circlePositionCounts = new HashMap<>();
    private static final float DEFAULT_ZOOM = 13f; // Zoom level to cover approximately 5km

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Get circle size from SharedPreferences or use default
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        circleSizeInSquareMeters = prefs.getInt("circle_size", 1000);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        dbHelper = new DatabaseHelper(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enable zoom controls
        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);

        loadAndDisplayPositions();
        moveToLastPosition();
    }

    private void loadAndDisplayPositions() {
        List<LatLng> positions = dbHelper.getAllPositions();
        for (LatLng position : positions) {
            addOrUpdateCircle(position);
        }
    }

    private void moveToLastPosition() {
        List<LatLng> positions = dbHelper.getAllPositions();
        if (!positions.isEmpty()) {
            LatLng lastPosition = positions.get(positions.size() - 1);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, DEFAULT_ZOOM));
        }
    }

    private void addOrUpdateCircle(LatLng position) {
        LatLng circleCenter = findNearestCircleCenter(position);

        if (circleCenter == null) {
            // Create new circle
            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(position)
                    .radius(Math.sqrt(circleSizeInSquareMeters) / 2)
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(70, 255, 0, 0)));
            drawnCircles.put(position, circle);
            circlePositionCounts.put(position, 1);
            addTextToCircle(circle, "1");
        } else {
            // Update existing circle
            int count = circlePositionCounts.get(circleCenter) + 1;
            circlePositionCounts.put(circleCenter, count);
            Circle circle = drawnCircles.get(circleCenter);
            addTextToCircle(circle, String.valueOf(count));
        }
    }


    private LatLng findNearestCircleCenter(LatLng position) {
        double minDistance = Double.MAX_VALUE;
        LatLng nearestCenter = null;

        for (LatLng center : drawnCircles.keySet()) {
            double distance = calculateDistance(center, position);
            double circleRadius = Math.sqrt(circleSizeInSquareMeters) / 2;

            if (distance <= circleRadius && distance < minDistance) {
                minDistance = distance;
                nearestCenter = center;
            }
        }

        return nearestCenter;
    }

    private double calculateDistance(LatLng point1, LatLng point2) {
        double earthRadius = 6371000; // meters
        double lat1 = Math.toRadians(point1.latitude);
        double lat2 = Math.toRadians(point2.latitude);
        double lng1 = Math.toRadians(point1.longitude);
        double lng2 = Math.toRadians(point2.longitude);

        double dlat = lat2 - lat1;
        double dlng = lng2 - lng1;

        double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dlng/2) * Math.sin(dlng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return earthRadius * c;
    }

    private void addTextToCircle(Circle circle, String text) {
        // Remove existing text if any
        mMap.clear();

        // Redraw all circles
        for (Map.Entry<LatLng, Circle> entry : drawnCircles.entrySet()) {
            Circle c = mMap.addCircle(new CircleOptions()
                    .center(entry.getValue().getCenter())
                    .radius(entry.getValue().getRadius())
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(70, 255, 0, 0)));
            entry.setValue(c);
        }

        // Add text
        MarkerOptions markerOptions = new MarkerOptions()
                .position(circle.getCenter())
                .icon(BitmapDescriptorFactory.fromBitmap(createTextBitmap(text)))
                .anchor(0.5f, 0.5f);
        mMap.addMarker(markerOptions);
    }

    private Bitmap createTextBitmap(String text) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(40);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline = -paint.ascent();
        int width = (int) (paint.measureText(text) + 0.5f);
        int height = (int) (baseline + paint.descent() + 0.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }
}
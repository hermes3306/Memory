package com.jason.memory;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.CircleOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private DatabaseHelper dbHelper;
    private int circleSizeInSquareMeters = 1000; // Default size, can be changed in settings
    private Map<LatLng, Circle> drawnCircles = new HashMap<>();
    private Map<LatLng, Integer> circlePositionCounts = new HashMap<>();
    private Map<LatLng, Marker> circleTextMarkers = new HashMap<>(); // New map to store text markers
    private SharedPreferences prefs;



    private static final int MAX_COUNT = 10; // Maximum count for color scaling
    private static final int[] COLORS = {
            Color.rgb(0, 255, 0),    // Green
            Color.rgb(128, 255, 0),  // Light Green
            Color.rgb(255, 255, 0),  // Yellow
            Color.rgb(255, 128, 0),  // Orange
            Color.rgb(255, 0, 0)     // Red
    };


    // ... 원 그리기  ...
    private static final String PREF_CIRCLE_SIZE = "circle_size";
    private static final int DEFAULT_CIRCLE_SIZE = 785000; // 약 500미터 반경의 원 면적 (제곱미터)
    private static final float DEFAULT_ZOOM = 13f; // 약 3킬로미터 범위가 보이는 줌 레벨




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Get circle size from SharedPreferences or use default
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        circleSizeInSquareMeters = prefs.getInt(PREF_CIRCLE_SIZE, DEFAULT_CIRCLE_SIZE);

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

    private void moveToLastPosition() {
        List<LatLng> positions = dbHelper.getAllPositions();
        if (!positions.isEmpty()) {
            LatLng lastPosition = positions.get(positions.size() - 1);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, DEFAULT_ZOOM));
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


    private void addOrUpdateCircle(LatLng position) {
        LatLng circleCenter = findNearestCircleCenter(position);

        if (circleCenter == null) {
            // Create new circle
            int fillColor = getColorForCount(1);
            fillColor = Color.argb(128, Color.red(fillColor), Color.green(fillColor), Color.blue(fillColor));

            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(position)
                    .radius(Math.sqrt(circleSizeInSquareMeters / Math.PI)) // 반경 계산
                    .strokeColor(Color.argb(128, 0, 0, 0))  // 50% 투명한 검정색 테두리
                    .fillColor(fillColor));
            drawnCircles.put(position, circle);
            circlePositionCounts.put(position, 1);
            addTextToCircle(position, "1");
        } else {
            // Update existing circle
            int count = circlePositionCounts.get(circleCenter) + 1;
            circlePositionCounts.put(circleCenter, count);
            Circle circle = drawnCircles.get(circleCenter);

            int fillColor = getColorForCount(count);
            fillColor = Color.argb(128, Color.red(fillColor), Color.green(fillColor), Color.blue(fillColor));

            circle.setFillColor(fillColor);
            addTextToCircle(circleCenter, String.valueOf(count));
        }
    }

    private int getColorForCount(int count) {
        float fraction = Math.min(1.0f, (float) count / MAX_COUNT);
        int index = (int) (fraction * (COLORS.length - 1));
        int lowColor = COLORS[index];
        int highColor = COLORS[Math.min(index + 1, COLORS.length - 1)];
        return interpolateColor(lowColor, highColor, fraction * (COLORS.length - 1) - index);
    }

    private int interpolateColor(int lowColor, int highColor, float fraction) {
        float inverseFraction = 1f - fraction;
        float r = (Color.red(lowColor) * inverseFraction) + (Color.red(highColor) * fraction);
        float g = (Color.green(lowColor) * inverseFraction) + (Color.green(highColor) * fraction);
        float b = (Color.blue(lowColor) * inverseFraction) + (Color.blue(highColor) * fraction);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    private void loadAndDisplayPositions() {
        // Clear existing circles and markers
        for (Circle circle : drawnCircles.values()) {
            circle.remove();
        }
        for (Marker marker : circleTextMarkers.values()) {
            marker.remove();
        }
        drawnCircles.clear();
        circlePositionCounts.clear();
        circleTextMarkers.clear();

        // Reload circle size from preferences
        circleSizeInSquareMeters = prefs.getInt(PREF_CIRCLE_SIZE, DEFAULT_CIRCLE_SIZE);

        List<LatLng> positions = dbHelper.getAllPositions();
        for (LatLng position : positions) {
            addOrUpdateCircle(position);
        }
    }


    private void addTextToCircle(LatLng center, String text) {
        // Remove existing text marker if any
        Marker existingMarker = circleTextMarkers.get(center);
        if (existingMarker != null) {
            existingMarker.remove();
        }

        // Add new text marker
        MarkerOptions markerOptions = new MarkerOptions()
                .position(center)
                .icon(BitmapDescriptorFactory.fromBitmap(createTextBitmap(text)))
                .anchor(0.5f, 0.5f);
        Marker marker = mMap.addMarker(markerOptions);
        circleTextMarkers.put(center, marker);
    }

    private Bitmap createTextBitmap(String text) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(40);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.CENTER);
        float baseline = -paint.ascent();
        int width = (int) (paint.measureText(text) + 0.5f);
        int height = (int) (baseline + paint.descent() + 0.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, width / 2, baseline, paint);
        return image;
    }


}
package com.jason.memory;
import java.util.Locale;

public class LocationData {
    private long id;
    private double latitude;
    private double longitude;
    private double altitude;
    private long timestamp;

    public LocationData(long id, double latitude, double longitude, double altitude, long timestamp) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "Lat: %.6f, Lon: %.6f, Alt: %.2f", latitude, longitude, altitude);
    }
}
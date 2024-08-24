package com.jason.memory;



public class LocationData {
    private long id;
    private double latitude;
    private double longitude;
    private double altitude;
    private long time;

    public LocationData(long id, double latitude, double longitude, double altitude, long time) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.time = time;
    }

    // Getters
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

    public long getTime() {
        return time;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public void setTime(long time) {
        this.time = time;
    }
}

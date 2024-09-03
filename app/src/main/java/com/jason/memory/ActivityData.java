package com.jason.memory;


import java.io.Serializable;

public class ActivityData implements Serializable {
    private static final long serialVersionUID = 1L; // Add this line

    private long id;
    private String filename; // New field
    private String type;
    private String name;
    private long startTimestamp;
    private long endTimestamp;
    private long startLocationId;
    private long endLocationId;
    private double distance;
    private long elapsedTime;
    private String address;

    public ActivityData() {
    }


    public void updateFrom(ActivityData other) {
        if (other == null) return;

        this.type = other.type;
        this.name = other.name;
        this.startTimestamp = other.startTimestamp;
        this.endTimestamp = other.endTimestamp;
        this.startLocationId = other.startLocationId;
        this.endLocationId = other.endLocationId;
        this.distance = other.distance;
        this.elapsedTime = other.elapsedTime;
        this.address = other.address;
        // Don't update id and filename as they should remain the same
    }

    public ActivityData(long id, String filename, String type, String name, long startTimestamp, long endTimestamp,
                        long startLocationId, long endLocationId, double distance, long elapsedTime, String address) {
        this.id = id;
        this.filename = filename;
        this.type = type;
        this.name = name;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.startLocationId = startLocationId;
        this.endLocationId = endLocationId;
        this.distance = distance;
        this.elapsedTime = elapsedTime;
        this.address = address;
    }


    public ActivityData(long id, String type, String name, long startTimestamp, long endTimestamp,
                        long startLocationId, long endLocationId, double distance, long elapsedTime, String address) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.startLocationId = startLocationId;
        this.endLocationId = endLocationId;
        this.distance = distance;
        this.elapsedTime = elapsedTime;
        this.address = address;
    }

    // Getters
    public long getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public long getStartTimestamp() { return startTimestamp; }
    public long getEndTimestamp() { return endTimestamp; }
    public long getStartLocationId() { return startLocationId; }
    public long getEndLocationId() { return endLocationId; }
    public double getDistance() { return distance; }
    public long getElapsedTime() { return elapsedTime; }
    // Getter and setter for filename
    public String getFilename() { return filename; }


    // Setters (if needed)
    public void setId(long id) {this.id = id; }
    public void setName(String name) { this.name = name; }
    // Add getter and setter for address
    public String getAddress() {
        return address;
    }
    public void setFilename(String filename) { this.filename = filename; }
    public void setAddress(String address) {
        this.address = address;
    }
}


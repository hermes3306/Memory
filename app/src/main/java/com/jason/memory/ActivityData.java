package com.jason.memory;


import java.io.Serializable;

public class ActivityData implements Serializable {
    private long id;
    private String type;
    private String name;
    private long startTimestamp;
    private long endTimestamp;
    private long startLocation;
    private long endLocation;
    private double distance;
    private long elapsedTime;
    private String address;
    private String filename;

    // Constructor
    public ActivityData(long id, String type, String name, long startTimestamp, long endTimestamp,
                        long startLocation, long endLocation, double distance, long elapsedTime, String address) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.distance = distance;
        this.elapsedTime = elapsedTime;
        this.address = address;
    }

    public ActivityData(long id, String filename, String type, String name, long startTimestamp, long endTimestamp,
                        long startLocation, long endLocation, double distance, long elapsedTime, String address) {
        this.id = id;
        this.filename = filename;
        this.type = type;
        this.name = name;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.distance = distance;
        this.elapsedTime = elapsedTime;
        this.address = address;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public long getStartLocation() {
        return startLocation;
    }

    public long getEndLocation() {
        return endLocation;
    }

    public double getDistance() {
        return distance;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public String getAddress() {
        return address;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public void setStartLocation(long startLocation) {
        this.startLocation = startLocation;
    }

    public void setEndLocation(long endLocation) {
        this.endLocation = endLocation;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getStartLocationId() {
        return this.startLocation;
    }

    public long getEndLocationId() {
        return this.endLocation;
    }


}

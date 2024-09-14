package com.jason.daily;

public class Place {
    private long id;
    private String country;
    private String type;
    private String name;
    private String address;
    private long firstVisited;
    private int numberOfVisits;
    private long lastVisited;
    private double lat;
    private double lon;
    private double alt;
    private String memo;
    private String url;

    // Constructor
    public Place(long id, String country, String type, String name, String address,
                 long firstVisited, int numberOfVisits, long lastVisited,
                 double lat, double lon, double alt, String memo) {
        this.id = id;
        this.country = country;
        this.type = type;
        this.name = name;
        this.address = address;
        this.firstVisited = firstVisited;
        this.numberOfVisits = numberOfVisits;
        this.lastVisited = lastVisited;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
        this.memo = memo;
    }

    public Place(long id, String country, String type, String name, String address,
                 long firstVisited, int numberOfVisits, long lastVisited,
                 double lat, double lon, double alt, String memo, String url) {
        this.id = id;
        this.country = country;
        this.type = type;
        this.name = name;
        this.address = address;
        this.firstVisited = firstVisited;
        this.numberOfVisits = numberOfVisits;
        this.lastVisited = lastVisited;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
        this.memo = memo;
        this.url = url;
    }

    // Default constructor
    public Place() {
    }

    // Getters
    public long getId() { return id; }
    public String getCountry() { return country; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public long getFirstVisited() { return firstVisited; }
    public int getNumberOfVisits() { return numberOfVisits; }
    public long getLastVisited() { return lastVisited; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public double getAlt() { return alt; }
    public String getMemo() { return memo; }
    public String getMapUrl() {return null;}

    // Setters
    public void setId(long id) { this.id = id; }
    public void setCountry(String country) { this.country = country; }
    public void setType(String type) { this.type = type; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setFirstVisited(long firstVisited) { this.firstVisited = firstVisited; }
    public void setNumberOfVisits(int numberOfVisits) { this.numberOfVisits = numberOfVisits; }
    public void setLastVisited(long lastVisited) { this.lastVisited = lastVisited; }
    public void setLat(double lat) { this.lat = lat; }
    public void setLon(double lon) { this.lon = lon; }
    public void setAlt(double alt) { this.alt = alt; }
    public void setMemo(String memo) { this.memo = memo; }
}
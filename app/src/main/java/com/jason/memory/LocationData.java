package com.jason.memory;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;
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
    public void setId(long id) {this.id = id;}

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public String getSimpleAddress(Context context) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                String thoroughfare = address.getThoroughfare(); // Street name
                String subLocality = address.getSubLocality(); // Neighborhood or district

                if (Locale.getDefault().getLanguage().equals("ko")) {
                    // For Korean addresses
                    String dong = address.getSubLocality(); // "동" in Korean addresses
                    if (dong != null) {
                        return dong;
                    }
                } else {
                    // For English or other languages
                    if (subLocality != null && thoroughfare != null) {
                        return subLocality + ", " + thoroughfare;
                    } else if (subLocality != null) {
                        return subLocality;
                    } else if (thoroughfare != null) {
                        return thoroughfare;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown location";
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "Lat: %.6f, Lon: %.6f, Alt: %.2f", latitude, longitude, altitude);
    }
}
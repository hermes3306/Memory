package com.jason.memory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import android.location.Address;
import android.location.Geocoder;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "LocationDatabase";
    private static final int DATABASE_VERSION = 5;
    public static final String TABLE_LOCATIONS = "locations";
    private static final String COLUMN_ID = "id";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ALTITUDE = "altitude";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String TABLE_ACTIVITIES = "activities";
    private static final String COLUMN_ACTIVITY_ID = "id";
    private static final String COLUMN_ACTIVITY_TYPE = "activity_type";
    private static final String COLUMN_ACTIVITY_NAME = "name";
    private static final String COLUMN_START_TIMESTAMP = "start_timestamp";
    private static final String COLUMN_END_TIMESTAMP = "end_timestamp";
    private static final String COLUMN_START_LOCATION = "start_location";
    private static final String COLUMN_END_LOCATION = "end_location";
    private static final String COLUMN_DESC = "description";
    private static final String COLUMN_DISTANCE = "distance";
    private static final String COLUMN_ELAPSED_TIME = "elapsed_time";
    private static final String COLUMN_ADDRESS = "address"; // New column for address
    private Context context;
    private static final String TAG = "DatabaseHelper";

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public void beginTransaction() {
        getWritableDatabase().beginTransaction();
    }

    public void setTransactionSuccessful() {
        getWritableDatabase().setTransactionSuccessful();
    }

    public void endTransaction() {
        getWritableDatabase().endTransaction();
    }

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LOCATIONS, null, null);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Check if locations table exists
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{TABLE_LOCATIONS});
        if (cursor.getCount() == 0) {
            String CREATE_TABLE = "CREATE TABLE " + TABLE_LOCATIONS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_LATITUDE + " REAL,"
                    + COLUMN_LONGITUDE + " REAL,"
                    + COLUMN_ALTITUDE + " REAL,"
                    + COLUMN_TIMESTAMP + " INTEGER" + ")";
            db.execSQL(CREATE_TABLE);
        }
        cursor.close();

        // Check if activities table exists
        cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{TABLE_ACTIVITIES});
        if (cursor.getCount() == 0) {
            String CREATE_ACTIVITIES_TABLE = "CREATE TABLE " + TABLE_ACTIVITIES + "("
                    + COLUMN_ACTIVITY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_ACTIVITY_TYPE + " TEXT,"
                    + COLUMN_ACTIVITY_NAME + " TEXT,"
                    + COLUMN_START_TIMESTAMP + " INTEGER,"
                    + COLUMN_END_TIMESTAMP + " INTEGER,"
                    + COLUMN_START_LOCATION + " INTEGER,"
                    + COLUMN_END_LOCATION + " INTEGER,"
                    + COLUMN_DESC + " TEXT,"
                    + COLUMN_DISTANCE + " REAL,"
                    + COLUMN_ELAPSED_TIME + " INTEGER,"
                    + COLUMN_ADDRESS + " TEXT" // New column
                    + ")";
            db.execSQL(CREATE_ACTIVITIES_TABLE);
        }
        cursor.close();
    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if existed
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACTIVITIES);
        if (oldVersion < 5) {
            // Add the new address column to the existing table
            db.execSQL("ALTER TABLE " + TABLE_ACTIVITIES + " ADD COLUMN " + COLUMN_ADDRESS + " TEXT");
        }

        // Create tables again
        onCreate(db);
    }

    public List<LocationData> getLocationDataForDateRange(long startTime, long endTime) {
        List<LocationData> locationList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_LOCATIONS +
                " WHERE " + COLUMN_TIMESTAMP + " >= ? AND " + COLUMN_TIMESTAMP + " < ?" +
                " ORDER BY " + COLUMN_TIMESTAMP + " ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(startTime), String.valueOf(endTime)});

        if (cursor.moveToFirst()) {
            do {
                LocationData location = new LocationData(
                        cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_ALTITUDE)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                );
                locationList.add(location);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return locationList;
    }

    public long insertActivity(ActivityData activity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ACTIVITY_TYPE, activity.getType());
        values.put(COLUMN_ACTIVITY_NAME, activity.getName());
        values.put(COLUMN_START_TIMESTAMP, activity.getStartTimestamp());
        values.put(COLUMN_END_TIMESTAMP, activity.getEndTimestamp());
        values.put(COLUMN_DISTANCE, activity.getDistance());
        values.put(COLUMN_ELAPSED_TIME, activity.getElapsedTime());
        values.put(COLUMN_ADDRESS, activity.getAddress());

        return db.insert(TABLE_ACTIVITIES, null, values);
    }


    public long insertActivity(String activityType, String name, long startTimestamp, long startLocationId, String address) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ACTIVITY_TYPE, activityType);
        values.put(COLUMN_ACTIVITY_NAME, name);
        values.put(COLUMN_START_TIMESTAMP, startTimestamp);
        values.put(COLUMN_START_LOCATION, startLocationId);
        values.put(COLUMN_DESC, "Activity started");
        values.put(COLUMN_ADDRESS, address); // Add address
        return db.insert(TABLE_ACTIVITIES, null, values);
    }

    public void updateActivity(long activityId, long endTimestamp, long startLocationId, long endLocationId, double distance, long elapsedTime, String address) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_END_TIMESTAMP, endTimestamp);
        values.put(COLUMN_START_LOCATION, startLocationId);
        values.put(COLUMN_END_LOCATION, endLocationId);
        values.put(COLUMN_DISTANCE, distance);
        values.put(COLUMN_ELAPSED_TIME, elapsedTime);
        values.put(COLUMN_DESC, "Activity completed");
        values.put(COLUMN_ADDRESS, address); // Update address
        db.update(TABLE_ACTIVITIES, values, COLUMN_ACTIVITY_ID + " = ?", new String[]{String.valueOf(activityId)});
    }

    public void deleteAllActivities() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ACTIVITIES, null, null);
    }

    public ActivityData getUnfinishedActivity() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_ACTIVITIES +
                " WHERE " + COLUMN_END_TIMESTAMP + " IS NULL " +
                "ORDER BY " + COLUMN_START_TIMESTAMP + " DESC LIMIT 1";

        Cursor cursor = db.rawQuery(query, null);
        ActivityData activity = null;

        if (cursor.moveToFirst()) {
            activity = new ActivityData(
                    cursor.getLong(cursor.getColumnIndex(COLUMN_ACTIVITY_ID)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_ACTIVITY_TYPE)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_ACTIVITY_NAME)),
                    cursor.getLong(cursor.getColumnIndex(COLUMN_START_TIMESTAMP)),
                    cursor.getLong(cursor.getColumnIndex(COLUMN_END_TIMESTAMP)),
                    cursor.getLong(cursor.getColumnIndex(COLUMN_START_LOCATION)),
                    cursor.getLong(cursor.getColumnIndex(COLUMN_END_LOCATION)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_DISTANCE)),
                    cursor.getLong(cursor.getColumnIndex(COLUMN_ELAPSED_TIME)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_ADDRESS))
            );
        }

        cursor.close();
        return activity;
    }

    public List<ActivityData> getAllActivities() {
        List<ActivityData> activities = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_ACTIVITIES +
                " ORDER BY " + COLUMN_START_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                ActivityData activity = new ActivityData(
                        cursor.getLong(cursor.getColumnIndex(COLUMN_ACTIVITY_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_ACTIVITY_TYPE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_ACTIVITY_NAME)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_START_TIMESTAMP)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_END_TIMESTAMP)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_START_LOCATION)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_END_LOCATION)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_DISTANCE)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_ELAPSED_TIME)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_ADDRESS))
                );

                activities.add(activity);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return activities;
    }


    public ActivityData getActivity(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ACTIVITIES, null, COLUMN_ACTIVITY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);

        ActivityData activity = null;

        if (cursor.moveToFirst()) {
            activity = new ActivityData(
                    cursor.getLong(cursor.getColumnIndex(COLUMN_ACTIVITY_ID)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_ACTIVITY_TYPE)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_ACTIVITY_NAME)),
                    cursor.getLong(cursor.getColumnIndex(COLUMN_START_TIMESTAMP)),
                    cursor.getLong(cursor.getColumnIndex(COLUMN_END_TIMESTAMP)),
                    cursor.getLong(cursor.getColumnIndex(COLUMN_START_LOCATION)),
                    cursor.getLong(cursor.getColumnIndex(COLUMN_END_LOCATION)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_DISTANCE)),
                    cursor.getLong(cursor.getColumnIndex(COLUMN_ELAPSED_TIME)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_ADDRESS))
            );
        }
        cursor.close();
        return activity;
    }

    public void deleteActivity(long activityId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ACTIVITIES, COLUMN_ACTIVITY_ID + " = ?", new String[]{String.valueOf(activityId)});
    }

    public LocationData getFirstLocationAfterTimestamp(long timestamp) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_LOCATIONS +
                " WHERE " + COLUMN_TIMESTAMP + " >= ?" +
                " ORDER BY " + COLUMN_TIMESTAMP + " ASC LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(timestamp)});

        LocationData location = null;
        if (cursor.moveToFirst()) {
            location = new LocationData(
                    cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_ALTITUDE)),
                    cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))
            );
        }
        cursor.close();
        return location;
    }

    public List<LocationData> getLocationsBetweenTimestamps(long startTimestamp, long endTimestamp) {
        List<LocationData> locations = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_LOCATIONS +
                " WHERE " + COLUMN_TIMESTAMP + " BETWEEN ? AND ?" +
                " ORDER BY " + COLUMN_TIMESTAMP + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(startTimestamp), String.valueOf(endTimestamp)});

        if (cursor.moveToFirst()) {
            do {
                LocationData location = new LocationData(
                        cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_ALTITUDE)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                );
                locations.add(location);
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (locations.isEmpty()) return null;
        else return locations;
    }

    public List<LocationData> getAllLocationsDesc() {
        List<LocationData> locationList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_LOCATIONS + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                LocationData location = new LocationData(
                        cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_ALTITUDE)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                );
                locationList.add(location);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return locationList;
    }


    public List<LatLng> getAllPositions() {
        List<LatLng> positions = new ArrayList<>();
        String selectQuery = "SELECT " + COLUMN_LATITUDE + ", " + COLUMN_LONGITUDE + " FROM " + TABLE_LOCATIONS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                double latitude = cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE));
                double longitude = cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE));
                LatLng position = new LatLng(latitude, longitude);
                positions.add(position);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return positions;
    }

    public List<LocationData> getAllLocations() {
        List<LocationData> locationList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_LOCATIONS + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                LocationData location = new LocationData(
                        cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(COLUMN_ALTITUDE)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                );
                locationList.add(location);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return locationList;
    }

    public LocationData getLatestLocation() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LOCATIONS, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC", "1");

        LocationData location = null;
        if (cursor.moveToFirst()) {
            location = new LocationData(
                    cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_ALTITUDE)),
                    cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))
            );
        }
        cursor.close();
        return location;
    }

    public void addLocation(double latitude, double longitude, double altitude, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);
        values.put(COLUMN_ALTITUDE, altitude);
        values.put(COLUMN_TIMESTAMP, timestamp);
        db.insert(TABLE_LOCATIONS, null, values);
        db.close();
    }

    public int getLocationCount() {
        String countQuery = "SELECT * FROM " + TABLE_LOCATIONS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public LocationData getLastLocation() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LOCATIONS, null, null, null, null, null, COLUMN_ID + " DESC", "1");

        LocationData location = null;
        if (cursor.moveToFirst()) {
            location = new LocationData(
                    cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_ALTITUDE)),
                    cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))
            );
        }
        cursor.close();
        return location;
    }

    public void createLocationsInTransaction(List<LocationData> locations) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();

            for (LocationData location : locations) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_LATITUDE, location.getLatitude());
                values.put(COLUMN_LONGITUDE, location.getLongitude());
                values.put(COLUMN_ALTITUDE, location.getAltitude());
                values.put(COLUMN_TIMESTAMP, location.getTimestamp());

                db.insert(TABLE_LOCATIONS, null, values);
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "--m-- Error inserting locations in transaction", e);
        } finally {
            db.endTransaction();
        }
    }

    public long insertOrUpdateActivityWithLocations(ActivityData activity, List<LocationData> locations) {
        long activityId;
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.beginTransaction();

            // Insert or update the activity
            ContentValues activityValues = new ContentValues();
            activityValues.put(COLUMN_ACTIVITY_TYPE, activity.getType());
            activityValues.put(COLUMN_ACTIVITY_NAME, activity.getName());
            activityValues.put(COLUMN_START_TIMESTAMP, activity.getStartTimestamp());
            activityValues.put(COLUMN_END_TIMESTAMP, activity.getEndTimestamp());
            activityValues.put(COLUMN_DISTANCE, activity.getDistance());
            activityValues.put(COLUMN_ELAPSED_TIME, activity.getElapsedTime());
            activityValues.put(COLUMN_ADDRESS, activity.getAddress());

            if (activity.getId() > 0) {
                // Update existing activity
                db.update(TABLE_ACTIVITIES, activityValues, COLUMN_ACTIVITY_ID + " = ?", new String[]{String.valueOf(activity.getId())});
                activityId = activity.getId();
            } else {
                // Insert new activity
                activityId = db.insert(TABLE_ACTIVITIES, null, activityValues);
            }

            // Insert locations
            for (LocationData location : locations) {
                ContentValues locationValues = new ContentValues();
                locationValues.put(COLUMN_LATITUDE, location.getLatitude());
                locationValues.put(COLUMN_LONGITUDE, location.getLongitude());
                locationValues.put(COLUMN_ALTITUDE, location.getAltitude());
                locationValues.put(COLUMN_TIMESTAMP, location.getTimestamp());

                db.insert(TABLE_LOCATIONS, null, locationValues);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return activityId;
    }


    public void createPlacesTable() {
        String CREATE_PLACES_TABLE = "CREATE TABLE IF NOT EXISTS places (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "country TEXT DEFAULT 'Current Country'," +
                "type TEXT DEFAULT 'place'," +
                "name TEXT," +
                "address TEXT," +
                "first_visited INTEGER," +
                "number_of_visits INTEGER DEFAULT 0," +
                "last_visited INTEGER," +
                "lat REAL," +
                "lon REAL," +
                "alt REAL," +
                "memo TEXT)";
        getWritableDatabase().execSQL(CREATE_PLACES_TABLE);
    }

    // Add this new method
    public LatLng getLastKnownLocation() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_LATITUDE + ", " + COLUMN_LONGITUDE +
                " FROM " + TABLE_LOCATIONS +
                " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT 1";

        Cursor cursor = db.rawQuery(query, null);
        LatLng lastLocation = null;

        if (cursor.moveToFirst()) {
            double latitude = cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE));
            double longitude = cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE));
            lastLocation = new LatLng(latitude, longitude);
        }

        cursor.close();
        return lastLocation;
    }

    public int updatePlace(Place place) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        // Add all place fields to values
        return db.update("places", values, "id = ?", new String[]{String.valueOf(place.getId())});
    }

    public void deletePlace(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("places", "id = ?", new String[]{String.valueOf(id)});
    }


    public long addPlace(Place place) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("country", place.getCountry());
        values.put("type", place.getType());
        values.put("name", place.getName());
        values.put("address", place.getAddress());
        values.put("first_visited", place.getFirstVisited());
        values.put("number_of_visits", place.getNumberOfVisits());
        values.put("last_visited", place.getLastVisited());
        values.put("lat", place.getLat());
        values.put("lon", place.getLon());
        values.put("alt", place.getAlt());
        values.put("memo", place.getMemo());
        return db.insert("places", null, values);
    }

    public List<Place> getAllPlaces() {
        List<Place> places = new ArrayList<>();
        String selectQuery = "SELECT * FROM places";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Place place = new Place(
                        cursor.getLong(cursor.getColumnIndex("id")),
                        cursor.getString(cursor.getColumnIndex("country")),
                        cursor.getString(cursor.getColumnIndex("type")),
                        cursor.getString(cursor.getColumnIndex("name")),
                        cursor.getString(cursor.getColumnIndex("address")),
                        cursor.getLong(cursor.getColumnIndex("first_visited")),
                        cursor.getInt(cursor.getColumnIndex("number_of_visits")),
                        cursor.getLong(cursor.getColumnIndex("last_visited")),
                        cursor.getDouble(cursor.getColumnIndex("lat")),
                        cursor.getDouble(cursor.getColumnIndex("lon")),
                        cursor.getDouble(cursor.getColumnIndex("alt")),
                        cursor.getString(cursor.getColumnIndex("memo"))
                );
                places.add(place);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return places;
    }

}
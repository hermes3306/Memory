package com.jason.memory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
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
    private static final int DATABASE_VERSION = 10; // Increment this from 9 to 10
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

    // Add these constants
    private static final String TABLE_PLACES = "places";
    private static final String COLUMN_COUNTRY = "country";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_FIRST_VISITED = "first_visited";
    private static final String COLUMN_NUMBER_OF_VISITS = "number_of_visits";
    private static final String COLUMN_LAST_VISITED = "last_visited";
    private static final String COLUMN_LAT = "lat";
    private static final String COLUMN_LON = "lon";
    private static final String COLUMN_MEMO = "memo";

    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_MESSAGE_ID = "id";
    private static final String COLUMN_MESSAGE_SENDER = "sender";
    private static final String COLUMN_MESSAGE_CONTENT = "content";
    private static final String COLUMN_MESSAGE_IS_IMAGE = "is_image";
    private static final String COLUMN_MESSAGE_TIMESTAMP = "timestamp";


    private Context context;
    private static final String TAG = "DatabaseHelper";

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LOCATIONS, null, null);
    }

    public List<Place> searchPlacesByDistance(LatLng currentLocation, int distanceKm) {
        List<Place> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM places";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Place place = createPlaceFromCursor(cursor);
                double distance = calculateDistance(currentLocation.latitude, currentLocation.longitude,
                        place.getLat(), place.getLon());
                if (distance <= distanceKm) {
                    result.add(place);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        return result;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371; // in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return earthRadius * c;
    }


    private Place createPlaceFromCursor(Cursor cursor) {
        try {
            return new Place(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("country")),
                    cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("address")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("first_visited")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("number_of_visits")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("last_visited")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("lat")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("lon")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("alt")),
                    cursor.getString(cursor.getColumnIndexOrThrow("memo"))
            );
        } catch (IllegalArgumentException e) {
            Log.e("DatabaseHelper", "Column not found in cursor", e);
            return null;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error creating Place from cursor", e);
            return null;
        }
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
                    + COLUMN_ADDRESS + " TEXT"
                    + ")";
            db.execSQL(CREATE_ACTIVITIES_TABLE);
        }
        cursor.close();

        // Add this code to create the memories table
        String CREATE_MEMORIES_TABLE = "CREATE TABLE " + TABLE_MEMORIES + "("
                + COLUMN_MEMORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_MEMORY_TITLE + " TEXT,"
                + COLUMN_MEMORY_DATE + " TEXT,"
                + COLUMN_MEMORY_TEXT + " TEXT"
                + ")";
        db.execSQL(CREATE_MEMORIES_TABLE);

        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + COLUMN_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_MESSAGE_SENDER + " TEXT,"
                + COLUMN_MESSAGE_CONTENT + " TEXT,"
                + COLUMN_MESSAGE_IS_IMAGE + " INTEGER,"
                + COLUMN_MESSAGE_TIMESTAMP + " TEXT"
                + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);
    }

    public long addMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MESSAGE_SENDER, message.getSender());
        values.put(COLUMN_MESSAGE_CONTENT, message.getContent());
        values.put(COLUMN_MESSAGE_IS_IMAGE, message.isImage() ? 1 : 0);
        values.put(COLUMN_MESSAGE_TIMESTAMP, message.getTimestamp());
        return db.insert(TABLE_MESSAGES, null, values);
    }

    public List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " ORDER BY " + COLUMN_MESSAGE_TIMESTAMP + " ASC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Message message = new Message(
                        cursor.getString(cursor.getColumnIndex(COLUMN_MESSAGE_SENDER)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_MESSAGE_CONTENT)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_MESSAGE_IS_IMAGE)) == 1
                );
                message.setTimestamp(cursor.getString(cursor.getColumnIndex(COLUMN_MESSAGE_TIMESTAMP)));
                messages.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return messages;
    }

    public void deleteMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, COLUMN_MESSAGE_ID + " = ?", new String[]{String.valueOf(message.getId())});
        db.close();
    }

    public void clearAllMessages() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, null, null);
        db.close();
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion < 8)  {
        // Create messages table if it doesn't exist
        String CREATE_MESSAGES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGES + "("
                + COLUMN_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_MESSAGE_SENDER + " TEXT,"
                + COLUMN_MESSAGE_CONTENT + " TEXT,"
                + COLUMN_MESSAGE_IS_IMAGE + " INTEGER,"
                + COLUMN_MESSAGE_TIMESTAMP + " TEXT"
                + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);
        }

        if (oldVersion < 9) { // Adjust this condition based on when you're adding the table
            String CREATE_MEMORIES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_MEMORIES + "("
                    + COLUMN_MEMORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_MEMORY_TITLE + " TEXT,"
                    + COLUMN_MEMORY_DATE + " TEXT,"
                    + COLUMN_MEMORY_TEXT + " TEXT"
                    + ")";
            db.execSQL(CREATE_MEMORIES_TABLE);
        }
    }

    public void insertLocationsBatch(List<LocationData> locations) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            LocationData previousValidLocation = null;
            for (LocationData location : locations) {
                if (isValidLocation(location, previousValidLocation)) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_LATITUDE, location.getLatitude());
                    values.put(COLUMN_LONGITUDE, location.getLongitude());
                    values.put(COLUMN_ALTITUDE, location.getAltitude());
                    values.put(COLUMN_TIMESTAMP, location.getTimestamp());

                    long id = db.insert(TABLE_LOCATIONS, null, values);
                    location.setId(id);  // Set the ID of the LocationData object
                    previousValidLocation = location;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateActivity(ActivityData activity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_START_LOCATION, activity.getStartLocation());
        values.put(COLUMN_END_LOCATION, activity.getEndLocation());
        db.update(TABLE_ACTIVITIES, values, COLUMN_ACTIVITY_ID + " = ?",
                new String[]{String.valueOf(activity.getId())});
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

    public int updateActivity(long activityId, long endTimestamp, long startLocationId, long endLocationId, double distance, long elapsedTime, String address) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_END_TIMESTAMP, endTimestamp);
        values.put(COLUMN_START_LOCATION, startLocationId);
        values.put(COLUMN_END_LOCATION, endLocationId);
        values.put(COLUMN_DISTANCE, distance);
        values.put(COLUMN_ELAPSED_TIME, elapsedTime);
        values.put(COLUMN_DESC, "Activity completed");
        values.put(COLUMN_ADDRESS, address);

        int updatedRows = db.update(TABLE_ACTIVITIES, values, COLUMN_ACTIVITY_ID + " = ?", new String[]{String.valueOf(activityId)});
        Log.d(TAG, "--m-- Updated activity in database. Rows affected: " + updatedRows);
        return updatedRows;
    }


    public void updateActivity_old(long activityId, long endTimestamp, long startLocationId, long endLocationId, double distance, long elapsedTime, String address) {
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
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ActivityData activity = new ActivityData(
                            getColumnLong(cursor, COLUMN_ACTIVITY_ID),
                            getColumnString(cursor, COLUMN_ACTIVITY_TYPE),
                            getColumnString(cursor, COLUMN_ACTIVITY_NAME),
                            getColumnLong(cursor, COLUMN_START_TIMESTAMP),
                            getColumnLong(cursor, COLUMN_END_TIMESTAMP),
                            getColumnLong(cursor, COLUMN_START_LOCATION),
                            getColumnLong(cursor, COLUMN_END_LOCATION),
                            getColumnDouble(cursor, COLUMN_DISTANCE),
                            getColumnLong(cursor, COLUMN_ELAPSED_TIME),
                            getColumnString(cursor, COLUMN_ADDRESS)
                    );

                    activities.add(activity);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting activities: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }

        return activities;
    }

    // Helper methods to safely get column values
    private String getColumnString(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getString(columnIndex) : "";
    }

    private long getColumnLong(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getLong(columnIndex) : 0;
    }

    private double getColumnDouble(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getDouble(columnIndex) : 0.0;
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

    public boolean isActivityExist(String activityName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_ACTIVITIES + " WHERE " + COLUMN_NAME + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{activityName});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
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

    public boolean isValidLocation(LocationData currentLocation, LocationData previousLocation) {
        if (previousLocation == null) {
            return true; // First location is always valid
        }

        double distanceKm = calculateDistanceBetweenPoints(previousLocation, currentLocation);
        long timeDifferenceSeconds = (currentLocation.getTimestamp() - previousLocation.getTimestamp()) / 1000;

        // Check if the distance and time difference meet the thresholds
        if (distanceKm < Config.MIN_DISTANCE_THRESHOLD_KM) {
            return false; // Too close to the previous point
        }

        if (distanceKm > Config.MAX_DISTANCE_THRESHOLD_KM) {
            return false; // Too far from the previous point
        }

        if (timeDifferenceSeconds < Config.MIN_TIME_THRESHOLD_SECONDS) {
            return false; // Too soon after the previous point
        }

        // Calculate speed in km/h
        double speedKmh = (distanceKm / timeDifferenceSeconds) * 3600;

        if (speedKmh > Config.MAX_SPEED_THRESHOLD_KMH) {
            return false; // Speed is unreasonably high
        }

        return true;
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

    private double calculateDistanceBetweenPoints(LocationData start, LocationData end) {
        double earthRadius = 6371; // in kilometers
        double dLat = Math.toRadians(end.getLatitude() - start.getLatitude());
        double dLon = Math.toRadians(end.getLongitude() - start.getLongitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(start.getLatitude())) * Math.cos(Math.toRadians(end.getLatitude())) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c; // Distance in kilometers
    }

    public void deleteAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LOCATIONS, null, null);
        db.delete(TABLE_ACTIVITIES, null, null);
        db.delete(TABLE_PLACES, null, null);
        db.delete(TABLE_MEMORIES, null, null);
        db.delete(TABLE_MESSAGES, null, null);
    }


    public void addLocation(double latitude, double longitude, double altitude, long timestamp) {
        LocationData newLocation = new LocationData(0, latitude, longitude, altitude, timestamp);
        LocationData previousLocation = getLastLocation();

        if (isValidLocation(newLocation, previousLocation)) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_LATITUDE, latitude);
            values.put(COLUMN_LONGITUDE, longitude);
            values.put(COLUMN_ALTITUDE, altitude);
            values.put(COLUMN_TIMESTAMP, timestamp);
            db.insert(TABLE_LOCATIONS, null, values);
            db.close();
        }
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
        values.put("country", place.getCountry());
        values.put("type", place.getType());
        values.put("name", place.getName());
        values.put("address", place.getAddress());
        values.put("number_of_visits", place.getNumberOfVisits());
        values.put("last_visited", place.getLastVisited());
        values.put("lat", place.getLat());
        values.put("lon", place.getLon());
        values.put("alt", place.getAlt());
        values.put("memo", place.getMemo());
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
        String selectQuery = "SELECT * FROM places ORDER BY last_visited DESC";
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

    public List<Place> searchPlaces(String name, String address, String type, String memo) {
        List<Place> searchResults = new ArrayList<>();
        String query = "SELECT * FROM places WHERE " +
                "name LIKE ? AND " +
                "address LIKE ? AND " +
                "type LIKE ? AND " +
                "memo LIKE ?";

        String[] selectionArgs = new String[]{
                "%" + name + "%",
                "%" + address + "%",
                "%" + type + "%",
                "%" + memo + "%"
        };

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(query, selectionArgs);
            Log.d("DatabaseHelper", "Search query: " + query);
            Log.d("DatabaseHelper", "Search parameters: name=" + name + ", address=" + address + ", type=" + type + ", memo=" + memo);

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
                    searchResults.add(place);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error in searchPlaces: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.d("DatabaseHelper", "Search results count: " + searchResults.size());
        return searchResults;
    }

    public Place getPlaceByName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Place place = null;

        try {
            cursor = db.query(TABLE_PLACES, null, COLUMN_NAME + "=?", new String[]{name}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                place = new Place(
                        getColumnLongValue(cursor, COLUMN_ID),
                        getColumnStringValue(cursor, COLUMN_COUNTRY),
                        getColumnStringValue(cursor, COLUMN_TYPE),
                        getColumnStringValue(cursor, COLUMN_NAME),
                        getColumnStringValue(cursor, COLUMN_ADDRESS),
                        getColumnLongValue(cursor, COLUMN_FIRST_VISITED),
                        getColumnIntValue(cursor, COLUMN_NUMBER_OF_VISITS),
                        getColumnLongValue(cursor, COLUMN_LAST_VISITED),
                        getColumnDoubleValue(cursor, COLUMN_LAT),
                        getColumnDoubleValue(cursor, COLUMN_LON),
                        getColumnDoubleValue(cursor, COLUMN_ALTITUDE),
                        getColumnStringValue(cursor, COLUMN_MEMO)
                );
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error in getPlaceByName: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return place;
    }

    // Helper methods to safely get values from cursor
    private long getColumnLongValue(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex != -1) ? cursor.getLong(columnIndex) : 0;
    }

    private int getColumnIntValue(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex != -1) ? cursor.getInt(columnIndex) : 0;
    }

    private double getColumnDoubleValue(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex != -1) ? cursor.getDouble(columnIndex) : 0.0;
    }

    private String getColumnStringValue(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex != -1) ? cursor.getString(columnIndex) : "";
    }


    private static final String TABLE_MEMORIES = "memories";
    private static final String COLUMN_MEMORY_ID = "id";
    private static final String COLUMN_MEMORY_TITLE = "title";
    private static final String COLUMN_MEMORY_DATE = "date";
    private static final String COLUMN_MEMORY_TEXT = "memory_text";

    public long addMemory(MemoryItem memory) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MEMORY_TITLE, memory.getTitle());
        values.put(COLUMN_MEMORY_DATE, memory.getDate());
        values.put(COLUMN_MEMORY_TEXT, memory.getMemoryText());
        return db.insert(TABLE_MEMORIES, null, values);
    }

    public int updateMemory(MemoryItem memory) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MEMORY_TITLE, memory.getTitle());
        values.put(COLUMN_MEMORY_DATE, memory.getDate());
        values.put(COLUMN_MEMORY_TEXT, memory.getMemoryText());
        return db.update(TABLE_MEMORIES, values, COLUMN_MEMORY_ID + " = ?",
                new String[]{String.valueOf(memory.getId())});
    }

    public List<MemoryItem> getAllMemories() {
        List<MemoryItem> memories = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_MEMORIES + " ORDER BY " + COLUMN_MEMORY_DATE + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                MemoryItem memory = new MemoryItem(
                        cursor.getLong(cursor.getColumnIndex(COLUMN_MEMORY_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_MEMORY_TITLE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_MEMORY_DATE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_MEMORY_TEXT))
                );
                memories.add(memory);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return memories;
    }

    public MemoryItem getMemory(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEMORIES, null, COLUMN_MEMORY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);

        MemoryItem memory = null;
        if (cursor != null && cursor.moveToFirst()) {
            memory = new MemoryItem(
                    cursor.getLong(cursor.getColumnIndex(COLUMN_MEMORY_ID)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_MEMORY_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_MEMORY_DATE)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_MEMORY_TEXT))
            );
            cursor.close();
        }
        return memory;
    }

    public MemoryItem getMemoryItemByText(String memoryText) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEMORIES, null, COLUMN_MEMORY_TEXT + "=?",
                new String[]{memoryText}, null, null, null);

        MemoryItem memory = null;
        if (cursor != null && cursor.moveToFirst()) {
            memory = new MemoryItem(
                    cursor.getLong(cursor.getColumnIndex(COLUMN_MEMORY_ID)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_MEMORY_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_MEMORY_DATE)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_MEMORY_TEXT))
            );
            cursor.close();
        }
        return memory;
    }

    public long addMemoryItem(MemoryItem memoryItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MEMORY_TITLE, memoryItem.getTitle());
        values.put(COLUMN_MEMORY_DATE, memoryItem.getDate());
        values.put(COLUMN_MEMORY_TEXT, memoryItem.getMemoryText());
        return db.insert(TABLE_MEMORIES, null, values);
    }

    public int updateMemoryItem(MemoryItem memoryItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MEMORY_TITLE, memoryItem.getTitle());
        values.put(COLUMN_MEMORY_DATE, memoryItem.getDate());
        values.put(COLUMN_MEMORY_TEXT, memoryItem.getMemoryText());
        return db.update(TABLE_MEMORIES, values, COLUMN_MEMORY_ID + " = ?",
                new String[]{String.valueOf(memoryItem.getId())});
    }



    public int deleteMemory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_MEMORIES, COLUMN_MEMORY_ID + " = ?", new String[]{String.valueOf(id)});
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

    public void insertActivitiesBatch(List<ActivityData> activities) {
        SQLiteDatabase db = getWritableDatabase();
        for (ActivityData activity : activities) {
            ContentValues values = new ContentValues();
            // Set values for each column
            db.insert("activities", null, values);
        }
    }


}
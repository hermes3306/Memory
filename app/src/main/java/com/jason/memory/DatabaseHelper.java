package com.jason.memory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "LocationDatabase";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "locations";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_ALTITUDE = "altitude";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_LATITUDE + " REAL,"
                + COLUMN_LONGITUDE + " REAL,"
                + COLUMN_ALTITUDE + " REAL,"
                + COLUMN_TIMESTAMP + " INTEGER" + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public List<LocationData> getAllLocationsDesc() {
        List<LocationData> locationList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";

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

    public List<LocationData> getAllLocations() {
        List<LocationData> locationList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";

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
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC", "1");

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
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public int getLocationCount() {
        String countQuery = "SELECT * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public LocationData getLastLocation() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_ID + " DESC", "1");

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
}
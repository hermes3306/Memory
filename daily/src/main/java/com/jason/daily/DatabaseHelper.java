package com.jason.daily;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "LocationDatabase";
    private static final int DATABASE_VERSION = 2;
    public static final String TABLE_LOCS = "LOCS";
    private static final String COLUMN_ID = "id";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ALTITUDE = "altitude";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_IS_UPLOADED = "is_uploaded";


    private static final String TAG = "DatabaseHelper";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_LOCS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_LATITUDE + " REAL,"
                + COLUMN_LONGITUDE + " REAL,"
                + COLUMN_ALTITUDE + " REAL,"
                + COLUMN_TIMESTAMP + " INTEGER,"
                + COLUMN_IS_UPLOADED + " INTEGER DEFAULT 0" + ")";
        db.execSQL(CREATE_TABLE);
    }


    public void addLocation(double latitude, double longitude, double altitude, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);
        values.put(COLUMN_ALTITUDE, altitude);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_IS_UPLOADED, 0);
        db.insert(TABLE_LOCS, null, values);
        db.close();
    }

    public List<LocationData> getLocationsToUpload(long lastUploadTime) {
        List<LocationData> locations = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_LOCS +
                " WHERE " + COLUMN_TIMESTAMP + " > ? AND " + COLUMN_IS_UPLOADED + " = 0" +
                " ORDER BY " + COLUMN_TIMESTAMP + " ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(lastUploadTime)});

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
        return locations;
    }

    public List<LocationData> getLocationDataForDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long endOfDay = calendar.getTimeInMillis();

        return getLocationDataForDateRange(startOfDay, endOfDay);
    }

    public List<LocationData> getLocationDataForWeek(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfWeek = calendar.getTimeInMillis();

        calendar.add(Calendar.WEEK_OF_YEAR, 1);
        long endOfWeek = calendar.getTimeInMillis();

        return getLocationDataForDateRange(startOfWeek, endOfWeek);
    }

    public List<LocationData> getLocationDataForMonth(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        long endOfMonth = calendar.getTimeInMillis();

        return getLocationDataForDateRange(startOfMonth, endOfMonth);
    }

    // Existing method, kept for reference
    public List<LocationData> getLocationDataForDateRange(long startTime, long endTime) {
        List<LocationData> locationList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_LOCS +
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

    public void markLocationsAsUploaded(List<LocationData> locations) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (LocationData location : locations) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_IS_UPLOADED, 1);
                db.update(TABLE_LOCS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(location.getId())});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCS);
        onCreate(db);
    }

    public LocationData getLastLocation() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LOCS, null, null, null, null, null, COLUMN_ID + " DESC", "1");

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
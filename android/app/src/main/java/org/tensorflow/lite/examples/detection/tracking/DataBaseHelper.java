package org.tensorflow.lite.examples.detection.tracking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DataBaseHelper extends SQLiteOpenHelper {
    public static final String LOCATION_TABLE = "LOCATION_TABLE";
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_CITY = "City";
    public static final String COLUMN_STREET = "Street";
    public static final String COLUMN_LATITUDE = "Latitude";
    public static final String COLUMN_LONGITUDE = "Longitude";
    public static final String COLUMN_TIME = "Time_GPS";
    public static final String COLUMN_DISTANCE_BETWEEN_2_POINTS = "DistanceBetween2Points";
    public static final String COLUMN_PARKING_STATUS = "Parking_Status";
    public static final String COLUMN_TIME_DETECTOR = "Time_Detector";
    public static final String COLUMN_ACCURACY = "Accuracy";



    public DataBaseHelper(@Nullable Context context) {
        super(context, "final_GPS_TEST_15_May_01.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableStatement = "CREATE TABLE " + LOCATION_TABLE + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_CITY + " TEXT, " + COLUMN_STREET + " TEXT, " + COLUMN_LATITUDE + " DOUBLE, " + COLUMN_LONGITUDE + " DOUBLE, " + COLUMN_TIME + " TEXT, " + COLUMN_DISTANCE_BETWEEN_2_POINTS + " FLOAT, " + COLUMN_PARKING_STATUS + " INT, " + COLUMN_TIME_DETECTOR + " TEXT, " + COLUMN_ACCURACY + " FLOAT)";
        db.execSQL(createTableStatement);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public Boolean addOne(LocationData locationData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CITY,locationData.getCity());
        cv.put(COLUMN_STREET,locationData.getStreet());
        cv.put(COLUMN_LATITUDE, locationData.getLatitude());
        cv.put(COLUMN_LONGITUDE, locationData.getLongitude());
        cv.put(COLUMN_TIME, locationData.getTime().toString());
        cv.put(COLUMN_DISTANCE_BETWEEN_2_POINTS, locationData.getDistanceBetween2Points());
        cv.put(COLUMN_PARKING_STATUS, locationData.getParking_status());
        cv.put(COLUMN_TIME_DETECTOR, locationData.getTime_detector());
        cv.put(COLUMN_ACCURACY,locationData.getAccuracy_in_meters());

        long insert = db.insert(LOCATION_TABLE, null, cv);
        if (insert == -1) {
            return false;
        } else {
            return true;
        }
    }

   public void clearSQLiteDataBase() {

        SQLiteDatabase db = this.getWritableDatabase();
        if (db != null) {
            db.execSQL(String.format("DELETE FROM %s;", LOCATION_TABLE));
            db.execSQL("UPDATE sqlite_sequence SET seq = 0 WHERE name = ?;", new String[]{LOCATION_TABLE});
        }
    }

    public List<LocationData> getEveryone() {
        List<LocationData> locationDataList = new ArrayList<>();
        //get Data from the database

        String queryString = "SELECT * FROM " + LOCATION_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String city= cursor.getString(1);
                String street= cursor.getString(2);
                double latitude = cursor.getDouble(3);
                double longitude = cursor.getDouble(4);
                String time = cursor.getString(5);
                float distanceBetween2Points = cursor.getFloat(6);
                int parking_status = cursor.getInt(7);
                String time_detector = cursor.getString(8);
                float accuracy_in_meters = cursor.getFloat(9);

                LocationData LocationData = new LocationData(id,city,street, latitude, longitude, time, distanceBetween2Points,parking_status,time_detector,accuracy_in_meters);
                //LocationData LocationData = new LocationData(id,latitude,longitude,time);
                //LocationData.setDistanceBetween2Points(distanceBetween2Points);
                locationDataList.add(LocationData);
            } while (cursor.moveToNext());
        } else {

        }

        //close both the cursor and the db when done
        cursor.close();
        db.close();
        return locationDataList;
    }

    public LocationData getTheLastUpdate() {

        String queryString = "SELECT * FROM " + LOCATION_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        LocationData locationData = new LocationData();
        Cursor cursor = db.rawQuery(queryString, null);

        if (cursor.moveToLast()) {
            int id = cursor.getInt(0);
            String city= cursor.getString(1);
            String street= cursor.getString(2);
            double latitude = cursor.getDouble(3);
            double longitude = cursor.getDouble(4);
            String time = cursor.getString(5);
            float distanceBetween2Points = cursor.getFloat(6);
            int parking_status = cursor.getInt(7);
            String time_detector = cursor.getString(8);
            float accuracy_in_meters = cursor.getFloat(9);
            locationData = new LocationData(id,city,street, latitude, longitude, time, distanceBetween2Points,parking_status,time_detector,accuracy_in_meters);
        }

        //close both the cursor and the db when done
        cursor.close();
        db.close();
        return locationData;
    }
}


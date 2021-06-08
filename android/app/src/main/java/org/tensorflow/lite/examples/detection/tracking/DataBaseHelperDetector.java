package org.tensorflow.lite.examples.detection.tracking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DataBaseHelperDetector extends SQLiteOpenHelper {
    public static final String DETECTION_TABLE = "DETECTION_TABLE";
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_PARKING_STATUS = "Parking_Status";
    public static final String COLUMN_TIME_DETECTOR = "Time_Detector";
    public static final String COLUMN_FRAME_PER_SECOND = "Frame_Per_miliSecond";


    public DataBaseHelperDetector(@Nullable Context context) {
        super(context, "detector_4_25.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableStatement = "CREATE TABLE " + DETECTION_TABLE + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_PARKING_STATUS + " INT, " + COLUMN_TIME_DETECTOR + " TEXT, " + COLUMN_FRAME_PER_SECOND + " LONG)";
        db.execSQL(createTableStatement);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public Boolean addOne(DetectorResult detectorResult) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PARKING_STATUS, detectorResult.getParking_status());
        cv.put(COLUMN_TIME_DETECTOR, detectorResult.getTime_detector());
        cv.put(COLUMN_FRAME_PER_SECOND, detectorResult.getLastProcessingTimeMs());
        long insert = db.insert(DETECTION_TABLE, null, cv);
        if (insert == -1) {
            return false;
        } else {
            return true;
        }
    }

    public void clearSQLiteDataBase() {

        SQLiteDatabase db = this.getWritableDatabase();
        if (db != null) {
            db.execSQL(String.format("DELETE FROM %s;", DETECTION_TABLE));
            db.execSQL("UPDATE sqlite_sequence SET seq = 0 WHERE name = ?;", new String[]{DETECTION_TABLE});
        }
    }

    public DetectorResult getTheLastUpdate() {

        String queryString = "SELECT * FROM " + DETECTION_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        DetectorResult detectorResult = new DetectorResult();
        Cursor cursor = db.rawQuery(queryString, null);

        if (cursor.moveToLast()) {
            int id = cursor.getInt(0);
            int parking_status = cursor.getInt(1);
            String time_detector = cursor.getString(2);
            long lastProcessingTimeMs= cursor.getLong(3);

            detectorResult = new DetectorResult(id,parking_status,time_detector,lastProcessingTimeMs);
        }

        //close both the cursor and the db when done
        cursor.close();
        db.close();
        return detectorResult;
    }
}

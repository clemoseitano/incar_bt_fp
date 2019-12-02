package com.fgtit.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import java.io.File;

/**
 * The class of configuring the SQLite database
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final int DB_USER_VERSION = 1;
    private static final String DB_USER = "user.db";
    public static final String TABLE_USER = "User";
    public static final String TABLE_USER_ID = "userId";
    public static final String TABLE_USER_ENROL1 = "userEnrol1";

    // fp_respondent table fields for enrolment
    public static final String FP_RESPONDENT_TABLE = "fp_respondent";
    public static final String FP_RESPONDENT_ENROLMENT_ID = "enrolment_id";
    public static final String FP_RESPONDENT_VERIFICATION_COUNT = "verification_count";
    public static final String FP_RESPONDENT_HAS_IMAGES = "has_images";
    public static final String CREATED_AT = "created_at";

    // fp_finger_records fields
    public static final String FP_FINGER_RECORDS_TABLE = "fp_finger_records";
    /**
     * the enrolment_id of the respondent
     */
    public static final String FINGER_RECORD_RESPONDENT_ID = "fp_respondent_id";
    /**
     * the finger that is recorded, i.e. thumb, pinky, etc. {@see Constants#FINGERS}
     */
    public static final String FINGER_RECORD_FINGER_ID = "fp_finger_id";
    public static final String FINGER_RECORD_JPG_IMAGE = "fp_jpg_image";
    public static final String FINGER_RECORD_WSQ_IMAGE = "fp_wsq_image";
    public static final String FINGER_RECORD_DATA = "fp_data";
    public static final String FINGER_RECORD_RAW_FILE = "fp_raw_file";
    public static final String FINGER_RECORD_VERIFICATION_INDEX = "fp_verification_index";
    /**
     * the id of the fingerprint that is taken
     */
    public static final String FINGER_RECORD_FINGER_RECORD_ID = "fp_finger_record_id";

    private static final String CREATE_FP_RESPONDENT_TABLE = "CREATE TABLE " +
            FP_RESPONDENT_TABLE +
            " ( " +
            " `id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
            FP_RESPONDENT_ENROLMENT_ID +
            " TEXT NOT NULL UNIQUE, " +
            FP_RESPONDENT_VERIFICATION_COUNT +
            " INTEGER NOT NULL, " +
            CREATED_AT +
            " DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            FP_RESPONDENT_HAS_IMAGES +
            " INTEGER NOT NULL DEFAULT 0" +
            ");";

    private static final String CREATE_FP_FINGER_RECORDS_TABLE = "CREATE TABLE " +
            FP_FINGER_RECORDS_TABLE +
            " ( " +
            " `id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
            FINGER_RECORD_RESPONDENT_ID +
            " TEXT NOT NULL, " +
            FINGER_RECORD_FINGER_ID +
            " INTEGER NOT NULL, " +
            FINGER_RECORD_JPG_IMAGE +
            " TEXT, " +
            FINGER_RECORD_DATA +
            " BLOB, " +
            FINGER_RECORD_WSQ_IMAGE +
            " TEXT, " +
            FINGER_RECORD_RAW_FILE +
            " TEXT, " +
            FINGER_RECORD_VERIFICATION_INDEX +
            " INTEGER NOT NULL, " +
            CREATED_AT +
            " DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            FINGER_RECORD_FINGER_RECORD_ID +
            " TEXT NOT NULL " +
            ");";


    public DBHelper(Context context) {
        super(context, getMyDatabaseName(context), null, DB_USER_VERSION);
    }

    /**
     * re-write getMyDatabaseName method
     * save it into SD card
     *
     * @param context
     * @return name of the database
     */
    private static String getMyDatabaseName(Context context) {
        String databasename;
        boolean isSdcardEnable = false;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {  //check SDCard is plug in or not
            isSdcardEnable = true;
        }
        String dbPath;
        if (isSdcardEnable) {
            dbPath = Environment.getExternalStorageDirectory().getPath() + "/Fingerprint Data/";
        } else {   //if SDCard is not plugged save it into memory
            dbPath = context.getFilesDir().getPath() + "/Fingerprint Data/";
        }
        File dbp = new File(dbPath);
        if (!dbp.exists()) {
            dbp.mkdirs();
        }
        databasename = dbPath + DB_USER;
        return databasename;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String sql = "create table if not exists " + TABLE_USER + " ("
                + TABLE_USER_ID + " integer primary key AUTOINCREMENT,"
                + TABLE_USER_ENROL1 + " text)";
        sqLiteDatabase.execSQL(sql);
        sqLiteDatabase.execSQL(CREATE_FP_RESPONDENT_TABLE);
        sqLiteDatabase.execSQL(CREATE_FP_FINGER_RECORDS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}

package com.fgtit.utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.fgtit.fpcore.FPMatch;

import java.io.File;
import java.util.ArrayList;

/**
 * The class of configuring the SQLite database
 */
public class FPDatabase {

    private static final int DB_FP_VERSION = 1;
    private static final String FP_DATABASE_NAME = "fp_db";
    private DbHandler ourHandler;
    private Context mContext;
    private SQLiteDatabase ourDatabase;

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


    public FPDatabase(Context context) {
        mContext = context;
        ourHandler = new DbHandler(mContext, FP_DATABASE_NAME, null, DB_FP_VERSION);
        Log.d("Survey DB", "Database Created");
    }

    private class DbHandler extends SQLiteOpenHelper {

        public DbHandler(Context context, String DATABASE_NAME, SQLiteDatabase.CursorFactory k, int DATABASE_VERSION) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
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

    public FPDatabase open() {
        try {
            if(ourDatabase == null){
                ourDatabase = ourHandler.getWritableDatabase();
            }else if(!ourDatabase.isOpen()){
                ourDatabase = ourHandler.getWritableDatabase();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public boolean isOpen() throws SQLException {
        return ourDatabase != null && ourDatabase.isOpen();
    }

    public void close() throws SQLException {
        if(ourDatabase != null)
            ourDatabase.close();
    }

    public boolean isDBLocked() {
        return ourDatabase.inTransaction() || ourDatabase.isDbLockedByCurrentThread();
    }

    public void deleteAllUsers(){
        try {
            ourDatabase.delete(FPDatabase.TABLE_USER, null, null);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void execSQL(String sql){
        try {
            ourDatabase.execSQL("update sqlite_sequence set seq=0 where name='User'");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insertUser(byte mRefData[]){
        try {
            ContentValues values = new ContentValues();
            values.put(FPDatabase.TABLE_USER_ENROL1, mRefData);
            Log.e("DEBUG_DATA", "Ref Data length: " + mRefData.length);
            ourDatabase.insertOrThrow(FPDatabase.TABLE_USER, null, values);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public ArrayList<String> getUsers(byte mMatData[]){
        ArrayList<String> statusList = new ArrayList<>();
        try {
            Cursor cursor = ourDatabase.query(FPDatabase.TABLE_USER, null, null,
                    null, null, null, null, null);
            boolean matchFlag = false;
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(FPDatabase.TABLE_USER_ID));
                byte[] enrol1 = cursor.getBlob(cursor.getColumnIndex(FPDatabase.TABLE_USER_ENROL1));
                int ret = FPMatch.getInstance().MatchFingerData(enrol1, mMatData);
                if (ret > 70) {
                    statusList.add("Match OK,Finger = " + id + "!!");
                    matchFlag = true;
                    break;
                }
            }
            if(!matchFlag){
                statusList.add("Match Fail !!");
            }
            if(cursor.getCount() == 0){
                statusList.add("Match Fail !!");
            }
            cursor.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return statusList;
    }

    public FingerprintRespondent getFingerprintRespondent(String enrolmentId){
        try{
            String where = FP_RESPONDENT_ENROLMENT_ID + " = '" + enrolmentId+"'";
            @SuppressLint("Recycle") Cursor cursor = ourDatabase.query(FP_RESPONDENT_TABLE, null, where, null, null, null, null);
            if (cursor.getCount() > 0){
                cursor.moveToFirst();
                FingerprintRespondent fingerprintRespondent = new FingerprintRespondent();
                fingerprintRespondent.setEnrolmentId(enrolmentId);
                fingerprintRespondent.setId(cursor.getInt(cursor.getColumnIndex("id")));
                fingerprintRespondent.setCreatedAt(cursor.getString(cursor.getColumnIndex(CREATED_AT)));
                fingerprintRespondent.setHasImages(cursor.getInt(cursor.getColumnIndex(FP_RESPONDENT_HAS_IMAGES)));
                fingerprintRespondent.setVerificationCount(cursor.getInt(cursor.getColumnIndex(FP_RESPONDENT_VERIFICATION_COUNT)));
                return fingerprintRespondent;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<FingerprintRespondent> getFingerprintRespondents(){
        ArrayList<FingerprintRespondent> fingerprintRespondents = new ArrayList<>();
        try{
            @SuppressLint("Recycle") Cursor cursor = ourDatabase.query(FP_RESPONDENT_TABLE, null, null, null, null, null, null);
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    FingerprintRespondent fingerprintRespondent = new FingerprintRespondent();
                    fingerprintRespondent.setEnrolmentId(cursor.getString(cursor.getColumnIndex(FP_RESPONDENT_ENROLMENT_ID)));
                    fingerprintRespondent.setId(cursor.getInt(cursor.getColumnIndex("id")));
                    fingerprintRespondent.setCreatedAt(cursor.getString(cursor.getColumnIndex(CREATED_AT)));
                    fingerprintRespondent.setHasImages(cursor.getInt(cursor.getColumnIndex(FP_RESPONDENT_HAS_IMAGES)));
                    fingerprintRespondent.setVerificationCount(cursor.getInt(cursor.getColumnIndex(FP_RESPONDENT_VERIFICATION_COUNT)));
                    fingerprintRespondents.add(fingerprintRespondent);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return fingerprintRespondents;
    }

    public FingerprintResponse getFingerPrintResponse(String fingerPrintId){
        try{
            String where = FINGER_RECORD_FINGER_RECORD_ID + " = '" + fingerPrintId+"'";
            @SuppressLint("Recycle") Cursor cursor = ourDatabase.query(FP_FINGER_RECORDS_TABLE, null, where, null, null, null, null);
            if (cursor.getCount() > 0){
                cursor.moveToFirst();
                FingerprintResponse fingerprintResponse = new FingerprintResponse();
                fingerprintResponse.setFpFingerRecordId(fingerPrintId);
                fingerprintResponse.setId(cursor.getInt(cursor.getColumnIndex("id")));
                fingerprintResponse.setCreatedAt(cursor.getString(cursor.getColumnIndex(CREATED_AT)));
                fingerprintResponse.setFpData(cursor.getBlob(cursor.getColumnIndex(FINGER_RECORD_DATA)));
                fingerprintResponse.setFpJPGImage(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_JPG_IMAGE)));
                fingerprintResponse.setFpWSQImage(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_WSQ_IMAGE)));
                fingerprintResponse.setFpRawImage(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_RAW_FILE)));
                fingerprintResponse.setFpVerificationIndex(cursor.getInt(cursor.getColumnIndex(FINGER_RECORD_VERIFICATION_INDEX)));
                fingerprintResponse.setFpRespondentId(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_RESPONDENT_ID)));
                fingerprintResponse.setFpFingerId(cursor.getInt(cursor.getColumnIndex(FINGER_RECORD_FINGER_ID)));
                return fingerprintResponse;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<FingerprintResponse> getFingerprintResponses(){
        ArrayList<FingerprintResponse> fingerprintResponses = new ArrayList<>();
        try{
            @SuppressLint("Recycle") Cursor cursor = ourDatabase.query(FP_FINGER_RECORDS_TABLE, null, null, null, null, null, null);
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    FingerprintResponse fingerprintResponse = new FingerprintResponse();
                    fingerprintResponse.setFpFingerRecordId(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_FINGER_RECORD_ID)));
                    fingerprintResponse.setId(cursor.getInt(cursor.getColumnIndex("id")));
                    fingerprintResponse.setCreatedAt(cursor.getString(cursor.getColumnIndex(CREATED_AT)));
                    fingerprintResponse.setFpData(cursor.getBlob(cursor.getColumnIndex(FINGER_RECORD_DATA)));
                    fingerprintResponse.setFpJPGImage(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_JPG_IMAGE)));
                    fingerprintResponse.setFpWSQImage(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_WSQ_IMAGE)));
                    fingerprintResponse.setFpRawImage(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_RAW_FILE)));
                    fingerprintResponse.setFpVerificationIndex(cursor.getInt(cursor.getColumnIndex(FINGER_RECORD_VERIFICATION_INDEX)));
                    fingerprintResponse.setFpRespondentId(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_RESPONDENT_ID)));
                    fingerprintResponse.setFpFingerId(cursor.getInt(cursor.getColumnIndex(FINGER_RECORD_FINGER_ID)));

                    fingerprintResponses.add(fingerprintResponse);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return fingerprintResponses;
    }

    public ArrayList<FingerprintResponse> getFingerprintResponses(String fpRespondentId){
        ArrayList<FingerprintResponse> fingerprintResponses = new ArrayList<>();
        try{
            String where = FINGER_RECORD_RESPONDENT_ID + " = '" + fpRespondentId+"'";
            @SuppressLint("Recycle") Cursor cursor = ourDatabase.query(FP_FINGER_RECORDS_TABLE, null, where, null, null, null, null);
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    FingerprintResponse fingerprintResponse = new FingerprintResponse();
                    fingerprintResponse.setFpFingerRecordId(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_FINGER_RECORD_ID)));
                    fingerprintResponse.setId(cursor.getInt(cursor.getColumnIndex("id")));
                    fingerprintResponse.setCreatedAt(cursor.getString(cursor.getColumnIndex(CREATED_AT)));
                    fingerprintResponse.setFpData(cursor.getBlob(cursor.getColumnIndex(FINGER_RECORD_DATA)));
                    fingerprintResponse.setFpJPGImage(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_JPG_IMAGE)));
                    fingerprintResponse.setFpWSQImage(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_WSQ_IMAGE)));
                    fingerprintResponse.setFpRawImage(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_RAW_FILE)));
                    fingerprintResponse.setFpVerificationIndex(cursor.getInt(cursor.getColumnIndex(FINGER_RECORD_VERIFICATION_INDEX)));
                    fingerprintResponse.setFpRespondentId(cursor.getString(cursor.getColumnIndex(FINGER_RECORD_RESPONDENT_ID)));
                    fingerprintResponse.setFpFingerId(cursor.getInt(cursor.getColumnIndex(FINGER_RECORD_FINGER_ID)));

                    fingerprintResponses.add(fingerprintResponse);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return fingerprintResponses;
    }

    public void insertRespondent(String fpRespondentId, boolean captureImages, int requiredEnrolment){
        try{
            ContentValues values = new ContentValues();
            values.put(FPDatabase.FP_RESPONDENT_ENROLMENT_ID, fpRespondentId);
            values.put(FPDatabase.FP_RESPONDENT_HAS_IMAGES, captureImages ? 1 : 0);
            values.put(FPDatabase.FP_RESPONDENT_VERIFICATION_COUNT, requiredEnrolment);
            ourDatabase.insertOrThrow(FP_RESPONDENT_TABLE, null, values);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void createFingerprintResponse(String fpRespondentId, String currentFinger, byte[] mRefData, String fingerResponseId, int vc, String currentJPGFile, String currentWSQFile, String currentRAWFile ){
        try {
            ContentValues values = new ContentValues();
            values.put(FPDatabase.FINGER_RECORD_RESPONDENT_ID, fpRespondentId);
            if (TextUtils.isDigitsOnly(currentFinger)) {
                values.put(FPDatabase.FINGER_RECORD_FINGER_ID, Integer.parseInt(currentFinger));
            } else {
                values.put(FPDatabase.FINGER_RECORD_FINGER_ID, 0);
            }
            values.put(FPDatabase.FINGER_RECORD_DATA, mRefData);
            values.put(FPDatabase.FINGER_RECORD_FINGER_RECORD_ID, fingerResponseId);
            values.put(FPDatabase.FINGER_RECORD_VERIFICATION_INDEX, vc);
            values.put(FPDatabase.FINGER_RECORD_JPG_IMAGE, currentJPGFile);
            values.put(FPDatabase.FINGER_RECORD_WSQ_IMAGE, currentWSQFile);
            values.put(FPDatabase.FINGER_RECORD_RAW_FILE, currentRAWFile);
            ourDatabase.insertOrThrow(FPDatabase.FP_FINGER_RECORDS_TABLE, null, values);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

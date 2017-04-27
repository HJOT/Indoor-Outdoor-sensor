package com.aware.plugin.inorout;


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;


//Provider is copied from previous AWARE projects. Github: Heppu and Denzil.
public class Provider extends ContentProvider {
    /**
     * Authority of this content provider
     */
    public static String AUTHORITY = "com.aware.plugin.inorout.provider.inorout";
    /**
     * ContentProvider database version. Increment every time you modify the database structure
     */
    public static final int DATABASE_VERSION = 3;
    public static final class InOrOut implements BaseColumns {
        private InOrOut(){};
        /**
         * Your ContentProvider table content URI.<br/>
         * The last segment needs to match your database table name
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/plugin_inorout");
        /**
         * How your data collection is identified internally in Android (vnd.android.cursor.dir). <br/>
         * It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.inorout";
        /**
         * How each row is identified individually internally in Android (vnd.android.cursor.item). <br/>
         * It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.inorout";
        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String CURRENT_LIGHT = "light";
        public static final String CURRENT_SIGNAL1 = "signal1";
        public static final String CURRENT_SIGNAL2 = "signal2";
        public static final String CURRENT_PROXI = "proximity";
        public static final String CURRENT_SUNRISE = "sunrise";
        public static final String CURRENT_SUNSET = "sunset";
        public static final String CURRENT_BATTEMP = "battery_temperature";
        public static final String CURRENT_IN = "indoor_probability";
        public static final String CURRENT_OUT = "outdoor_probability";
        public static final String ELAPSED_INDOOR = "indoor_time";
        public static final String ELAPSED_OUTDOOR = "outdoor_time";
    }
    //ContentProvider query indexes
    private static final int INOROUT = 1;
    private static final int INOROUT_ID = 2;

    /**
     * Database stored in external folder: /AWARE/plugin_device_usage.db
     */
    public static final String DATABASE_NAME = Environment.getExternalStorageDirectory() + "/AWARE/plugin_inorout.db";
    /**
     * Database tables:<br/>
     * - plugin_phone_usage
     */
    public static final String[] DATABASE_TABLES = {"plugin_inorout"};
    /**
     * Database table fields
     */
    public static final String[] TABLES_FIELDS = {
            InOrOut._ID + " integer primary key autoincrement," +
                    InOrOut.TIMESTAMP + " real default 0," +
                    InOrOut.DEVICE_ID + " text default ''," +
                    InOrOut.CURRENT_LIGHT + " real default 0," +
                    InOrOut.CURRENT_SIGNAL1 + " real default 0," +
                    InOrOut.CURRENT_SIGNAL2 + " real default 0," +
                    InOrOut.CURRENT_PROXI + " real default 0," +
                    InOrOut.CURRENT_SUNRISE + " real default 0," +
                    InOrOut.CURRENT_SUNSET + " real default 0," +
                    InOrOut.CURRENT_BATTEMP + " real default 0," +
                    InOrOut.CURRENT_IN + " real default 0," +
                    InOrOut.CURRENT_OUT + " real default 0," +
                    InOrOut.ELAPSED_INDOOR + " real default 0," +
                    InOrOut.ELAPSED_OUTDOOR + " real default 0," +
                    "UNIQUE (" + InOrOut.TIMESTAMP+ "," + InOrOut.DEVICE_ID+")"
    };
    private static UriMatcher sUriMatcher = null;
    private static HashMap<String, String> tableMap = null;
    private static DatabaseHelper databaseHelper = null;
    private static SQLiteDatabase database = null;
    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper( getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen() )) {
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null && databaseHelper != null);
    }
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY, "Database unavailable...");
            return 0;
        }
        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case INOROUT:
                count = database.delete(DATABASE_TABLES[0], selection,
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case INOROUT:
                return InOrOut.CONTENT_TYPE;
            case INOROUT_ID:
                return InOrOut.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }
    @Override
    public Uri insert(Uri uri, ContentValues new_values) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }
        ContentValues values = (new_values != null) ? new ContentValues(new_values) : new ContentValues();
        switch (sUriMatcher.match(uri)) {
            case INOROUT:
                long _id = database.insert(DATABASE_TABLES[0],
                        InOrOut.DEVICE_ID, values);
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(
                            InOrOut.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                throw new android.database.SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }
    @Override
    public boolean onCreate() {
        AUTHORITY = getContext().getPackageName() + ".provider.inorout";
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], INOROUT); //URI for all records
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0]+"/#", INOROUT_ID); //URI for a single record
        tableMap = new HashMap<String, String>();
        tableMap.put(InOrOut._ID, InOrOut._ID);
        tableMap.put(InOrOut.TIMESTAMP, InOrOut.TIMESTAMP);
        tableMap.put(InOrOut.DEVICE_ID, InOrOut.DEVICE_ID);
        tableMap.put(InOrOut.CURRENT_LIGHT, InOrOut.CURRENT_LIGHT);
        tableMap.put(InOrOut.CURRENT_SIGNAL1, InOrOut.CURRENT_SIGNAL1);
        tableMap.put(InOrOut.CURRENT_SIGNAL2, InOrOut.CURRENT_SIGNAL2);
        tableMap.put(InOrOut.CURRENT_PROXI, InOrOut.CURRENT_PROXI);
        tableMap.put(InOrOut.CURRENT_SUNRISE, InOrOut.CURRENT_SUNRISE);
        tableMap.put(InOrOut.CURRENT_SUNSET, InOrOut.CURRENT_SUNSET);
        tableMap.put(InOrOut.CURRENT_BATTEMP, InOrOut.CURRENT_BATTEMP);
        tableMap.put(InOrOut.CURRENT_IN, InOrOut.CURRENT_IN);
        tableMap.put(InOrOut.CURRENT_OUT, InOrOut.CURRENT_OUT);
        tableMap.put(InOrOut.ELAPSED_INDOOR, InOrOut.ELAPSED_INDOOR);
        tableMap.put(InOrOut.ELAPSED_OUTDOOR, InOrOut.ELAPSED_OUTDOOR);
        return true;
    }
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case INOROUT:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(tableMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }
        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case INOROUT:
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                break;
            default:
                database.close();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}

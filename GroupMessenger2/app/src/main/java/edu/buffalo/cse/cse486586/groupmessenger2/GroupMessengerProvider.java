package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    private static final String TAG = "GroupMessengerProvider";
    private static final String DATABASE_NAME = "groupmessenger.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "MESSAGES";
    private DBHelper dBHelper;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        if (values.containsKey("key") && values.containsKey("value")){

            SQLiteDatabase database = dBHelper.getWritableDatabase();
            long rowId;
            try{
                rowId = database.insertOrThrow(TABLE_NAME, null, values);
            }
            catch (SQLiteException exception){
                String[] args = {values.getAsString("key")};
                rowId = database.update(TABLE_NAME,values,"key = ?",args);
//                Log.d(TAG, "rowId = " + String.valueOf(rowId));
            }
//            Log.i(TAG, "rowId = " + String.valueOf(rowId));

            if (rowId > 0) {
//                Log.v("insert", values.toString());
                return uri;
            }
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        dBHelper = new DBHelper(getContext());
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        String[] columnNames = {"key", "value"};
        MatrixCursor matrixCursor = new MatrixCursor(columnNames);

        SQLiteDatabase sqLiteDatabase = dBHelper.getReadableDatabase();
        String[] actualSelectionArgs = {selection};
        Cursor cursor = sqLiteDatabase.query(TABLE_NAME, columnNames, "key= ?", actualSelectionArgs,
                null, null, null);

        if( cursor.getColumnCount() == 2){
            cursor.moveToFirst();
            int keyColValue = cursor.getColumnIndex("key");
            int valueColValue = cursor.getColumnIndex("value");
            String key = null;
            String value = null;
            try{
                key = cursor.getString(keyColValue);
                value = cursor.getString(valueColValue);

            }
            catch (CursorIndexOutOfBoundsException exception){
                Log.e(TAG,selection);
                Log.e(TAG,selection.getClass().toString());
            }
            if(key != null && value != null){
                Object[] columnValues = {key, value};
                matrixCursor.addRow(columnValues);
            }

            return matrixCursor;
        }

//        Log.v("query", selection);
        return null;
    }

    private static class DBHelper extends SQLiteOpenHelper {

        DBHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            try{
                sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME + "("
                        + "key TEXT PRIMARY KEY,"
                        + "value TEXT"
                        + ")");
            }
            catch (SQLiteException exception){

                Log.e(TAG, exception.getMessage());
                exception.printStackTrace();
//                Log.e(TAG, exception.getStackTrace().toString());
            }

            Log.d(TAG, "Created DB");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
//            Log.w(TAG, "Upgrading database from version " + i + " to "
//                    + i1 + ", which will destroy all old data");
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(sqLiteDatabase);
        }
    }
}

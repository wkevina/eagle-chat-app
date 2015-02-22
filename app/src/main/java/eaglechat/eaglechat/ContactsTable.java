package eaglechat.eaglechat;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by kevinward on 2/15/15.
 */
public class ContactsTable {

    public static final String TABLE_NAME = "contacts";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_NETWORK_ID = "network_id";
    public static final String COLUMN_PUBLIC_KEY = "public_key";


    private static final String TABLE_CREATE = "create table " +
            TABLE_NAME +
            "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_NAME + " text not null, " +
            COLUMN_NETWORK_ID + " text not null, " +
            COLUMN_PUBLIC_KEY + " text not null" +
            ");";

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(ContactsTable.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}

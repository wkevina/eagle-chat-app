package eaglechat.eaglechat;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by kevinward on 2/15/15.
 */
public class MessagesTable {

    public static final String TABLE_NAME = "message";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SENDER = "sender";
    public static final String COLUMN_RECEIVER = "receiver";
    public static final String COLUMN_CONTENT = "content";

    private static final String TABLE_CREATE = "create table " +
            TABLE_NAME +
            "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_SENDER + " integer not null, " +
            COLUMN_RECEIVER + " integer not null, " +
            COLUMN_CONTENT + " text not null" +
            ");";

    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MessagesTable.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}

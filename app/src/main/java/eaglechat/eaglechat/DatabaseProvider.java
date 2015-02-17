package eaglechat.eaglechat;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class DatabaseProvider extends ContentProvider {
    public static final String AUTHORITY_STRING = "eaglechat.eaglechat.provider";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY_STRING);
    public static final Uri CONTACTS_URI =
            Uri.withAppendedPath(AUTHORITY_URI, ContactsTable.TABLE_NAME);

    public static final Uri MESSAGE_URI =
            Uri.withAppendedPath(AUTHORITY_URI, MessageTable.TABLE_NAME);

    public static final Uri CONTACTS_WITH_LAST_MESSAGE_URI =
            Uri.withAppendedPath(CONTACTS_URI, "with_message");

    public static final Uri DELETE_URI =
            Uri.withAppendedPath(AUTHORITY_URI, "delete");

    private static final int CONTACTS = 1;
    private static final int CONTACTS_ID = 2;
    private static final int MESSAGE = 3;
    private static final int MESSAGE_ID = 4;
    private static final int CONTACTS_WITH_LAST_MESSAGE = 5;
    private static final int ALL = 6;

    public static final String[] CONTACTS_WITH_LAST_MESSAGE_PROJECTION =
            new String[]{
                    ContactsTable.COLUMN_ID,
                    ContactsTable.COLUMN_NAME,
                    MessageTable.COLUMN_CONTENT
            };

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY_STRING, "delete", ALL);

        sUriMatcher.addURI(AUTHORITY_STRING, CONTACTS_URI.getPath(), CONTACTS);
        sUriMatcher.addURI(AUTHORITY_STRING, CONTACTS_URI.getPath() + "/#", CONTACTS_ID);

        sUriMatcher.addURI(AUTHORITY_STRING, MESSAGE_URI.getPath(), MESSAGE);
        sUriMatcher.addURI(AUTHORITY_STRING, MESSAGE_URI.getPath() + "/#", MESSAGE_ID);

        sUriMatcher.addURI(AUTHORITY_STRING, CONTACTS_WITH_LAST_MESSAGE_URI.getPath(), CONTACTS_WITH_LAST_MESSAGE);
    }

    private DatabaseHelper mHelper;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case ALL:
                deleteDatabase();
                getContext().getContentResolver().notifyChange(uri, null);
                getContext().getContentResolver().notifyChange(MESSAGE_URI, null);
                getContext().getContentResolver().notifyChange(CONTACTS_URI, null);
                return 0;
        }
        throw new UnsupportedOperationException(String.format("Not yet implemented, delete uri=%s", uri));
    }

    private void deleteDatabase() {
        //SQLiteDatabase db = mHelper.getWritableDatabase();
        getContext().deleteDatabase(mHelper.getDatabaseName());
        mHelper.close();
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri newUri = null;
        long id;
        switch (sUriMatcher.match(uri)) {
            case CONTACTS:
                id = insertIntoTable(ContactsTable.TABLE_NAME, values);
                newUri = insertUri(CONTACTS_URI, id);
                break;
            case MESSAGE:
                id = insertIntoTable(MessageTable.TABLE_NAME, values);
                newUri = insertUri(MESSAGE_URI, id);
                break;
            default:
                break;
        }
        if (newUri != null) {
            getContext().getContentResolver().notifyChange(AUTHORITY_URI, null);
            Log.d(getClass().getSimpleName(), String.format("Inserted row at: %s", newUri));
            return newUri;
        }
        //Cursor cursor = query.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        throw new UnsupportedOperationException("Uri not yet implemented: " + uri.toString());
    }

    private long insertIntoTable(String table, ContentValues values) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        return db.insert(table, null, values);
    }

    private Uri insertUri(Uri baseUri, long id) {
        return baseUri.buildUpon().appendPath(String.valueOf(id)).build();
    }


    @Override
    public boolean onCreate() {
        mHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Cursor cursor = null;
        switch (sUriMatcher.match(uri)) {
            case CONTACTS:
                cursor = queryTable(ContactsTable.TABLE_NAME, projection, selection, selectionArgs, sortOrder);
                break;
            case MESSAGE:
                cursor = queryTable(MessageTable.TABLE_NAME, projection, selection, selectionArgs, sortOrder);
                break;
            case CONTACTS_WITH_LAST_MESSAGE:
                cursor = queryContactsWithLastMessage(projection);
                break;
            default:
                break;
        }
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), AUTHORITY_URI);
            return cursor;
        }
        //Cursor cursor = query.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        throw new UnsupportedOperationException("Uri not yet implemented: " + uri.toString());
    }

    private Cursor queryContactsWithLastMessage(String[] projection) {
        String contacts = ContactsTable.TABLE_NAME;
        String contactsId = contacts + "." + ContactsTable.COLUMN_ID;

        String message = MessageTable.TABLE_NAME;
        String messageId = MessageTable.TABLE_NAME + "." + MessageTable.COLUMN_ID;
        String messageSender = message + "." + MessageTable.COLUMN_SENDER;
        String messageReceiver = message + "." + MessageTable.COLUMN_RECEIVER;

        SQLiteDatabase db = mHelper.getReadableDatabase();
        SQLiteQueryBuilder query = new SQLiteQueryBuilder();
        query.setTables(String.format(
                "%s LEFT OUTER JOIN %s " + // contacts ... message
                        "ON (%s = (select max(%s) FROM %s where %s = %s OR %s = %s)) ",
/* ON message.id = (select max(message.id) where message.receiver = contacts.id OR message.sender = contacts.id */
                contacts, message,
                messageId, messageId, message, messageReceiver, contactsId, messageSender, contactsId
        ));
        return query.query(db, projection, null, null, null, null, ContactsTable.COLUMN_NAME);
    }

    private Cursor queryTable(String table, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        SQLiteQueryBuilder query = new SQLiteQueryBuilder();
        query.setTables(table);
        Cursor cursor = query.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

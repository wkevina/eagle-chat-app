package eaglechat.eaglechat;

import android.content.ContentProvider;
import android.content.ContentUris;
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

    public static final Uri MESSAGES_URI =
            Uri.withAppendedPath(AUTHORITY_URI, MessagesTable.TABLE_NAME);

    public static final Uri CONTACTS_WITH_LAST_MESSAGE_URI =
            Uri.withAppendedPath(CONTACTS_URI, "with_messages");

    public static final Uri MESSAGES_FROM_CONTACT_URI =
            Uri.withAppendedPath(MESSAGES_URI, "contact");

    public static final Uri DELETE_URI =
            Uri.withAppendedPath(AUTHORITY_URI, "delete");

    private static final int CONTACTS = 1;
    private static final int CONTACTS_ID = 2;
    private static final int MESSAGES = 3;
    private static final int MESSAGES_ID = 4;
    private static final int CONTACTS_WITH_LAST_MESSAGE = 5;
    private static final int MESSAGES_FROM_CONTACT = 6;
    private static final int ALL = 7;

    public static final String[] CONTACTS_WITH_LAST_MESSAGE_PROJECTION =
            new String[]{
                    ContactsTable.COLUMN_ID,
                    ContactsTable.COLUMN_NAME,
                    MessagesTable.COLUMN_CONTENT
            };

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY_STRING, "delete", ALL);

        sUriMatcher.addURI(AUTHORITY_STRING, CONTACTS_URI.getPath(), CONTACTS);
        sUriMatcher.addURI(AUTHORITY_STRING, CONTACTS_URI.getPath() + "/#", CONTACTS_ID);

        sUriMatcher.addURI(AUTHORITY_STRING, MESSAGES_URI.getPath(), MESSAGES);
        sUriMatcher.addURI(AUTHORITY_STRING, MESSAGES_URI.getPath() + "/#", MESSAGES_ID);

        sUriMatcher.addURI(AUTHORITY_STRING, CONTACTS_WITH_LAST_MESSAGE_URI.getPath(), CONTACTS_WITH_LAST_MESSAGE);
        sUriMatcher.addURI(AUTHORITY_STRING, MESSAGES_FROM_CONTACT_URI.getPath() + "/#", MESSAGES_FROM_CONTACT);
    }

    private DatabaseHelper mHelper;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case ALL:
                deleteDatabase();
                getContext().getContentResolver().notifyChange(uri, null);
                getContext().getContentResolver().notifyChange(MESSAGES_URI, null);
                getContext().getContentResolver().notifyChange(CONTACTS_URI, null);
                return 0;
        }
        throw new UnsupportedOperationException(String.format("Not yet implemented, delete uri=%s", uri));
    }

    private void deleteDatabase() {
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
            case MESSAGES:
                id = insertIntoTable(MessagesTable.TABLE_NAME, values);
                newUri = insertUri(MESSAGES_URI, id);
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
            case CONTACTS_ID:
                Log.d(getCallingPackage(), uri.toString());
                cursor = queryTable(
                        ContactsTable.TABLE_NAME,
                        //"CONTAKT",
                        projection,
                        ContactsTable.COLUMN_ID + " = ?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))},
                        sortOrder);
                break;
            case MESSAGES:
                cursor = queryTable(MessagesTable.TABLE_NAME, projection, selection, selectionArgs, sortOrder);
                break;
            case CONTACTS_WITH_LAST_MESSAGE:
                cursor = queryContactsWithLastMessage(projection);
                break;
            case MESSAGES_FROM_CONTACT:
                cursor = queryMessagesWithContact(uri, projection, selection, selectionArgs, sortOrder);
                break;
            default:
                break;
        }
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), AUTHORITY_URI);
            return cursor;
        }

        throw new UnsupportedOperationException("Uri not yet implemented: " + uri.toString());
    }

    private Cursor queryMessagesWithContact(Uri uri, String[] projection, String selection,
                                            String[] selectionArgs, String sortOrder) {
        long id = ContentUris.parseId(uri);

        SQLiteDatabase db = mHelper.getReadableDatabase();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        builder.setTables(MessagesTable.TABLE_NAME);

        builder.appendWhere( // Select messages where the sender or receiver is the contact._id
                String.format(
                        "%s = %s OR %s = %s",
                        MessagesTable.COLUMN_RECEIVER, id, MessagesTable.COLUMN_SENDER, id
                )
        );

        return builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        /*
        return new CursorLoader(this, DatabaseProvider.MESSAGES_URI, null,
                MessagesTable.COLUMN_RECEIVER +" = ? OR "+ MessagesTable.COLUMN_SENDER + " = ?",
                new String[] {String.valueOf(mContactId), String.valueOf(mContactId)},
                MessagesTable.COLUMN_ID);
        */
    }

    private Cursor queryContactsWithLastMessage(String[] projection) {
        String contacts = ContactsTable.TABLE_NAME;
        String contactsId = contacts + "." + ContactsTable.COLUMN_ID;

        String message = MessagesTable.TABLE_NAME;
        String messageId = MessagesTable.TABLE_NAME + "." + MessagesTable.COLUMN_ID;
        String messageSender = message + "." + MessagesTable.COLUMN_SENDER;
        String messageReceiver = message + "." + MessagesTable.COLUMN_RECEIVER;

        SQLiteDatabase db = mHelper.getReadableDatabase();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(String.format(
                "%s LEFT OUTER JOIN %s " + // contacts ... message
                        "ON (%s = (select max(%s) FROM %s where %s = %s OR %s = %s)) ",
/* ON message.id = (select max(message.id) where message.receiver = contacts.id OR message.sender = contacts.id */
                contacts, message,
                messageId, messageId, message, messageReceiver, contactsId, messageSender, contactsId
        ));
        return builder.query(db, projection, null, null, null, null, ContactsTable.COLUMN_NAME);
    }

    private Cursor queryTable(String table, String[] projection, String selection,
                              String[] selectionArgs, String sortOrder)
    {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(table);
        return builder.query(db, projection, selection, selectionArgs,
                null, null, sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
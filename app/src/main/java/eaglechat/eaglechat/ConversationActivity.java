package eaglechat.eaglechat;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ConversationActivity extends CompatListActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {
    public static final String TAG = "ConversationActivity";
    public static final String CONTACT_ID = TAG + ".CONTACT_ID";

    private static final int MESSAGES_LOADER = 0;
    private static final int CONTACTS_LOADER = 1;

    private static final String
            NO_CONTENT_STRING = "There are no messages between you and this contact. " +
            "Use the text field below to send a message.";
    private EditText mTextMessage;

    private long mContactId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mContactId = bundle.getLong(CONTACT_ID, 0);
            Log.d(getLocalClassName(), String.format("Contact id=%d", mContactId));
        }

        ListAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.two_line_list_item,
                null,
                new String[]{MessageTable.COLUMN_SENDER, MessageTable.COLUMN_CONTENT},
                new int[]{android.R.id.text1, android.R.id.text2},
                SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        );
        setListAdapter(adapter);

        TextView emptyText = (TextView) findViewById(android.R.id.empty);
        if (emptyText != null) {
            emptyText.setText(NO_CONTENT_STRING);
        }

        getLoaderManager().initLoader(MESSAGES_LOADER, null, this);

        mTextMessage = (EditText) findViewById(R.id.text_message);
        findViewById(R.id.button_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        /*
        MatrixCursor cursor = new MatrixCursor(new String[]{"_id", "SENDER", "MESSAGE"});

        ListAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.two_line_list_item,
                cursor,
                new String[]{"SENDER", "MESSAGE"},
                new int[]{android.R.id.text1, android.R.id.text2},
                SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        );
        setListAdapter(adapter);

        for (int i = 1; i < 25; ++i) {
            cursor.addRow(new Object[]{i, "me", "this is a message " + Integer.toString(i)});
        }
        */
    }

    private void sendMessage() {
        final String message = mTextMessage.getText().toString();
        if (!message.isEmpty()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    ContentValues values = new ContentValues();
                    values.put(MessageTable.COLUMN_RECEIVER, mContactId);
                    values.put(MessageTable.COLUMN_SENDER, 0);
                    values.put(MessageTable.COLUMN_CONTENT, message);
                    getContentResolver().insert(DatabaseProvider.MESSAGE_URI, values);
                    return null;
                }
            }.execute();
            mTextMessage.setText("");
        }
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_conversation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_contacts:
                handleLaunchContactsActivity();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleLaunchContactsActivity() {
        Intent actionIntent = new Intent(this, ContactsActivity.class);
        startActivity(actionIntent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(getLocalClassName(), "onCreateLoader called");
        return new CursorLoader(this, DatabaseProvider.MESSAGE_URI, null,
                MessageTable.COLUMN_RECEIVER +" = ? OR "+ MessageTable.COLUMN_SENDER + " = ?",
                new String[] {String.valueOf(mContactId), String.valueOf(mContactId)},
                MessageTable.COLUMN_ID);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(getLocalClassName(), "onLoadFinished called");
        if (data.getCount() > 0) {
            ((SimpleCursorAdapter) getListAdapter()).changeCursor(data);
        } else {
            ((SimpleCursorAdapter) getListAdapter()).changeCursor(null);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(getLocalClassName(), "onLoaderReset called");
        ((SimpleCursorAdapter) getListAdapter()).changeCursor(null);
    }
}

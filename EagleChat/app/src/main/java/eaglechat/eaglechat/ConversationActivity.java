package eaglechat.eaglechat;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ConversationActivity extends CompatListActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {
    public static final String TAG = "ConversationActivity";
    public static final String CONTACT_ID = TAG + ".CONTACT_ID";

    private static final int MESSAGES_LOADER = 1;
    private static final int CONTACTS_LOADER = 2;

    private static final String
            NO_CONTENT_STRING = "There are no messages between you and this contact. " +
            "Use the text field below to send a message.";
    private EditText mTextMessage;

    private long mContactId;

    private String mContactName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mContactId = bundle.getLong(CONTACT_ID, 0);
            Log.d(getPackageName(), String.format("Contact id=%d", mContactId));
        }

        MultiLayoutCursorAdapter adapter = new MultiLayoutCursorAdapter(
                this,
                new MessagesDelegate(MessagesTable.COLUMN_SENDER),
                R.layout.right_message_item,
                null,
                new String[]{MessagesTable.COLUMN_CONTENT},
                new int[]{android.R.id.text2},
                SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        );
        /*
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                int nameColumn = cursor.getColumnIndex(MessagesTable.COLUMN_SENDER);
                if (columnIndex != nameColumn) {
                    return false;
                }
                long senderId = cursor.getLong(columnIndex);
                if (senderId == 0) {
                    view.setVisibility(View.GONE);
                    return true;
                }
                ((TextView) view).setText(mContactName);
                return true;
            }
        });
        */

        setListAdapter(adapter);

        getListView().setDivider(null);
        getListView().setDividerHeight(0);
        getListView().setVerticalScrollBarEnabled(false);

        setEmptyText(NO_CONTENT_STRING);
        setListShown(false, false);

        getLoaderManager().initLoader(CONTACTS_LOADER, null, this);

        mTextMessage = (EditText) findViewById(R.id.text_message);
        findViewById(R.id.button_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        final String message = mTextMessage.getText().toString();
        if (!message.isEmpty()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {

                    ContentValues values = new ContentValues();

                    values.put(MessagesTable.COLUMN_RECEIVER, mContactId);
                    values.put(MessagesTable.COLUMN_SENDER, 0);
                    values.put(MessagesTable.COLUMN_CONTENT, message);
                    values.put(MessagesTable.COLUMN_SENT, MessagesTable.UNSENT);
                    values.put(MessagesTable.COLUMN_SEQNUM, Util.uniqueSequenceNumber(getApplicationContext()));

                    getContentResolver().insert(DatabaseProvider.MESSAGES_URI, values);

                    return null;
                }
            }.execute();
            mTextMessage.setText("");
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focused = getCurrentFocus();
        if (focused != null) {
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
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
            case R.id.action_burn:
                Util.burn(this);
                finish();
                return true;
            case R.id.action_fake:
                FakeMessageFragment.newInstance(mContactId).show(getSupportFragmentManager(), "fake");
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
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(getPackageName(), newConfig.toString());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(getPackageName(), "onCreateLoader called");
        Log.d(getPackageName(), String.format("Loader id=%d", id));

        if (id == MESSAGES_LOADER) { // Load the messages related cursor
            // Append the contact Id to the URI for messages filtered by contact
            Uri uri = ContentUris.withAppendedId(DatabaseProvider.MESSAGES_FROM_CONTACT_URI, mContactId);

            // The columns we want to return
            String[] projection = new String[]{
                    MessagesTable.COLUMN_ID,
                    MessagesTable.COLUMN_SENDER,
                    MessagesTable.COLUMN_CONTENT
            };

            return new CursorLoader(this, uri, projection, null, null, MessagesTable.COLUMN_ID);
        } else if (id == CONTACTS_LOADER) { // Load the contacts cursor
            // Uri to get our contact
            Uri uri = ContentUris.withAppendedId(DatabaseProvider.CONTACTS_URI, mContactId);

            String[] projection = new String[]{
                    ContactsTable.COLUMN_ID,
                    ContactsTable.COLUMN_NAME
            };

            return new CursorLoader(this, uri, projection, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(getPackageName(), "onLoadFinished called");
        if (loader.getId() == MESSAGES_LOADER) {
            if (data.getCount() > 0) {
                ((SimpleCursorAdapter) getListAdapter()).changeCursor(data);

                ListView listView = getListView();
                SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();

                listView.requestFocus();
                listView.setSelection(adapter.getCount());
            } else {
                ((SimpleCursorAdapter) getListAdapter()).changeCursor(null);
            }
            setListShown(true, false);
        } else if (loader.getId() == CONTACTS_LOADER) {
            if (data.getCount() > 0) {
                data.moveToFirst();
                int name_column = data.getColumnIndex(ContactsTable.COLUMN_NAME);
                String name = data.getString(name_column);
                mContactName = name;
                getSupportActionBar().setTitle(mContactName);
                /* Now that we have our contact's name, start the messages loader */
                getLoaderManager().initLoader(MESSAGES_LOADER, null, this);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(getPackageName(), "onLoaderReset called");
        ((SimpleCursorAdapter) getListAdapter()).changeCursor(null);
    }

    private class MessagesDelegate implements MultiLayoutCursorAdapter.Delegate {

        String mSelectionColumn;

        private MessagesDelegate(String selectionColumn) {
            mSelectionColumn = selectionColumn;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position, Cursor cursor) {
            cursor.moveToPosition(position);
            int selectionColumn = cursor.getColumnIndex(mSelectionColumn);
            long selectionId = cursor.getLong(selectionColumn);
            return selectionId == 0 ? 0 : 1;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent, LayoutInflater inflater) {
            int type = getItemViewType(cursor.getPosition(), cursor);
            View v;
            if (type == 0) {
                v = inflater.inflate(R.layout.right_message_item, parent, false);
            } else {
                v = inflater.inflate(R.layout.left_message_item, parent, false);
            }
            return v;
        }
    }
}

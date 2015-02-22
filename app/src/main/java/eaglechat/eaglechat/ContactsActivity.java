package eaglechat.eaglechat;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.melnykov.fab.FloatingActionButton;


public class ContactsActivity extends CompatListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    FloatingActionButton mAddButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(this.getLocalClassName(), "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        mAddButton = (FloatingActionButton) findViewById(R.id.action_add_contact);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLaunchAddContactsActivity();
            }
        });

        ListAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.two_line_list_item,
                null,
                new String[]{ContactsTable.COLUMN_NAME, MessagesTable.COLUMN_CONTENT},
                new int[]{android.R.id.text1, android.R.id.text2},
                SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        );
        setListAdapter(adapter);

        setListShown(false, false);

        getLoaderManager().initLoader(0, null, this).forceLoad();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_contacts, menu);
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
                Config.burn(this);
                finish();
                return true;
            case R.id.action_my_details:
                MyDetailsActivity.Util.launchMyDetailsActivity(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean handleLaunchAddContactsActivity() {
        Intent activityIntent = new Intent(this, AddContactActivity.class);
        startActivity(activityIntent);
        return true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(getLocalClassName(), String.format("You selected id=%d", id));
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(ConversationActivity.CONTACT_ID, id);
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(getLocalClassName(), "onCreateLoader called");
        return new CursorLoader(this, DatabaseProvider.CONTACTS_WITH_LAST_MESSAGE_URI,
                new String[]{"contacts._id",
                        ContactsTable.COLUMN_NAME,
                        MessagesTable.COLUMN_CONTENT}, null, null,
                ContactsTable.COLUMN_NAME);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(getLocalClassName(), "onLoadFinished called");
        if (data.getCount() > 0) {
            ((SimpleCursorAdapter) getListAdapter()).changeCursor(data);
        } else {
            ((SimpleCursorAdapter) getListAdapter()).changeCursor(null);
        }
        setListShown(true, false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(getLocalClassName(), "onLoaderReset called");
        ((SimpleCursorAdapter) getListAdapter()).changeCursor(null);
    }
}

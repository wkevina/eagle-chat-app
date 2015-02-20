package eaglechat.eaglechat;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import org.spongycastle.util.encoders.Base64;


public class ContactsActivity extends CompatListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(this.getLocalClassName(), "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

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
    protected void onResume() {
        super.onResume();
        Log.d(this.getLocalClassName(), "onResume called");
        //getLoaderManager().initLoader(0, null, this).forceLoad();
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
            case R.id.action_add_contact:
                return handleLaunchAddContactsActivity();
            case R.id.action_burn:
                SharedPreferences.Editor editor =
                        getSharedPreferences(getString(R.string.shared_prefs_file), MODE_PRIVATE)
                                .edit();

                editor.clear().commit();
                getContentResolver().delete(DatabaseProvider.DELETE_URI, null, null);
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            case R.id.action_my_details:
                SharedPreferences prefs = getSharedPreferences(getString(R.string.shared_prefs_file), MODE_PRIVATE);
                Intent activityIntent = new Intent(this, MyDetailsActivity.class);
                activityIntent.putExtra(Config.PUBLIC_KEY, Base64.decode(prefs.getString(Config.PUBLIC_KEY, "")));
                activityIntent.putExtra(Config.NETWORK_ID, Base64.decode(prefs.getString(Config.NETWORK_ID, "")));
                startActivity(activityIntent);
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

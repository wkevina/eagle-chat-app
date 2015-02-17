package eaglechat.eaglechat;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class AddContactActivity extends ActionBarActivity {

    EditText mName, mNetworkId;
    Button mSubmitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        mName = (EditText)findViewById(R.id.text_name);
        mNetworkId = (EditText)findViewById(R.id.text_id);
        mSubmitButton = (Button)findViewById(R.id.button_submit);

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
    }

    private void submit() {
        String contactName = mName.getText().toString();
        String networkId = mNetworkId.getText().toString();

        boolean doesValidate = true;

        if (contactName.isEmpty()) {
            mName.setError("Enter a name");
            doesValidate = false;
        }
        if (networkId.isEmpty()) {
            mNetworkId.setError("Enter network ID");
            doesValidate = false;
        }
        if (doesValidate == false) {
            Log.d(this.getLocalClassName(), "Some fields are missing. Cannot continue.");
        } else {
            addContact(networkId, contactName);
        }
    }

    private void addContact(String networkId, String contactName) {
        ContentValues values = new ContentValues();
        values.put(ContactsTable.COLUMN_NETWORK_ID, networkId);
        values.put(ContactsTable.COLUMN_NAME, contactName);
        getContentResolver().insert(DatabaseProvider.CONTACTS_URI, values);
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_contact, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

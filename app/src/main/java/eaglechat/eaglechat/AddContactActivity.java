package eaglechat.eaglechat;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.melnykov.fab.FloatingActionButton;

import org.spongycastle.util.encoders.Base64;


public class AddContactActivity extends ActionBarActivity {

    EditText mNameText, mNetworkIdText, mPublicKeyText;
    Button mScanButton;
    FloatingActionButton mSubmitButton;

    String mPublicKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        mNameText = (EditText) findViewById(R.id.text_name);
        mNetworkIdText = (EditText) findViewById(R.id.text_id);
        mPublicKeyText = (EditText) findViewById(R.id.text_publicKey);

        mSubmitButton = (FloatingActionButton) findViewById(R.id.button_submit);
        mScanButton = (Button) findViewById(R.id.button_scan);

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });
    }

    private void startScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan(ImmutableList.of("QR_CODE"));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (intent != null && scanResult != null) {
            Log.d(getPackageName(), scanResult.toString());
            boolean isQRCode = scanResult.getFormatName().equalsIgnoreCase("QR_CODE");
            String contents = scanResult.getContents();
            if (contents != null) {
                decodeQRCode(contents);
            } else {
                Toast.makeText(this, "Could not read code", Toast.LENGTH_LONG).show();
                return;
            }
        }
        // else continue with any other code you need in the method

    }

    private void decodeQRCode(String contents) {
        String[] chunks = contents.split(":");
        boolean isEagleChat = chunks[0].equalsIgnoreCase("eaglechat"); // Chunk #1 must be 'eaglechat'
        if (!isEagleChat || !(chunks.length == 3 || chunks.length == 4)) {
            Toast.makeText(this, "Not an EagleChat code", Toast.LENGTH_LONG).show();
            return;
        }
        String networkId = Config.padHex(chunks[1], 4); // Decode the network ID from chunk #2
        byte[] publicKeyBytes = Base64.decode(chunks[2]); // Decode the public key from chunk #3

        if (!Config.validateNetworkId(networkId)) {
            Toast.makeText(this, "Invalid network ID", Toast.LENGTH_LONG).show();
            return;
        }

        if (publicKeyBytes.length != 32) {
            Toast.makeText(this, "Invalid public key", Toast.LENGTH_LONG).show();
            return;
        }

        if (chunks.length == 4 && mNameText.getText().length() == 0) {
            mNameText.setText(chunks[3]);
        }
        mPublicKey = Base64.toBase64String(publicKeyBytes);
        mPublicKeyText.setText(Config.bytesToString(publicKeyBytes, " "));
        mNetworkIdText.setText(networkId);
        mScanButton.setText("Fingerprint: " + Config.fingerprint(publicKeyBytes, Config.hexStringToBytes(networkId)));
    }

    private void submit() {
        String contactName = mNameText.getText().toString();
        String networkId = Config.padHex(mNetworkIdText.getText().toString(), 4);

        boolean doesValidate = true;

        if (contactName.isEmpty()) {
            mNameText.setError("Enter a name");
            doesValidate = false;
        }
        if (networkId.isEmpty()) {
            mNetworkIdText.setError("Enter network ID");
            doesValidate = false;
        } else if (!Config.validateNetworkId(networkId)) {
            mNetworkIdText.setError("Contains invalid characters. Must be hex-format number.");
            doesValidate = false;
        }
        if (mPublicKey == null || mPublicKey.isEmpty()) {
            Toast.makeText(this, "Invalid public key", Toast.LENGTH_LONG).show();
            doesValidate = false;
        }
        if (!doesValidate) {
            Log.d(this.getLocalClassName(), "Some fields are missing. Cannot continue.");
        } else {
            addContact(networkId, contactName, mPublicKey);
        }
    }

    private void addContact(String networkId, String contactName, String publicKey) {
        ContentValues values = new ContentValues();
        values.put(ContactsTable.COLUMN_NETWORK_ID, networkId);
        values.put(ContactsTable.COLUMN_NAME, contactName);
        values.put(ContactsTable.COLUMN_PUBLIC_KEY, mPublicKey);
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
}

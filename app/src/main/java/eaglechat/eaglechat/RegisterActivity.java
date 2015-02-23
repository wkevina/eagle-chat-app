package eaglechat.eaglechat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;

import org.spongycastle.util.encoders.Base64;

import java.security.SecureRandom;


public class RegisterActivity extends ActionBarActivity {
    EditText mNameText, mNetworkIdText;
    TextView mKeyLabel;
    FloatingActionButton mDoneButton;
    Button mGenerateButton;

    String mPublicKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mNameText = (EditText) findViewById(R.id.text_name);
        mNetworkIdText = (EditText) findViewById(R.id.text_id);
        mKeyLabel = (TextView) findViewById(R.id.text_publicKey);

        mDoneButton = (FloatingActionButton) findViewById(R.id.button_submit);

        mGenerateButton = (Button) findViewById(R.id.button_generateKeys);

        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });

        mGenerateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dummyEncryptionSetup(); // this is not what we would do in production
            }
        });
    }

    private void submit() {
        boolean isReady = true;
        String name = mNameText.getText().toString();

        if (name == null || name.isEmpty()) {
            isReady = false;
            mNameText.setError("Enter a name");
        }

        String networkIdString = null;
        try {
            int networkId = Integer.parseInt(mNetworkIdText.getText().toString(), 16) & 0xFFFF;
            networkIdString = Integer.toHexString(networkId);
            networkIdString = Config.padHex(networkIdString, 4);
        } catch (NumberFormatException ex) {
            isReady = false;
            mNetworkIdText.setError("Invalid ID");
        }

        if (mPublicKey == null || mPublicKey.isEmpty()) {
            isReady = false;
            Toast.makeText(this, "You must generate encryption keys", Toast.LENGTH_LONG).show();
        }

        if (isReady) {
            writeData(name, networkIdString, mPublicKey);
            Config.restart(this);
            finish();
        }

    }

    private void writeData(String name, String id, String key) {
        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.shared_prefs_file), MODE_PRIVATE).edit();

        editor
                .putString(Config.NAME, name)
                .putString(Config.NETWORK_ID, id)
                .putString(Config.PUBLIC_KEY, key)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_register, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    private void dummyEncryptionSetup() {
        byte[] publicKey = new byte[32];

        SecureRandom r = new SecureRandom(new byte[]{0x10, 0x02, 0x03, 0x04});
        r.nextBytes(publicKey);

        if (mNetworkIdText.getText().toString().isEmpty()) { // If no id has been supplied by the user
            byte[] networkId = new byte[2];
            r.nextBytes(networkId);
            mNetworkIdText.setText(Config.bytesToString(networkId, ""));
        }

        mKeyLabel.setText(Config.bytesToString(publicKey, " "));
        mPublicKey = Base64.toBase64String(publicKey);
    }

}

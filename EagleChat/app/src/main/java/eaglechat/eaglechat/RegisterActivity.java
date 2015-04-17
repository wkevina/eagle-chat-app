package eaglechat.eaglechat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;

import org.jdeferred.AlwaysCallback;
import org.jdeferred.DoneCallback;
import org.jdeferred.DonePipe;
import org.jdeferred.Promise;
import org.spongycastle.util.encoders.Base64;

import java.security.SecureRandom;


public class RegisterActivity extends PeregrineActivity {

    EditText mNameText, mNetworkIdText;
    TextView mKeyLabel;
    FloatingActionButton mDoneButton;
    String mPublicKey;
    String mName;
    private EditText mPasswordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mNameText = (EditText) findViewById(R.id.text_name);
        mPasswordText = (EditText) findViewById(R.id.text_password);
        mNetworkIdText = (EditText) findViewById(R.id.text_id);
        mKeyLabel = (TextView) findViewById(R.id.text_publicKey);

        mDoneButton = (FloatingActionButton) findViewById(R.id.button_submit);

        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
    }

    @Override
    void onPeregrineAvailable() {

    }


    private void submit() {
        boolean isReady = true;
        String name = mNameText.getText().toString();



        if (name == null || name.isEmpty()) {
            isReady = false;
            mNameText.setError("Enter a name");
        }

        String networkIdString = mNetworkIdText.getText().toString();
        if (!EagleChatConfiguration.validateNodeId(networkIdString)) {
            mNetworkIdText.setError("Invalid ID");
            isReady = false;
        }

        String passwordString = mPasswordText.getText().toString();
        String passwordHash = null;
        if (passwordString.isEmpty() || passwordString.length() < 4) {
            isReady = false;
            mPasswordText.setError("Invalid password");
        } else {
            passwordHash = EagleChatConfiguration.getPasswordHash(passwordString);
            Log.d(TAG, "Hash: " + passwordHash);
        }

        if (isReady) {

            mName = name;

            commitConfiguration(networkIdString, passwordHash);
        }

    }

    private void commitConfiguration(final String nodeId, final String passwordHash) {
        if (peregrineAvailable()) {
            mDoneButton.setEnabled(false);
            getPeregrine().commandSetPassword(passwordHash)

                    .then(new DoneCallback<String>() {
                        @Override
                        public void onDone(String result) {
                            Log.d(TAG, "Password set.");
                        }
                    })
                    .then(new DonePipe<String, String, String, String>() {
                        @Override
                        public Promise<String, String, String> pipeDone(String result) {
                            return mPeregrine.commandSetId(nodeId);
                        }
                    })
                    .then(new DonePipe<String, String, String, String>() {
                        @Override
                        public Promise<String, String, String> pipeDone(String result) {
                            return mPeregrine.commandGenerateKeys();
                        }
                    })
                    .then(new DonePipe<String, String, String, String>() {
                        @Override
                        public Promise<String, String, String> pipeDone(String result) {
                            return mPeregrine.commandCommit();
                        }
                    })
                    .then(new DonePipe<String, Integer, String, String>() {
                        @Override
                        public Promise<Integer, String, String> pipeDone(String result) {
                            return mPeregrine.requestStatus();
                        }
                    })
                    .done(new DoneCallback<Integer>() {
                        @Override
                        public void onDone(Integer result) {
                            Log.d(TAG, "Status: " + result);
                            Toast.makeText(RegisterActivity.this, "Successfully registered", Toast.LENGTH_SHORT).show();
                            finishRegistration();
                        }
                    })
                    .always(new AlwaysCallback<Integer, String>() {
                        @Override
                        public void onAlways(Promise.State state, Integer resolved, String rejected) {
                            mDoneButton.setEnabled(true);
                        }
                    });
        } else {
            Toast.makeText(this, "EagleChat device unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    private void finishRegistration() {

        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.shared_prefs_file), MODE_PRIVATE).edit();

        editor.putString(Util.NAME, mName).apply();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Util.restart(RegisterActivity.this);
            }
        });
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

}

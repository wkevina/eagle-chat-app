package eaglechat.eaglechat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jdeferred.DoneCallback;
import org.jdeferred.DonePipe;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.spongycastle.util.encoders.Base64;

import java.security.Security;

public class MainActivity extends PeregrineActivity {

    private static final String TAG = "eaglechat.eaglechat";

    private final int STATE_LAUNCH_LIST_ACTIVITY = 0;
    private final int STATE_LAUNCH_CONTACTS_ACTIVITY = 1;
    private final int STATE_LAUNCH_DETAILS_ACTIVITY = 2;
    private final int STATE_LAUNCH_REGISTER_ACTIVITY = 3;
    private final int STATE_LAUNCH_USB_ACTIVITY = 4;

    EagleChatConfiguration mEagleConfig;
    private int mState = 0;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private TextView mStatusTextView;
    private String mPublicKey;
    private int mNodeId;
    private EditText mPasswordText;

    /**
     * Fetches configuration status from EagleChat board
     */
    private void retrievePeripheralStatus() {
        getPeregrine().requestStatus()
                .done(new DoneCallback<Integer>() {

                    @Override
                    public void onDone(Integer result) {

                        Log.d(TAG, "Peripheral status = " + result);

                        mStatusTextView.setText(getString(R.string.status_checkingConfiguration));

                        mEagleConfig = new EagleChatConfiguration(result);

                        if (mEagleConfig.isConfigured()) {

                            // launch get public key and node id commands to see if this is a known board
                            Log.d(TAG, "EagleChat configured. Should authenticate.");
                            showAuthenticationScreen();
                            //getIdentifiers();

                        } else {

                            Log.d(TAG, "EagleChat not configured.");
                            Log.d(TAG, "hasNodeId = " + mEagleConfig.hasNodeId());
                            Log.d(TAG, "hasPassword = " + mEagleConfig.hasPassword());
                            Log.d(TAG, "hasKeyPair = " + mEagleConfig.hasKeyPair());
                            Log.d(TAG, "isConfigured = " + mEagleConfig.isConfigured());

                            handleLaunchRegisterActivity();
                        }
                    }

                })
                .fail(new FailCallback<String>() {

                    @Override
                    public void onFail(String result) {
                        Log.d(TAG, "Checking status failed. Result = " + result);
                    }

                });
    }

    private void showAuthenticationScreen() {

        // Remove spinner and show password box
        findViewById(R.id.loadingScreen).setVisibility(View.GONE);
        findViewById(R.id.passwordScreen).setVisibility(View.VISIBLE);

    }

    private void authenticatePeripheral() {
        // Send password to device and try to authenticate
    }

    private void getIdentifiers() {
        getPeregrine().requestPublicKey().then(new DonePipe<String, Integer, String, String>() {
            @Override
            public Promise<Integer, String, String> pipeDone(String result) {
                mPublicKey = result;
                Log.d(TAG, "Public key: " + result);
                return mPeregrine.requestId();
            }
        }).done(new DoneCallback<Integer>() {
            @Override
            public void onDone(Integer result) {
                mNodeId = result;
                compareCredentials();
            }
        }).fail(new FailCallback<String>() {
            @Override
            public void onFail(String result) {
                mStatusTextView.setText("Cannot communicate with EagleChat device");
            }
        });
    }

    private void compareCredentials() {

        Log.d(TAG, "compareCredentials");

        // Compare public key and node id to
        SharedPreferences prefs = getSharedPreferences(getString(R.string.shared_prefs_file), Context.MODE_PRIVATE);

        String ourPublicKey = prefs.getString(Util.PUBLIC_KEY, "");
        int ourNodeId = prefs.getInt(Util.NODE_ID, 255);


        if ( (ourNodeId == 255 || ourPublicKey.isEmpty())
                || !(ourPublicKey.contentEquals(mPublicKey) && ourNodeId == mNodeId) ) {
            // We have never had a board connected before or this is a new board
            // Don't delete anything, just update info
            prefs.edit()
                    .putString(Util.PUBLIC_KEY, mPublicKey)
                    .putInt(Util.NODE_ID, mNodeId)
                .apply();

        }

        Log.d(TAG, "done saving");


        if (peregrineAvailable()) {
            Log.d(TAG, "starting send manager");

            getPeregrine().startSendManager();
        }

        Log.d(TAG, "about to launch contacts activity");

        handleLaunchContactsActivity();


        /*

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "about to launch contacts activity");
                handleLaunchContactsActivity();
            }
        }, 500);
        /*
        return prefs.contains(Util.PUBLIC_KEY)
                && prefs.contains(Util.NODE_ID)
                && prefs.contains(Util.NAME);
                */
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mStatusTextView = (TextView) findViewById(R.id.statusText);

        mPasswordText = (EditText) findViewById(R.id.text_password);

        findViewById(R.id.passwordScreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPassword(v);
            }
        });

        //mState = determineState();

        //mState = STATE_LAUNCH_USB_ACTIVITY;

        /*
        switch (mState) {
            case STATE_LAUNCH_LIST_ACTIVITY:
                handleLaunchListActivity();
                break;
            case STATE_LAUNCH_CONTACTS_ACTIVITY:
                handleLaunchContactsActivity();
                break;
            case STATE_LAUNCH_DETAILS_ACTIVITY:
                handleLaunchDetailsActivity();
                break;
            case STATE_LAUNCH_REGISTER_ACTIVITY:
                handleLaunchRegisterActivity();
                break;
            case STATE_LAUNCH_USB_ACTIVITY:
                handleLaunchUsbActivity();
                break;
            default:
                finish();
                break;

        }
        */
    }

    private void submitPassword(View v) {

        clearKeyboard(v);

        String pwd = mPasswordText.getText().toString();
        if (EagleChatConfiguration.validatePassword(pwd)) {
            String hash = EagleChatConfiguration.getPasswordHash(pwd);
            if (peregrineAvailable()) {
                getPeregrine().commandAuthenticate(hash).done(new DoneCallback<String>() {
                    @Override
                    public void onDone(String result) {
                        // We have authenticated
                        Log.d(TAG, "Authentication successful.");
                        Toast.makeText(MainActivity.this, "Authenticated", Toast.LENGTH_SHORT).show();
                        getIdentifiers();
                    }
                }).fail(new FailCallback<String>() {
                    @Override
                    public void onFail(String result) {
                        Log.d(TAG, "Couldn't log in.");
                        Toast.makeText(MainActivity.this, "Log in failed", Toast.LENGTH_SHORT).show();
                        mPasswordText.setText("");
                    }
                });
            }
        }
    }

    private void clearKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) v.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }


    @Override
    void onPeregrineAvailable() {
        retrievePeripheralStatus();
    }

    @Override
    void onPeregrineUnavailable() {
        Util.restart(this);
        finish();
    }

    private void handleLaunchUsbActivity() {
        Intent activityIntent = new Intent(this, UsbTestActivity.class);
        startActivity(activityIntent);
        finish();
    }

    private void handleLaunchRegisterActivity() {
        Intent activityIntent = new Intent(this, RegisterActivity.class);
        startActivity(activityIntent);
        finish();
    }

    private int determineState() {
        if (!Util.isSetup(this)) {
            return STATE_LAUNCH_REGISTER_ACTIVITY;
        }
        return STATE_LAUNCH_CONTACTS_ACTIVITY;
    }

    private void handleLaunchContactsActivity() {
        Log.d(TAG, "handleLaunchContactsActivity");
        Intent activityIntent = new Intent(this, ContactsActivity.class);
        startActivity(activityIntent);
        finish();
    }

    private void handleLaunchListActivity() {
        Intent activityIntent = new Intent(this, ConversationActivity.class);
        startActivity(activityIntent);
        finish();
    }

    private void handleLaunchDetailsActivity() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.shared_prefs_file), MODE_PRIVATE);
        Intent activityIntent = new Intent(this, MyDetailsActivity.class);
        activityIntent.putExtra(Util.PUBLIC_KEY, Base64.decode(prefs.getString(Util.PUBLIC_KEY, "")));
        activityIntent.putExtra(Util.NODE_ID, Base64.decode(prefs.getString(Util.NODE_ID, "")));

        startActivity(activityIntent);
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

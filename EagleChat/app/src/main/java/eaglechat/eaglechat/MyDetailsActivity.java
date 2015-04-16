package eaglechat.eaglechat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.spongycastle.util.encoders.Base64;

import java.util.HashMap;
import java.util.Map;

/**
 * An activity that displays this user's contact information in a QR code. *
 */

public class MyDetailsActivity extends ActionBarActivity {

    private static final int MARGIN = 0;
    private static final String TAG = "eaglechat.eaglechat";
    private int CODE_SIZE = 200;
    byte[] mPublicKey = new byte[32];

    String mNetworkId;

    String mName;

    ImageView mQRCodeView;
    TextView mFingerPrintText, mKeyText, mNetworkIdText;

    /**
     * Utility method for launching the MyDetails activity
     *
     * @param activity Context to use for launching MyDetails
     */
    public static void launchMyDetailsActivity(Context activity) {
        String filename = activity.getString(R.string.shared_prefs_file);
        SharedPreferences prefs = activity.getSharedPreferences(filename, MODE_PRIVATE);
        Intent activityIntent = new Intent(activity, MyDetailsActivity.class);
        activityIntent.putExtra(eaglechat.eaglechat.Util.PUBLIC_KEY, prefs.getString(eaglechat.eaglechat.Util.PUBLIC_KEY, ""));
        activityIntent.putExtra(eaglechat.eaglechat.Util.NODE_ID, prefs.getInt(eaglechat.eaglechat.Util.NODE_ID, 255));
        activityIntent.putExtra(eaglechat.eaglechat.Util.NAME, prefs.getString(eaglechat.eaglechat.Util.NAME, ""));
        activity.startActivity(activityIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);

        retrieveArgumentsOrFinish();

        mQRCodeView = (ImageView) findViewById(R.id.image_qr);
        mFingerPrintText = (TextView) findViewById(R.id.text_fingerprint);
        mKeyText = (TextView) findViewById(R.id.text_publicKey);
        mNetworkIdText = (TextView) findViewById(R.id.text_address);

        Log.d(getPackageName(), String.format("Public key=%s", Util.bytesToString(mPublicKey, ":")));

        String f = Util.fingerprint(mPublicKey, eaglechat.eaglechat.Util.hexStringToBytes(mNetworkId));
        mFingerPrintText.setText(f);

        mKeyText.setText(eaglechat.eaglechat.Util.bytesToString(mPublicKey, " "));

        mNetworkIdText.setText(mNetworkId);

    }


    @Override
    protected void onResume() {
        super.onResume();
        showCode();
    }

    private void retrieveArgumentsOrFinish() {
        Bundle args = getIntent().getExtras();
        if (args == null) {
            finish(); // bail!
            return;
        }

        String key = args.getString(eaglechat.eaglechat.Util.PUBLIC_KEY);
        if (key == null || !EagleChatConfiguration.validatePublicKey(key)) {
            Log.d(TAG, "MyDetailsActivity: no public key or invalid key");
            finish();
            return;
        }

        mPublicKey =  Util.hexStringToBytes(key);
        mNetworkId = Util.intToString(args.getInt(eaglechat.eaglechat.Util.NODE_ID));
        mName = args.getString(eaglechat.eaglechat.Util.NAME);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my_contact_details, menu);
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
                eaglechat.eaglechat.Util.burn(this);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showCode() {
        String publicKeyString = Base64.toBase64String(mPublicKey);
        String addressString = mNetworkId;
        String dataString = String.format("eaglechat:%s:%s:%s", addressString, publicKeyString, mName);

        Log.d(getPackageName(), dataString);

        mQRCodeView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        int measuredSize = mQRCodeView.getMeasuredWidth();

        CODE_SIZE = measuredSize > 0 ? measuredSize : CODE_SIZE;

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bits;
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, MARGIN);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            bits = writer.encode(dataString, BarcodeFormat.QR_CODE, CODE_SIZE, CODE_SIZE, hints);
        } catch (WriterException ex) {
            return;
        }

        Bitmap qrCode = eaglechat.eaglechat.Util.bitMatrixToBitmap(this, bits);
        mQRCodeView.setImageBitmap(qrCode);
    }

}

package eaglechat.eaglechat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.spongycastle.util.encoders.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * An activity that displays this user's contact information in a QR code. *
 */

public class MyDetailsActivity extends ActionBarActivity {

    private static final int MARGIN = 0;
    private static final int CODE_SIZE = 200;
    byte[] mPublicKey = new byte[32];

    byte[] mAddress = new byte[]{0x00, 0x01};

    ImageView mQRCodeView;
    TextView mFingerPrintText, mKeyText, mAddressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);

        retrieveArgumentsOrFinish();

        mQRCodeView = (ImageView) findViewById(R.id.image_qr);
        mFingerPrintText = (TextView) findViewById(R.id.text_fingerprint);
        mKeyText = (TextView) findViewById(R.id.text_key);
        mAddressText = (TextView) findViewById(R.id.text_address);

        Log.d(getPackageName(), String.format("Public key=%s", bytesToString(mPublicKey, ":")));

        String f = getFingerPrint(mPublicKey, mAddress);
        mFingerPrintText.setText(f);

        mKeyText.setText(bytesToString(mPublicKey, " "));

        mAddressText.setText(bytesToString(mAddress, ""));

        showCode();
    }

    private void retrieveArgumentsOrFinish() {
        Bundle args = getIntent().getExtras();
        if (args == null) {
            finish(); // bail!
        }
        mPublicKey = args.getByteArray(Config.PUBLIC_KEY);
        mAddress = args.getByteArray(Config.NETWORK_ID);
    }

    private String bytesToString(byte[] bytes, String separator) {
        StringBuilder s = new StringBuilder();
        for (byte b : bytes) {
            s.append(String.format("%02x%s", b, separator));
        }
        if (!separator.isEmpty()) {
            s.deleteCharAt(s.length() - 1); // delete the last separator character
        }

        return s.toString();
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

        switch(id) {
            case R.id.action_burn:
                Config.burn(this);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showCode() {
        String publicKeyString = Base64.toBase64String(mPublicKey);
        String addressString = Base64.toBase64String(mAddress);
        String dataString = String.format("eaglechat:%s:%s", addressString, publicKeyString);

        Log.d(getPackageName(), dataString);

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

        Bitmap qrCode = bitMatrixToBitmap(bits);
        mQRCodeView.setImageBitmap(qrCode);
    }

    private String getFingerPrint(byte[] key, byte[] address) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("sha256");
            sha256.update(key);
            sha256.update(address);
            byte[] hash = sha256.digest();
            String fingerprint = bytesToString(hash, "").substring(0, 2 * 4);
            return fingerprint;
        } catch (NoSuchAlgorithmException ex) {
            return "";
        }
    }

    private Bitmap bitMatrixToBitmap(BitMatrix bits) {
        Bitmap bitmap = Bitmap.createBitmap(bits.getWidth(), bits.getHeight(), Bitmap.Config.RGB_565);
        int background = getResources().getColor(R.color.background_material_light);
        for (int x = 0; x < bits.getWidth(); ++x) {
            for (int y = 0; y < bits.getHeight(); ++y) {
                bitmap.setPixel(x, y, bits.get(x, y) ? Color.BLACK : background);
            }
        }
        return bitmap;
    }

    public static class Util {
        /**
         * Utility method for launching the MyDetails activity
         * @param activity Context to use for launching MyDetails
         */
        public static void launchMyDetailsActivity(Context activity) {
            String filename = activity.getString(R.string.shared_prefs_file);
            SharedPreferences prefs = activity.getSharedPreferences(filename, MODE_PRIVATE);
            Intent activityIntent = new Intent(activity, MyDetailsActivity.class);
            activityIntent.putExtra(Config.PUBLIC_KEY, Base64.decode(prefs.getString(Config.PUBLIC_KEY, "")));
            activityIntent.putExtra(Config.NETWORK_ID, Base64.decode(prefs.getString(Config.NETWORK_ID, "")));
            activity.startActivity(activityIntent);
        }
    }
}

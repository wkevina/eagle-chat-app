package eaglechat.eaglechat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by kevinward on 2/20/15.
 */
public class Config {
    private static final String BASE = "eaglechat.eaglechat.";

    public static final String PUBLIC_KEY = BASE + "public_key";

    public static final String NETWORK_ID = BASE + "network_id";

    public static final String NAME = BASE + "name";

    public static String fingerprint(byte[] key, byte[] address) {
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

    public static String bytesToString(byte[] bytes, String separator) {
        StringBuilder s = new StringBuilder();
        for (byte b : bytes) {
            s.append(String.format("%02x%s", b, separator));
        }
        if (!separator.isEmpty()) {
            s.deleteCharAt(s.length() - 1); // delete the last separator character
        }

        return s.toString();
    }

    public static byte[] stringToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @SuppressLint("CommitPrefEdits")
    public static void burn(Context activity) {
        SharedPreferences.Editor editor =
                activity.getSharedPreferences(
                        activity.getString(R.string.shared_prefs_file),
                        Context.MODE_PRIVATE)
                .edit();

        editor.clear().commit();
        activity.getContentResolver().delete(DatabaseProvider.DELETE_URI, null, null);
        restart(activity);
    }

    public static void restart(Context activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    public static boolean isSetup(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(ctx.getString(R.string.shared_prefs_file), Context.MODE_PRIVATE);
        return prefs.contains(Config.PUBLIC_KEY)
                && prefs.contains(Config.NETWORK_ID)
                && prefs.contains(Config.NAME);

    }
}

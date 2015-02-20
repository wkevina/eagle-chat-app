package eaglechat.eaglechat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by kevinward on 2/20/15.
 */
public class Config {
    private static final String BASE = "eaglechat.eaglechat.";

    public static final String PUBLIC_KEY = BASE + "public_key";

    public static final String NETWORK_ID = BASE + "network_id";

    public static String getFingerPrint(byte[] key, byte[] address) {
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
}

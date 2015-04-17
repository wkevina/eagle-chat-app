package eaglechat.eaglechat;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by kevinward on 4/14/15.
 */
public class EagleChatConfiguration {
    /*

    #define FLAGS_UNSET         0xFF // The status register looks like this when it is unconfigured
    #define FLAGS_CONFIGURED    0b10000000
    #define FLAGS_PRIVATE_KEY   0b01000000
    #define FLAGS_PUBLIC_KEY    0b00100000
    #define FLAGS_NODE_ID		0b00010000
    #define FLAGS_PASSWORD      0b00001000

     */

    public static final int CONFIGURED = 128;
    public static final int PRIVATE_KEY = 64;
    public static final int PUBLIC_KEY = 32;
    public static final int NODE_ID = 16;
    public static final int PASSWORD = 8;

    public static final int PASSWORD_HASH_LENGTH = 60;
    private static final int PASSWORD_LENGTH_MIN = 4;

    private int mStatus;

    public EagleChatConfiguration(int status) {
        mStatus = status;
    }

    /**
     * Returns true if peripheral has valid keypair
     * @return
     */
    public boolean hasKeyPair() {
        return (mStatus & (PRIVATE_KEY | PUBLIC_KEY)) != 0;
    }

    /**
     * Returns true if peripheral has a password set
     * @return
     */
    public boolean hasPassword() {
        return (mStatus & PASSWORD) != 0;
    }

    /**
     * Returns true if peripheral has a valid node id
     * @return
     */
    public boolean hasNodeId() {
        return (mStatus & NODE_ID) != 0;
    }

    /**
     * Returns true if this peripheral has all required parameters set
     * @return
     */
    public boolean isConfigured() {
        return hasKeyPair() && hasPassword() && hasNodeId() && ((mStatus & CONFIGURED) != 0);
    }

    /**
     * Returns true or false if the network ID string is in the correct format. This is the de-facto
     * specification for the network ID strings.
     *
     * Network IDs must be no more than 2 characters long, and must encode a number in hexadecimal format.
     * That means valid characters are 0-9|A-F.
     *
     * @param s
     * @return
     */
    public static boolean validateNodeId(String s) {
        if (s.length() > 2) {
            return false;
        }
        if (s.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(s, 16);
        } catch (NumberFormatException ex) {
            return false;
        }
        return true;
    }

    public static boolean validatePublicKey(String s) {
        s = Util.stripSeparators(s);

        return s.matches("^[0-9A-F]{64,}$");
    }

    public static String getPasswordHash(String pass) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("sha256");
            sha256.update(pass.getBytes());
            byte[] hash = sha256.digest();
            return Util.bytesToString(hash, "").substring(0, PASSWORD_HASH_LENGTH);
        } catch (NoSuchAlgorithmException ex) {
            return "";
        }
    }

    public static boolean validatePassword(String pwd) {
        return (pwd.length() >= PASSWORD_LENGTH_MIN);
    }

    public static String formatMessage(String content, Integer seqNum) {
        StringBuilder sb = new StringBuilder();
        byte[] buf = ByteBuffer.allocate(4).putInt(seqNum).array();

        sb
                .append("m")
                .append(Base64.encodeToString(buf, Base64.NO_PADDING|Base64.NO_WRAP))
                .append(content);

        return sb.toString();
    }

    public static class DecodedMessage {

        public String content;
        public Integer seqNum;

        public DecodedMessage(String encoded) {

            if (encoded.startsWith("m")) {
                // strip prefix
                encoded = encoded.replaceFirst("m", "");
            } else {
                throw new IllegalArgumentException("String argument was not an encoded message");
            }


            if (encoded.length() < 6) // must contain at least the 6 seqNum characaters
                throw new IllegalArgumentException("String argument does not have delimiter after seqNum");

            byte[] seqNumBytes = Base64.decode(encoded.substring(0, 6),
                    Base64.NO_PADDING|Base64.NO_WRAP);

            seqNum = ByteBuffer.wrap(seqNumBytes).getInt();

            content = encoded.substring(6);
        }
    }

}

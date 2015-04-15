package eaglechat.eaglechat;

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
}

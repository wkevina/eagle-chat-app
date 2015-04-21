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

    public static final int ACK = 1;
    public static final int TEXT = 2;
    public static final int PICTURE = 3;

    public static final int PASSWORD_HASH_LENGTH = 60;
    private static final int PASSWORD_LENGTH_MIN = 4;

    private int mStatus;

    public EagleChatConfiguration(int status) {
        mStatus = status;
    }

    /**
     * Returns true or false if the network ID string is in the correct format. This is the de-facto
     * specification for the network ID strings.
     * <p/>
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

    public static String formatMessage(String content, Integer seqNum, Integer type) {
        StringBuilder sb = new StringBuilder();
        byte[] buf = ByteBuffer.allocate(4).putInt(seqNum).array();

        switch(type) {
            case TEXT:
                sb.append("m");
                break;
            case ACK:
                sb.append("a");
                break;
            case PICTURE:
                sb.append("p");
                break;
        }

        sb
                .append(Base64.encodeToString(buf, Base64.NO_PADDING | Base64.NO_WRAP))
                .append(content);

        return sb.toString();
    }

    /**
     * Returns true if peripheral has valid keypair
     *
     * @return
     */
    public boolean hasKeyPair() {
        return (mStatus & (PRIVATE_KEY | PUBLIC_KEY)) != 0;
    }

    /**
     * Returns true if peripheral has a password set
     *
     * @return
     */
    public boolean hasPassword() {
        return (mStatus & PASSWORD) != 0;
    }

    /**
     * Returns true if peripheral has a valid node id
     *
     * @return
     */
    public boolean hasNodeId() {
        return (mStatus & NODE_ID) != 0;
    }

    /**
     * Returns true if this peripheral has all required parameters set
     *
     * @return
     */
    public boolean isConfigured() {
        return hasKeyPair() && hasPassword() && hasNodeId() && ((mStatus & CONFIGURED) != 0);
    }

    public static class DecodedMessage {

        public String hexId;
        public Integer type;
        public int seqNum;
        public String content;

        public DecodedMessage(final String encoded) {

            if (!encoded.startsWith("r:")) {
                throw new IllegalArgumentException("String argument was not an encoded message. Got: " + encoded);
            }

            String stripped = encoded.replaceFirst("r:", "");

            String[] chunks = stripped.split(":", 1);

            if (chunks.length < 2) {
                throw new IllegalArgumentException("String argument was not an encoded message. Got: " + encoded);
            }

            try {
                int tempId = Integer.parseInt(chunks[0], 16); // hexId should parse to an int
                hexId = chunks[0];
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Sender id was invalid. Got: " + encoded);
            }

            String remainder = chunks[1];
            // Decode message type
            if (remainder.startsWith("m")) {
                // strip prefix
                remainder = remainder.replaceFirst("m", "");
                type = TEXT;

            } else if (remainder.startsWith("a")) {

                remainder = remainder.replaceFirst("a", "");
                type = ACK;

            } else if (remainder.startsWith("p")) {

                remainder = remainder.replaceFirst("p", "");
                type = PICTURE;

            } else {
                throw new IllegalArgumentException("String argument doesn't have valid message type. Got: " + encoded);
            }


            if (remainder.length() < 6) // must contain at least the 6 seqNum characters
                throw new IllegalArgumentException("String argument does not have valid base64-encoded sequence number. Got: " + encoded);

            byte[] seqNumBytes = Base64.decode(encoded.substring(0, 6),
                    Base64.NO_PADDING | Base64.NO_WRAP);

            seqNum = ByteBuffer.wrap(seqNumBytes).getInt();

            content = encoded.substring(6);
        }
    }

}

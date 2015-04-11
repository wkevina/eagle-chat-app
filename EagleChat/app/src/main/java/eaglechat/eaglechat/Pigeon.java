package eaglechat.eaglechat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by kevinward on 4/10/15.
 */
public class Pigeon {

    public static final String DELIM = ":";
    public static final String END = "\n";

    public static final String SEND_MESSAGE = "s";
    public static final String SET_KEY = "p";

    private Queue<String> messageQueue;
    private String buffer;

    public Pigeon() {
        messageQueue = new LinkedList<>();
        buffer = new String();
    }

    public void onData(String data) {
        buffer += data; // Store data in buffer

        // Split buffer on newlines
        int breakIndex;
        while( (breakIndex = buffer.indexOf(DELIM)) != -1 ) {
            String temp = buffer.substring(0, breakIndex);
            messageQueue.add(temp); // Queue the message string
            // Reset buffer
            buffer = buffer.substring(breakIndex + 1); // Strip the first message and the DELIM
        }
    }

    public byte[] formatSendMessage(int dest, String message) {
        return (SEND_MESSAGE + DELIM + String.valueOf(dest) + DELIM + message + END).getBytes();
    }

    public byte[] formatSendPublicKey(int dest, byte[] key) {
        if (key.length != 32) {
            throw new IllegalArgumentException("key must be 32 bytes long");
        }
        return (SET_KEY + DELIM + String.valueOf(dest) + DELIM + key + END).getBytes();
    }
}

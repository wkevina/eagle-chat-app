package eaglechat.eaglechat;

import android.os.Handler;
import android.util.Log;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.jdeferred.Deferred;
import org.jdeferred.DoneFilter;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by kevinward on 4/10/15.
 */
public class Peregrine {

    public static final String DELIM = ":";
    //public static final String RECEIVE = "r"; // ^r:\d+:.*
    public static final String RECEIVE_REGEX = "^r" + DELIM + "\\d+" + DELIM + ".+$"; // r:123:message
    public static final String GET_KEY = "^g" + DELIM + "p" + DELIM + ".{32}$";
    public static final String END = "\n";
    public static final String OK = "OK";
    public static final String FAIL = "FAIL";
    public static final String YES = "YES";
    public static final String NO = "NO";
    public static final String SEND = "s"; // ^s:\d+:.*
    public static final String SET_ID = "i"; //i:123
    public static final String SET_KEY = "p"; //p:123:[32 bytes]
    public static final String GET = "g";
    public static final String TAG = "eaglechat.eaglechat";

    public static final int TIMEOUT_AFTER = 1000;

    private static final String REPLY = "x";
    private static final String FAIL_REPLY = REPLY + DELIM + FAIL;
    public static final String GET_STATUS_REPLY = "^" + REPLY + DELIM + "[0-9A-F]{2,}";


    // get status g:s:255   ^g:s:\d{1,3}
    // get public key g:p:[32 bytes]    ^g:p:.{32}
    protected final Queue<String> mQueue;
    final Handler mHandler = new Handler();
    protected String buffer;
    protected SerialInputOutputManager mSerial;

    protected Deferred<String, String, String> mDeferred; // The current promise waiting to be resolved
    protected MessageResolver mResolver;
    private int statusCount = 0;

    public Peregrine() {
        mQueue = new LinkedList<>();
        buffer = "";
        //statusTest();
        Log.d(TAG, "Created a peregrine. SQUAAAW");
    }

    public Peregrine(SerialInputOutputManager serial) {
        this();
        mSerial = serial;
    }

    public void setSerial(SerialInputOutputManager mSerial) {
        this.mSerial = mSerial;
    }

    // Builds a message queue from incoming data, buffering it until END is found in the stream
    public void onData(String data) {
        buffer += data; // Store data in buffer

        // Split buffer on newlines
        int breakIndex;
        while ((breakIndex = buffer.indexOf(END)) != -1) {
            String temp = buffer.substring(0, breakIndex);
            mQueue.add(temp); // Queue the message string
            // Reset buffer
            buffer = buffer.substring(breakIndex + 1); // Strip the first message and the END
        }
        processQueue();
    }

    private void processQueue() {

        while (!mQueue.isEmpty()) {

            String head = mQueue.remove();

            Log.d(TAG, "processQueue: Head: " + head);

            if (mResolver != null && !mResolver.isDone()) {
                mResolver.filter(head);
            } else {
                mResolver = null; // if mResolver is done, get rid of it
            }
        }
    }

    // Get promise that resolves to the peripherals status flags
    public Promise<Integer, String, String> requestStatus() {

        mResolver = new MessageResolver(new MessageResolverFilter() {
            @Override
            public int filter(String msg) {
                if (!msg.startsWith(REPLY)) {
                    Log.d(TAG, "Reply doesn't match format.");
                    return SKIP;
                }
                if (msg.contains(FAIL_REPLY)) {
                    Log.d(TAG, "Reply was a fail message.");
                    return REJECT;
                }
                else if (msg.matches(GET_STATUS_REPLY)) {
                    Log.d(TAG, "Reply valid, resolving promise.");
                    return RESOLVE;
                }

                Log.d(TAG, "Skipping message.");

                return SKIP;
            }
        }, TIMEOUT_AFTER);

        Promise<String,String,String> p = mResolver.getPromise();

        if (mSerial != null) {
            mSerial.writeAsync(formatGetStatus());
        } else {
            mResolver.cancel("NO DEVICE CONNECTED");
        }

        return p.then(new DoneFilter<String, Integer>() {

            // De-hexilify result
            @Override
            public Integer filterDone(String result) {
                String chunks[] = result.split(DELIM);
                return Integer.parseInt(chunks[1], 16);
            }
        });

    }


    private byte[] formatGetStatus() {
        return ("g:s" + END).getBytes();
    }

    private byte[] formatGetStatusDebug(boolean fail, boolean timeout) {
        if (fail) {
            return ("g:s:f" + END).getBytes();
        } else if (timeout) {
            return ("g:s:t" + END).getBytes();
        }
        return ("g:s" + END).getBytes();
    }

    public byte[] formatSendMessage(int dest, String message) {
        return (SEND + DELIM + String.valueOf(dest) + DELIM + message + END).getBytes();
    }

    public byte[] formatSendPublicKey(int dest, byte[] key) {
        if (key.length != 32) {
            throw new IllegalArgumentException("key must be 32 bytes long");
        }
        return (SET_KEY + DELIM + String.valueOf(dest) + DELIM + new String(key) + END).getBytes();
    }

    private interface MessageResolverFilter {
        static final int RESOLVE = 0;
        static final int REJECT = 1;
        static final int SKIP = 2;

        // return either RESOLVE, REJECT, or SKIP
        public int filter(String msg);
    }

    private static class MessageResolver {
        private eaglechat.eaglechat.DeferredObjectWithTimeout<String, String, String> mDeferred;
        private MessageResolverFilter mFilter;

        public MessageResolver(MessageResolverFilter filter, long timeoutAfter) {
            mFilter = filter;
            mDeferred = new DeferredObjectWithTimeout<>(timeoutAfter, "TIMEOUT");
        }

        Promise<String, String, String> getPromise() {
            return mDeferred.promise();
        }

        public void cancel(String reason) {
            mDeferred.reject(reason);
        }

        public void filter(String msg) {
            if (mDeferred.isPending()) {
                int result = mFilter.filter(msg);

                Log.d(TAG, "Filtering reply");

                switch (result) {
                    case MessageResolverFilter.RESOLVE:
                        mDeferred.resolve(msg);
                        break;
                    case MessageResolverFilter.REJECT:
                        mDeferred.reject(msg);
                        break;
                    case MessageResolverFilter.SKIP:
                        break;
                }
            }
        }

        public boolean isDone() {
            return !mDeferred.isPending();
        }
    }

}

    // protected Promise requestStatus() {};

    // protected Promise sendMessage(int dest, String message);

    // protected Promise getPublicKey();

    //


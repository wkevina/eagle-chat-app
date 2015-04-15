package eaglechat.eaglechat;

import android.os.Handler;
import android.util.Log;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.jdeferred.AlwaysCallback;
import org.jdeferred.Deferred;
import org.jdeferred.DoneFilter;
import org.jdeferred.Promise;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by kevinward on 4/10/15.
 */
public class Peregrine {

    public static final String DELIM = ":";

    public static final String END = "\n";
    public static final String OK = "OK";
    public static final String FAIL = "FAIL";
    public static final String SEND = "s"; // ^s:\d+:.*
    public static final String ID = "i"; //i:AA
    public static final String KEY = "p"; //p:123:[32 bytes]
    public static final String GET = "g";
    public static final String TAG = "eaglechat.eaglechat";

    public static final int TIMEOUT_AFTER = 1000;
    public static final String COMMIT = "c";
    private static final String SET_PASSWORD = "h";
    private static final String AUTHENTICATE = "a";
    private static final String REPLY = "x";
    private static final String FAIL_REPLY = REPLY + DELIM + FAIL;
    private static final String OK_REPLY = REPLY + DELIM + OK;
    private static final String GET_STATUS_REPLY = "^" + REPLY + DELIM + "[0-9A-F]{2,}";
    private static final String GET_ID_REPLY = GET_STATUS_REPLY; // Shares format with GET_STATUS_REPLY
    private static final String SEND_COMMAND_REPLY = "^" + REPLY + DELIM + OK;
    private static final String GET_PUBLIC_KEY_REPLY = "^" + REPLY + DELIM + "[0-9A-F]{64,}$";
    private static final String GEN_KEYS = "k";
    // get status g:s:255   ^g:s:\d{1,3}
    // get public key g:p:[32 bytes]    ^g:p:.{32}
    protected final Queue<String> mInputQueue;
    protected final Deque<MessageResolver> mResolverQueue;
    final Handler mHandler = new Handler();
    protected String buffer;
    protected SerialInputOutputManager mSerial;

    protected Deferred<String, String, String> mDeferred; // The current promise waiting to be resolved
    protected MessageResolver mResolver;
    private int statusCount = 0;

    public Peregrine() {
        mInputQueue = new LinkedList<>();
        mResolverQueue = new LinkedList<>();
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
            mInputQueue.add(temp); // Queue the message string
            // Reset buffer
            buffer = buffer.substring(breakIndex + 1); // Strip the first message and the END
        }
        processQueue();
    }

    private void processQueue() {

        while (!mInputQueue.isEmpty()) {

            String currentMessage = mInputQueue.remove();

            Log.d(TAG, "processQueue: Head: " + currentMessage);

            // Handle incoming messages here


            // Service deferred tasks
            if (!mResolverQueue.isEmpty()) {

                Log.d(TAG, "ResolverQueue is not empty");

                // If there are resolvers
                MessageResolver headResolver = null;

                // Clear out finished resolvers, probably due to timeout
                while (!mResolverQueue.isEmpty()) {

                    headResolver = mResolverQueue.peek();

                    if (headResolver.isDone()) {
                        Log.d(TAG, "Removing head resolver.");
                        mResolverQueue.remove();
                    } else
                        break;
                }

                headResolver = mResolverQueue.peek();
                if (headResolver != null) {
                    boolean clearResolver = headResolver.filter(currentMessage);
                    if (clearResolver) {
                        mResolverQueue.remove();
                        Log.d(TAG, "Clearing recently resolved resolver.");
                    }
                }

            }
        }
    }


    private byte[] formatStatusRequest() {
        return ("g:s" + END).getBytes();
    }


    /**
     * Sends status request to peripheral, returns Promise that resolves to that value
     *
     * @return Promise which resolves to status flags of peripheral
     */
    public Promise<Integer, String, String> requestStatus() {

        byte[] statusRequestBytes = formatStatusRequest();

        Promise<String, String, String> statusPromise;

        statusPromise = deferredWrite(statusRequestBytes, new MessageResolverFilter() {
            @Override
            public int filter(String msg) {
                if (!msg.startsWith(REPLY)) {
                    Log.d(TAG, "Reply doesn't match format.");
                    return SKIP;
                }
                if (msg.contains(FAIL_REPLY)) {
                    Log.d(TAG, "Reply was a fail message.");
                    return REJECT;
                } else if (msg.matches(GET_STATUS_REPLY)) {
                    Log.d(TAG, "Reply valid, resolving promise.");
                    return RESOLVE;
                }

                Log.d(TAG, "Skipping message.");

                return SKIP;
            }
        });

        return statusPromise.then(new DoneFilter<String, Integer>() {

            // De-hexlify result
            @Override
            public Integer filterDone(String result) {
                String chunks[] = result.split(DELIM);
                return Integer.parseInt(chunks[1], 16);
            }
        });

    }

    private void writeOrCancel(byte[] msg, MessageResolver resolver) {
        if (mSerial != null) {
            mSerial.writeAsync(msg);
        } else {
            resolver.cancel("NO DEVICE CONNECTED");
        }
    }


    public byte[] formatSendCommand(int dest, String message) {
        return (SEND + DELIM + String.valueOf(dest) + DELIM + message + END).getBytes();
    }

    /**
     * Requests the peripheral to send a message to another node
     *
     * @param dest
     * @param message
     * @return
     */
    public Promise<String, String, String> commandSendMessage(int dest, String message) {

        if (dest > 254 || dest < 1) {
            throw new IllegalArgumentException("dest must be in range of 1-254");
        }

        byte[] sendMessageBytes = formatSendCommand(dest, message);

        Promise<String, String, String> sendMessagePromise;

        sendMessagePromise = deferredWrite(sendMessageBytes, new MessageResolverFilter() {
            @Override
            public int filter(String msg) {
                if (!msg.startsWith(REPLY)) {
                    Log.d(TAG, "Reply doesn't match format.");
                    return SKIP;
                }
                if (msg.contains(FAIL_REPLY)) {
                    Log.d(TAG, "Reply was a fail message.");
                    return REJECT;
                } else if (msg.matches(SEND_COMMAND_REPLY)) {
                    Log.d(TAG, "Reply valid, message sent.");
                    return RESOLVE;
                }

                Log.d(TAG, "Skipping message.");

                return SKIP;
            }
        });

        return sendMessagePromise;
    }

    public byte[] formatSetIdCommand(String nodeId) {
        return (ID + DELIM + nodeId + END).getBytes();
    }

    /**
     * Sets the node id of the peripheral
     *
     * @param nodeId
     * @return
     */
    public Promise<String, String, String> commandSetId(String nodeId) {

        if (nodeId.length() != 2) {
            throw new IllegalArgumentException("nodeId must be in range of 0-254");
        }

        Promise<String, String, String> setIdPromise;

        byte[] setIdMessage = formatSetIdCommand(nodeId.toUpperCase());

        setIdPromise = deferredWrite(setIdMessage, new MessageResolverFilter() {
            @Override
            public int filter(String msg) {
                if (!msg.startsWith(REPLY)) {
                    Log.d(TAG, "Reply doesn't match format.");
                    return SKIP;
                }
                if (msg.contains(FAIL_REPLY)) {
                    Log.d(TAG, "Reply was a fail message.");
                    return REJECT;
                } else if (msg.matches(OK_REPLY)) {
                    Log.d(TAG, "Reply valid, nodeId set.");
                    return RESOLVE;
                }

                Log.d(TAG, "Skipping message.");

                return SKIP;
            }
        });

        return setIdPromise;

    }


    private byte[] formatIdRequest() {
        return (GET + DELIM + ID + END).getBytes();
    }

    public Promise<Integer, String, String> requestId() {

        Promise<String, String, String> requestIdPromise;

        byte[] requestIdMessage = formatIdRequest();

        requestIdPromise = deferredWrite(requestIdMessage, new MessageResolverFilter() {
            @Override
            public int filter(String msg) {
                if (!msg.startsWith(REPLY)) {
                    Log.d(TAG, "Reply doesn't match format.");
                    return SKIP;
                }
                if (msg.contains(FAIL_REPLY)) {
                    Log.d(TAG, "Reply was a fail message.");
                    return REJECT;
                } else if (msg.matches(GET_ID_REPLY)) {
                    Log.d(TAG, "Reply valid, got node id.");
                    return RESOLVE;
                }

                Log.d(TAG, "Skipping message.");

                return SKIP;
            }
        });

        return requestIdPromise.then(new DoneFilter<String, Integer>() {
            @Override
            public Integer filterDone(String result) {
                result = result.split(DELIM)[1];
                return Integer.parseInt(result, 16);
            }
        });
    }


    private byte[] formatPublicKeyRequest() {
        return (GET + DELIM + KEY + END).getBytes();
    }


    public Promise<String, String, String> requestPublicKey() {

        Promise<String, String, String> requestPublicKeyPromise;

        byte[] requestIdMessage = formatPublicKeyRequest();

        requestPublicKeyPromise = deferredWrite(requestIdMessage, new MessageResolverFilter() {
            @Override
            public int filter(String msg) {
                if (!msg.startsWith(REPLY)) {
                    Log.d(TAG, "Reply doesn't match format.");
                    return SKIP;
                }
                if (msg.contains(FAIL_REPLY)) {
                    Log.d(TAG, "Reply was a fail message.");
                    return REJECT;
                } else if (msg.matches(GET_PUBLIC_KEY_REPLY)) {
                    Log.d(TAG, "Reply valid, resolving public key.");
                    return RESOLVE;
                }

                Log.d(TAG, "Skipping message.");

                return SKIP;
            }
        });

        return requestPublicKeyPromise.then(new DoneFilter<String, String>() {
            @Override
            public String filterDone(String result) {
                return result.split(DELIM)[1];
            }
        });
    }

    private byte[] formatSetPasswordCommand(String hash) {
        return (SET_PASSWORD + DELIM + hash + END).getBytes();
    }

    public Promise<String, String, String> commandSetPassword(String hash) {
        if (hash.length() != EagleChatConfiguration.PASSWORD_HASH_LENGTH) {
            throw new IllegalArgumentException("Password wrong length");
        }

        Promise<String, String, String> setPasswordPromise;

        byte[] setPasswordMessage = formatSetPasswordCommand(hash);

        setPasswordPromise = deferredWrite(setPasswordMessage, new MessageResolverFilter() {
            @Override
            public int filter(String msg) {
                if (!msg.startsWith(REPLY)) {
                    Log.d(TAG, "Reply doesn't match format.");
                    return SKIP;
                }
                if (msg.contains(FAIL_REPLY)) {
                    Log.d(TAG, "Reply was a fail message.");
                    return REJECT;
                } else if (msg.matches(OK_REPLY)) {
                    Log.d(TAG, "Reply valid, password set.");
                    return RESOLVE;
                }

                Log.d(TAG, "Skipping message.");

                return SKIP;
            }
        });

        return setPasswordPromise;

    }


    private byte[] formatAuthenticateCommand(String hash) {
        return (AUTHENTICATE + DELIM + hash + END).getBytes();
    }

    public Promise<String, String, String> commandAuthenticate(String hash) {

        if (hash.length() != EagleChatConfiguration.PASSWORD_HASH_LENGTH) {
            throw new IllegalArgumentException("Password wrong length");
        }

        Promise<String, String, String> authPromise;

        byte[] authMessage = formatAuthenticateCommand(hash);

        authPromise = deferredWrite(authMessage, new MessageResolverFilter() {
            @Override
            public int filter(String msg) {
                if (!msg.startsWith(REPLY)) {
                    Log.d(TAG, "Reply doesn't match format.");
                    return SKIP;
                }
                if (msg.contains(FAIL_REPLY)) {
                    Log.d(TAG, "Reply was a fail message.");
                    return REJECT;
                } else if (msg.matches(OK_REPLY)) {
                    Log.d(TAG, "Reply valid, authenticated.");
                    return RESOLVE;
                }

                Log.d(TAG, "Skipping message.");

                return SKIP;
            }
        });

        return authPromise;

    }


    private byte[] formatGenerateKeysCommand() {

        return (GEN_KEYS + END).getBytes();
    }

    public Promise<String, String, String> commandGenerateKeys() {

        Promise<String, String, String> generateKeysPromise;

        byte[] generateKeysMessage = formatGenerateKeysCommand();

        generateKeysPromise = deferredWrite(generateKeysMessage, new MessageResolverFilter() {
            @Override
            public int filter(String msg) {
                if (!msg.startsWith(REPLY)) {
                    Log.d(TAG, "Reply doesn't match format.");
                    return SKIP;
                }
                if (msg.contains(FAIL_REPLY)) {
                    Log.d(TAG, "Reply was a fail message.");
                    return REJECT;
                } else if (msg.matches(OK_REPLY)) {
                    Log.d(TAG, "Reply valid, resolving generate command.");
                    return RESOLVE;
                }

                Log.d(TAG, "Skipping message.");

                return SKIP;
            }
        });

        return generateKeysPromise;
    }


    private byte[] formatCommitCommand() {
        return (COMMIT + END).getBytes();
    }

    public Promise<String, String, String> commandCommit() {

        Promise<String, String, String> commitPromise;

        byte[] commandCommitMessage = formatCommitCommand();

        commitPromise = deferredWrite(commandCommitMessage, new MessageResolverFilter() {
            @Override
            public int filter(String msg) {
                if (!msg.startsWith(REPLY)) {
                    Log.d(TAG, "Reply doesn't match format.");
                    return SKIP;
                }
                if (msg.contains(FAIL_REPLY)) {
                    Log.d(TAG, "Reply was a fail message.");
                    return REJECT;
                } else if (msg.matches(OK_REPLY)) {
                    Log.d(TAG, "Reply valid, resolving generate command.");
                    return RESOLVE;
                }

                Log.d(TAG, "Skipping message.");

                return SKIP;
            }
        });

        return commitPromise;
    }

    private Promise<String, String, String> deferredWrite(byte[] toPeripheral, MessageResolverFilter filter) {

        final MessageResolver writeResolver = new MessageResolver(filter, TIMEOUT_AFTER);
        final byte[] messageBytes = toPeripheral;

        // Get the most recently added resolver
        MessageResolver lastResolver = mResolverQueue.peekLast();

        if (lastResolver != null && !lastResolver.isDone()) { // get in line behind other request
            Log.d(TAG, "Queuing write operation.");
            lastResolver.getPromise().always(new AlwaysCallback<String, String>() {

                @Override
                public void onAlways(Promise.State state, String resolved, String rejected) {

                    writeOrCancel(messageBytes, writeResolver);
                    writeResolver.startTimer();

                }

            });

        } else {
            Log.d(TAG, "Writing immediately.");

            writeOrCancel(messageBytes, writeResolver);
            writeResolver.startTimer();

        }

        // Queue the resolver
        mResolverQueue.offer(writeResolver);

        return writeResolver.getPromise();
    }

    public byte[] formatSendPublicKey(int dest, byte[] key) {
        if (key.length != 32) {
            throw new IllegalArgumentException("key must be 32 bytes long");
        }
        return (KEY + DELIM + String.valueOf(dest) + DELIM + new String(key) + END).getBytes();
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

        public boolean filter(String msg) {
            if (mDeferred.isPending()) {
                int result = mFilter.filter(msg);

                Log.d(TAG, "Filtering reply");

                switch (result) {
                    case MessageResolverFilter.RESOLVE:
                        mDeferred.resolve(msg);
                        return true;
                    case MessageResolverFilter.REJECT:
                        mDeferred.reject(msg);
                        return true;
                    case MessageResolverFilter.SKIP:
                        return false;
                }
            }
            return true;
        }

        public void startTimer() {
            if (mDeferred.isPending())
                mDeferred.startTimer();
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


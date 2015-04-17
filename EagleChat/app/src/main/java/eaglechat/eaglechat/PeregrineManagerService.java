package eaglechat.eaglechat;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SendWrapper;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.impl.DeferredObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeregrineManagerService extends Service {

    private static final String TAG = "eaglechat.eaglechat";

    public static final String SERVICE_AVAILABLE = TAG + ".SERVICE_AVAILABLE";
    public static final String SERVICE_DISCONNECTED = TAG + ".SERVICE_DISCONNECTED";
    public static String BURN = TAG + ".BURN";
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {
                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            PeregrineManagerService.this.updateReceivedData(data);
                        }
                    });
                }
            };
    public static boolean isConnected = false;
    private final IBinder mBinder = new PeregrineBinder();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    UsbDevice mUsbDevice;
    Handler mHandler;
    Hashtable<Pair<Integer, String>, String> mSeqNumMap = new Hashtable<>();
    private UsbManager mUsbManager;
    private SerialInputOutputManager mSerialIoManager;
    private UsbSerialPort mPort;
    private Peregrine mPeregrine;
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action.equals(DeviceConnectionReceiver.DEVICE_DETACHED)) {

                Log.d(TAG, "Device detached. Stopping service.");


                Intent disconnected = new Intent(SERVICE_DISCONNECTED);
                LocalBroadcastManager.getInstance(PeregrineManagerService.this).sendBroadcastSync(disconnected);

                stopIoManager();

                stopSelf();
            }

            if (action.equals(BURN)) {
                if (mPeregrine != null) {
                    mPeregrine.commandBurn();
                }
            }
        }
    };
    private SendManager mSendManager;

    public PeregrineManagerService() {

    }

    /**
     * Called on new data from peripheral
     *
     * @param data
     */
    private void updateReceivedData(byte[] data) {
        String boardSays = new String(data);
        Log.d(TAG, boardSays);
        mPeregrine.onData(boardSays);
    }


    @Override
    public void onCreate() {

        super.onCreate();
        mHandler = new Handler();


        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(DeviceConnectionReceiver.DEVICE_DETACHED));
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(BURN));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "Received onStartCommand");

        if (intent != null) {

            Log.d(TAG, intent.toString());
            Toast.makeText(this, getString(R.string.note_deviceConnected), Toast.LENGTH_SHORT).show();

            mUsbDevice = null;
            mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            mUsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

            UsbSerialDriver driver = new CdcAcmSerialDriver(mUsbDevice);
            onHasPort(driver);

            LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);

            manager.sendBroadcast(new Intent(SERVICE_AVAILABLE));

            isConnected = true;
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "Client re-binding to manager service.");
    }

    private void runOnUiThread(Runnable runnable) {
        mHandler.post(runnable);
    }

    private void onHasPort(UsbSerialDriver driver) {
        UsbSerialPort port = driver.getPorts().get(0);
        UsbDeviceConnection connection = mUsbManager.openDevice(driver.getDevice());
        if (connection == null) {
            Log.d(TAG, "Could not open connection.");
            return;
        }

        Log.d(TAG, "Open connection on port: " + port.toString());

        Log.d(TAG, "Serial number: " + connection.getSerial());
        try {
            port.open(connection);
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            /*try {
                Log.d(TAG, "Writing to device.");
                //port.write("\n".getBytes(), 1000);
            } catch (IOException ex) {
                Log.e(TAG, ex.toString());
            }*/

        } catch (IOException ex) {
            Log.e(TAG, "Open failed. Exception: ");
            Log.e(TAG, ex.toString());
            try {
                port.close();
            } catch (IOException ex2) {
                // Ignore.
            }
            return;
        }

        mPort = port; // save newly opened port

        onDeviceStateChange();


    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
        try {
            mPort.setDTR(true);
        } catch (IOException ex) {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isConnected = false;

        Log.d(TAG, "Destroying EagleChatCommService");
        stopIoManager();

    }

    private void stopIoManager() {
        isConnected = false;

        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
        if (mPeregrine != null) {
            mPeregrine.setSerial(null);
            mPeregrine = null;
        }
    }

    private void startIoManager() {
        if (mPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(mPort, mListener);
            mExecutor.submit(mSerialIoManager);
            mPeregrine = new Peregrine(this, mSerialIoManager);
            //mPeregrine.setSerial(mSerialIoManager);

            isConnected = true;
        }
    }

    public void startSendManager() {
        if (mSendManager == null) {
            mSendManager = new SendManager();
            mSendManager.start();
        }
    }

    void handleReceivedMessage(String msg) {
        (new AsyncTask<String, Void, Void>() {

            @Override
            protected Void doInBackground(String... params) {
                if (params.length < 1)
                    return null;

                String[] chunks = params[0].split(Peregrine.DELIM);

                if (chunks.length < 3)
                    return null;

                String sourceIdHex = chunks[1];

                String content;
                Integer seqNum = null;

                String encoded;

                if (chunks.length > 3) {

                    String[] sub = Arrays.copyOfRange(chunks, 2, chunks.length);

                    encoded = TextUtils.join(Peregrine.DELIM, sub);
                } else {

                    encoded = chunks[2];
                }

                try {

                    EagleChatConfiguration.DecodedMessage decodedMessage =
                            new EagleChatConfiguration.DecodedMessage(encoded);

                    content = decodedMessage.content;
                    seqNum = decodedMessage.seqNum;

                } catch (IllegalArgumentException ex) {

                    Log.d(TAG, ex.toString());

                    content = chunks[2];
                }

                if (seqNum == null) {

                    Log.d(TAG, "Received message without sequence number.");
                    Log.d(TAG, "Skipping message.");
                    Log.d(TAG, "Message content = " + content);

                    return null;

                } else {

                    Pair<Integer, String> p = new Pair<>(seqNum, sourceIdHex);

                    // Check if we have seen this sequence number from this sender before
                    if (mSeqNumMap.containsKey(p)) {

                        // We have seen it. Are the contents the same?
                        if (mSeqNumMap.get(p).equals(content)) {

                            // we have seen this message before, ignore it
                            Log.d(TAG, "Received duplicate message. Skipping.");
                            return null;
                        }
                    }

                    // Store this message, indexed by seqNum and sourceId
                    mSeqNumMap.put(p, content);
                }


                String[] proj = {ContactsTable.COLUMN_ID, ContactsTable.COLUMN_NODE_ID};

                Cursor contactCursor =
                        getContentResolver()
                                .query(DatabaseProvider.CONTACTS_URI, proj,
                                        ContactsTable.COLUMN_NODE_ID + " = ?", new String[]{sourceIdHex},
                                        null);

                if (contactCursor.moveToNext()) {
                    // sanity check
                    int nodeIdIndex = contactCursor.getColumnIndex(ContactsTable.COLUMN_NODE_ID);
                    int contactIndex = contactCursor.getColumnIndex(ContactsTable.COLUMN_ID);

                    String contactIdHex = contactCursor.getString(nodeIdIndex);

                    if (!sourceIdHex.equals(contactIdHex)) {
                        Log.d(TAG, "Sender not found in contacts.");
                        Log.d(TAG, "Received: " + params[0]);
                        return null;
                    }

                    long contactId = contactCursor.getInt(contactIndex);

                    ContentValues values = new ContentValues();
                    values.put(MessagesTable.COLUMN_RECEIVER, 0);
                    values.put(MessagesTable.COLUMN_SENDER, contactId);
                    values.put(MessagesTable.COLUMN_CONTENT, content);
                    values.put(MessagesTable.COLUMN_SENT, MessagesTable.SENT);
                    values.put(MessagesTable.COLUMN_SEQNUM, seqNum);
                    getContentResolver().insert(DatabaseProvider.MESSAGES_URI, values);

                    Log.d(TAG, "Stored message.");

                }

                contactCursor.close();

                return null;
            }
        }).execute(msg);
    }

    public class PeregrineBinder extends Binder {
        Peregrine getService() {
            return mPeregrine;
        }
    }

    private class SendManager {

        private final String[] msgProj =
                {MessagesTable.COLUMN_ID, MessagesTable.COLUMN_RECEIVER,
                        MessagesTable.COLUMN_CONTENT, MessagesTable.COLUMN_SENT,
                        MessagesTable.COLUMN_SEQNUM};
        private final String[] contactProj =
                {ContactsTable.COLUMN_ID, ContactsTable.COLUMN_NODE_ID, ContactsTable.COLUMN_PUBLIC_KEY};
        Cursor mCursor;
        boolean started = false;
        private Map<Integer, String> mNodeIdsMap;
        private Map<Integer, String> mKeysMap;
        private boolean mBusySending;

        public SendManager() {
            Log.d(TAG, "SendManager created");
            mNodeIdsMap = new Hashtable<>();
            mKeysMap = new Hashtable<>();
        }

        public void start() {
            if (!started) {
                queryAndSend();
            }
            started = true;
        }

        synchronized private void queryAndSend() {
            Log.d(TAG, "queryAndSend");
            if (mBusySending)
                return;

            Log.d(TAG, "queryAndSend running.");

            mBusySending = true;

            if (mPeregrine == null) {
                Log.d(TAG, "queryAndSend, board unavailable");

                //Toast.makeText(PeregrineManagerService.this, "EagleChat device unavailable", Toast.LENGTH_SHORT)
                //        .show();
            }


            mCursor = getContentResolver().query(
                    DatabaseProvider.MESSAGES_UNSENT_URI,
                    msgProj, null, null, null);
            mCursor.registerContentObserver(mObserver);

            int msgIndex = mCursor.getColumnIndex(MessagesTable.COLUMN_ID);
            int recIndex = mCursor.getColumnIndex(MessagesTable.COLUMN_RECEIVER); // contact id for dest
            int contentIndex = mCursor.getColumnIndex(MessagesTable.COLUMN_CONTENT);
            int sentIndex = mCursor.getColumnIndex(MessagesTable.COLUMN_SENT);
            int seqNumIndex = mCursor.getColumnIndex(MessagesTable.COLUMN_SEQNUM);

            Log.d(TAG, "seqNumIndex = " + seqNumIndex);

            List<SendWrapper> wrapperList = new ArrayList<>();
            while (mCursor.moveToNext()) {
                // sanity check
                int sent = mCursor.getInt(sentIndex);
                if (sent == MessagesTable.SENT) {
                    Log.d(TAG, "Message already sent.");
                    continue;
                }

                int msgId = mCursor.getInt(msgIndex); // id of this row

                int recId = mCursor.getInt(recIndex);

                String recNodeId;
                String publicKey;

                if (mNodeIdsMap.containsKey(recId)) {

                    recNodeId = mNodeIdsMap.get(recId);
                    publicKey = mKeysMap.get(recId);

                } else {

                    Uri contactUri = ContentUris.withAppendedId(DatabaseProvider.CONTACTS_URI, recId);
                    Cursor contact = getContentResolver().query(contactUri, contactProj, null, null, null);

                    if (contact.moveToNext()) {

                        recNodeId = contact.getString(contact.getColumnIndex(ContactsTable.COLUMN_NODE_ID));
                        publicKey = contact.getString(contact.getColumnIndex(ContactsTable.COLUMN_PUBLIC_KEY));
                        mNodeIdsMap.put(recId, recNodeId);
                        mKeysMap.put(recId, publicKey);

                    } else {
                        // We can't send this message because we don't know how to address it
                        // this should never happen
                        Log.d(TAG, "Skipping message with id = " + msgId + " because there is no contact with id = " + recId);
                        continue;
                    }
                    contact.close();
                }

                String content = mCursor.getString(contentIndex);
                Integer seqNum = mCursor.getInt(seqNumIndex);

                SendWrapper wrapper = new SendWrapper();

                wrapper.nodeId = recNodeId;
                wrapper.publicKey = publicKey;
                // Encode message content before sending
                wrapper.content = EagleChatConfiguration.formatMessage(content, seqNum);
                wrapper.msgId = msgId;

                wrapperList.add(wrapper);

            }

            AsyncTask<List<SendWrapper>, Void, Void> sendTask = new AsyncTask<List<SendWrapper>, Void, Void>() {
                @Override
                protected Void doInBackground(List<SendWrapper>... params) {

                    List<SendWrapper> list = params[0];
                    if (list == null)
                        return null;

                    for (int i = 0; i < list.size(); ++i) {
                        final SendWrapper w = list.get(i);

                        final Deferred sendDeferred = new DeferredObject();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "Main thread, running sendmessage");
                                sendMessage(sendDeferred, Integer.parseInt(w.nodeId, 16), w.publicKey, w.content, w.msgId);
                            }
                        });

                        try {
                            sendDeferred.waitSafely();
                        } catch (InterruptedException ex) {

                        }

                        if (sendDeferred.isRejected()) {
                            Log.d(TAG, "Trying to send public key.");
                            final Deferred keyDeferred = new DeferredObject();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "Main thread, running sendmessage");
                                    sendKey(keyDeferred, Integer.parseInt(w.nodeId, 16), w.publicKey);
                                }
                            });

                            try {
                                keyDeferred.waitSafely();
                            } catch (InterruptedException ex) {

                            }

                            if (keyDeferred.isResolved()) {
                                Log.d(TAG, "Sent key.");

                                final Deferred resendDeferred = new DeferredObject();

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, "Main thread, running sendmessage");
                                        sendMessage(resendDeferred, Integer.parseInt(w.nodeId, 16), w.publicKey, w.content, w.msgId);
                                    }
                                });

                                try {
                                    resendDeferred.waitSafely();
                                } catch (InterruptedException ex) {

                                }
                                if (resendDeferred.isResolved()) {
                                    Log.d(TAG, "Sent 1 message.");
                                    markMessageSent(w.msgId);
                                }
                            } else {
                                Log.d(TAG, "Something else happened.");
                            }
                        } else {
                            Log.d(TAG, "Sent 1 message.");
                            markMessageSent(w.msgId);
                        }

                    }
                    mBusySending = false;

                    return null;
                }
            };

            sendTask.execute(wrapperList);

        }

        private void markMessageSent(long msgId) {

            Uri msgUri = ContentUris.withAppendedId(DatabaseProvider.MESSAGES_URI, msgId);

            ContentValues values = new ContentValues();
            values.put(MessagesTable.COLUMN_SENT, MessagesTable.SENT);
            getContentResolver().update(msgUri, values, null, null);
            //getContentResolver().
        }

        private void sendMessage(final Deferred outerDeferred, final int nodeId, final String publicKey, final String content, final int messageId) {


            Log.d(TAG, "== 383 == commandSendMessage");
            if (mPeregrine != null) {
                mPeregrine.commandSendMessage(nodeId, content)

                        .done(new DoneCallback<String>() {
                            @Override
                            public void onDone(String result) {
                                Log.d(TAG, "sendMessage resolving outerDeferred.");
                                outerDeferred.resolve("DONE");
                            }
                        }).fail(new FailCallback<String>() {
                    @Override
                    public void onFail(String result) {
                        Log.d(TAG, "sendMessage rejecting outerDeferred.");
                        outerDeferred.reject("SEND PUBLIC KEY");
                    }
                });
            } else {
                Log.d(TAG, "Failing to send message. No Peregrine available.");
                outerDeferred.reject("NO PEREGRINE.");
            }


        }

        private void sendKey(final Deferred outerDeferred, final int nodeId, final String publicKey) {

            Log.d(TAG, "sendKey ====");
            if (mPeregrine != null) {
                mPeregrine.commandSendPublicKey(nodeId, publicKey)

                        .done(new DoneCallback<String>() {
                            @Override
                            public void onDone(String result) {
                                Log.d(TAG, "sendKey resolving outerDeferred.");
                                outerDeferred.resolve("DONE");
                            }
                        })
                        .fail(new FailCallback<String>() {
                            @Override
                            public void onFail(String result) {
                                Log.d(TAG, "sendKey failing outerDeferred");
                                outerDeferred.reject("FAIL");
                            }
                        });
            } else {
                Log.d(TAG, "Failing to send key. No Peregrine available.");
                outerDeferred.reject("NO PEREGRINE.");
            }
        }

        private final ContentObserver mObserver = new ContentObserver(mHandler) {

            @Override
            public void onChange(boolean selfChange) {
                Log.d(TAG, "onChange");
                queryAndSend();
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        queryAndSend();
//                    }
//                });

            }

        };


    }


}


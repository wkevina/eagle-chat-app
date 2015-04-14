package eaglechat.eaglechat;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EagleChatCommService extends Service {
    private static final String TAG = "eaglechat.eaglechat";
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
                            EagleChatCommService.this.updateReceivedData(data);
                        }
                    });
                }
            };
    private static final int STATE_DO_NOTHING = 0;
    private static final int STATE_JUST_READ = 1;
    private final IBinder mBinder = new EagleBinder();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    UsbDevice mUsbDevice;
    Handler handler;
    private UsbManager mUsbManager;
    private List<String> buffer = new ArrayList<>();
    private SerialInputOutputManager mSerialIoManager;
    private UsbSerialPort mPort;

    private Peregrine mPeregrine;

    public EagleChatCommService() {

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
        handler = new Handler();
        super.onCreate();

        //mPeregrine = new Peregrine();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("eaglechat.eaglechat", "Received onStartCommand");
        if (intent != null) {
            Log.d("eaglechat.eaglechat", intent.toString());
            Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

            mUsbDevice = null;
            mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            Intent usbIntent = intent;
            mUsbDevice = usbIntent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

            UsbSerialDriver driver = new CdcAcmSerialDriver(mUsbDevice);
            onHasPort(driver);

        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
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

        mPeregrine.requestStatus().done(new DoneCallback<Integer>() {
            @Override
            public void onDone(Integer result) {
                Log.d(TAG, "Status: " + result);
            }
        }).fail(new FailCallback<String>() {
            @Override
            public void onFail(String result) {
                Log.d(TAG, "Failed with: " + result);
            }
        });

        mPeregrine.commandSetId(15).done(new DoneCallback<String>() {
            @Override
            public void onDone(String result) {
                Log.d(TAG, "Set id reply = " + result);

            }
        }).fail(new FailCallback<String>() {
            @Override
            public void onFail(String result) {
                Log.d(TAG, "Failed with: " + result);
            }
        });

        mPeregrine.requestId().done(new DoneCallback<Integer>() {
            @Override
            public void onDone(Integer result) {
                Log.d(TAG, "Request id reply = " + result);
            }
        });
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
        try {
            mPort.setDTR(true);
        } catch (IOException ex) {
        }
        /*
        if (mPort != null) {
            try {
                mPort.write("\n".getBytes(), 200); // wake up board
            } catch (IOException ex) {

            }
        }
        */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopIoManager();
    }

    private void stopIoManager() {
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
            mPeregrine = new Peregrine(mSerialIoManager);
            //mPeregrine.setSerial(mSerialIoManager);
        }
    }

    public class EagleBinder extends Binder {
        EagleChatCommService getService() {
            return EagleChatCommService.this;
        }
    }
}

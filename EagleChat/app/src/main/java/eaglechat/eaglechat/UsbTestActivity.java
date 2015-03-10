package eaglechat.eaglechat;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class UsbTestActivity extends ActionBarActivity {

    private static final String TAG = "eaglechat.eaglechat";
    private static final int STATE_DO_NOTHING = 0;
    private static final int STATE_JUST_READ = 1;
    private UsbManager mUsbManager;
    UsbDevice mUsbDevice;
    private List<String> buffer = new ArrayList<>();
    private Button mWriteButton, mReadButton;
    private TextView mLogTextView, mOutputTextView;
    private EditText mInputTextView;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {
                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    UsbTestActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UsbTestActivity.this.updateReceivedData(data);
                        }
                    });
                }
            };
    private SerialInputOutputManager mSerialIoManager;
    private UsbSerialPort mPort;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private int mState = STATE_DO_NOTHING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_test);

        mUsbDevice = null;

        Intent usbIntent = getIntent();
        if (usbIntent != null) {
            mUsbDevice = usbIntent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        }

        mWriteButton = (Button) findViewById(R.id.board_write);
        mReadButton = (Button) findViewById(R.id.board_read);

        mInputTextView = (EditText) findViewById(R.id.board_input);
        mOutputTextView = (TextView) findViewById(R.id.board_output);
        mLogTextView = (TextView) findViewById(R.id.board_log);

        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "READ clicked.");
                /*try {*/

                mSerialIoManager.writeAsync(new byte[]{'R', 0x0A});
                //mSerialIoManager.writeAsync(new byte[]{0x0A});
                mState = STATE_JUST_READ;
                /*} catch (IOException ex) {
                    Log.d(TAG, "Write failed.");
                }*/
            }
        });

        mWriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "READ clicked.");
                /*try {*/
                if (mInputTextView.getText().length() > 0) {
                    String dump = mInputTextView.getText().toString();
                    mSerialIoManager.writeAsync(new byte[]{'W'});
                    mSerialIoManager.writeAsync(dump.getBytes());
                    mSerialIoManager.writeAsync(new byte[]{0x0A});
                }
                //mState = STATE_JUST_READ;
                /*} catch (IOException ex) {
                    Log.d(TAG, "Write failed.");
                }*/
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        if (mUsbDevice != null) {
            UsbSerialDriver driver = new CdcAcmSerialDriver(mUsbDevice);
            onHasPort(driver);
        } else {
            Log.d(TAG, "mUsbDevice is null");
            grabDevice();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (mPort != null) {
            try {
                mPort.close();
            } catch (IOException ex) {
                Log.e(TAG, "Error closing COM port.");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_usb_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateReceivedData(byte[] data) {
        Log.d(TAG, "New data.");
        Log.d(TAG, HexDump.dumpHexString(data));

        //mLogTextView.setText(new String(data));
        buffer.add(new String(data));
        if (buffer.size() > 3) {
            buffer.remove(0);
        }
        String dump = new String();
        for (String s : buffer) {
            dump += s;
        }
        mLogTextView.setText(dump);

        if (mState == STATE_JUST_READ) {
            parseReadData(new String(data));
        }

        /*
        try {
            Log.d(TAG, "Writing to device.");
            mPort.write("String\n".getBytes(), 1000);
        } catch (IOException ex) {
            Log.e(TAG, ex.toString());
        }
        */
    }

    private void parseReadData(String s) {
        String [] chunks = s.replace("\n", "").split(":");
        Log.d(TAG, "parseReadData: " + chunks.toString());
        if (chunks.length == 3) {
            mOutputTextView.setText(chunks[2]);
        }
        mState = STATE_DO_NOTHING;
    }

    private void grabDevice() {
        new AsyncTask<Void, Void, UsbSerialDriver>() {
            @Override
            protected UsbSerialDriver doInBackground(Void... params) {
                final List<UsbSerialDriver> drivers =
                        UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

                for (final UsbSerialDriver driver : drivers) {
                    UsbDevice d = driver.getDevice();
                    final List<UsbSerialPort> ports = driver.getPorts();
                    Log.d(TAG, String.format("+ %s: %s port%s",
                            driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));

                    if (ports.size() > 0) {
                        return driver;
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(UsbSerialDriver result) {
                if (result != null) {
                    onHasPort(result);
                } else {
                    Log.d(TAG, "No port found.");
                }
            }

        }.execute((Void) null);
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
        } catch (IOException ex) {}
        /*
        if (mPort != null) {
            try {
                mPort.write("\n".getBytes(), 200); // wake up board
            } catch (IOException ex) {

            }
        }
        */
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (mPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(mPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }
}

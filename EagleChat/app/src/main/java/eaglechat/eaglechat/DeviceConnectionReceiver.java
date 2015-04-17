package eaglechat.eaglechat;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbId;

public class DeviceConnectionReceiver extends BroadcastReceiver {
    private static final String TAG = "eaglechat.eaglechat";

    private static final String ACTION_USB_PERMISSION =
            TAG + ".USB_PERMISSION";

    public static final String DEVICE_ATTACHED = TAG + ".DEVICE_ATTACHED";
    public static final String DEVICE_DETACHED = TAG + ".DEVICE_DETACHED";

    private Context mContext;
    public DeviceConnectionReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        String action = intent.getAction();
        Log.d(TAG, action);

        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        Log.d(TAG, "Device: " + device);

        if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        notifyDeviceAttached(device);
                    }
                } else {
                    Log.d(TAG, "permission denied for device " + device);
                }
            }
        } else {
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                // filter devices
                boolean isEagleChat = device.getProductId() == UsbId.EAGLE_CHAT && device.getVendorId() == UsbId.VENDOR_ATMEL;
                Log.d(TAG, "Is EagleChat device: " + isEagleChat);
                if (!isEagleChat) {
                    Log.d(TAG, "Not an EagleChat device.");
                    return;
                }

                if (!manager.hasPermission(device)) { // if we do not have permission to access this device
                    Log.d(TAG, "Requesting permission to access device.");
                    final PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    manager.requestPermission(device, permissionIntent); // request it!
                } else {
                    notifyDeviceAttached(device);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                notifyDeviceDetached();
            }
        }
    }

    private void notifyDeviceAttached(UsbDevice device) {
        Log.d(TAG, "Device attached: " + device);
        Intent intent = new Intent(mContext, PeregrineManagerService.class);
        intent.putExtra(UsbManager.EXTRA_DEVICE, device);
        mContext.startService(intent);

        Intent attachedIntent = new Intent(DEVICE_ATTACHED);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(attachedIntent);
    }

    private void notifyDeviceDetached() {
        Log.d(TAG, "Device detached.");
        //Intent intent = new Intent(mContext, PeregrineManagerService.class);
        //mContext.stopService(intent);

        Intent detachedIntent = new Intent(DEVICE_DETACHED);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(detachedIntent);
    }
}

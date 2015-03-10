package eaglechat.eaglechat;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class DeviceAttachedReceiver extends BroadcastReceiver {
    private static final String TAG = "eaglechat.eaglechat";

    private static final String ACTION_USB_PERMISSION =
            "eaglechat.eaglechat.USB_PERMISSION";

    public DeviceAttachedReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

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
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (!manager.hasPermission(device)) { // if we do not have permission to access this device
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
    }

    private void notifyDeviceDetached() {
        Log.d(TAG, "Device detached.");
    }
}

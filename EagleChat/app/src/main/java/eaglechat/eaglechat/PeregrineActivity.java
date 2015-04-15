package eaglechat.eaglechat;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;

import eaglechat.eaglechat.PeregrineManagerService.PeregrineBinder;

/**
 * Created by kevinward on 4/15/15.
 */
abstract public class PeregrineActivity extends ActionBarActivity {
    protected static final String TAG = "eaglechat.eaglechat";

    final Handler mHandler = new Handler();

    protected Peregrine mPeregrine;

    protected boolean mBound = false;

    protected ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.d(TAG, "Bound to service.");

            PeregrineBinder binder = (PeregrineBinder) service;
            mPeregrine = binder.getService();
            mBound = true;

            mPeregrine.requestStatus().done(new DoneCallback<Integer>() {
                @Override
                public void onDone(Integer result) {
                    Log.d(TAG, "Peripheral status = " + result);
                }
            }).fail(new FailCallback<String>() {
                @Override
                public void onFail(String result) {
                    Log.d(TAG, "Checking status failed. Result =" + result);
                }
            });

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onPeregrineAvailable();
                }
            });

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service unbound. Removing peregrine references.");
            mBound = false;
            mPeregrine = null;

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onPeregrineUnavailable();
                }
            });
        }
    };

    protected BroadcastReceiver mServiceAvailableReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action  = intent.getAction();

            if (action.contentEquals(PeregrineManagerService.SERVICE_AVAILABLE)) {

                Log.d(TAG, "Service available broadcast received. Binding service.");
                Intent eagleIntent = new Intent(PeregrineActivity.this, PeregrineManagerService.class);
                bindService(eagleIntent, mConnection, BIND_IMPORTANT);
            }

            if (action.contentEquals(PeregrineManagerService.SERVICE_DISCONNECTED)) {

                Log.d(TAG, "Service unavailable. Attempting to stop service.");

                if (mBound) {
                    unbindService(mConnection);
                }
            }
        }
    };

    public Peregrine getPeregrine() {
        return mPeregrine;
    }

    public boolean peregrineAvailable() {
        return mBound;
    }

    protected void bindEagleChat() {
        Intent eagleIntent = new Intent(PeregrineActivity.this, PeregrineManagerService.class);
        bindService(eagleIntent, mConnection, BIND_IMPORTANT);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);

        manager.registerReceiver(mServiceAvailableReceiver,
                new IntentFilter(PeregrineManagerService.SERVICE_AVAILABLE));
        manager.registerReceiver(mServiceAvailableReceiver,
                new IntentFilter(PeregrineManagerService.SERVICE_DISCONNECTED));

        if (PeregrineManagerService.isConnected) {
            bindEagleChat();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
        }
    }

    abstract void onPeregrineAvailable();

    abstract void onPeregrineUnavailable();
}

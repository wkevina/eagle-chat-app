package eaglechat.eaglechat;

import android.os.Handler;

import org.jdeferred.Deferred;
import org.jdeferred.android.AndroidDeferredObject;
import org.jdeferred.impl.DeferredObject;

/**
 * Created by kevinward on 4/14/15.
 */
public class DeferredObjectWithTimeout<D, F, P> extends AndroidDeferredObject<D, F, P> {
    protected final Handler mHandler = new Handler();
    protected Runnable mTimeoutRunnable;
    protected final long mMillis;

    public DeferredObjectWithTimeout(long millis, F failWith) {
        super(new DeferredObject<D, F, P>());

        mMillis = millis;
        final F failObject = failWith;

        mTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPending()) {
                    reject(failObject);
                }
            }
        };
    }

    public void startTimer() {
        if (mMillis > 0) {
            mHandler.postDelayed(mTimeoutRunnable, mMillis);
        }
    }

    public void stopTimer() {
        mHandler.removeCallbacks(mTimeoutRunnable);
    }

    @Override
    public Deferred<D, F, P> resolve(D resolve) {
        mHandler.removeCallbacks(mTimeoutRunnable);
        return super.resolve(resolve);
    }
}

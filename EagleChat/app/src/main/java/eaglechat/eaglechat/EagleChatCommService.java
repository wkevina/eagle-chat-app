package eaglechat.eaglechat;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class EagleChatCommService extends Service {
    public EagleChatCommService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

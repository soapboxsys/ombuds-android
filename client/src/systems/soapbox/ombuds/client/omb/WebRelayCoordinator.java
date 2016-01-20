package systems.soapbox.ombuds.client.omb;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import systems.soapbox.ombuds.client.omb.memory.PublicRecordDbHelper;
import systems.soapbox.ombuds.client.omb.model.service.WebRelayService;
import systems.soapbox.ombuds.client.omb.model.service.WebRelayServiceManager;

public class WebRelayCoordinator extends Service {

    WebRelayServiceManager webRelayServiceManager;

    public WebRelayCoordinator() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        webRelayServiceManager = new WebRelayServiceManager(new WebRelayService(), new PublicRecordDbHelper(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            String action = intent.getAction();
            if("REFRESH".equals(action)) {
                getNewBulltins();
            }
        }

        return START_STICKY;
    }


    private void getNewBulltins() {
        webRelayServiceManager.getNewBulletins();
    }
}

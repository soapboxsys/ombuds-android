package systems.soapbox.ombuds.client.omb;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import systems.soapbox.ombuds.client.omb.memory.PublicRecordDbHelper;
import systems.soapbox.ombuds.client.omb.model.service.WebRelayService;
import systems.soapbox.ombuds.client.omb.model.service.WebRelayServiceManager;

public class WebRelayCoordinator extends Service {

    public static final String ACTION_REFRESH_NEW_BLTNS = ".refresh_new_bulletins";
    WebRelayServiceManager webRelayServiceManager;

    public static void refreshNewBltns(Context context) {
        Intent intent = new Intent(context, WebRelayCoordinator.class);
        intent.setAction(ACTION_REFRESH_NEW_BLTNS);
        context.startService(intent);
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
            if(ACTION_REFRESH_NEW_BLTNS.equals(action)) {
                getNewBulltins();
            }
        }

        return START_STICKY;
    }


    private void getNewBulltins() {
        webRelayServiceManager.getNewBulletins();
    }
}

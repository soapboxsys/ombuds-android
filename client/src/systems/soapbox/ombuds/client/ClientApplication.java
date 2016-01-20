package systems.soapbox.ombuds.client;

import android.content.Intent;

import systems.soapbox.ombuds.client.omb.WebRelayCoordinator;

/**
 * Created by askuck on 1/19/16.
 */
public class ClientApplication extends WalletApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        startService( new Intent(this, WebRelayCoordinator.class) );
    }
}

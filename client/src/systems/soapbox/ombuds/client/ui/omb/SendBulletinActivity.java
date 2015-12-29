package systems.soapbox.ombuds.client.ui.omb;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import systems.soapbox.ombuds.client.ui.AbstractBindServiceActivity;
import systems.soapbox.ombuds.client_test.R;

public class SendBulletinActivity extends AbstractBindServiceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_send_bulletin);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWalletApplication().startBlockchainService(false);
    }

    @Override
    public void onPause() {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.onPause();
    }
}

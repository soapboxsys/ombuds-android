package systems.soapbox.ombuds.client.omb.model.service;

import android.util.Log;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;
import systems.soapbox.ombuds.client.omb.memory.PublicRecordDbHelper;
import systems.soapbox.ombuds.client.omb.model.response.NewBulletinsResponse;

/**
 * Created by askuck on 1/19/16.
 */
public class WebRelayServiceManager {

    private static final String TAG = "WebRelayServiceManager";
    private WebRelayService relayService;
    private PublicRecordDbHelper pubrecDb;

    public WebRelayServiceManager(WebRelayService service, PublicRecordDbHelper db) {
        this.relayService = service;
        this.pubrecDb = db;
    }

    public void getNewBulletins() {
        // maybe eventbus

        Call<NewBulletinsResponse> response = relayService.getNewBulletins();
        response.enqueue(new Callback<NewBulletinsResponse>() {
            @Override
            public void onResponse(Response<NewBulletinsResponse> response, Retrofit retrofit) {
                if(response.isSuccess()){
                    Log.d(TAG, "received successful response");
                    Log.d(TAG, response.message());
                    pubrecDb.updateNewBulletins(response.body());
                } else {
                    Log.d(TAG, "received BAD response");
                    Log.d(TAG, response.message());
                    // do something
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "FAIL NO RESPONSE");
                // do something
            }
        });
    }
}

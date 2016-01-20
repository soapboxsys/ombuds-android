package systems.soapbox.ombuds.client.omb.model.service;

import retrofit.Call;
import systems.soapbox.ombuds.client.omb.model.response.NewBulletinsResponse;

/**
 * Created by askuck on 1/19/16.
 */
public class WebRelayService extends WebRelayServiceBase {

    public Call<NewBulletinsResponse> getNewBulletins(){
        return getInterfaceInstance().getNewBulletins();
    }
}

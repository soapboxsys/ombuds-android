package systems.soapbox.ombuds.client.omb.model.service;

import retrofit.Call;
import retrofit.http.GET;
import systems.soapbox.ombuds.client.omb.model.response.NewBulletinsResponse;

/**
 * Created by askuck on 1/19/16.
 */
public interface WebRelayServiceInterface {

    @GET("/new")
    Call<NewBulletinsResponse> getNewBulletins();
}

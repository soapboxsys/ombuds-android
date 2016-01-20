package systems.soapbox.ombuds.client.omb.model.service;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

/**
 * Created by askuck on 1/19/16.
 */
public class WebRelayServiceBase {

    private static WebRelayServiceInterface webRelayInterface ;
    private static final String SERVER_URL = "http://52.3.85.241/";

    protected static synchronized WebRelayServiceInterface getInterfaceInstance() {
        if(webRelayInterface == null) {
            webRelayInterface = buildClient().create(WebRelayServiceInterface.class);
        }
        return webRelayInterface;
    }

    private static Retrofit buildClient() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit;
    }

}

package systems.soapbox.ombuds.client.omb.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by askuck on 1/19/16.
 */
public class LocationResponse {

    @SerializedName("lat")
    @Expose
    public Long lat;

    @SerializedName("lon")
    @Expose
    public Long lon;

    @SerializedName("h")
    @Expose
    public Long h;
}

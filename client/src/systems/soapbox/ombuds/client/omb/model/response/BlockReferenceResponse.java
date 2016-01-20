package systems.soapbox.ombuds.client.omb.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by askuck on 1/19/16.
 */
public class BlockReferenceResponse {
    @SerializedName("hash")
    @Expose
    public String hash;

    @SerializedName("h")
    @Expose
    public Long height;

    @SerializedName("ts")
    @Expose
    public Long timeStamp;
}

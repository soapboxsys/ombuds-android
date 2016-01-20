package systems.soapbox.ombuds.client.omb.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by askuck on 1/19/16.
 */
public class BulletinResponse {

    @SerializedName("txid")
    @Expose
    public String txid;

    @SerializedName("author")
    @Expose
    public String author;

    @SerializedName("msg")
    @Expose
    public String msg;

    @SerializedName("timestamp")
    @Expose
    public Long timestamp;

    @SerializedName("numEndos")
    @Expose
    public Long numEndos;

    @SerializedName("blkref")
    @Expose
    public BlockReferenceResponse blockReference;

    @SerializedName("loc")
    @Expose
    public LocationResponse loc;

    @SerializedName("endos")
    @Expose
    public Object endos;
}

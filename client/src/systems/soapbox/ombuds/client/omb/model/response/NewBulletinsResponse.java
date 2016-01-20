package systems.soapbox.ombuds.client.omb.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by askuck on 1/19/16.
 */
public class NewBulletinsResponse {

    @SerializedName("start")
    @Expose
    public String start;

    @SerializedName("stop")
    @Expose
    public String stop;

    @SerializedName("bulletins")
    @Expose
    public List<BulletinResponse> bulletins = new ArrayList<BulletinResponse>();

    @SerializedName("endorsements")
    @Expose
    public Object endorsements;
}

package systems.soapbox.ombuds.client.omb.event;

/**
 * Created by askuck on 1/22/16.
 */
public class SendEndoEvent {
    String bulletinId;
    public SendEndoEvent(String txid) {
        this.bulletinId = txid;
    }

    public String getTxid() {
        return bulletinId;
    }
}

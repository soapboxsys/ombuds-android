package systems.soapbox.ombuds.client.omb.event;

/**
 * Created by askuck on 1/19/16.
 */
public class NewBulletinsEvent {
    private boolean wasSuccess;

    public NewBulletinsEvent(boolean wasSuccess) {
        this.wasSuccess = wasSuccess;
    }

    public boolean wasSuccess() {
        return wasSuccess;
    }

}

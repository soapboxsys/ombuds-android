package systems.soapbox.ombuds.client.omb.memory;

/**
 * Created by askuck on 1/18/16.
 */
public class NotABulletinException extends Exception {
    private static final long serialVersionUID = 1788356585376833797L;

    public NotABulletinException(String s) {super(s);}
    public NotABulletinException(Exception e) {super(e);}
}

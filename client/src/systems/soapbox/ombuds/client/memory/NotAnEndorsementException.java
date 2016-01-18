package systems.soapbox.ombuds.client.memory;

/**
 * Created by askuck on 1/18/16.
 */
public class NotAnEndorsementException extends Exception {
    private static final long serialVersionUID = -803004303604892254L;

    public NotAnEndorsementException(String s) {super(s);}
    public NotAnEndorsementException(Exception e) {super(e);}
}

package systems.soapbox.ombuds.client.ui.omb;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by askuck on 1/18/16.
 */
public class Utils {
    public static String listToHashtagString(List<String> topics) {
        String topicString = "";
        Iterator<String> itr = topics.iterator();
        while(itr.hasNext()) {
            String topic = itr.next();
            if(itr.hasNext())
                topicString += "#"+ topic +", ";
            else
                topicString += "#"+ topic;
        }
        return topicString;
    }

    public static final byte[] md5(final String s) throws NoSuchAlgorithmException {
        final String MD5 = "MD5";

        MessageDigest digest = java.security.MessageDigest.getInstance(MD5);
        digest.update(s.getBytes());
        return digest.digest();
    }

    public static String colorAddr(String address) throws NoSuchAlgorithmException {
        byte[] hash = md5(address);
        int red     = Math.abs(hash[0]) % 256;
        int blue    = Math.abs(hash[1]) % 256;
        int green   = Math.abs(hash[2]) % 256;

        String redHex   = maybeAddPreceedingZero( Integer.toHexString(red) );
        String blueHex  = maybeAddPreceedingZero( Integer.toHexString(blue) );
        String greenHex = maybeAddPreceedingZero( Integer.toHexString(green) );

        return "#" + redHex + blueHex + greenHex;
    }

    private static String maybeAddPreceedingZero(String str) {
        return str.length() == 1 ? "0" + str : str;
    }

    public static String intToHex(int i) {
        char[] hexChar = {'1','2','3','4','5','6','7','8','9','a','b','c','d','e','f',};
        String hexStr = "";
        do{
            hexStr += hexChar[i%16];
            i = i/16;
        } while(i > 16);
        return hexStr;
    }
}

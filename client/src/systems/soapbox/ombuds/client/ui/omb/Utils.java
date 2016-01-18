package systems.soapbox.ombuds.client.ui.omb;

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
}

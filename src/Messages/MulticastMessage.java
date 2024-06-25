package Messages;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import utils.Constants;

public class MulticastMessage extends AbstractMessage {


    /**
     * @return
     */
    @Override
    public Gson gson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setExclusionStrategies(new ExclusionStrategy() {
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                if (fieldAttributes.getName().equals("sent")) {
                    return true;
                }
                if ("roomID".equals(fieldAttributes.getName())) {
                    return roomID == -1;
                }
                return false;
            }

            public boolean shouldSkipClass(Class aClass) {
                return false;
            }
        });
        return builder.create();
    }

    public MulticastMessage(int clientID,
            int type,
            int roomID,
            int[] vectorTimestamp) {
        super(clientID, type, roomID, vectorTimestamp);
    }

    ExclusionStrategy exclusionStrategy = new ExclusionStrategy() {
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            if (fieldAttributes.getName().equals("sent")) {
                return true;
            }
            if ("roomID".equals(fieldAttributes.getName())) {
                return roomID == -1;
            }
            return false;
        }

        public boolean shouldSkipClass(Class aClass) {
            return false;
        }
    };

    public static MulticastMessage getWelcomeMessage(int clientID) {
        return new MulticastMessage(
                clientID,
                Constants.MESSAGE_TYPE_WELCOME,
                Constants.DEFAULT_GROUP_ROOMID,
                null
        );


    }

}



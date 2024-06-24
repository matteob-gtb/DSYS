package Messages;

import com.google.gson.*;
import com.google.gson.annotations.Expose;

public class Message {


     ExclusionStrategy exclusionStrategy = new ExclusionStrategy() {
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            if ("roomID".equals(fieldAttributes.getName())) {
                return roomID == -1;
            }
            return false;
        }

        public boolean shouldSkipClass(Class aClass) {
            return false;
        }
    };

    public  Gson gson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setExclusionStrategies(exclusionStrategy);
        return builder.create();
    }


    private transient boolean isRoomMessage = false;

    private int messageType;

    private int senderID;

    //-1 if it's not meant to be a room message
    private int roomID;

    private int recipientID;

    private int[] vectorTimestamp;


    public Message(int senderID,int messageType, int roomID, int recipientID, int[] vectorTimestamp) {
        this.messageType = messageType;
        this.roomID = roomID;
        this.recipientID = recipientID;
        this.vectorTimestamp = vectorTimestamp;
    }


}

package Messages;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import utils.Constants;

public class Message {


    public static Message getWelcomeMessage(int clientID) {
        return new Message(
                clientID,
                Constants.MESSAGE_TYPE_WELCOME,
                Constants.DEFAULT_GROUP_ROOMID,
                -1,
                null
        );


    }


    ExclusionStrategy exclusionStrategy = new ExclusionStrategy() {
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            if (fieldAttributes.getName().equals("recipientID")) {
                return recipientID == -1;
            }
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

    public Gson gson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setExclusionStrategies(exclusionStrategy);
        return builder.create();
    }

    public String toJSONString() {
        return gson().toJson(this);
    }


    private transient boolean isRoomMessage = false;


    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    @Expose(serialize = false, deserialize = false)
    private boolean sent = false;

    public boolean isRoomMessage() {
        return isRoomMessage;
    }

    public int getMessageType() {
        return messageType;
    }

    public int getSenderID() {
        return senderID;
    }

    public int getRoomID() {
        return roomID;
    }

    public int getRecipientID() {
        return recipientID;
    }

    public int[] getVectorTimestamp() {
        return vectorTimestamp;
    }

    public ExclusionStrategy getExclusionStrategy() {
        return exclusionStrategy;
    }

    private int messageType;
    private int senderID;

    //-1 if it's not meant to be a room message
    private int roomID;

    private int recipientID;

    private int[] vectorTimestamp;

    public Message() {
        this.recipientID = -1;
    }

    public Message(int senderID, int messageType, int roomID, int recipientID, int[] vectorTimestamp) {
        this.senderID = senderID;
        this.messageType = messageType;
        this.roomID = roomID;
        this.recipientID = recipientID;
        this.vectorTimestamp = vectorTimestamp;
    }


}

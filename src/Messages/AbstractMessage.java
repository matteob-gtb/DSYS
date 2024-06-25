package Messages;

import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import utils.Constants;

public abstract class AbstractMessage implements MessageInterface {

    protected transient boolean isRoomMessage = false;
    protected int messageType;
    protected int senderID;

    //-1 if it's not meant to be a room message
    protected int roomID;

    protected int[] vectorTimestamp;


    public abstract Gson gson();

    @Override
    public String toJSONString() {
        return gson().toJson(this);
    }


    @Override
    public boolean isSent() {
        return sent;
    }

    @Override
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

    public int[] getVectorTimestamp() {
        return vectorTimestamp;
    }



    public AbstractMessage() {
    }

    public AbstractMessage(int senderID, int messageType, int roomID, int[] vectorTimestamp) {
        this.senderID = senderID;
        this.messageType = messageType;
        this.roomID = roomID;
        this.vectorTimestamp = vectorTimestamp;
    }


}

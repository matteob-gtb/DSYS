package Messages;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

public abstract class AbstractMessage implements MessageInterface {
    protected String payload;
    protected transient boolean isRoomMessage = false;
    protected int messageType;
    protected int senderID;
    protected String username = null    ;

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPayload() {
        return payload;
    }


    //-1 if it's not meant to be a room message
    protected int roomID;

    public void setPayload(String rawPayload) {
        this.payload = rawPayload;
    }

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

    public AbstractMessage( int senderID, int messageType, int roomID, int[] vectorTimestamp) {
        this.senderID = senderID;
        this.messageType = messageType;
        this.roomID = roomID;
        this.vectorTimestamp = vectorTimestamp;
    }

}

package Messages;

import VectorTimestamp.VectorTimestamp;
import com.google.gson.*;
import com.google.gson.annotations.Expose;

import java.lang.reflect.Type;

import static utils.Constants.*;
import static utils.Constants.ROOM_MESSAGE;

public abstract class AbstractMessage implements MessageInterface {
    protected String payload;
    protected transient boolean isRoomMessage = false;
    protected int messageType = -1;
    protected int senderID;
    protected String username = null;

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

    protected VectorTimestamp vectorTimestamp;


    public Gson gson() {
        GsonBuilder builder = new GsonBuilder();
        //TODO register polymorphic serializer
        return builder.create();
    }

    ;

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


    public AbstractMessage() {
    }

    public AbstractMessage(int senderID, int messageType, int roomID) {
        this.senderID = senderID;
        this.messageType = messageType;
        this.roomID = roomID;
    }

    public String getMessageDebugString() {
        String messageTypeStr;
        int messageType = this.messageType;
        int sender = this.senderID;
        String payload = this.payload;
        messageTypeStr = switch (messageType) {
            case MESSAGE_TYPE_WELCOME -> "Welcome";
            case MESSAGE_TYPE_ROOM_MESSAGE -> "Room Message";
            case MESSAGE_TYPE_HELLO -> "Hello";
            case MESSAGE_TYPE_CREATE_ROOM -> "Create Room";
            case MESSAGE_TYPE_ROOM_FINALIZED -> "Room Finalized";
            case MESSAGE_TYPE_JOIN_ROOM_ACCEPT -> "Join Room Accept";
            case MESSAGE_TYPE_JOIN_ROOM_REFUSE -> "Join Room Refuse";
            case ROOM_MESSAGE -> "Room Message (Causal Order)";
            default -> "Unknown";
        };

        return String.format("Message Type: %s, Sender: %s, Payload: %s", messageTypeStr, sender, payload);
    }

    static class AbstractMessageDeserializer implements JsonDeserializer<AbstractMessage> {


        //TODO fix polymorphic deserializer
        @Override
        public AbstractMessage deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
           System.out.println(jsonElement.toString());
           return null;
        }
    }
}

package Messages.CommonMulticastMessages;

import Messages.CommonMulticastMessages.AnonymousMessages.*;
import Messages.CommonMulticastMessages.Room.RequestRetransmission;
import Messages.CommonMulticastMessages.Room.RoomMulticastMessage;
import com.google.gson.*;
import com.google.gson.annotations.Expose;

import java.lang.reflect.Type;
import java.net.InetAddress;

import static utils.Constants.*;

public abstract class AbstractMessage implements MessageInterface {
    protected String payload;
    protected int messageType = -1;
    protected int senderID;
    protected String username = null;


    protected  boolean sent = false;

    protected int roomID;

    public boolean shouldRetransmit() {
        return !sent;
    }

    public boolean canDelete() {
        return sent;
    }

    public void setPayload(String rawPayload) {
        this.payload = rawPayload;
    }

    public String getUsername() {
        return username;
    }

    public Gson gson() {
        GsonBuilder builder = new GsonBuilder();
        return builder.create();
    }


    @Override
    public String toJSONString() {
        return gson().toJson(this);
    }


    @Override
    public void setSent(boolean inbound) {
        //once set to true it can't be reverted
        this.sent |= inbound;
    }

    @Override
    public boolean getSent() {
        return sent;
    }

    public boolean isRetransmission() {
        return sent ;
    }


    public int getSenderID() {
        return senderID;
    }

    public int getRoomID() {
        return roomID;
    }

    public int getMessageType() {
        return messageType;
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
            case MESSAGE_TYPE_HELLO -> "Hello";
            case MESSAGE_TYPE_CREATE_ROOM -> "Create Room";
            case MESSAGE_TYPE_ROOM_FINALIZED -> "Room Finalized";
            case MESSAGE_TYPE_JOIN_ROOM_ACCEPT -> "Join Room Accept";
            case MESSAGE_TYPE_JOIN_ROOM_REFUSE -> "Join Room Refuse";
            case MESSAGE_TYPE_ROOM_MESSAGE -> "Room Message (Causal Order)";
            default -> "Unknown";
        };

        return String.format("Message Type: %s, Sender: %s, Payload: %s", messageTypeStr, sender, payload);
    }

    public static class AbstractMessageDeserializer implements JsonDeserializer<AbstractMessage> {


        @Override
        public AbstractMessage deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            String username = null;
            int senderID = jsonElement.getAsJsonObject().get("senderID").getAsInt();
            if (jsonElement.getAsJsonObject().has("username"))
                username = jsonElement.getAsJsonObject().get("username").getAsString();
            int roomID = jsonElement.getAsJsonObject().get("roomID").getAsInt();

            switch (jsonElement.getAsJsonObject().get("messageType").getAsInt()) {
                case MESSAGE_TYPE_CONNECTION_PROBE -> {
                    return jsonDeserializationContext.deserialize(jsonElement, ProbeMessage.class);
                }
                case MESSAGE_TYPE_WELCOME -> {
                    return jsonDeserializationContext.deserialize(jsonElement, WelcomeMessage.class);

                }
                case MESSAGE_TYPE_HELLO -> {
                    return jsonDeserializationContext.deserialize(jsonElement, HelloMessage.class);

                }
                case MESSAGE_TYPE_CREATE_ROOM -> {
                    return jsonDeserializationContext.deserialize(jsonElement, CreateRoomRequest.class);

                }
                case MESSAGE_TYPE_ROOM_FINALIZED -> {
                    return jsonDeserializationContext.deserialize(jsonElement, RoomFinalizedMessage.class);
                }
                case MESSAGE_TYPE_JOIN_ROOM_ACCEPT -> {
                    return jsonDeserializationContext.deserialize(jsonElement, AcceptRoomRequest.class);
                }
                case MESSAGE_TYPE_JOIN_ROOM_REFUSE -> {
                    return jsonDeserializationContext.deserialize(jsonElement, RefuseRoomRequest.class);
                }
                case MESSAGE_TYPE_ROOM_MESSAGE -> {
                    return jsonDeserializationContext.deserialize(jsonElement, RoomMulticastMessage.class);
                }
                case MESSAGE_TYPE_ACK -> {
                    return jsonDeserializationContext.deserialize(jsonElement, AckMessage.class);
                }
                case MESSAGE_TYPE_DELETE_ROOM -> {
                    return jsonDeserializationContext.deserialize(jsonElement, DeleteRoom.class);
                }
                case MESSAGE_TYPE_REQUEST_RTO -> {
                    return jsonDeserializationContext.deserialize(jsonElement, RequestRetransmission.class);
                }
                case MESSAGE_TYPE_HEARTBEAT -> {
                    return jsonDeserializationContext.deserialize(jsonElement, HeartbeatMessage.class);
                }
                default -> throw new RuntimeException("Unknown message type, deserialization aborted");
            }

        }
    }

    public String toChatString() {
        return toJSONString();
    }

    public boolean isUnicast() {
        return false;
    }

    public InetAddress getDestinationAddress() {
        throw new UnsupportedOperationException("Not a unicast message");
    }

}

package Messages;

import Peer.ChatClient;
import com.google.gson.*;
import com.google.gson.annotations.Expose;

import static utils.Constants.*;


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


    public MulticastMessage(
            int userID,
            int type,
            int roomID) {
        super(userID, type, roomID, null);
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
                MESSAGE_TYPE_WELCOME,
                DEFAULT_GROUP_ROOMID
        );


    }


    public String getMessageDebugString() {
        String messageTypeStr;
        int messageType = this.messageType;
        int sender = this.senderID;
        String payload = this.payload;
        switch (messageType) {
            case MESSAGE_TYPE_WELCOME:
                messageTypeStr = "Welcome";
                break;
            case MESSAGE_TYPE_ROOM_MESSAGE:
                messageTypeStr = "Room Message";
                break;
            case MESSAGE_TYPE_HELLO:
                messageTypeStr = "Hello";
                break;
            case MESSAGE_TYPE_CREATE_ROOM:
                messageTypeStr = "Create Room";
                break;
            case MESSAGE_TYPE_ROOM_FINALIZED:
                messageTypeStr = "Room Finalized";
                break;
            case MESSAGE_TYPE_JOIN_ROOM_ACCEPT:
                messageTypeStr = "Join Room Accept";
                break;
            case MESSAGE_TYPE_JOIN_ROOM_REFUSE:
                messageTypeStr = "Join Room Refuse";
                break;
            case ROOM_MESSAGE:
                messageTypeStr = "Room Message (Causal Order)";
                break;
            default:
                messageTypeStr = "Unknown";
                break;
        }

        return String.format("Message Type: %s, Sender: %s, Payload: %s", messageTypeStr, sender, payload);
    }


}



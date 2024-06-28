package Messages.AnonymousMessages;

import Messages.AbstractMessage;
import utils.Constants;

public class AcceptRoomRequest extends AbstractMessage {

    public AcceptRoomRequest(int clientID, int roomID) {
        this.senderID = clientID;
        this.roomID = roomID;
        this.messageType = Constants.MESSAGE_TYPE_JOIN_ROOM_ACCEPT;
    }
}

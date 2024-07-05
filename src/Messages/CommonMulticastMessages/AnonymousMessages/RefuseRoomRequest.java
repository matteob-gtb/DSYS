package Messages.CommonMulticastMessages.AnonymousMessages;

import Messages.CommonMulticastMessages.AbstractMessage;
import utils.Constants;

public class RefuseRoomRequest extends AbstractMessage {

    public RefuseRoomRequest(int clientID, int roomID) {
        this.senderID = clientID;
        this.roomID = roomID;
        this.messageType = Constants.MESSAGE_TYPE_JOIN_ROOM_REFUSE;
    }
}

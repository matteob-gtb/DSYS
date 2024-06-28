package Messages.AnonymousMessages;

import Messages.AbstractMessage;
import com.google.gson.Gson;
import utils.Constants;

public class RoomFinalizedMessage extends AbstractMessage {


    public RoomFinalizedMessage(int clientID, int roomID) {
        this.senderID = clientID;
        this.roomID = roomID;
        this.messageType = Constants.MESSAGE_TYPE_ROOM_FINALIZED;
    }


}

package Messages.AnonymousMessages;

import Messages.AbstractMessage;
import utils.Constants;

public class HelloMessage extends AbstractMessage {

    public HelloMessage(int clientID, int roomID, String username) {
        this.senderID = clientID;
        this.roomID = roomID;
        this.messageType = Constants.MESSAGE_TYPE_HELLO;
        this.username = username;
    }


}

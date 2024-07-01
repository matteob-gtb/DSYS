package Messages.AnonymousMessages;

import Messages.AbstractMessage;
import utils.Constants;

public class WelcomeMessage extends AbstractMessage {

    public WelcomeMessage(int clientID,String username) {
        this.roomID = Constants.DEFAULT_GROUP_ROOMID;
        this.senderID = clientID;
        this.username = username;
        this.messageType = Constants.MESSAGE_TYPE_WELCOME;
    }




}

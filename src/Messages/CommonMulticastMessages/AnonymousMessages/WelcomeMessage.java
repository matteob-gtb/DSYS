package Messages.CommonMulticastMessages.AnonymousMessages;

import Messages.CommonMulticastMessages.AbstractMessage;
import utils.Constants;

public class WelcomeMessage extends AbstractMessage {

    public WelcomeMessage(int clientID, String username) {
        this.senderID = clientID;
        this.roomID = Constants.DEFAULT_GROUP_ROOMID;
        this.username = username;
        this.messageType = Constants.MESSAGE_TYPE_WELCOME;
    }


}

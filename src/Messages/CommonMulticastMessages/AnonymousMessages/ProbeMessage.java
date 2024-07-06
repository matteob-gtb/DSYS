package Messages.CommonMulticastMessages.AnonymousMessages;

import Messages.CommonMulticastMessages.AbstractMessage;
import utils.Constants;

public class ProbeMessage extends AbstractMessage {

    public ProbeMessage(int clientID) {
        this.senderID = clientID;
        this.messageType = Constants.MESSAGE_TYPE_CONNECTION_PROBE;
    }

}

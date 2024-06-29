package Messages.AnonymousMessages;

import Messages.AbstractMessage;
import com.google.gson.Gson;
import utils.Constants;

public class WelcomeMessage extends AbstractMessage {

    public WelcomeMessage(int clientID,String username) {
        this.senderID = clientID;
        this.username = username;
        this.messageType = Constants.MESSAGE_TYPE_WELCOME;
    }




}

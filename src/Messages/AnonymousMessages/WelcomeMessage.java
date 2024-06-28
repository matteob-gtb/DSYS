package Messages.AnonymousMessages;

import Messages.AbstractMessage;
import com.google.gson.Gson;

public class WelcomeMessage extends AbstractMessage {

    public WelcomeMessage(int clientID,String username) {
        this.senderID = clientID;
        this.username = username;
    }



    @Override
    public Gson gson() {
        return null;
    }
}

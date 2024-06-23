package Messages;

import Peer.AbstractClient;
import Peer.ChatClient;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.MulticastSocket;
import java.util.Optional;

public class MessageMiddleware extends Middleware {


    public MessageMiddleware(AbstractClient client) throws IOException {
        super(client);
    }


    /**
     * @param msgObject
     */


    /**
     * @param chatRoomID
     * @return
     */
    @Override
    public boolean pollMessage(Optional<Integer> chatRoomID) {
        return false;
    }

    /**
     * @param chatRoomID
     * @return
     */
    @Override
    public JsonObject getMessage(Optional<Integer> chatRoomID) {
        return null;
    }
}

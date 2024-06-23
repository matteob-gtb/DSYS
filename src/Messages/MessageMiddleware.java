package Messages;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.MulticastSocket;
import java.util.Optional;

public class MessageMiddleware extends Middleware {


    public MessageMiddleware(int CLIENT_ID) throws IOException {
        super(CLIENT_ID);
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

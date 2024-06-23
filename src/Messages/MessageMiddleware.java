package Messages;

import com.google.gson.JsonObject;

import java.util.Optional;

public class MessageMiddleware extends MiddlewareInterface {
    /**
     * @param msgObject
     */
    @Override
    public void sendMessage(JsonObject msgObject) {

    }

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

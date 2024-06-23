package Messages;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public abstract class MiddlewareInterface {

    protected HashMap<Integer, ChatRoom> roomsMap;

    public abstract void sendMessage(JsonObject msgObject);

    public abstract boolean pollMessage(Optional<Integer> chatRoomID);

    public abstract JsonObject getMessage(Optional<Integer> chatRoomID);


}

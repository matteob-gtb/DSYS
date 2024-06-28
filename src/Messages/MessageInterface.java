package Messages;

import ChatRoom.ChatRoom;
import com.google.gson.JsonObject;

public interface MessageInterface {
    String toJSONString();

    boolean isSent();

    void setPayload(String payload);

    void setSent(boolean sent);
}

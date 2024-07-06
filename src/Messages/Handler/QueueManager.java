package Messages.Handler;

import ChatRoom.ChatRoom;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface QueueManager extends Runnable {
    void registerRoom(ChatRoom room);

    public void deleteRoom(ChatRoom room);

    public List<ChatRoom> getRooms();

    String getOnlineClients();

    Optional<ChatRoom> getChatRoom(int roomID);
}

package Messages;

import ChatRoom.ChatRoom;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface QueueManager extends Runnable {
    void registerRoom(ChatRoom room);

    /*

      Sanity check
       */
    public void deleteRoom(ChatRoom room);


    public List<ChatRoom> getRooms();

    Set<Integer> getOnlineClients();

    Optional<ChatRoom> getChatRoom(int roomID);
}

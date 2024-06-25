package Messages;

import ChatRoom.ChatRoom;

import java.util.Optional;
import java.util.Set;

public interface QueueManager extends Runnable {
    void registerRoom(ChatRoom room);
      /*

        Sanity check
         */


    Set<Integer> getOnlineClients();

    Optional<ChatRoom> getChatRoom(int roomID);
}

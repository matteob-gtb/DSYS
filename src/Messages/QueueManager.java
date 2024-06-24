package Messages;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Optional;
import java.util.Set;

import static utils.Constants.*;

public interface QueueManager extends Runnable {
    void addRoom(ChatRoom room);
    void sendMessage(Message m,ChatRoom room);

    /*

        Sanity check
         */


    Set<Integer> getOnlineClients();

    Optional<ChatRoom> getChatRoom(int roomID);
}

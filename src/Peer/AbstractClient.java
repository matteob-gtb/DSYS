package Peer;

import ChatRoom.ChatRoom;
import Events.AbstractEvent;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.*;

public abstract class AbstractClient {
    protected String userName;

    protected int CLIENT_ID;
    public List<AbstractEvent> eventsToProcess = Collections.synchronizedList(new ArrayList<AbstractEvent>());
    protected static Map<Integer, String> idUsernameMappings = new HashMap<>();

    public int getID() {
        return CLIENT_ID;
    }

    public void addEvent(AbstractEvent event) {
        eventsToProcess.add(event);
    }


    public String getUserName(){
        return this.userName;
    }

    public abstract ChatRoom getDefaultRoom();
    public abstract void announceSelf() throws IOException;

    public abstract void print(String queueThreadBootstrapped);


    public void addUsernameMapping(int userID, String username) {
        if (idUsernameMappings.containsKey(userID) && !idUsernameMappings.get(userID).equals(username))
            throw new RuntimeException("Bad luck, duplicate user id");
        idUsernameMappings.put(userID, username);
    }




}

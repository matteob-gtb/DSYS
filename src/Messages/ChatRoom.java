package Messages;

import com.google.gson.JsonObject;
import com.sun.source.tree.Tree;

import java.net.*;
import java.util.*;

import static java.lang.System.exit;
import static utils.Constants.GROUP_PORT;

public class ChatRoom {
    private final int chatID;
    private ArrayList<JsonObject> messageList;
    private Set<Integer> participantIDs = new TreeSet<Integer>();
    private int[] ownVectorTimestamp;
    private MyMulticastSocketWrapper dedicatedRoomSocket = null;
    private boolean connected = false;




    public int getChatID() {
        return chatID;
    }

    public ChatRoom(int chatID,String groupName) throws Exception {
        this.chatID = chatID;
        this.dedicatedRoomSocket = new MyMulticastSocketWrapper(groupName);
    }

    public void sendMessage(String message) throws Exception {
         dedicatedRoomSocket.sendPacket(message);
    }

    public void probeSocket(){

    }

    public int[] getParticipants() {
        return participantIDs.stream().mapToInt(i -> i).toArray();
    }

    public boolean addParticipant(Integer participantID) {
        return participantIDs.add(participantID);
    }

    public int getMessageCount() {
        return messageList.size();
    }

}

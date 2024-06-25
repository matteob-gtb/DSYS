package Messages;

import java.net.*;
import java.util.*;

public class ChatRoom {
    private final int chatID;
    private ArrayList<MulticastMessage> messageList;
    private Set<Integer> participantIDs = new TreeSet<Integer>();
    private int[] ownVectorTimestamp;
    private MyMulticastSocketWrapper dedicatedRoomSocket = null;
    private boolean connected = false;
    private ArrayList<MulticastMessage> outGoingMessageQueue = new ArrayList<>();

    public void addOutgoingMessage(MulticastMessage message) {
        outGoingMessageQueue.add(message);
    }


    public InetAddress getRoomAddress(){
        return dedicatedRoomSocket.getMCastAddress();
    }

    public MyMulticastSocketWrapper getDedicatedRoomSocket() {
        return dedicatedRoomSocket;
    }

    public int getChatID() {
        return chatID;
    }

    public ChatRoom(int chatID,String groupName) throws Exception {
        this.chatID = chatID;
        this.dedicatedRoomSocket = new MyMulticastSocketWrapper(groupName);
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

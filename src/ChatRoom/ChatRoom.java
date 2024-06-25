package ChatRoom;

import Messages.MulticastMessage;
import Messages.MyMulticastSocketWrapper;

import java.net.*;
import java.util.*;

public class ChatRoom {
    private final int chatID;
    //it is mean as a VERY rough estimate in order to wait forever for a response,
    //by no means accurate
    private final Long creationTimestamp = System.currentTimeMillis();

    private final static int MAX_ROOM_CREATION_WAIT_MILLI = 60 * 1000;

    private ArrayList<MulticastMessage> messageList;
    private Set<Integer> participantIDs = new TreeSet<Integer>();


    public boolean checkRoomStatus() {
        if (System.currentTimeMillis() > creationTimestamp + MAX_ROOM_CREATION_WAIT_MILLI) roomFinalized = true;
        if(roomFinalized) System.out.println("Room finalized");
        return roomFinalized;
    }

    public boolean isRoomFinalized() {
        return roomFinalized;
    }

    private boolean roomFinalized = false; //finalized 60 seconds after the initial room creation request was acked
    private int[] ownVectorTimestamp;
    private MyMulticastSocketWrapper dedicatedRoomSocket = null;
    private boolean connected = false;
    private ArrayList<MulticastMessage> outGoingMessageQueue = new ArrayList<>();


    public void addOutgoingMessage(MulticastMessage message) {
        outGoingMessageQueue.add(message);
    }

    public void setRoomFinalized(boolean roomFinalized) {
        this.roomFinalized = roomFinalized;
    }


    public InetAddress getRoomAddress() {
        return dedicatedRoomSocket.getMCastAddress();
    }

    public MyMulticastSocketWrapper getDedicatedRoomSocket() {
        return dedicatedRoomSocket;
    }

    public int getChatID() {
        return chatID;
    }

    public ChatRoom(int chatID, String groupName) throws Exception {
        this.chatID = chatID;
        this.dedicatedRoomSocket = new MyMulticastSocketWrapper(groupName);
    }


    public void probeSocket() {

    }

    public int[] getParticipants() {
        return participantIDs.stream().mapToInt(i -> i).toArray();
    }


    public boolean addParticipant(Integer participantID) {
        if (roomFinalized || System.currentTimeMillis() > creationTimestamp + MAX_ROOM_CREATION_WAIT_MILLI) //rooms are immutable
            return false;
        return participantIDs.add(participantID);
    }

    public int getMessageCount() {
        return messageList.size();
    }

}

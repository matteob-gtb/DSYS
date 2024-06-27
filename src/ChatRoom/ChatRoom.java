package ChatRoom;

import Messages.MessageInterface;
import Messages.MulticastMessage;
import Messages.RoomMulticastMessage;
import Networking.MyMulticastSocketWrapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.*;
import java.util.*;

import static utils.Constants.*;

public class ChatRoom {
    private final int chatID;
    private boolean onlineStatus;
    private Long lastReconnectAttempt = -1L;

    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    //it is mean as a VERY rough estimate in order to wait forever for a response,
    //by no means accurate
    private final Long creationTimestamp = System.currentTimeMillis();
    private final String groupName;
    private final static int MAX_ROOM_CREATION_WAIT_MILLI = 5 * 1000;
    private final static int MIN_SOCKET_RECONNECT_DELAY = 5 * 1000;

    private ArrayList<MulticastMessage> messageList;
    private Set<Integer> participantIDs = new TreeSet<Integer>();
    private HashMap<Integer, ArrayList<RoomMulticastMessage>> perParticipantMessageQueue = new HashMap<>();
    private HashMap<Integer, Integer> currentClientVectorTimestamp;


    public void printMessages() {
        throw new UnsupportedOperationException();
    }

    public void forceFinalizeRoom(Set<Integer> participantIDs) {
        System.out.println("Room " + this.chatID + " has been finalized");
        this.participantIDs = participantIDs;
        this.roomFinalized = true;
    }

    public boolean finalizeRoom() {
        if (!roomFinalized && System.currentTimeMillis() > creationTimestamp + MAX_ROOM_CREATION_WAIT_MILLI) {
            roomFinalized = true;
            currentClientVectorTimestamp = new HashMap<>(participantIDs.size());
            participantIDs.forEach(id -> {
                currentClientVectorTimestamp.put(id, 0);
                perParticipantMessageQueue.put(id, new ArrayList<RoomMulticastMessage>());
            });
            System.out.println("Room finalized,participants: ");
            System.out.println(participantIDs);
            if (participantIDs.size() == 0) {
                System.out.println("No participants, deleting room...");
            }
            return true;
        }
        return false;
    }

    public Set<Integer> getParticipantIDs() {
        if (!roomFinalized) {
            throw new RuntimeException("Room not finalized yet");
        }
        return participantIDs;
    }

    public boolean isOnline() {
        return this.onlineStatus;
    }

    public String getStatusString() {
        return this.onlineStatus ? "online" : "offline";
    }


    public void getBackOnline() {
        if (this.lastReconnectAttempt - System.currentTimeMillis() < MIN_SOCKET_RECONNECT_DELAY) {
            return;
        }
        this.lastReconnectAttempt = System.currentTimeMillis();
        try {
            dedicatedRoomSocket.close();
            dedicatedRoomSocket = new MyMulticastSocketWrapper(dedicatedRoomSocket.getMCastAddress().toString());
        } catch (Exception e) {
            System.out.println("Reconnect attempt failed,trying later...");
        }
    }

    public boolean isRoomFinalized() {
        return roomFinalized;
    }

    private boolean roomFinalized = false; //finalized 60 seconds after the initial room creation request was acked
    private int[] ownVectorTimestamp;
    private MyMulticastSocketWrapper dedicatedRoomSocket = null;
    private boolean connected = false;
    private List<MessageInterface> outGoingMessageQueue = Collections.synchronizedList(new ArrayList<>());


    public void updateOutQueue() {
        MessageInterface out;
        if (outGoingMessageQueue.getFirst().isSent()) {
            out = outGoingMessageQueue.removeFirst();
            if (out instanceof RoomMulticastMessage)
                this.ownVectorTimestamp = ((RoomMulticastMessage) out).getVectorTimestamp();
        } else {
            //could try a few more times honestly
            this.connected = false;
        }
    }

    public Optional<MessageInterface> getOutgoingMessage() {
        if (!outGoingMessageQueue.isEmpty() && !outGoingMessageQueue.getFirst().isSent()) {
            return Optional.of(outGoingMessageQueue.getFirst());
        }
        return Optional.empty();
    }

    public void announceRoomFinalized(int clientID,ChatRoom defaultChannel) {
        MessageInterface msg = new MulticastMessage(
                clientID,
                MESSAGE_TYPE_ROOM_FINALIZED,
                defaultChannel.getChatID()
        );
        JsonObject payload = new JsonObject();
        JsonArray participants = new JsonArray();
        this.getParticipantIDs().forEach(participants::add);
        payload.add(FIELD_ROOM_PARTICIPANTS, participants);
        payload.addProperty(ROOM_MULTICAST_GROUP_ADDRESS, this.getDedicatedRoomSocket().getMCastAddress().toString());
        msg.setPayload(payload.toString());
        defaultChannel.addOutgoingMessage(msg);
    }

    public void addOutgoingMessage(MessageInterface message) {
        outGoingMessageQueue.add(message);
    }

    public void sendInRoomMessage(MessageInterface message) {
        //add to the queue first
        //TODO VECTOR CLOCK HERE
        message.setVectorTimestamp(this.ownVectorTimestamp);
        outGoingMessageQueue.add(message);
    }

    public void setRoomFinalized(boolean roomFinalized) {
        this.roomFinalized = roomFinalized;
    }


    public String getRoomAddress() {
        return groupName;
    }

    public MyMulticastSocketWrapper getDedicatedRoomSocket() {
        return dedicatedRoomSocket;
    }

    public int getChatID() {
        return chatID;
    }

    public ChatRoom(int chatID, String groupName) {
        this.chatID = chatID;
        this.dedicatedRoomSocket = new MyMulticastSocketWrapper(groupName);
        this.onlineStatus = dedicatedRoomSocket.isConnected();
        this.groupName = groupName;
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

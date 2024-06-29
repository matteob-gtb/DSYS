package ChatRoom;

import Messages.AbstractMessage;
import Messages.MessageInterface;
import Messages.MulticastMessage;
import Messages.AnonymousMessages.RoomFinalizedMessage;
import Messages.Room.RoomMulticastMessage;
import Networking.MyMulticastSocketWrapper;
import VectorTimestamp.VectorTimestamp;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;

import static utils.Constants.*;

public class ChatRoom {
    private final int chatID;

    private VectorTimestamp lastMessageTimestamp;

    public int getOwnerID() {
        return ownerID;
    }

    private final int ownerID;
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


    public void addIncomingMessage(RoomMulticastMessage inbound) {
        if (!perParticipantMessageQueue.containsKey(inbound.getSenderID()))
            throw new RuntimeException("Bad room finalization");
        perParticipantMessageQueue.get(inbound.getSenderID()).add(inbound);
    }


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
            lastMessageTimestamp = new VectorTimestamp(new int[participantIDs.size()]);
            currentClientVectorTimestamp = new HashMap<>(participantIDs.size());
            participantIDs.forEach(id -> {
                currentClientVectorTimestamp.put(id, 0);
                perParticipantMessageQueue.put(id, new ArrayList<RoomMulticastMessage>());
            });
            System.out.println("Room finalized,participants: ");
            System.out.println(participantIDs);
            if (participantIDs.isEmpty()) {
                System.out.println("The room is empty...");
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
    private VectorTimestamp ownVectorTimestamp;
    private MyMulticastSocketWrapper dedicatedRoomSocket = null;
    private boolean connected = false;
    private List<AbstractMessage> outGoingMessageQueue = Collections.synchronizedList(new ArrayList<>());


    public void updateOutQueue() {
        MessageInterface out;
        if (outGoingMessageQueue.getFirst().isSent()) {
            out = outGoingMessageQueue.removeFirst();
            if (out instanceof RoomMulticastMessage)
                this.ownVectorTimestamp = ((RoomMulticastMessage) out).getTimestamp();
        } else {
            //could try a few more times honestly
            this.connected = false;
        }
    }

    public Optional<AbstractMessage> getOutgoingMessage() {
        if (!outGoingMessageQueue.isEmpty() && !outGoingMessageQueue.getFirst().isSent()) {
            return Optional.of(outGoingMessageQueue.getFirst());
        }
        return Optional.empty();
    }

    public void announceRoomFinalized(int clientID, ChatRoom defaultChannel) {
        AbstractMessage msg = new RoomFinalizedMessage(
                clientID,
                this.chatID,
                this.getParticipantIDs(),
                this.getDedicatedRoomSocket().getMCastAddress().toString()
        );
//        JsonObject payload = new JsonObject();
//        JsonArray participants = new JsonArray();
//        this.getParticipantIDs().forEach(participants::add);
//        payload.add(FIELD_ROOM_PARTICIPANTS, participants);
//        payload.addProperty(ROOM_MULTICAST_GROUP_ADDRESS, this.getDedicatedRoomSocket().getMCastAddress().toString());
//        msg.setPayload(payload.toString());
        defaultChannel.addOutgoingMessage(msg);
    }

    public void sendInRoomMessage(String payload, int clientID) {
        //TODO search for client id
        VectorTimestamp messageTimestamp = lastMessageTimestamp.increment(clientID);
        RoomMulticastMessage out = new RoomMulticastMessage(
                clientID,
                this.getChatID(),
                messageTimestamp
        );
        outGoingMessageQueue.add(out);
    }

    public void addOutgoingMessage(AbstractMessage message) {

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

    public ChatRoom(int owner, int chatID, String groupName) {
        this.ownerID = owner;
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

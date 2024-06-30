package ChatRoom;

import Messages.AbstractMessage;
import Messages.MessageInterface;
import Messages.AnonymousMessages.RoomFinalizedMessage;
import Messages.Room.RoomMulticastMessage;
import Networking.MyMulticastSocketWrapper;
import VectorTimestamp.VectorTimestamp;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ChatRoom {
    private final int chatID;

    private VectorTimestamp lastMessageTimestamp;

    public int getOwnerID() {
        return ownerID;
    }

    private final int ownerID;
    private boolean onlineStatus;
    private Long lastReconnectAttempt = 0L;

    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    //it is mean as a VERY rough estimate in order to wait forever for a response,
    //by no means accurate
    private final Long creationTimestamp = System.currentTimeMillis();
    private final String groupName;
    private final static int MAX_ROOM_CREATION_WAIT_MILLI = 5 * 1000;
    private final static int MIN_SOCKET_RECONNECT_DELAY = 1 * 1000;

    private List<AbstractMessage> observedMessageOrder = new LinkedList<>();
    private Set<Integer> participantIDs = new TreeSet<Integer>();
    private HashMap<Integer, ArrayList<RoomMulticastMessage>> perParticipantMessageQueue = new HashMap<>();

    //associate client id to positions in the vector (lookup is probably more efficient)
    private HashMap<Integer, Integer> clientVectorIndex;

    public synchronized void addIncomingMessage(RoomMulticastMessage inbound) {
        if (!perParticipantMessageQueue.containsKey(inbound.getSenderID()))
            throw new RuntimeException("Bad room finalization");
        var clientMessageList = perParticipantMessageQueue.get(inbound.getSenderID());
        clientMessageList.add(inbound);

        //Sorts only by the vector timestamp of the CLIENT in the CLIENT's queue, ensuring essentially FIFO ordering of the message
        //causality is not enforced here
        clientMessageList.sort(new RoomMulticastMessage.RoomMulticastMessageComparator());
        //check, for all queues if any message can now be received

        Collection<ArrayList<RoomMulticastMessage>> queues = perParticipantMessageQueue.values();


        for (ArrayList<RoomMulticastMessage> queue : queues) {
            System.out.println("Queue " + queue.size());
            while (!queue.isEmpty() && this.lastMessageTimestamp.comesBefore(queue.getFirst().getTimestamp())) {
                System.out.println("Comparing " + queue.getFirst().getTimestamp());
                System.out.println("Comparing " + this.lastMessageTimestamp);
                System.out.println("Result " + this.lastMessageTimestamp.comesBefore(queue.getFirst().getTimestamp()));
                observedMessageOrder.add(inbound);
                queue.removeFirst();
            }

        }

    }


    public void printMessages() {
        System.out.println("Chat room messages as observed by client #" + chatID);
        observedMessageOrder.forEach(msg -> System.out.println("     " + msg.toChatString()));
    }

    public void forceFinalizeRoom(Set<Integer> participantIDs) {
        System.out.println("Room " + this.chatID + " has been finalized");
        System.out.println("Participants " + participantIDs);
        this.participantIDs = participantIDs;
        this.roomFinalized = true;
        this.clientVectorIndex = new HashMap<>(participantIDs.size());
        lastMessageTimestamp = new VectorTimestamp(new int[participantIDs.size()]);

        final AtomicReference<Integer> k = new AtomicReference<>(0);
        this.participantIDs.stream().sorted().forEach(participantID -> {
            perParticipantMessageQueue.put(participantID, new ArrayList<>());
            clientVectorIndex.put(participantID, k.getAndAccumulate(1, Integer::sum));
            System.out.println("Client " + participantID + " has been finalized : index " + k.get());
        });
    }

    public boolean finalizeRoom() {
        if (!roomFinalized && System.currentTimeMillis() > creationTimestamp + MAX_ROOM_CREATION_WAIT_MILLI) {
            roomFinalized = true;
            lastMessageTimestamp = new VectorTimestamp(new int[participantIDs.size()]);
            clientVectorIndex = new HashMap<>(participantIDs.size());

            final AtomicReference<Integer> k = new AtomicReference<>();
            k.set(0);
            participantIDs.forEach(id -> {
                perParticipantMessageQueue.put(id, new ArrayList<RoomMulticastMessage>());
                clientVectorIndex.put(id, k.getAndAccumulate(1, Integer::sum));
                System.out.println("Client " + id + " has been finalized : index " + k.get());
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
        System.out.println("Last " + lastReconnectAttempt + " Curr " + System.currentTimeMillis());
        if (System.currentTimeMillis() - this.lastReconnectAttempt < MIN_SOCKET_RECONNECT_DELAY) {
            System.out.println("Not enough time yet");
            return;
        }
        this.lastReconnectAttempt = System.currentTimeMillis();
        try {
           /* dedicatedRoomSocket.close();
            dedicatedRoomSocket = new MyMulticastSocketWrapper(this.groupName);*/
            dedicatedRoomSocket.probeConnection();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Reconnect attempt failed,trying later...");
        }
        //no exception throw -> connection re-established
        this.onlineStatus = true;

    }

    public boolean isRoomFinalized() {
        return roomFinalized;
    }

    private boolean roomFinalized = false; //finalized 60 seconds after the initial room creation request was acked
    private VectorTimestamp ownVectorTimestamp;
    private MyMulticastSocketWrapper dedicatedRoomSocket = null;
    private List<AbstractMessage> outGoingMessageQueue = Collections.synchronizedList(new ArrayList<>());


    public void updateOutQueue() {
        MessageInterface out;
        if (outGoingMessageQueue.getFirst().isSent()) {
            out = outGoingMessageQueue.removeFirst();
            if (out instanceof RoomMulticastMessage)
                this.ownVectorTimestamp = ((RoomMulticastMessage) out).getTimestamp();
        }
    }

    public Optional<AbstractMessage> getOutgoingMessage() {
        return outGoingMessageQueue.stream().filter(message -> !message.isSent()).findFirst();
    }

    public void announceRoomFinalized(int clientID, ChatRoom defaultChannel) {
        AbstractMessage msg = new RoomFinalizedMessage(
                clientID,
                this.chatID,
                this.getParticipantIDs(),
                this.getDedicatedRoomSocket().getMCastAddress().toString()
        );

        defaultChannel.addOutgoingMessage(msg);
    }

    public void sendInRoomMessage(String payload, int clientID) {
        //Client id mapping --> sort
        //E.G. clients 1231,456246,215 will have index 215 -> 0,1231 ->1,456246->3 in the vector timestamp array

        int clientIndex = clientVectorIndex.get(clientID);
        VectorTimestamp messageTimestamp = lastMessageTimestamp.increment(clientIndex);
        RoomMulticastMessage out = new RoomMulticastMessage(
                clientID,
                this.getChatID(),
                messageTimestamp,
                payload
        );
        observedMessageOrder.add(out);
        outGoingMessageQueue.add(out);
    }

    public void addOutgoingMessage(AbstractMessage message) {
        outGoingMessageQueue.add(message);
    }

    public void setRoomFinalized(boolean roomFinalized) {
        this.roomFinalized = roomFinalized;
    }

    public void setOffline(boolean isOffline) {
        System.out.println("Room " + this.chatID + " is offline, attempting reconnect in a bit");
        this.onlineStatus = !isOffline;
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
        return observedMessageOrder.size();
    }

}

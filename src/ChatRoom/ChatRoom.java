package ChatRoom;

import Messages.AbstractMessage;
import Messages.Room.DummyMessage;
import Messages.MessageInterface;
import Messages.AnonymousMessages.RoomFinalizedMessage;
import Messages.Room.AbstractOrderedMessage;
import Messages.Room.RoomMulticastMessage;
import Networking.MyMulticastSocketWrapper;
import VectorTimestamp.VectorTimestamp;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static utils.Constants.MAX_ROOM_CREATION_WAIT_MILLI;
import static utils.Constants.MIN_SOCKET_RECONNECT_DELAY;

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

    private List<AbstractOrderedMessage> observedMessageOrder = new LinkedList<>();
    private Set<Integer> participantIDs = new TreeSet<Integer>();
    private HashMap<Integer, LinkedList<RoomMulticastMessage>> perParticipantMessageQueue = new HashMap<>();

    //associate client id to positions in the vector (lookup is probably more efficient)
    private HashMap<Integer, Integer> clientVectorIndex;

    public synchronized void addIncomingMessage(RoomMulticastMessage inbound) {
        if (!perParticipantMessageQueue.containsKey(inbound.getSenderID()))
            throw new RuntimeException("Unknown participant ID, something went wrong");

        var clientMessageList = perParticipantMessageQueue.get(inbound.getSenderID());
        //TODO check if not inserted

        //check if it can be delivered right now
        boolean inserted = false;

        //Insertion if detected as stale or old i.e. ts(m) < this.currentTimestamp
        // if (inbound.getTimestamp().lessThan(this.lastMessageTimestamp)) {

        //Case 1) I can directly deliver it in causal order

        //Case 1) Message can just be delivered


        System.out.println("Trying to insert " + inbound.getTimestamp().toString());

        if (this.lastMessageTimestamp.comesBefore(inbound.getTimestamp())) {
            inserted = true;
            observedMessageOrder.add(inbound);
            this.lastMessageTimestamp = VectorTimestamp.merge(this.lastMessageTimestamp, inbound.getTimestamp());

            //check the queues if something can now be deliverd
            for (LinkedList<RoomMulticastMessage> queue : perParticipantMessageQueue.values()) {
                while (!queue.isEmpty() && this.lastMessageTimestamp.comesBefore(queue.getFirst().getTimestamp())) {
                    AbstractOrderedMessage message = queue.removeFirst();
                    observedMessageOrder.add(message);
                    this.lastMessageTimestamp = VectorTimestamp.merge(this.lastMessageTimestamp, message.getTimestamp());
                }
            }

        } else if (inbound.getTimestamp().isConcurrent(this.lastMessageTimestamp)) {
            System.out.println("Case 2");
            //Case 2) it's an old message so i have to insert it in the right place or
            //it's just concurrent so i can place it wherever i want
            ListIterator<AbstractOrderedMessage> listIterator = observedMessageOrder.listIterator();
            AbstractOrderedMessage current = null;
            while (listIterator.hasNext()) {
                current = listIterator.next();
                if (current.getTimestamp().comesBefore(inbound.getTimestamp())) {
                    listIterator.add(inbound);
                    inserted = true;
                    AbstractOrderedMessage last = current;
                    //Update all the following messages to reconcile the state
                    for (; listIterator.hasNext(); current = listIterator.next()) {
                        current.setTimestamp(VectorTimestamp.merge(last.getTimestamp(), current.getTimestamp()));
                    }

                }
            }
            this.lastMessageTimestamp = VectorTimestamp.merge(this.lastMessageTimestamp, observedMessageOrder.get(observedMessageOrder.size() - 1).getTimestamp());

        }
        if (!inserted) {
            //case 3) it's a new message that i can't deliver yet
            clientMessageList.add(inbound);
            clientMessageList.sort(new RoomMulticastMessage.RoomMulticastMessageComparator(clientVectorIndex.get(inbound.getSenderID())));
        }

        if (inserted) System.out.println("Message delivered to the client");
        else System.out.println("Message not delivered to the client, queued until the missing message is received");


    }


    public void printMessages() {
        System.out.println("Chat room #" + this.chatID);
        observedMessageOrder.
                stream().
                filter(msg -> !(msg instanceof DummyMessage)).
                forEach(msg -> System.out.println("     " + msg.toChatString()));
        System.out.println("Current Timestamp " + this.lastMessageTimestamp);
    }

    public void forceFinalizeRoom(Set<Integer> participantIDs) {
        System.out.println("Room " + this.chatID + " has been finalized");
        this.participantIDs = participantIDs;
        this.roomFinalized = true;
        this.clientVectorIndex = new HashMap<>(participantIDs.size());
        lastMessageTimestamp = new VectorTimestamp(new int[participantIDs.size()]);

        final AtomicReference<Integer> k = new AtomicReference<>(0);
        this.participantIDs.stream().sorted().forEach(participantID -> {
            perParticipantMessageQueue.put(participantID, new LinkedList<>());
            clientVectorIndex.put(participantID, k.getAndAccumulate(1, Integer::sum));
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
                perParticipantMessageQueue.put(id, new LinkedList<>());
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
        if (System.currentTimeMillis() - this.lastReconnectAttempt < MIN_SOCKET_RECONNECT_DELAY) {
            return;
        }
        this.lastReconnectAttempt = System.currentTimeMillis();
        boolean exceptionThrown = false;
        try {
           /* dedicatedRoomSocket.close();
            dedicatedRoomSocket = new MyMulticastSocketWrapper(this.groupName);*/
            dedicatedRoomSocket.probeConnection();
        } catch (Exception e) {
            exceptionThrown = true;
            //System.out.print("Reconnect attempt failed,trying later...  ");
        }
        //no exception thrown -> connection re-established
        //System.out.println("Reconnect attempt completed");
        this.onlineStatus = !exceptionThrown;

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
        if (outGoingMessageQueue.get(0).isSent()) {
            out = outGoingMessageQueue.remove(0);
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
        this.lastMessageTimestamp = lastMessageTimestamp.increment(clientIndex);
        RoomMulticastMessage out = new RoomMulticastMessage(
                clientID,
                this.getChatID(),
                this.lastMessageTimestamp,
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

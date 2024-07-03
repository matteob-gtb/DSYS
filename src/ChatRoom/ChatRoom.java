package ChatRoom;

import Messages.AbstractMessage;
import Messages.AnonymousMessages.AckMessage;
import Messages.Logger;
import Messages.Room.DummyMessage;
import Messages.MessageInterface;
import Messages.AnonymousMessages.RoomFinalizedMessage;
import Messages.Room.AbstractOrderedMessage;
import Messages.Room.RoomMulticastMessage;
import Networking.MyMulticastSocketWrapper;
import VectorTimestamp.VectorTimestamp;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.toList;
import static utils.Constants.*;

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

    private Set<AbstractOrderedMessage> observedMessageOrder = new LinkedHashSet<>();
    private Set<Integer> participantIDs = new TreeSet<Integer>();
    private HashMap<Integer, LinkedList<RoomMulticastMessage>> perParticipantMessageQueue = new HashMap<>();

    private Set<RoomMulticastMessage> incomingMessageQueue = new LinkedHashSet<>();

    //associate client id to positions in the vector (lookup is probably more efficient)
    private HashMap<Integer, Integer> clientVectorIndex;


    public static String writeCurrentTime() {
        LocalTime currentTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return (currentTime.format(formatter));
    }

    public synchronized void addIncomingMessage(RoomMulticastMessage inbound) {

        Logger.writeLog(writeCurrentTime() +
                " BEFORE message queue status " + Arrays.toString(incomingMessageQueue.stream().map(AbstractMessage::toJSONString).toArray(String[]::new))
        );

        Logger.writeLog(
                writeCurrentTime() +
                        " BEFORE observed message order status " + Arrays.toString(observedMessageOrder.stream().map(AbstractMessage::toJSONString).toArray(String[]::new))
        );

        Logger.writeLog(
                writeCurrentTime() +
                        " BEFORE received message " + inbound.toJSONString()
        );

        boolean inserted = false;
        //check, for every queue that a message can be delivered
        if (incomingMessageQueue.contains(inbound) || observedMessageOrder.contains(inbound)) {
            Logger.writeLog("Not delivered");
            return;
        }
        incomingMessageQueue.add(inbound);
        Iterator<RoomMulticastMessage> iterator = incomingMessageQueue.iterator();
        while (iterator.hasNext()) {
            RoomMulticastMessage message = iterator.next();
            if (this.lastMessageTimestamp.canDeliver(message.getTimestamp())) {
                inserted = true;
                observedMessageOrder.add(message);
                iterator.remove();
                lastMessageTimestamp = VectorTimestamp.merge(lastMessageTimestamp, message.getTimestamp());
            }
        }

        if (inserted) System.out.println("Message delivered to the client");
        else System.out.println("Message not delivered to the client, queued until the missing message is received");


        Logger.writeLog(writeCurrentTime() +
                " AFTER message queue status " + Arrays.toString(incomingMessageQueue.stream().map(AbstractMessage::toJSONString).toArray(String[]::new))
        );

        Logger.writeLog(
                writeCurrentTime() +
                        " AFTER observed message order status " + Arrays.toString(observedMessageOrder.stream().map(AbstractMessage::toJSONString).toArray(String[]::new))
        );


    }

    //it's just reading it can be not synchronized, temporary discrepancies are ok
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


    public synchronized void ackMessage(AckMessage messageToAck) {
        if (this.chatID == DEFAULT_GROUP_ROOMID) return;


        Logger.writeLog(writeCurrentTime() +
                " ACK " + messageToAck.toJSONString());

        Logger.writeLog(writeCurrentTime() +
                " RECEIVED ACK outgoing queue " + Arrays.toString(outGoingMessageQueue.stream().map(AbstractMessage::toJSONString).toArray(String[]::new))
        );


        var toAckSameTimestamp = outGoingMessageQueue.stream().filter(
                m -> m.getSenderID() == messageToAck.getRecipientID() && ((AbstractOrderedMessage) m).getTimestamp().equals(messageToAck.getTimestamp())
        ).toList();

        AbstractOrderedMessage msg = null;
        if (toAckSameTimestamp.size() != 1) {
            Logger.writeLog(writeCurrentTime() +
                    " ERROR,same ts(m) " + Arrays.toString(toAckSameTimestamp.stream().map(AbstractMessage::toJSONString).toArray(String[]::new))
            );
        } else {
            msg = (AbstractOrderedMessage) toAckSameTimestamp.get(0);
            msg.addAckedBy(messageToAck.getSenderID());
            msg.setAcked(msg.getAckedBySize() == this.participantIDs.size() - 1);
        }

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
            dedicatedRoomSocket.probeConnection();
        } catch (Exception e) {
            exceptionThrown = true;
        }
        this.onlineStatus = !exceptionThrown;

    }

    public boolean isRoomFinalized() {
        return roomFinalized;
    }

    private boolean roomFinalized = false; //finalized 60 seconds after the initial room creation request was acked
    private VectorTimestamp ownVectorTimestamp;
    private MyMulticastSocketWrapper dedicatedRoomSocket = null;
    private final List<AbstractMessage> outGoingMessageQueue = Collections.synchronizedList(new ArrayList<>());


    public synchronized void updateOutQueue() {
        ListIterator<AbstractMessage> iterator = outGoingMessageQueue.listIterator();
        AbstractMessage out = null;
        while (iterator.hasNext()) {
            out = iterator.next();
            if (out instanceof AckMessage || !out.shouldRetransmit()) { //no need to ack acks
                iterator.remove();
            }
        }
    }

    public synchronized List<AbstractMessage> getOutgoingMessages() {
        return outGoingMessageQueue.stream().filter(AbstractMessage::shouldRetransmit).toList();
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

    public synchronized void sendInRoomMessage(String payload, int clientID) {
        //Client id mapping --> sort
        //E.G. clients 1231,456246,215 will have index 215 -> 0,1231 ->1,456246->3 in the vector timestamp array

        int clientIndex = clientVectorIndex.get(clientID);
        this.lastMessageTimestamp = lastMessageTimestamp.increment(clientIndex);
        RoomMulticastMessage out = new RoomMulticastMessage(
                clientID,
                this.getRoomId(),
                this.lastMessageTimestamp,
                payload
        );
        out.setMilliTimestamp(System.currentTimeMillis());
        observedMessageOrder.add(out);
        outGoingMessageQueue.add(out);
    }

    public synchronized void addOutgoingMessage(AbstractMessage message) {
        outGoingMessageQueue.add(message);
    }

    public synchronized void setRoomFinalized(boolean roomFinalized) {
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

    public int getRoomId() {
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


}

package ChatRoom;

import Messages.AbstractMessage;
import Messages.AnonymousMessages.AckMessage;
import Messages.DeleteRoom;
import Messages.Logger;
import Messages.Room.DummyMessage;
import Messages.AnonymousMessages.RoomFinalizedMessage;
import Messages.Room.AbstractOrderedMessage;
import Messages.Room.RequestRetransmission;
import Messages.Room.RoomMulticastMessage;
import Networking.MyMulticastSocketWrapper;
import Peer.ChatClient;
import VectorTimestamp.VectorTimestamp;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static utils.Constants.*;

public class ChatRoom {
    private final int chatID;

    private VectorTimestamp lastMessageTimestamp;

    public int getOwnerID() {
        return ownerID;
    }

    private final int ownerID;
    private boolean onlineStatus;
    private Long lastReconnectAttempt = 0L, lastRTORequest = 0L;

    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    //it is mean as a VERY rough estimate in order to wait forever for a response,
    //by no means accurate
    private final Long creationTimestamp = System.currentTimeMillis();
    private final String groupName;

    private final Set<RoomMulticastMessage> observedMessageOrder = Collections.synchronizedSet(new LinkedHashSet<>());
    private Set<Integer> participantIDs = new TreeSet<Integer>();
    private HashMap<Integer, LinkedList<RoomMulticastMessage>> perParticipantMessageQueue = new HashMap<>();

    private Set<RoomMulticastMessage> incomingMessageQueue = new LinkedHashSet<>();

    //associate client id to positions in the vector (lookup is probably more efficient)
    private HashMap<Integer, Integer> clientVectorIndex;

    private boolean scheduledForDeletion = false;
    private boolean roomFinalized = false; //finalized 60 seconds after the initial room creation request was acked
    private MyMulticastSocketWrapper dedicatedRoomSocket = null;
    private final List<AbstractMessage> outGoingMessageQueue = Collections.synchronizedList(new ArrayList<>());

    public static String writeCurrentTime() {
        LocalTime currentTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return (currentTime.format(formatter));
    }


    public synchronized void updateInQueue() {
        int queueSizeBefore = incomingMessageQueue.size();
        Iterator<RoomMulticastMessage> iterator = incomingMessageQueue.iterator();
        while (iterator.hasNext()) {
            RoomMulticastMessage message = iterator.next();
            if (this.lastMessageTimestamp.canDeliver(message.getTimestamp())) {
                observedMessageOrder.add(message);
                iterator.remove();
                lastMessageTimestamp = VectorTimestamp.merge(lastMessageTimestamp, message.getTimestamp());
            }
        }
        if (!incomingMessageQueue.isEmpty() && incomingMessageQueue.size() == queueSizeBefore) {
            System.out.println("Asking for a retransmission request " + incomingMessageQueue.size() + " - " + queueSizeBefore);
            incomingMessageQueue.forEach(message -> System.out.println(message.toJSONString()));
            if (System.currentTimeMillis() - lastRTORequest > MIN_RTO_REQUEST_WAIT_MS) {
                //Fail to deliver, the sender MIGHT be dead --> request a retransmission
                RequestRetransmission rto = new RequestRetransmission(
                        ChatClient.ID,
                        this.chatID,
                        this.lastMessageTimestamp
                );
                addOutgoingMessage(rto);
                lastRTORequest = System.currentTimeMillis();
            }
        }
    }

    //return them with ACKED equal to true, they don't have to be acked again, only one client lost the message
    public synchronized List<RoomMulticastMessage> getObservedMessagesFrom(VectorTimestamp timestamp) {
        return observedMessageOrder.stream().
                filter(message -> message.getTimestamp().greaterThanOrEqual(timestamp)).
                map(message -> new RoomMulticastMessage((RoomMulticastMessage) message)).
                collect(Collectors.toList());
    }


    public synchronized void addIncomingMessage(RoomMulticastMessage inbound) {


        //check, for every queue that a message can be delivered
        if (incomingMessageQueue.contains(inbound) || observedMessageOrder.contains(inbound)) {
            return;
        }
        System.out.println(incomingMessageQueue.contains(inbound) + " - " + observedMessageOrder.contains(inbound));
        System.out.println("Adding to the inbound queue " + inbound.toJSONString());
        incomingMessageQueue.add(inbound);
        Iterator<RoomMulticastMessage> iterator = incomingMessageQueue.iterator();
        while (iterator.hasNext()) {
            RoomMulticastMessage message = iterator.next();
            if (this.lastMessageTimestamp.canDeliver(message.getTimestamp())) {
                observedMessageOrder.add(message);
                iterator.remove();
                lastMessageTimestamp = VectorTimestamp.merge(lastMessageTimestamp, message.getTimestamp());
            }
        }


    }

    //it's just reading it can be not synchronized, temporary discrepancies are ok
    public void printMessages() {
        System.out.println("Chat room #" + this.chatID + " - Owner #" + this.ownerID);
        synchronized (observedMessageOrder) {
            observedMessageOrder.
                    stream().
                    forEach(msg -> System.out.println("     " + msg.toChatString()));
            System.out.println("Current Timestamp " + this.lastMessageTimestamp);
        }
    }

    public void sendRawMessageNoQueue(AbstractMessage message) {
        this.dedicatedRoomSocket.sendPacket(message);
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
        if (!roomFinalized && System.currentTimeMillis() > creationTimestamp + MAX_ROOM_CREATION_WAIT_MS) {
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
        if (System.currentTimeMillis() - this.lastReconnectAttempt < MIN_SOCKET_RECONNECT_DELAY_MS) {
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


    public synchronized void updateOutQueue() {
        ListIterator<AbstractMessage> iterator = outGoingMessageQueue.listIterator();
        AbstractMessage out = null;
        while (iterator.hasNext()) {
            out = iterator.next();
            if (out.canDelete())
                iterator.remove();
        }
    }

    public synchronized List<AbstractMessage> getOutgoingMessages() {
        return outGoingMessageQueue.stream().filter(AbstractMessage::shouldRetransmit).toList();
    }

    public synchronized void announceRoomFinalized(int clientID, ChatRoom defaultChannel) {
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

        if (scheduledForDeletion) {
            System.out.println("The room is scheduled for deletion, not accepting any more messages... ");
            return;
        }

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

    public synchronized void setOffline(boolean isOffline) {
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
        if (chatID == DEFAULT_GROUP_ROOMID) {
            this.ownerID = -1; //no one owns the default channel
        } else this.ownerID = owner;

        this.chatID = chatID;
        this.dedicatedRoomSocket = new MyMulticastSocketWrapper(groupName);
        this.onlineStatus = dedicatedRoomSocket.isConnected();
        this.groupName = groupName;
    }

    public boolean isScheduledForDeletion() {
        return this.scheduledForDeletion;
    }

    public synchronized void cleanup() {
        this.dedicatedRoomSocket.close();
    }

    public synchronized void displayWarningMessage() {
        observedMessageOrder.clear();
        observedMessageOrder.add(new RoomMulticastMessage(
                ChatClient.ID,
                -1,
                new VectorTimestamp(new int[]{-1}),
                "This room has been deleted please exit the room by typing q"));
    }

    public boolean addParticipant(Integer participantID) {
        if (roomFinalized || System.currentTimeMillis() > creationTimestamp + MAX_ROOM_CREATION_WAIT_MS) //rooms are immutable
            return false;
        return participantIDs.add(participantID);
    }

    public boolean canDelete(int participantID) {
        return this.getOwnerID() == participantID;
    }

    public synchronized void scheduleDeletion(boolean iOwnIt) {
        if (iOwnIt) {
            int clientIndex = clientVectorIndex.get(ChatClient.ID);
            this.lastMessageTimestamp = lastMessageTimestamp.increment(clientIndex);
            //room deletion causally ordered because why not
            DeleteRoom delete = new DeleteRoom(this.chatID, ChatClient.ID, this.lastMessageTimestamp);
            this.outGoingMessageQueue.add(delete);
        }
        // once it's scheduled for deletion DO NOT ACCEPT MESSAGES,
        // wait until  all queues are empty (outgoing messages all acked) then delete the room
        this.scheduledForDeletion = true;
    }


}

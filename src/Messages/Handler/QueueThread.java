package Messages.Handler;

import java.io.IOException;
import java.net.*;

import ChatRoom.ChatRoom;
import Events.AbstractEvent;
import Events.GenericNotifyEvent;
import Messages.CommonMulticastMessages.AnonymousMessages.*;
import Messages.CommonMulticastMessages.AbstractMessage;
import Messages.CommonMulticastMessages.Room.AbstractOrderedMessage;
import Messages.CommonMulticastMessages.Room.RequestRetransmission;
import Messages.CommonMulticastMessages.Room.RoomMulticastMessage;
import Peer.AbstractClient;
import Events.ReplyToRoomRequestEvent;
import Peer.ChatClient;
import VectorTimestamp.VectorTimestamp;
import com.google.gson.*;

import java.util.*;
import java.util.stream.Collectors;

import static utils.Constants.*;

public class QueueThread implements QueueManager {

    private final Map<Integer, Long> onlineClientsLastHeard = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, InetAddress> onlineClientsAddresses = Collections.synchronizedMap(new HashMap<>());

    private final AbstractClient client;

    private final Map<Integer, ChatRoom> roomsMap = Collections.synchronizedMap(new HashMap<>());
    private ChatRoom commonMulticastChannel;
    private ChatRoom currentRoom = null;

    private List<Integer> roomIDs = Collections.synchronizedList(new ArrayList<>(15));

    private int currentIDIndex = 0;

    public void deleteRoom(ChatRoom room) {
        synchronized (roomsMap) {
            if (!roomsMap.containsKey(room.getRoomId())) return;
            roomsMap.remove(room.getRoomId());
            roomIDs.remove((Integer) room.getRoomId());
        }
    }


    private void cycleRooms() {
        synchronized (roomsMap) {
            currentRoom = roomsMap.get(roomIDs.get(currentIDIndex));
            currentIDIndex = currentIDIndex + 1 == roomIDs.size() ? 0 : currentIDIndex + 1;
        }
    }

    public void addParticipantToRoom(int roomID, int senderID) {
        synchronized (roomsMap) {
            roomsMap.get(roomID).addParticipant(senderID);
        }
    }


    @Override
    public String getOnlineClients() {
        StringBuilder b = new StringBuilder(10 * onlineClientsAddresses.size());
        synchronized (onlineClientsLastHeard) {
            onlineClientsLastHeard.entrySet().stream().forEach(
                    entry -> b.append("\t\t").append(entry.getKey()).append(" -> Last Heard : ").append((System.currentTimeMillis() - entry.getValue()) / 1000).append(" seconds ago\n")
            );
        }
        return b.toString();
    }

    /**
     * @param roomID
     * @return
     */
    @Override
    public Optional<ChatRoom> getChatRoom(int roomID) {
        if (roomID == commonMulticastChannel.getRoomId()) return Optional.empty();
        synchronized (roomsMap) {
            if (roomsMap.containsKey(roomID))
                return Optional.of(roomsMap.get(roomID));
            return Optional.empty();
        }
    }

    public void registerRoom(ChatRoom chatRoom) {
        synchronized (roomsMap) {
            if (roomsMap.containsKey(chatRoom.getRoomId())) throw new RuntimeException("Duplicate chat room");
            roomsMap.put(chatRoom.getRoomId(), chatRoom);
            roomIDs.add(chatRoom.getRoomId());
        }
    }

    @Override
    public String listRoomsStatus() {
        StringBuilder sb = new StringBuilder("\n");
        synchronized (roomsMap) {
            roomsMap.values().forEach(
                    room -> {
                        sb.append("\t");
                        if (room.isRoomFinalized()) {
                            sb.append("Room #").append(room.getRoomId()).append(" Online : [").append(room.isOnline()).append("] \n");
                            synchronized (onlineClientsLastHeard) {
                                room.getParticipantIDs().stream().filter(id -> id != ChatClient.ID).forEach(pid -> sb.append("\t\tClient #").append(pid).append(" - Index -> [").append(room.getClientIndex(pid)).append("]\n"));
                            }
                        } else sb.append("Room #").append(room.getRoomId()).append(" not finalized yet");
                        sb.append("\n");
                    }
            );
        }
        return sb.toString();

    }

    public QueueThread(AbstractClient client, ChatRoom commonMulticastChannel) throws IOException {
        this.commonMulticastChannel = commonMulticastChannel;
        this.roomIDs.add(commonMulticastChannel.getRoomId());
        this.client = client;
        this.currentRoom = commonMulticastChannel;
        registerRoom(currentRoom);
    }

    private final static Long MAX_HELLO_INTERVAL_MS = 10000L;

    private void updateOnlineClients() {
        synchronized (onlineClientsLastHeard) {
            Collection<Integer> idsToRemove = onlineClientsLastHeard.entrySet().
                    stream().
                    filter(entry -> System.currentTimeMillis() - entry.getValue() > MAX_HELLO_INTERVAL_MS).
                    map(Map.Entry::getKey).
                    toList();
            idsToRemove.forEach(onlineClientsLastHeard::remove);
        }
    }

    private Long lastHeartBeat = 0L;

    public void sendHeartBeat() {
        if (System.currentTimeMillis() - lastHeartBeat < HEARTBEAT_INTERVAL_MS
                || !commonMulticastChannel.isOnline()) return;
        lastHeartBeat = System.currentTimeMillis();
        HeartbeatMessage hello = new HeartbeatMessage(
                this.client.getID(),
                commonMulticastChannel.getRoomId()
        );
        commonMulticastChannel.addOutgoingMessage(hello);
    }

    /**
     * The thread takes care of the queue, waits for messages on the socket and is in charge
     * of sending messages
     */
    @Override
    public void run() {
        System.out.println("QueueThread Online");
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        Gson gson = new GsonBuilder().registerTypeAdapter(AbstractMessage.class, new AbstractMessage.AbstractMessageDeserializer()).create();
        boolean packetReceived;
        long last = 0;
        while (true) {
            packetReceived = false;
            cycleRooms();
            if (!currentRoom.isRoomFinalized()) {
                if (currentRoom.getOwnerID() == this.client.getID() && currentRoom.finalizeRoom()) {
                    //listen on its socket only AFTER it's been finalized
                    currentRoom.announceRoomFinalized(client.getID(), client.getDefaultRoom());
                }

            }

            updateOnlineClients();

            if (currentRoom.isOnline()) {
                //check if any queued messages can now be delivered
                sendHeartBeat();
                currentRoom.updateInQueue();
                List<AbstractMessage> nextMsg = currentRoom.getOutgoingMessages();


                if(System.currentTimeMillis() - last > 3000) {
                    nextMsg.forEach(System.out::println);
                }

                if (currentRoom.isScheduledForDeletion() && nextMsg.isEmpty()) {
                    //all messages acked, delete the room
                    currentRoom.displayWarningMessage();
                    currentRoom.cleanup();
                    deleteRoom(currentRoom);
                    continue;
                }

                nextMsg.forEach(m -> {
                    boolean sendOutcome = currentRoom.getDedicatedRoomSocket().sendPacket(m);
                    String t = "";

                    if (m instanceof AbstractOrderedMessage)
                        ((AbstractOrderedMessage) m).setMilliTimestamp(System.currentTimeMillis());
                    if (!sendOutcome) {
                        currentRoom.setOffline(true);
                        client.addEvent(new GenericNotifyEvent("Detected network loss"));
                    }
                    currentRoom.updateOutQueue();
                });
                packet = new DatagramPacket(buffer, buffer.length);
                //now attempt to receive
                packetReceived = currentRoom.getDedicatedRoomSocket().receive(packet);
            } else {
                currentRoom.getBackOnline();
            }
            //check for incoming packets
            AbstractMessage inbound = null;
            if (packetReceived) {
                String jsonString = new String(packet.getData(), 0, packet.getLength());
                try {
                    JsonObject jsonInboundMessage = JsonParser.parseString(jsonString).getAsJsonObject();
                    inbound = gson.fromJson(jsonInboundMessage, AbstractMessage.class);
                } catch (Exception e) {
                    System.out.println("Bad message detected");
                    continue;
                }
                int sender = inbound.getSenderID();
                int roomID = inbound.getRoomID();
                if (sender != ChatClient.ID) {
                    //ignore my messages
//                    if (!(inbound instanceof HeartbeatMessage))
//                        System.out.println("Received " + inbound.getClass().getName() + " from #" + sender);

                    switch (inbound.getMessageType()) {
                        //Actionable messages
                        case MESSAGE_TYPE_HELLO -> {
                            String username = inbound.getUsername();
                            client.addUsernameMapping(sender, username);
                            client.addEvent(new GenericNotifyEvent("Received an hello from #" + sender + " replying with WELCOME"));
                            synchronized (onlineClientsLastHeard) {
                                onlineClientsLastHeard.put(sender, System.currentTimeMillis());
                                onlineClientsAddresses.put(sender, packet.getAddress());
                            }
                            AbstractMessage welcome = new WelcomeMessage(this.client.getID(), this.client.getUserName());
                            commonMulticastChannel.addOutgoingMessage(welcome);
                        }
                        case MESSAGE_TYPE_HEARTBEAT -> {
                            synchronized (onlineClientsLastHeard) {
                                onlineClientsLastHeard.put(sender, System.currentTimeMillis());
                                onlineClientsAddresses.put(sender, packet.getAddress());
                            }
                        }
                        case MESSAGE_TYPE_WELCOME -> {
                            String prompt = "Received a WELCOME from #" + sender + "\nAdded client " + sender + " to the list of known clients";
                            client.addEvent(new GenericNotifyEvent(prompt));
                            synchronized (onlineClientsLastHeard) {
                                onlineClientsLastHeard.put(sender, System.currentTimeMillis());
                                onlineClientsAddresses.put(sender, packet.getAddress());
                            }
                        }
                        case MESSAGE_TYPE_JOIN_ROOM_ACCEPT -> { //sent only to who created the room
                            //if false i haven't created the room
                            if (roomsMap.containsKey(inbound.getRoomID())) {
                                client.addEvent(new GenericNotifyEvent("Client #" + sender + " agreed to participate in the chat room"));
                                addParticipantToRoom(roomID, sender);
                            }
                        }
                        case MESSAGE_TYPE_CREATE_ROOM -> {
                            CreateRoomRequest req = (CreateRoomRequest) inbound;

                            if (!roomsMap.containsKey(req.getRoomID())) { //don't ask the user multiple times
                                AbstractEvent eventToProcess = new ReplyToRoomRequestEvent(req.getSenderID(), this.client.getID(), req.getGroupname(), roomID, sender, "y", "n");
                                client.addEvent(eventToProcess);
                            }
                        }
                        case MESSAGE_TYPE_ROOM_FINALIZED -> {
                            synchronized (roomsMap) {
                                RoomFinalizedMessage fin = (RoomFinalizedMessage) inbound;
                                ChatRoom room = roomsMap.get(roomID);
                                if (room == null) {
                                    System.out.println("Non-existent room, missed the CREATE_ROOM_MESSAGE");
                                } else {
                                    if (!fin.getParticipantIds().contains(client.getID()) && roomsMap.containsKey(roomID)) {
                                        //We didn't reply fast enough to the room creation message, we're not participating in the room
                                        deleteRoom(room);
                                    } else {
                                        room.forceFinalizeRoom(fin.getParticipantIds());
                                        client.addEvent(new GenericNotifyEvent("Room " + room.getRoomId() + " has been finalized"));
                                    }
                                }
                            }
                        }
                        case MESSAGE_TYPE_ROOM_MESSAGE -> {
                            synchronized (roomsMap) {
                                ChatRoom dedicatedRoom = roomsMap.get(roomID);
                                if (!(inbound instanceof RoomMulticastMessage))
                                    throw new RuntimeException("Illegal Message Type");
                                dedicatedRoom.addIncomingMessage((RoomMulticastMessage) inbound);
                                AckMessage ackMessage = new AckMessage(
                                        this.client.getID(),
                                        inbound.getSenderID(),
                                        ((RoomMulticastMessage) inbound).getTimestamp(),
                                        dedicatedRoom.getRoomId(),
                                        packet.getAddress()
                                );
                                dedicatedRoom.sendRawMessageNoQueue(ackMessage);
                            }
                        }
                        case MESSAGE_TYPE_ACK -> {
                            AckMessage m = (AckMessage) inbound;
                            if (m.getRecipientID() != this.client.getID()) break;
                            synchronized (roomsMap) {
                                ChatRoom dedicatedRoom = roomsMap.get(roomID);
                                dedicatedRoom.ackMessage((AckMessage) inbound);
                            }
                        }
                        case MESSAGE_TYPE_DELETE_ROOM -> {
                            DeleteRoom message = (DeleteRoom) inbound;
                            InetAddress ackUnicastDestination = null;
                            synchronized (onlineClientsLastHeard) {
                                ackUnicastDestination = onlineClientsAddresses.get(inbound.getSenderID());
                            }
                            synchronized (roomsMap) {
                                ChatRoom room = roomsMap.get(message.getRoomID());
                                if (room != null) {
                                    room.scheduleDeletion(false);
                                    AckMessage ackMessage = new AckMessage(ChatClient.ID, inbound.getSenderID(), message.getTimestamp(), room.getRoomId(), ackUnicastDestination);
                                    room.sendRawMessageNoQueue(ackMessage);
                                }
                            }
                        }
                        case MESSAGE_TYPE_REQUEST_RTO -> {
                            RequestRetransmission rto = (RequestRetransmission) inbound;

                            synchronized (roomsMap) {
                                ChatRoom dedicatedRoom = roomsMap.get(rto.getRoomID());

                                List<RoomMulticastMessage> toRetransmit = dedicatedRoom.getObservedMessagesFrom(rto.getTimestamp());

                                toRetransmit.forEach(dedicatedRoom::addOutgoingMessage);
                            }

                        }
                    }
                }

            }
        }
    }
}

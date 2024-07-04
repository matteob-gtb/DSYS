package Messages;

import java.io.IOException;
import java.net.*;

import ChatRoom.ChatRoom;
import Events.AbstractEvent;
import Events.GenericNotifyEvent;
import Messages.AnonymousMessages.AckMessage;
import Messages.AnonymousMessages.CreateRoomRequest;
import Messages.AnonymousMessages.RoomFinalizedMessage;
import Messages.AnonymousMessages.WelcomeMessage;
import Messages.Room.AbstractOrderedMessage;
import Messages.Room.RoomMulticastMessage;
import Networking.MyMulticastSocketWrapper;
import Peer.AbstractClient;
import Events.ReplyToRoomRequestEvent;
import Peer.ChatClient;
import com.google.gson.*;

import java.util.*;

import static utils.Constants.*;

public class QueueThread implements QueueManager {

    private Set<Integer> onlineClients = new TreeSet<Integer>();
    private final AbstractClient client;
    private final Map<Integer, ChatRoom> roomsMap = Collections.synchronizedMap(new HashMap<>());
    private ChatRoom commonMulticastChannel;
    private ChatRoom currentRoom = null;

    private List<Integer> roomIDs = Collections.synchronizedList(new ArrayList<>());
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
    public Set<Integer> getOnlineClients() {
        return onlineClients;
    }

    /**
     * @param roomID
     * @return
     */
    @Override
    public Optional<ChatRoom> getChatRoom(int roomID) {
        if (roomsMap.containsKey(roomID))
            return Optional.of(roomsMap.get(roomID));
        return Optional.empty();
    }

    public void registerRoom(ChatRoom chatRoom) {
        synchronized (roomsMap) {
            if (roomsMap.containsKey(chatRoom.getRoomId())) throw new RuntimeException("Duplicate chat room");
            roomsMap.put(chatRoom.getRoomId(), chatRoom);
            roomIDs.add(chatRoom.getRoomId());
        }
    }

    @Override
    public List<ChatRoom> getRooms() {
        return new ArrayList<>(this.roomsMap.values());
    }

    public QueueThread(AbstractClient client, ChatRoom commonMulticastChannel) throws IOException {
        this.commonMulticastChannel = commonMulticastChannel;
        this.roomIDs.add(commonMulticastChannel.getRoomId());
        this.client = client;
        this.currentRoom = commonMulticastChannel;
        registerRoom(currentRoom);
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
        MulticastMessage nextMessage = null;
        JsonObject outgoingMessage;
        Gson gson = new GsonBuilder().registerTypeAdapter(AbstractMessage.class, new AbstractMessage.AbstractMessageDeserializer()).create();
        boolean packetReceived = false;
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

            if (currentRoom.isOnline()) {
                //check if any queued messages can now be delivered
                currentRoom.updateInQueue();

                List<AbstractMessage> nextMsg = currentRoom.getOutgoingMessages();

                if (System.currentTimeMillis() - last > 5000) {
                    System.out.println("Outgoing messages :" + nextMsg.size());
                    last = System.currentTimeMillis();
                }

                if (nextMsg.isEmpty()) { //all messages acked, delete the room
                    if (currentRoom.isScheduledForDeletion()) {
                        currentRoom.displayWarningMessage();
                        currentRoom.cleanup();
                        deleteRoom(currentRoom);
                        continue;
                    }
                }

                nextMsg.forEach(m -> {
                    boolean sendOutcome = currentRoom.getDedicatedRoomSocket().sendPacket(m);
                    String t = "";

                    if (m instanceof AbstractOrderedMessage)
                        t = ((AbstractOrderedMessage) m).getTimestamp().toString();

                    System.out.println("Sending message " + m.getClass().getSimpleName() + " in room #" + currentRoom.getRoomId() + " " + t);

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

                    //System.out.println("Received " + inbound.getClass().getName() + " from #" + sender);

                    switch (inbound.messageType) {
                        //Actionable messages
                        case MESSAGE_TYPE_HELLO -> {
                            String username = inbound.username;
                            client.addUsernameMapping(sender, username);
                            client.addEvent(new GenericNotifyEvent("Received an hello from #" + sender + " replying with WELCOME"));
                            onlineClients.add(sender);
                            AbstractMessage welcome = new WelcomeMessage(this.client.getID(), this.client.getUserName());
                            commonMulticastChannel.addOutgoingMessage(welcome);
                        }
                        case MESSAGE_TYPE_WELCOME -> {
                            String prompt = "Received a WELCOME from #" + sender + "\nAdded client " + sender + " to the list of known clients";
                            client.addEvent(new GenericNotifyEvent(prompt));
                            onlineClients.add(sender);
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
                                AbstractEvent eventToProcess = new ReplyToRoomRequestEvent(req.senderID, this.client.getID(), req.getGroupname(), roomID, sender, "y", "n");
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
                                        dedicatedRoom.getRoomId()
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
                            ChatRoom room = roomsMap.get(message.getRoomID());
                            if (room != null) {
                                room.scheduleDeletion(false);
                                AckMessage ackMessage = new AckMessage(ChatClient.ID, inbound.getSenderID(), message.getTimestamp(), room.getRoomId());
                                room.sendRawMessageNoQueue(ackMessage);
                            } else {
                                System.out.println("Deleting unknown roomID, ignoring it");
                            }
                        }
                    }
                }

            }
        }
    }
}

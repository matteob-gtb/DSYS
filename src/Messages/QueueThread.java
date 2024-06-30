package Messages;

import java.io.IOException;
import java.net.*;

import ChatRoom.ChatRoom;
import Events.AbstractEvent;
import Events.GenericNotifyEvent;
import Messages.AnonymousMessages.CreateRoomRequest;
import Messages.AnonymousMessages.RoomFinalizedMessage;
import Messages.AnonymousMessages.WelcomeMessage;
import Messages.Room.RoomMulticastMessage;
import Networking.MyMulticastSocketWrapper;
import Peer.AbstractClient;
import Events.ReplyToRoomRequestEvent;
import com.google.gson.*;

import java.util.*;

import static utils.Constants.*;

public class QueueThread implements QueueManager {

    private static final int SOCKET_TIMEOUT = 100;
    private final Object roomLock = new Object();
    private Set<Integer> onlineClients = new TreeSet<Integer>();
    private final AbstractClient client;
    private final Map<Integer, ChatRoom> roomsMap = Collections.synchronizedMap(new HashMap<>());
    private ChatRoom commonMulticastChannel;
    private MyMulticastSocketWrapper currentSocket = null;
    private ChatRoom currentRoom = null;

    private List<Integer> roomIDs = Collections.synchronizedList(new ArrayList<>());
    private int currentIDIndex = 0;

    public void deleteRoom(ChatRoom room) {
        //TODO exception maybe not a great idea
        if (!roomsMap.containsKey(room.getChatID())) throw new RuntimeException("Room doesn't exist");
        synchronized (roomLock) {
            roomsMap.remove(room.getChatID());
            //java moment
            roomIDs.remove((Integer) room.getChatID());
        }
    }


    private void cycleRooms() {
        synchronized (roomLock) {
            currentRoom = roomsMap.get(roomIDs.get(currentIDIndex));
            currentIDIndex = currentIDIndex + 1 == roomIDs.size() ? 0 : currentIDIndex + 1;
        }
    }

    public void addParticipantToRoom(int roomID, int senderID) {
        synchronized (roomLock) {
            roomsMap.get(roomID).addParticipant(senderID);
        }
    }

//    public void sendMessage(MulticastMessage m, int roomID) throws IOException {
//        InetAddress destination = null;
//        synchronized (roomLock) {
//            if (!roomsMap.containsKey(roomID))
//                throw new RuntimeException("No such room");
//            destination = roomsMap.get(roomID).getRoomAddress();
//        }
//        currentSocket.sendPacket(m);
//    }


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
        synchronized (roomLock) {
            if (roomsMap.containsKey(chatRoom.getChatID())) throw new RuntimeException("Duplicate chat room");
            roomsMap.put(chatRoom.getChatID(), chatRoom);
        }
    }

    @Override
    public List<ChatRoom> getRooms() {
        return new ArrayList<>(this.roomsMap.values());
    }

    public QueueThread(AbstractClient client, ChatRoom commonMulticastChannel) throws IOException {
        this.commonMulticastChannel = commonMulticastChannel;
        this.currentSocket = commonMulticastChannel.getDedicatedRoomSocket();
        this.roomIDs.add(commonMulticastChannel.getChatID());
        this.client = client;
        this.currentRoom = commonMulticastChannel;
        registerRoom(currentRoom);
    }

    public void setFinalized(int roomId) {
        synchronized (roomLock) {
            roomIDs.add(roomId);
        }
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
        while (true) {
            packetReceived = false;
            cycleRooms();
            if (!currentRoom.isRoomFinalized()) {
                if (currentRoom.getOwnerID() == this.client.getID() && currentRoom.finalizeRoom()) {
                    //listen on its socket only AFTER it's been finalized
                    roomIDs.add(currentRoom.getChatID());
                    currentRoom.announceRoomFinalized(client.getID(), client.getDefaultRoom());
                }

            }
            if (currentRoom.isOnline()) {
                Optional<AbstractMessage> nextMsg = currentRoom.getOutgoingMessage();
                nextMsg.ifPresent(messageInterface -> {
                    currentRoom.getDedicatedRoomSocket().sendPacket(messageInterface);
                    currentRoom.updateOutQueue();
                });
                packet = new DatagramPacket(buffer, buffer.length);
                packetReceived = currentRoom.getDedicatedRoomSocket().receive(packet);
            } else {
                currentRoom.getBackOnline();
            }
            //check for incoming packets

            if (packetReceived) {

                String jsonString = new String(packet.getData(), 0, packet.getLength());
                JsonObject jsonInboundMessage = JsonParser.parseString(jsonString).getAsJsonObject();


                //TODO deserialize based on the type of the message
                AbstractMessage inbound = gson.fromJson(jsonInboundMessage, AbstractMessage.class);

                int sender = inbound.getSenderID();

                System.out.println("Sender id : " + sender);
                if (sender == this.client.getID())
                    continue;
                int roomID = jsonInboundMessage.get(ROOM_ID_PROPERTY_NAME).getAsInt();
                switch (inbound.messageType) {
                    //Actionable messages
                    case MESSAGE_TYPE_HELLO -> {
                        String username = inbound.username;
                        client.addUsernameMapping(sender, username);
                        client.addEvent(new GenericNotifyEvent("Received an hello from #" + sender + " replying with WELCOME"));
                        onlineClients.add(sender);
                        System.out.println("Received an hello from #" + sender + " replying with WELCOME");
                        AbstractMessage welcome = new WelcomeMessage(this.client.getID(), this.client.getUserName());
                        commonMulticastChannel.addOutgoingMessage(welcome);
                    }
                    case MESSAGE_TYPE_WELCOME -> {
                        String prompt = "Received a WELCOME from #" + sender + "\nAdded client " + sender + " to the list of known clients";
                        client.addEvent(new GenericNotifyEvent(prompt));
                        onlineClients.add(sender);
                    }
                    case MESSAGE_TYPE_JOIN_ROOM_ACCEPT -> { //sent only to who created the room
                        System.out.println("Processing ROOM_JOIN incomingMessage");
                        client.addEvent(new GenericNotifyEvent("Client #" + sender + " agreed to participate in the chat room"));
                        addParticipantToRoom(roomID, sender);
                    }
                    case MESSAGE_TYPE_CREATE_ROOM -> {
                        System.out.println("Received a room invitation");
                        CreateRoomRequest req = gson.fromJson(jsonInboundMessage, CreateRoomRequest.class);
                        if (!roomsMap.containsKey(req.getRoomID())) { //don't ask the user multiple times
                            AbstractEvent eventToProcess = new ReplyToRoomRequestEvent(req.senderID, this.client.getID(), req.getGroupname(), roomID, sender, client.getBaseMessageStub(), "y", "n");
                            client.addEvent(eventToProcess);
                        }
                        break;
                    }
                    case MESSAGE_TYPE_ROOM_FINALIZED -> {
                        System.out.println("Received a finalized room incomingMessage from " + sender + " room - " + roomID);
                        synchronized (roomLock) {
                            RoomFinalizedMessage fin = (RoomFinalizedMessage) inbound;
                            ChatRoom room = roomsMap.get(roomID);
                            if (room == null) {
                                System.out.println("Non-existent room, missed the CREATE_ROOM_MESSAGE");
                            } else {
                                if (!fin.getParticipantIds().contains(client.getID()) && roomsMap.containsKey(roomID)) {
                                    //Something went wrong, we can't access the room
                                    deleteRoom(room);
                                } else {
                                    room.forceFinalizeRoom(fin.getParticipantIds());
                                    //overkill
                                    synchronized (roomLock) {
                                        roomIDs.add(room.getChatID());
                                    }
                                }
                            }
                        }
                    }
                    case MESSAGE_TYPE_ROOM_MESSAGE -> {
                        synchronized (roomLock) {
                            ChatRoom dedicatedRoom = roomsMap.get(roomID);
                            if (!(inbound instanceof RoomMulticastMessage))
                                throw new RuntimeException("Illegal Message Type");
                            dedicatedRoom.addIncomingMessage((RoomMulticastMessage) inbound);
                        }
                    }
                    //append to relevant queue
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException("I/O thread must not be interrupted");
                }
            }
        }
    }
}

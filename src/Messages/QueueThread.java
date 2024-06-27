package Messages;

import java.io.IOException;
import java.net.*;

import ChatRoom.ChatRoom;
import Events.AbstractEvent;
import Events.GenericNotifyEvent;
import Networking.MyMulticastSocketWrapper;
import Peer.AbstractClient;
import Events.ReplyToRoomRequestEvent;
import com.google.gson.*;

import java.util.*;
import java.util.stream.Collectors;

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
        if (!roomsMap.containsKey(room)) throw new RuntimeException("Room doesn't exist");
        synchronized (roomLock) {
            roomsMap.remove(room);
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

    public void sendMessage(MulticastMessage m, int roomID) throws IOException {
        InetAddress destination = null;
        synchronized (roomLock) {
            if (!roomsMap.containsKey(roomID))
                throw new RuntimeException("No such room");
            destination = roomsMap.get(roomID).getRoomAddress();
        }
        currentSocket.sendPacket(m);
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
        if (roomsMap.containsKey(chatRoom.getChatID())) throw new RuntimeException("Duplicate chat room");
        roomsMap.put(chatRoom.getChatID(), chatRoom);
        roomIDs.add(chatRoom.getChatID());
    }

    @Override
    public List<ChatRoom> getRooms() {
        return new ArrayList<>(this.roomsMap.values());
    }

    public QueueThread(AbstractClient client, ChatRoom commonMulticastChannel) throws IOException {
        this.commonMulticastChannel = commonMulticastChannel;
        this.currentSocket = commonMulticastChannel.getDedicatedRoomSocket();
        this.registerRoom(commonMulticastChannel);
        this.client = client;
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
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        boolean packetReceived = false;
        while (true) {
            cycleRooms();
            if (!currentRoom.isRoomFinalized()) {
                if (currentRoom.finalizeRoom()) {
                    currentRoom.announceRoomFinalized(client.getID());
                }

            }
            //TODO send messages, check queue

            if (currentRoom.isOnline()) {
                Optional<MessageInterface> nextMsg = currentRoom.getOutgoingMessage();
                nextMsg.ifPresent(messageInterface -> {
                    currentRoom.getDedicatedRoomSocket().sendPacket(messageInterface);
                    currentRoom.updateOutQueue();
                });
            } else {
                currentRoom.getBackOnline();
            }
            //check for incoming packets
            packet = new DatagramPacket(buffer, buffer.length);
            packetReceived = currentRoom.getDedicatedRoomSocket().receive(packet);
            if (packetReceived) {

                String jsonString = new String(packet.getData(), 0, packet.getLength());


                JsonObject jsonInboundMessage = JsonParser.parseString(jsonString).getAsJsonObject();
                MulticastMessage inbound = gson.fromJson(jsonInboundMessage, MulticastMessage.class);

                Logger.writeLog("Received Message\n" + inbound.toJSONString() + "\n");
                System.out.println(inbound.getMessageDebugString());

                int sender = inbound.getSenderID();
                MulticastMessage message = gson.fromJson(jsonInboundMessage, MulticastMessage.class);

                if (sender == this.client.getID())
                    continue;
                int roomID = jsonInboundMessage.get(ROOM_ID_PROPERTY_NAME).getAsInt();
                switch (message.messageType) {
                    //Actionable messages
                    case MESSAGE_TYPE_HELLO -> {
                        String username = message.username;
                        client.addUsernameMapping(sender, username);
                        client.addEvent(new GenericNotifyEvent("Received an hello from #" + sender + " replying with WELCOME"));
                        onlineClients.add(sender);
                        MulticastMessage welcome = MulticastMessage.getWelcomeMessage(this.client.getID());
                        commonMulticastChannel.addOutgoingMessage(welcome);
                    }
                    case MESSAGE_TYPE_WELCOME -> {
                        String prompt = "Received a WELCOME from #" + sender + "\nAdded client " + sender + " to the list of known clients";
                        client.addEvent(new GenericNotifyEvent(prompt));
                        onlineClients.add(sender);
                    }
                    case MESSAGE_TYPE_JOIN_ROOM_ACCEPT -> { //sent only to who created the room
                        System.out.println("Processing ROOM_JOIN message");
                        client.addEvent(new GenericNotifyEvent("Client #" + sender + " agreed to participate in the chat room"));
                        addParticipantToRoom(roomID, sender);
                    }
                    case MESSAGE_TYPE_CREATE_ROOM -> {
                        System.out.println("Received a room invitation");
                        AbstractEvent eventToProcess = new ReplyToRoomRequestEvent(this.client.getID(), roomID, sender, client.getBaseMessageStub(), "y", "n");
                        client.addEvent(eventToProcess);
                    }
                    case MESSAGE_TYPE_ROOM_FINALIZED -> {
                        System.out.println("Received a finalized room message from " + sender);
                        synchronized (roomsMap) {
                            ChatRoom room = roomsMap.get(roomID);
                            Set<Integer> finalParticipants = new HashSet<>();
                            JsonObject el = JsonParser.parseString(message.getPayload()).getAsJsonObject();
                            JsonArray array = el.getAsJsonObject().get(FIELD_ROOM_PARTICIPANTS).getAsJsonArray();
                            array.forEach(k -> finalParticipants.add(k.getAsInt()));
                            room.forceFinalizeRoom(finalParticipants);
                        }
                    }
                    case ROOM_MESSAGE -> {
                        // int chatRoomID = jsonInboundMessage.get(ROOM_ID_PROPERTY_NAME).getAsInt();


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

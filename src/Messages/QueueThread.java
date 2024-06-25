package Messages;

import java.io.IOException;
import java.net.*;

import ChatRoom.ChatRoom;
import Events.AbstractEvent;
import Events.GenericNotifyEvent;
import Peer.AbstractClient;
import Events.ReplyToRoomRequestEvent;
import com.google.gson.*;

import java.util.*;

import static utils.Constants.*;

public class QueueThread implements QueueManager {

    private static final int SOCKET_TIMEOUT = 100;
    private InetAddress group;
    private Set<Integer> onlineClients = new TreeSet<Integer>();
    private final AbstractClient client;
    //private List<ChatRoom> roomsList = Collections.synchronizedList(new ArrayList<>());
    private Map<Integer, ChatRoom> roomsMap = Collections.synchronizedMap(new HashMap<>());
    private ChatRoom commonMulticastChannel;
    private MyMulticastSocketWrapper currentSocket = null;
    private ChatRoom currentRoom = null;

    private List<Integer> roomIDs = Collections.synchronizedList(new ArrayList<>());
    private int currentIDIndex = 0;


    private void cycleRooms() {
        currentRoom = roomsMap.get(roomIDs.get(currentIDIndex));
        currentIDIndex = currentIDIndex + 1 == roomIDs.size() ? 0 : currentIDIndex + 1;
    }


    /**
     * @param m
     * @param room
     */
    @Override
    public void sendMessage(MulticastMessage m, ChatRoom room) {
        System.out.println("Sent message in room " + room.getChatID());
        room.addOutgoingMessage(m);
        room.getDedicatedRoomSocket().sendPacket(m);
    }

    public void addParticipantToRoom(int roomID, int senderID) {
        synchronized (roomsMap) {
            roomsMap.get(roomID).addParticipant(senderID);
        }
    }

    public void sendMessage(MulticastMessage m, int roomID) throws IOException {
        InetAddress destination = null;
        synchronized (roomsMap) {
            if (!roomsMap.containsKey(roomID))
                throw new RuntimeException("No such room");
            destination = roomsMap.get(roomID).getRoomAddress();
        }
        currentSocket.sendPacket(m);
    }


    private JsonObject prepareWelcomeMessage() {
        JsonObject welcomeMessage = client.getBaseMessageStub();
        welcomeMessage.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_TYPE_WELCOME);
        return welcomeMessage;
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
        return Optional.empty();
    }

    public void registerRoom(ChatRoom chatRoom) {
        if (roomsMap.containsKey(chatRoom.getChatID())) throw new RuntimeException("Duplicate chat room");
        roomsMap.put(chatRoom.getChatID(), chatRoom);
        roomIDs.add(chatRoom.getChatID());
    }

    public QueueThread(AbstractClient client, ChatRoom commonMulticastChannel) throws IOException {
        this.group = InetAddress.getByName(COMMON_GROUPNAME);
        this.commonMulticastChannel = commonMulticastChannel;
        this.currentSocket = this.commonMulticastChannel.getDedicatedRoomSocket();
        registerRoom(commonMulticastChannel);
        this.client = client;
    }

    /**
     * The thread takes care of the queue, waits for messages on the socket and is in charge
     * of sending messages
     */
    @Override
    public void run() {
        //client.print("QueueThread bootstrapped");
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        MulticastMessage nextMessage = null;
        JsonObject outgoingMessage;
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        boolean packetReceived = false;
        while (true) {
            cycleRooms();
            //TODO send messages, check queue

            if (!currentRoom.isRoomFinalized()) currentRoom.checkRoomStatus();


            packetReceived = currentRoom.getDedicatedRoomSocket().receive(packet);
            if (!packetReceived) {
                continue;
            }
            String jsonString = new String(packet.getData(), 0, packet.getLength());
            //TODO fix
            JsonObject jsonInboundMessage = JsonParser.parseString(jsonString).getAsJsonObject();
            MulticastMessage inbound = gson.fromJson(jsonInboundMessage, MulticastMessage.class);
            System.out.println(inbound.toJSONString());
            int messageType = inbound.getMessageType();
            int sender = inbound.getSenderID();


            //TODO check based on message type
            if (sender == this.client.getID())
                continue;

            switch (messageType) {
                //Actionable messages
                case MESSAGE_TYPE_HELLO -> {
                    client.addEvent(new GenericNotifyEvent("Received an hello from #" + sender + " replying with WELCOME"));
                    onlineClients.add(sender);
                    MulticastMessage welcome = MulticastMessage.getWelcomeMessage(this.client.getID());

                    sendMessage(welcome, commonMulticastChannel);
                }
                case MESSAGE_TYPE_WELCOME -> {
                    String prompt = "Received a WELCOME from #" + sender + "\nAdded client " + sender + " to the list of known clients";
                    client.addEvent(new GenericNotifyEvent(prompt));
                    onlineClients.add(sender);
                }
                case MESSAGE_TYPE_JOIN_ROOM_ACCEPT -> { //sent only to who created the room
                    int chatRoomID = jsonInboundMessage.get(ROOM_ID_PROPERTY_NAME).getAsInt();
                    client.addEvent(new GenericNotifyEvent("Client #" + sender + " agreed to participate in the chat room"));
                    addParticipantToRoom(chatRoomID, sender);
                }
                case MESSAGE_TYPE_CREATE_ROOM -> {
                    int roomID = jsonInboundMessage.get(ROOM_ID_PROPERTY_NAME).getAsInt();
                    AbstractEvent eventToProcess = new ReplyToRoomRequestEvent(this.client.getID(), roomID, sender, client.getBaseMessageStub(), "y", "n");
                    client.addEvent(eventToProcess);
                    System.out.println(client.eventsToProcess.size());
                }
                case ROOM_MESSAGE -> {
                    // int chatRoomID = jsonInboundMessage.get(ROOM_ID_PROPERTY_NAME).getAsInt();


                }
                //append to relevant queue
            }


        }
    }
}

package Messages;

import java.io.IOException;
import java.net.*;

import Events.AbstractEvent;
import Events.GenericNotifyEvent;
import Peer.AbstractClient;
import Events.ReplyToRoomRequestEvent;
import Peer.ChatClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.*;

import static utils.Constants.*;

public class QueueThread implements QueueManager {

    private static final int SOCKET_TIMEOUT = 100;
    private InetAddress group;
    private Set<Integer> onlineClients = new TreeSet<Integer>();
    private final AbstractClient client;
    //private List<ChatRoom> roomsList = Collections.synchronizedList(new ArrayList<>());
    private Map<Integer, ChatRoom> roomsMap = Collections.synchronizedMap(new HashMap<>());
    private MyMulticastSocketWrapper commonMulticastChannel;
    private MyMulticastSocketWrapper currentSocket = null;
    private ChatRoom currentRoom = null;

    private List<Integer> roomIDs = Collections.synchronizedList(new ArrayList<>());
    private int currentIDIndex = 0;


    private void cycleRooms() {
        currentRoom = roomsMap.get(roomIDs.get(currentIDIndex));
        currentIDIndex = currentIDIndex + 1 == roomIDs.size() ? 0 : currentIDIndex + 1;
    }


    @Override
    public void addRoom(ChatRoom room) {
        if (roomsMap.containsKey(room.getChatID())) throw new RuntimeException("Duplicate chat room");
        roomsMap.put(room.getChatID(), room);
    }

    /**
     * @param m
     * @param room
     */
    @Override
    public void sendMessage(Message m, ChatRoom room) {

    }

    public void sendMessage(Message m, int roomID) throws IOException {
        String pureJSON = m.toString();
        InetAddress destination = null;
        synchronized (roomsMap) {
            if (!roomsMap.containsKey(roomID))
                throw new RuntimeException("No such room");
            destination = roomsMap.get(roomID).getRoomAddress();
        }
        DatagramPacket packet = new DatagramPacket(pureJSON.getBytes(), pureJSON.length(), destination, GROUP_PORT);
        currentSocket.sendPacket(pureJSON);
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
        roomsMap.put(chatRoom.getChatID(), chatRoom);
    }

    public QueueThread(AbstractClient client, MyMulticastSocketWrapper commonMulticastChannel) throws IOException {
        this.group = InetAddress.getByName(COMMON_GROUPNAME);
        this.commonMulticastChannel = commonMulticastChannel;
        currentSocket = this.commonMulticastChannel;
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
        Message nextMessage = null;
        JsonObject outgoingMessage;
        while (true) {
            try {
                cycleRooms();
               //TODO send messages

                try {
                    currentSocket.receive(packet);
                    String jsonString = new String(packet.getData(), 0, packet.getLength());
                    JsonObject jsonInboundMessage = JsonParser.parseString(jsonString).getAsJsonObject();
                    int messageType = jsonInboundMessage.get(MESSAGE_TYPE_FIELD_NAME).getAsInt();
                    int sender = jsonInboundMessage.get(MESSAGE_PROPERTY_FIELD_CLIENTID).getAsInt();
                    if ((jsonInboundMessage.has(MESSAGE_INTENDED_RECIPIENT) &&
                            jsonInboundMessage.get(MESSAGE_INTENDED_RECIPIENT).getAsInt() != this.client.getID())
                            || sender == this.client.getID())
                        continue;

                    switch (messageType) {
                        //Actionable messages
                        case MESSAGE_TYPE_HELLO -> {
                            client.addEvent(new GenericNotifyEvent("Received an hello from #" + sender + " replying with WELCOME"));
                            onlineClients.add(sender);
                            JsonObject welcome = prepareWelcomeMessage();
                            sendMessage(welcome);
                        }
                        case MESSAGE_TYPE_WELCOME -> {
                            String prompt = "Received a WELCOME from #" + sender + "\nAdded client " + sender + " to the list of known clients";
                            client.addEvent(new GenericNotifyEvent(prompt));
                            onlineClients.add(sender);
                        }
                        case MESSAGE_TYPE_JOIN_ROOM_ACCEPT -> { //sent only to who created the room
                            int chatRoomID = jsonInboundMessage.get(ROOM_ID_PROPERTY_NAME).getAsInt();
                            client.addEvent(new GenericNotifyEvent("Client #" + sender + " agreed to participate in the chat room"));
                            middleware.addParticipantToRoom(chatRoomID, sender);
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


                } catch (SocketTimeoutException e) {
                    //System.out.println("Socket timed out " + System.currentTimeMillis());
                } catch (IOException e) {
                    System.out.println(e);
                }


            } catch (SocketException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                System.out.println("Send failure");
                System.out.println(e);
            }
        }
    }
}

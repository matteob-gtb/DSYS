package Messages;

import java.io.IOException;
import java.net.*;

import Peer.AbstractClient;
import Peer.Event;
import Peer.ReplyToRoomRequest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.*;

import static java.lang.System.exit;
import static utils.Constants.*;

public class QueueThread implements Runnable {

    private static final int SOCKET_TIMEOUT = 100;
    private MulticastSocket socket;
    private final Middleware middleware;
    private InetAddress group;
    private ArrayList<Integer> knownClients;
    private int CLIENT_ID;
    private SocketAddress addr;
    private NetworkInterface interfaceName;
    private Set<Integer> onlineClients = new TreeSet<Integer>();
    private final AbstractClient client;

    /*

    Sanity check
     */
    private void sendMessage(JsonObject outgoingMessage) throws IOException {
        if (!outgoingMessage.has(MESSAGE_PROPERTY_FIELD_CLIENTID) || !outgoingMessage.has(MESSAGE_TYPE_FIELD_NAME)) {
            throw new RuntimeException("Badly Formatted Message");
        }
        String pureJSON = outgoingMessage.toString();
        DatagramPacket packet = new DatagramPacket(pureJSON.getBytes(), pureJSON.length(), this.group, GROUP_PORT);
        socket.send(packet);
    }


    private JsonObject prepareWelcomeMessage() {
        JsonObject welcomeMessage = client.getBaseMessageStub();
        welcomeMessage.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_TYPE_WELCOME);
        //TODO ADD KNOWN CLIENTS
        return welcomeMessage;
    }

    public Set<Integer> getOnlineClients() {
        return onlineClients;
    }

    public QueueThread(Middleware mid, int CLIENT_ID) throws IOException {
        this.group = InetAddress.getByName(GROUPNAME);
        this.knownClients = new ArrayList<>();
        this.CLIENT_ID = CLIENT_ID;
        this.client = mid.getClient();
        boolean socketCreated = false;
        while (!socketCreated) {
            try {
                String interfaceName = "eth0";
                SocketAddress socketAddress = new InetSocketAddress(group, GROUP_PORT);
                NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
                boolean found = false;
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements() && !found) {
                    NetworkInterface netInt = interfaces.nextElement();
                    Enumeration<InetAddress> inetAddresses = netInt.getInetAddresses();
                    for (Iterator<InetAddress> it = inetAddresses.asIterator(); it.hasNext(); ) {
                        InetAddress address = it.next();
                        if (address instanceof Inet4Address && netInt.supportsMulticast()) {
                            networkInterface = NetworkInterface.getByInetAddress(address);
                            System.out.println("Selected interface: " + networkInterface.getName());
                            found = true;
                            break;
                        }
                    }
                }

                socket = new MulticastSocket(GROUP_PORT);
                //socket.joinGroup(group);
                socket.joinGroup(socketAddress, networkInterface);
                socketCreated = true;
            } catch (SocketException e) {
                exit(1);
            }
        }

        this.middleware = mid;
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
        Optional<JsonObject> nextMessage = Optional.empty();
        JsonObject outgoingMessage;
        while (true) {
            try {
                nextMessage = middleware.getFirstOutgoingMessages();
                while (nextMessage.isPresent()) {
                    outgoingMessage = nextMessage.get();
                    sendMessage(outgoingMessage);
                    nextMessage = middleware.getFirstOutgoingMessages();
                }

                socket.setSoTimeout(SOCKET_TIMEOUT);
                try {
                    socket.receive(packet);
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
                            client.print("Received an hello from " + sender + " replying with WELCOME");
                            onlineClients.add(sender);
                            JsonObject welcome = prepareWelcomeMessage();
                            sendMessage(welcome);
                        }
                        case MESSAGE_TYPE_WELCOME -> {
                            client.print("Received a WELCOME from #" + sender);
                            client.print("Added client " + sender + " to the list of known clients");
                            onlineClients.add(sender);
                        }
                        case MESSAGE_TYPE_JOIN_ROOM_ACCEPT -> { //sent only to who created the room
                            int chatRoomID = jsonInboundMessage.get(ROOM_ID_PROPERTY_NAME).getAsInt();
                            middleware.addParticipantToRoom(chatRoomID, sender);
                        }
                        case MESSAGE_TYPE_CREATE_ROOM -> {
                            client.print("Client " + sender + " created a new room");
                            int roomID = jsonInboundMessage.get(ROOM_ID_PROPERTY_NAME).getAsInt();

                            Event eventToProcess = new ReplyToRoomRequest(roomID, sender, client.getBaseMessageStub(), "y", "n");
                            client.addEvent(eventToProcess);

//                            String outcome = client.askUserCommand("Do you want to join [y/n]?", "n", "y", "n");
//                            if (outcome.equalsIgnoreCase("y")) {
//                                client.print("Joining room #" + roomID);
//                                JsonObject message = client.getBaseMessageStub();
//                                message.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_TYPE_JOIN_ROOM_ACCEPT);
//                                message.addProperty(MESSAGE_INTENDED_RECIPIENT, sender);
//                                sendMessage(message);
//                                ChatRoom room = new ChatRoom(roomID);
//                                room.addParticipant(sender);
//                                middleware.registerRoom(room);
//                                //SEND JOIN ROOM
//                            } else {
//                                client.print("Refusing to join room " + roomID);
//                            }
                        }
                        case ROOM_MESSAGE -> {

                        }
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

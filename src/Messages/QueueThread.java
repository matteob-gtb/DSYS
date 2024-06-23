package Messages;

import java.io.IOException;
import java.net.*;

import Peer.AbstractClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import static java.lang.System.exit;
import static utils.Constants.*;

public class QueueThread implements Runnable {

    private static final int SOCKET_TIMEOUT = 1000;
    private MulticastSocket socket;
    private final Middleware middleware;
    private InetAddress group;
    private ArrayList<Integer> knownClients;
    private int CLIENT_ID;
    private int port = -1;
    private SocketAddress addr;
    private NetworkInterface interfaceName;
    private Set<Integer> onlineClients;
    private final AbstractClient client;

    /*

    Sanity check
     */
    private void sendMessage(JsonObject outgoingMessage) throws IOException {
        if (!outgoingMessage.has(MESSAGE_PROPERTY_FIELD_CLIENTID) || !outgoingMessage.has(MESSAGE_TYPE_FIELD_NAME))
            throw new RuntimeException("Badly Formatted Message");
        String pureJSON = outgoingMessage.toString();
        DatagramPacket packet = new DatagramPacket(pureJSON.getBytes(), pureJSON.length(), this.group, this.port);
        socket.send(packet);
    }



    private JsonObject prepareWelcomeMessage() {
        JsonObject welcomeMessage = getBaseMessageStub();
        welcomeMessage.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_TYPE_WELCOME);
        //TODO ADD KNOWN CLIENTS
        return welcomeMessage;
    }


    public QueueThread(Middleware mid, int CLIENT_ID) throws IOException {
        this.group = InetAddress.getByName(GROUPNAME);
        this.knownClients = new ArrayList<>();
        this.CLIENT_ID = CLIENT_ID;
        this.client = mid.getClient();
        boolean socketCreated = false;
        while (!socketCreated) {
            try {
                port = GROUP_PORT;
                socket = new MulticastSocket(port);
                socket.joinGroup(group);
                socketCreated = true;
            } catch (SocketException e) {
                exit(1);
            }
        }

        // System.out.println("Client #" + this.CLIENT_ID + " online, announcing self...");
        this.socket = socket;
        this.middleware = mid;
    }

    /**
     * The thread takes care of the queue, waits for messages on the socket and is in charge
     * of sending messages
     */
    @Override
    public void run() {
        client.print("QueueThread bootstrapped");
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
                    //System.out.println("Sent message: " + pureJSON);
                    nextMessage = middleware.getFirstOutgoingMessages();
                }

                socket.setSoTimeout(SOCKET_TIMEOUT);
                try {
                    socket.receive(packet);
                    //System.out.println("Received message: " + new String(packet.getData()));
                    String jsonString = new String(packet.getData(), 0, packet.getLength());
                    JsonObject jsonInboundMessage = JsonParser.parseString(jsonString).getAsJsonObject();
                    int messageType = jsonInboundMessage.get(MESSAGE_TYPE_FIELD_NAME).getAsInt();
                    int sender = jsonInboundMessage.get(MESSAGE_PROPERTY_FIELD_CLIENTID).getAsInt();

                    if ((jsonInboundMessage.has(MESSAGE_INTENDED_RECIPIENT) &&
                            jsonInboundMessage.get(MESSAGE_INTENDED_RECIPIENT).getAsInt() != this.client.getID())
                            || sender == this.client.getID())
                        continue;

                    System.out.println(jsonString);
                    System.out.println("Thread received an inbound message");
                    // TODO MESSAGE HANDLING LOGIC

                    switch (messageType) {
                        //Actionable messages
                        case MESSAGE_TYPE_HELLO -> {
                            JsonObject welcome = prepareWelcomeMessage();
                            sendMessage(welcome);
                        }
                        case MESSAGE_TYPE_WELCOME -> {
                            int clientID = jsonInboundMessage.get(MESSAGE_PROPERTY_FIELD_CLIENTID).getAsInt();
                            client.print("Added client " + clientID + " to the list of known clients");
                            knownClients.add(clientID);
                        }
                        case MESSAGE_TYPE_CREATE_ROOM -> {
                            client.print("Client " + sender + " created a new room");
                            String outcome = client.askUserCommand("Do you want to join [y/n]?", "y", "n");
                            if (outcome.equalsIgnoreCase("y")) {
                                JsonObject message = getBaseMessageStub();
                                message.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_JOIN_ROOM_ACK);
                                message.addProperty(MESSAGE_INTENDED_RECIPIENT, sender);
                                //SEND JOIN ROOM
                            } else {

                            }
                        }
                    }


                } catch (SocketTimeoutException e) {
                    // System.out.println("Socket timed out " + System.currentTimeMillis());
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

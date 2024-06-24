package Messages;

import java.io.IOException;
import java.net.*;

import Events.AbstractEvent;
import Events.GenericNotifyEvent;
import Peer.AbstractClient;
import Events.ReplyToRoomRequestEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.*;

import static java.lang.System.exit;
import static utils.Constants.*;

public class QueueThread implements Runnable {

    private static final int SOCKET_TIMEOUT = 100;
    private final Middleware middleware;
    private InetAddress group;
    private Set<Integer> onlineClients = new TreeSet<Integer>();
    private final AbstractClient client;
    private List<ChatRoom> roomsList = Collections.synchronizedList(new ArrayList<>());


    public void addRoom(ChatRoom room) {
        roomsList.add(room);
    }

    /*

    Sanity check
     */
    private void sendMessage(JsonObject outgoingMessage) throws IOException {
        if (!outgoingMessage.has(MESSAGE_PROPERTY_FIELD_CLIENTID) || !outgoingMessage.has(MESSAGE_TYPE_FIELD_NAME)) {
            throw new RuntimeException("Badly Formatted Message");
        }
        String pureJSON = outgoingMessage.toString();
        DatagramPacket packet = new DatagramPacket(pureJSON.getBytes(), pureJSON.length(), this.group, GROUP_PORT);
        middleware.sendMulticastMessage(packet);
    }


    private JsonObject prepareWelcomeMessage() {
        JsonObject welcomeMessage = client.getBaseMessageStub();
        welcomeMessage.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_TYPE_WELCOME);
        return welcomeMessage;
    }

    public Set<Integer> getOnlineClients() {
        return onlineClients;
    }

    public QueueThread(Middleware mid, int CLIENT_ID) throws IOException {
        this.group = InetAddress.getByName(GROUPNAME);
        this.client = mid.getClient();
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

                genericCommsMulticast.setSoTimeout(SOCKET_TIMEOUT);
                try {
                    genericCommsMulticast.receive(packet);
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
                            AbstractEvent eventToProcess = new ReplyToRoomRequestEvent(roomID, sender, client.getBaseMessageStub(), "y", "n");
                            client.addEvent(eventToProcess);
                            System.out.println(client.eventsToProcess.size());
                        }
                        case ROOM_MESSAGE -> {
                            int chatRoomID = jsonInboundMessage.get(ROOM_ID_PROPERTY_NAME).getAsInt();

                            if (middleware.getChatRoom(chatRoomID).isEmpty()){};
                        }
                        //append to relevant queue
                    }



            } catch (SocketTimeoutException e) {
                //System.out.println("Socket timed out " + System.currentTimeMillis());
            } catch (IOException e) {
                System.out.println(e);
            }


        } catch(SocketException e){
            throw new RuntimeException(e);
        } catch(IOException e){
            System.out.println("Send failure");
            System.out.println(e);
        }
    }
}
}

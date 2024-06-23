package Messages;

import java.io.IOException;
import java.net.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import javax.swing.plaf.synth.SynthUI;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

import static utils.Constants.GROUPNAME;
import static utils.Constants.GROUP_PORT;

public class QueueThread implements Runnable {
    private static final int SOCKET_TIMEOUT = 1000;
    private MulticastSocket socket;
    private final Middleware middleware;
    private InetAddress group;
    private ArrayList<Integer> knownClients;
    private int CLIENT_ID;

    public QueueThread(Middleware mid, int CLIENT_ID) throws IOException {
        this.group = InetAddress.getByName(GROUPNAME);
        this.knownClients = new ArrayList<>();
        this.CLIENT_ID = CLIENT_ID;

        boolean socketCreated = false;
        while (!socketCreated) {
            try {
                socket = new MulticastSocket(GROUP_PORT);
                socket.joinGroup(group);
                socketCreated = true;
            } catch (SocketException e) {
                System.out.println(e);
            }
        }

        System.out.println("Client #" + this.CLIENT_ID + " online, announcing self...");
        this.socket = socket;
        this.middleware = mid;
    }

    /**
     * The thread takes care of the queue, waits for messages on the socket and is in charge
     * of sending messages
     */
    @Override
    public void run() {
        System.out.println("QueueThread bootstrapped");
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        Optional<JsonObject> nextMessage = Optional.empty();
        JsonObject outgoingMessage;
        while (true) {
            try {
                nextMessage = middleware.getFirstOutgoingMessages();
                while (nextMessage.isPresent()) {
                    System.out.println("Sending message...");
                    outgoingMessage = nextMessage.get();
                    String pureJSON = outgoingMessage.toString();
                    buffer = new byte[1024];
                    packet = new DatagramPacket(buffer, buffer.length, this.group, GROUP_PORT);
                    socket.send(packet);
                    nextMessage = middleware.getFirstOutgoingMessages();
                }

                socket.setSoTimeout(SOCKET_TIMEOUT);
                try {
                    // Receive the datagram packet
                    socket.receive(packet);

                    String jsonString = new String(packet.getData(), 0, packet.getLength());

                    JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
                    System.out.println("Thread received an inbound message");
                    // TODO MESSAGE HANDLING LOGIC


                } catch (SocketTimeoutException e) {
                    System.out.println("Socket timed out");
                } catch (IOException e) {

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

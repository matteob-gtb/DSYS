package Peer;

import com.google.gson.JsonObject;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;

public abstract class AbstractClient {
    protected static final int SOCKET_PORT_LOW = 2000, SOCKET_PORT_HIGH = 5000, GROUP_PORT = 5000;
    protected static final int RCV_BUFFER_SIZE = 1024;
    protected static final String MESSAGE_TYPE_FIELD_NAME = "MESSAGE_TYPE";
    protected static final String MESSAGE_PROPERTY_FIELD_CLIENTID = "ID";
    protected static final String MESSAGE_PROPERTY_FIELD_KNOWNCLIENTS = "PEERS ";
    protected static final int MESSAGE_TYPE_HELLO = 1;
    protected static final int MESSAGE_TYPE_WELCOME = 2;

    protected static final int MESSAGE_TYPE_CREATE_ROOM = 3;
    protected static final int MESSAGE_TYPE_JOIN_ROOM = 4;
    protected static final int MESSAGE_TYPE_ANNOUNCE_LEAVE = 5;

    protected static final String GROUPNAME = "228.5.6.254";
    protected InetAddress group;
    protected MulticastSocket socket;
    protected ArrayList<Integer> knownClients = new ArrayList<>();

    protected int CLIENT_ID;

    protected void sendMessage(JsonObject messageObject) throws IOException {
        if (!messageObject.has(MESSAGE_TYPE_FIELD_NAME) || !messageObject.has(MESSAGE_PROPERTY_FIELD_CLIENTID))
            throw new RuntimeException("Badly Formed Message");

        String messageString = messageObject.toString();
        DatagramPacket packet = new DatagramPacket(messageString.getBytes(),messageString.length(),this.group,GROUP_PORT);
        socket.send(packet);
    }

    public abstract void stayIdleAndReceive() throws IOException;

    public abstract void announceSelf() throws IOException;

    public abstract void createRoom() throws IOException;
}

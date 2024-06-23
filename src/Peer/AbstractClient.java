package Peer;

import Messages.MessageMiddleware;
import com.google.gson.JsonObject;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;

import static utils.Constants.MESSAGE_PROPERTY_FIELD_CLIENTID;

public abstract class AbstractClient {

    protected InetAddress group;
    protected MulticastSocket socket;
    protected ArrayList<Integer> knownClients = new ArrayList<>();

    protected int CLIENT_ID;

    public int getID(){
        return CLIENT_ID;
    }

    protected MessageMiddleware messageMiddleware = null;


    public abstract void stayIdleAndReceive() throws IOException;

    public abstract void announceSelf() throws IOException;

    public abstract void createRoom() throws IOException;

    public abstract void print(String queueThreadBootstrapped);

    public abstract String askUserCommand(String commandPrompt, String... choices);

    public JsonObject getBaseMessageStub() {
        JsonObject msg = new JsonObject();
        msg.addProperty(MESSAGE_PROPERTY_FIELD_CLIENTID, CLIENT_ID);
        return msg;
    }

}

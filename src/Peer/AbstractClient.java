package Peer;

import Messages.MessageMiddleware;
import com.google.gson.JsonObject;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static utils.Constants.MESSAGE_PROPERTY_FIELD_CLIENTID;

public abstract class AbstractClient {

    protected int CLIENT_ID;
    protected List<Event> eventsToProcess = Collections.synchronizedList(new ArrayList<Event>());

    public int getID(){
        return CLIENT_ID;
    }
    public void addEvent(Event event) {
        eventsToProcess.add(event);
    }
    protected MessageMiddleware messageMiddleware = null;

    public abstract void announceSelf() throws IOException;

    public abstract void print(String queueThreadBootstrapped);

    public abstract String askUserCommand(String commandPrompt, String defaultC,String... choices);

    public JsonObject getBaseMessageStub() {
        JsonObject msg = new JsonObject();
        msg.addProperty(MESSAGE_PROPERTY_FIELD_CLIENTID, CLIENT_ID);
        return msg;
    }


}

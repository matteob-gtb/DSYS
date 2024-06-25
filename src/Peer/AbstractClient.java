package Peer;

import Events.AbstractEvent;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static utils.Constants.MESSAGE_PROPERTY_FIELD_CLIENTID;

public abstract class AbstractClient {

    protected int CLIENT_ID;
    //TODO remove
    public List<AbstractEvent> eventsToProcess = Collections.synchronizedList(new ArrayList<AbstractEvent>());

    public int getID(){
        return CLIENT_ID;
    }
    public void addEvent(AbstractEvent event) {
        eventsToProcess.add(event);
    }


    public abstract void announceSelf() throws IOException;

    public abstract void print(String queueThreadBootstrapped);

    public abstract String askUserCommand(String commandPrompt, String defaultC,String... choices);

    public JsonObject getBaseMessageStub() {
        JsonObject msg = new JsonObject();
        msg.addProperty(MESSAGE_PROPERTY_FIELD_CLIENTID, CLIENT_ID);
        return msg;
    }


}

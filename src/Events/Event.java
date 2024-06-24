package Events;

import Messages.Message;
import Messages.MyMulticastSocketWrapper;
import com.google.gson.JsonObject;

import java.util.Optional;

public interface Event {

    public void executeEvent();

    public Optional<Message> executeEvent(String command);

    public String eventPrompt();
}

package Events;

import Messages.AbstractMessage;
import Messages.MulticastMessage;

import java.util.Optional;

public interface Event {

    public void executeEvent();

    public Optional<AbstractMessage> executeEvent(String command);

    public String eventPrompt();
}

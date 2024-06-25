package Events;

import Messages.MulticastMessage;

import java.util.Optional;

public interface Event {

    public void executeEvent();

    public Optional<MulticastMessage> executeEvent(String command);

    public String eventPrompt();
}

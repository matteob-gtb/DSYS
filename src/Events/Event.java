package Events;

import Messages.AbstractMessage;

import java.util.Optional;

public interface Event {

    public void executeEvent();

    public Optional<AbstractMessage> executeEvent(String command);

    public String eventPrompt();
}

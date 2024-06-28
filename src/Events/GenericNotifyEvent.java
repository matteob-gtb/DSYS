package Events;

import Messages.AbstractMessage;
import Messages.MulticastMessage;

import java.util.Optional;

public class GenericNotifyEvent extends AbstractEvent {
    private final String prompt;

    public GenericNotifyEvent(String prompt) {
        super(false);
        this.prompt = prompt;
    }



    /**
     *
     */
    @Override
    public void executeEvent() {

    }

    /**
     * @param command
     * @return
     */
    @Override
    public Optional<AbstractMessage> executeEvent(String command) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * @return
     */
    @Override
    public String eventPrompt() {
        return prompt;
    }
}

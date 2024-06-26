package Events;

import Messages.MulticastMessage;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Optional;

import static utils.Constants.*;

public class ReplyToRoomRequestEvent extends AbstractEvent {

    private final int clientID, roomID, recipientID;
    private final String[] acceptableOutcomes;
    private final JsonObject msg;

    public ReplyToRoomRequestEvent(int clientID, int roomID, int sender, JsonObject prepackagedMessage, String... acceptableOutcomes) {
        super(true);
        this.clientID = clientID;
        this.roomID = roomID;
        this.recipientID = sender;
        this.msg = prepackagedMessage;
        this.acceptableOutcomes = acceptableOutcomes;
    }

    /**
     *
     */

    public String eventPrompt() {
        return "Client #" + this.recipientID + " asked to join room #" + this.roomID + "\nDo you want to join [y/n]?";
    }

    /**
     *
     */
    @Override
    public void executeEvent() {
        throw new UnsupportedOperationException("Not supported");

    }

    @Override
    public Optional<MulticastMessage> executeEvent(String command) {
        Optional<String> foundMatch = Arrays.stream(acceptableOutcomes).filter(x -> x.contains(command)).findFirst();
        if (foundMatch.isEmpty())
            return Optional.empty();
        int type = -1;
        if (command.equals("y"))
            type = MESSAGE_TYPE_JOIN_ROOM_ACCEPT;
        else
            type = MESSAGE_TYPE_JOIN_ROOM_REFUSE;
        MulticastMessage msg = new MulticastMessage(
                this.clientID,
                type,
                this.roomID
        );

        return Optional.of(msg);
    }
}

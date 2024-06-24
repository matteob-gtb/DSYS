package Events;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Optional;

import static utils.Constants.*;

public class ReplyToRoomRequestEvent extends AbstractEvent {

    private final int roomID, sender;
    private final String[] acceptableOutcomes;
    private final JsonObject msg;

    public ReplyToRoomRequestEvent(int roomID, int sender, JsonObject prepackagedMessage, String... acceptableOutcomes) {
        super(true);
        this.roomID = roomID;
        this.sender = sender;
        this.msg = prepackagedMessage;
        this.acceptableOutcomes = acceptableOutcomes;
    }

    /**
     *
     */

    public String eventPrompt() {
        return "Client #" + this.sender + " asked to join room #" + this.roomID + "\nDo you want to join [y/n]?";
    }

    /**
     *
     */
    @Override
    public void executeEvent() {
        throw new UnsupportedOperationException("Not supported");

    }

    @Override
    public Optional<JsonObject> executeEvent(String command) {
        Optional<String> foundMatch = Arrays.stream(acceptableOutcomes).filter(x -> x.contains(command)).findFirst();
        if (foundMatch.isEmpty())
            return Optional.empty();
        if (command.equals("y"))
            msg.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_TYPE_JOIN_ROOM_ACCEPT);
        else
            msg.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_TYPE_JOIN_ROOM_REFUSE);
        //Send Message
        msg.addProperty(MESSAGE_INTENDED_RECIPIENT, this.sender);
        msg.addProperty(ROOM_ID_PROPERTY_NAME, this.roomID);

        return Optional.of(msg);
    }
}

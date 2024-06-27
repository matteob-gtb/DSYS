package Events;

import ChatRoom.ChatRoom;
import Messages.MulticastMessage;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Optional;

import static utils.Constants.*;

public class ReplyToRoomRequestEvent extends AbstractEvent {

    private final int clientID, roomID, recipientID;
    private final String[] acceptableOutcomes;
    private final JsonObject msg;
    private final String GROUPNAME;

    public ReplyToRoomRequestEvent(int clientID, String GROUPNAME, int roomID, int sender, JsonObject prepackagedMessage, String... acceptableOutcomes) {
        super(true);
        this.GROUPNAME = GROUPNAME.replace("\\/","");
        this.clientID = clientID;
        this.roomID = roomID;
        this.recipientID = sender;
        this.msg = prepackagedMessage;
        this.acceptableOutcomes = acceptableOutcomes;
    }

    public ChatRoom createRoomReference() {
        return new ChatRoom(
                clientID,
                GROUPNAME
        );
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
        if (command.equals("y")) {
            System.out.println("Accepted the invitation to room " + this.roomID);
            type = MESSAGE_TYPE_JOIN_ROOM_ACCEPT;
        } else {
            System.out.println("Refused the invitation to room " + this.roomID);
            type = MESSAGE_TYPE_JOIN_ROOM_REFUSE;
        }
        MulticastMessage msg = new MulticastMessage(
                this.clientID,
                type,
                this.roomID
        );
        return Optional.of(msg);
    }
}

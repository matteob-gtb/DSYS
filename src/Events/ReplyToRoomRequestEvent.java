package Events;

import ChatRoom.ChatRoom;
import Messages.AbstractMessage;
import Messages.AnonymousMessages.AcceptRoomRequest;
import Messages.AnonymousMessages.RefuseRoomRequest;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Optional;

public class ReplyToRoomRequestEvent extends AbstractEvent {

    private final int clientID, roomID, recipientID, ownerID;
    private final String[] acceptableOutcomes;
    private final String GROUPNAME;


    public ReplyToRoomRequestEvent(int roomOwner, int clientID, String GROUPNAME, int roomID, int sender, String... acceptableOutcomes) {
        super(true);
        this.ownerID = roomOwner;
        this.GROUPNAME = GROUPNAME.replace("\\/", "");
        this.clientID = clientID;
        this.roomID = roomID;
        this.recipientID = sender;
        this.acceptableOutcomes = acceptableOutcomes;
    }

    public ChatRoom createRoomReference() {
        return new ChatRoom(
                this.ownerID,
                this.roomID,
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
    public Optional<AbstractMessage> executeEvent(String command) {
        Optional<String> foundMatch = Arrays.stream(acceptableOutcomes).filter(x -> x.contains(command)).findFirst();
        if (foundMatch.isEmpty())
            return Optional.empty();
        AbstractMessage outcome = null;
        if (command.equalsIgnoreCase("y")) {
            System.out.println("Accepted the invitation to room " + this.roomID);
            outcome = new AcceptRoomRequest(this.clientID, this.roomID);
        } else if (command.equalsIgnoreCase("n")) {
            System.out.println("Refused the invitation to room " + this.roomID);
            outcome = new RefuseRoomRequest(this.clientID, this.roomID);
        }
        return Optional.ofNullable(outcome);
    }
}

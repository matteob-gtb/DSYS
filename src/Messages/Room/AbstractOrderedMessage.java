package Messages.Room;

import Messages.AbstractMessage;
import VectorTimestamp.VectorTimestamp;

import static utils.Constants.MESSAGE_TYPE_ROOM_MESSAGE;

public class AbstractOrderedMessage extends AbstractMessage {

    protected VectorTimestamp vectorTimestamp;

    public AbstractOrderedMessage() {}

    public AbstractOrderedMessage(int clientID, int messageType, int roomID) {
        super(clientID, messageType, roomID);
    }

    public VectorTimestamp getTimestamp() {
        return this.vectorTimestamp;
    }
}

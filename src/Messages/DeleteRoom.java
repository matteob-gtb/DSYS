package Messages;

import Messages.Room.AbstractOrderedMessage;
import VectorTimestamp.VectorTimestamp;

import static utils.Constants.MESSAGE_TYPE_DELETE_ROOM;

public class DeleteRoom extends AbstractOrderedMessage {

    private int sentNTimes = 0;

    public DeleteRoom(int roomID, int senderID, VectorTimestamp timestamp) {
        this.messageType = MESSAGE_TYPE_DELETE_ROOM;
        this.roomID = roomID;
        this.senderID = senderID;
        this.vectorTimestamp = new VectorTimestamp(timestamp);
    }

    @Override
    public boolean canDelete() {
        sentNTimes++;
        return sentNTimes == 5;
    }
}

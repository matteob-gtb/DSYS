package Messages.Room;

import Messages.AbstractMessage;
import VectorTimestamp.VectorTimestamp;


public class AbstractOrderedMessage extends AbstractMessage {

    protected VectorTimestamp vectorTimestamp;

    public AbstractOrderedMessage() {
    }

    public AbstractOrderedMessage(int clientID, int messageType, int roomID) {
        super(clientID, messageType, roomID);
    }

    public VectorTimestamp getTimestamp() {
        return this.vectorTimestamp;
    }

    public void setTimestamp(VectorTimestamp vectorTimestamp) {
        this.vectorTimestamp = vectorTimestamp;
    }

    public boolean equals(Object other) {
        if (other == null) return false;
        if (!(other instanceof AbstractOrderedMessage)) return false;
        AbstractOrderedMessage otherMessage = (AbstractOrderedMessage) other;
        return this.senderID == otherMessage.senderID
                && this.messageType == otherMessage.messageType &&
                this.roomID == otherMessage.roomID
                //CHECK TIMESTAMP
                && this.vectorTimestamp.equals(otherMessage.vectorTimestamp);
    }
}

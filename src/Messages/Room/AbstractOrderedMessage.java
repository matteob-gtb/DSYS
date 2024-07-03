package Messages.Room;

import Messages.AbstractMessage;
import VectorTimestamp.VectorTimestamp;
import com.google.gson.annotations.Expose;

import java.util.HashSet;
import java.util.Set;


public class AbstractOrderedMessage extends AbstractMessage {

    protected VectorTimestamp vectorTimestamp;

    public void setAcked(boolean acked) {
        this.acked = acked;
    }

    public boolean isAcked() {
        return acked;
    }

    @Expose(serialize = false, deserialize = false)
    private boolean acked = false;

    public Integer getAckedBySize() {
        return ackedBy.size();
    }

    public void setAckedBy(int ID) {
        this.ackedBy.add(ID);
    }

    @Expose(serialize = false, deserialize = false)
    private Set<Integer> ackedBy = new HashSet<>();



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

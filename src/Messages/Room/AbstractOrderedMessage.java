package Messages.Room;

import Messages.AbstractMessage;
import VectorTimestamp.VectorTimestamp;
import com.google.gson.annotations.Expose;

import java.util.HashSet;
import java.util.Set;

import static utils.Constants.MIN_RETRANSMIT_WAIT_MS;


public class AbstractOrderedMessage extends AbstractMessage {


    @Expose(serialize = false, deserialize = false)
    protected transient Long milliTimestamp;
    @Expose(serialize = false, deserialize = false)
    protected transient boolean acked = false;
    @Expose(serialize = false, deserialize = false)
    private  transient Set<Integer> ackedBy = new HashSet<>();


    protected VectorTimestamp vectorTimestamp;


    public Long getMilliTimestamp() {
        return milliTimestamp;
    }

    public void setMilliTimestamp(Long milliTimestamp) {
        this.milliTimestamp = milliTimestamp;
    }

    public void setAcked(boolean acked) {
        this.acked = acked;
    }

    public boolean shouldRetransmit() {
        //if acked is false wait at least MIN_RETR
        return !sent || !acked && (System.currentTimeMillis() - milliTimestamp > MIN_RETRANSMIT_WAIT_MS);
    }

    public boolean canDelete() {
        return sent && acked;
    }


    public Integer getAckedBySize() {
        return ackedBy.size();
    }

    public void addAckedBy(int ID) {
        this.ackedBy.add(ID);
    }


    public AbstractOrderedMessage() {
    }

    public AbstractOrderedMessage(int clientID, int messageType, int roomID) {

        super(clientID, messageType, roomID);
        this.milliTimestamp = System.currentTimeMillis();
    }

    public VectorTimestamp getTimestamp() {
        return this.vectorTimestamp;
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

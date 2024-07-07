package Messages.CommonMulticastMessages.Room;

import Messages.CommonMulticastMessages.AbstractMessage;
import VectorTimestamp.VectorTimestamp;
import com.google.gson.annotations.Expose;

import java.util.HashSet;
import java.util.Set;


public class AbstractOrderedMessage extends AbstractMessage {


    @Expose(serialize = false, deserialize = false)
    protected transient Long milliTimestamp;
    @Expose(serialize = false, deserialize = false)
    protected transient boolean acked = false;
    @Expose(serialize = false, deserialize = false)
    private transient Set<Integer> ackedBy = new HashSet<>();


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

    private static final long MS = 1000;
    private static final transient long[] retransmitTimeouts = {MS
            , 2 * MS, 5 * MS, 10 * MS, 12 * MS, 15 * MS, 17 * MS};

    public boolean shouldRetransmit() {
        //if acked is false wait at least MIN_RETR
        return !sent || (!acked && (sentHowManyTimes < retransmitTimeouts.length && System.currentTimeMillis() - milliTimestamp > retransmitTimeouts[sentHowManyTimes]));
    }

    private transient int sentHowManyTimes = 0;

    public boolean canDelete() {
        return sent && (acked || sentHowManyTimes >= retransmitTimeouts.length);
    }

    public void setSent(boolean sent) {
        this.sent = sent;
        this.sentHowManyTimes++;
        this.milliTimestamp = System.currentTimeMillis();
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

package Messages.Room;

import Messages.AbstractMessage;
import VectorTimestamp.VectorTimestamp;

//only used to record previous values of the timestamp at the receiver value,
//in order to reconcile the state after network partitions
public class DummyMessage extends AbstractOrderedMessage {
    public DummyMessage(VectorTimestamp timestamp) {
        this.vectorTimestamp = new VectorTimestamp(timestamp);
    }




}

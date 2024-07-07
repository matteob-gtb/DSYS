package Messages.CommonMulticastMessages.Room;

import VectorTimestamp.VectorTimestamp;
import utils.Constants;

//used to request retransmission of all the messages from one point in time
public class RequestRetransmission extends AbstractOrderedMessage {
    public RequestRetransmission(int senderID, int roomID, VectorTimestamp lastAvailableTimestam) {
        super(senderID, Constants.MESSAGE_TYPE_REQUEST_RTO, roomID);
        this.vectorTimestamp = lastAvailableTimestam;
    }

    //no need to ack rtos
    public boolean canDelete(){
        return sent;
    }


}

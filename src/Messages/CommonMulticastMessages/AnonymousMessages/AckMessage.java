package Messages.CommonMulticastMessages.AnonymousMessages;

import Messages.CommonMulticastMessages.Room.AbstractOrderedMessage;
import VectorTimestamp.VectorTimestamp;
import utils.Constants;

public class AckMessage extends AbstractOrderedMessage {
    //sender id AND message timestamp at a given sender

    public int getRecipientID() {
        return recipientID;
    }

    private final int recipientID;

    public AckMessage(int sender, int recipient, VectorTimestamp t,int roomID) {
        this.messageType = Constants.MESSAGE_TYPE_ACK;
        this.recipientID = recipient;
        this.senderID = sender;
        this.vectorTimestamp = t;
        this.roomID = roomID;
    }

    public boolean canDelete(){
        return true; //acks are not stored
    }

}

package Messages.CommonMulticastMessages.AnonymousMessages;

import Messages.CommonMulticastMessages.Room.AbstractOrderedMessage;
import VectorTimestamp.VectorTimestamp;
import utils.Constants;

import java.net.InetAddress;

public class AckMessage extends AbstractOrderedMessage {
    //sender id AND message timestamp at a given sender

    private transient InetAddress destination;

    public int getRecipientID() {
        return recipientID;
    }

    private final int recipientID;

    public AckMessage(int sender, int recipient, VectorTimestamp t, int roomID, InetAddress destination) {
        this.messageType = Constants.MESSAGE_TYPE_ACK;
        this.recipientID = recipient;
        this.senderID = sender;
        this.vectorTimestamp = t;
        this.roomID = roomID;
        this.destination = destination;
    }

    public boolean canDelete() {
        return true; //acks are not stored
    }

    @Override
    public boolean isUnicast() {
        return true;
    }

    @Override
    public InetAddress getDestinationAddress() {
        return this.destination;
    }
}

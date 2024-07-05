package Messages.Room;

import Messages.AbstractMessage;
import Peer.ChatClient;
import VectorTimestamp.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Comparator;

import static utils.Constants.MESSAGE_TYPE_ROOM_MESSAGE;

public class RoomMulticastMessage extends AbstractOrderedMessage {


    public static class RoomMulticastMessageComparator implements Comparator<RoomMulticastMessage> {
        private final int positionToCompare;

        public RoomMulticastMessageComparator(int positionToCompare) {
            this.positionToCompare = positionToCompare;
        }


        @Override
        public int compare(RoomMulticastMessage o1, RoomMulticastMessage o2) {
            if (o1.getSenderID() != o2.getSenderID())
                throw new RuntimeException("Use Vector clock comparison for this");
            return Integer.compare(o1.getTimestamp().getValueAtPosition(positionToCompare), o2.getTimestamp().getValueAtPosition(positionToCompare));
        }


    }


    public RoomMulticastMessage(int clientID, int roomID) {
        super(clientID, MESSAGE_TYPE_ROOM_MESSAGE, roomID);
        vectorTimestamp = null;
    }

    //Copies are not meant to be acked again
    public RoomMulticastMessage(RoomMulticastMessage toCopy) {
        super(toCopy.senderID, MESSAGE_TYPE_ROOM_MESSAGE, toCopy.roomID);
        vectorTimestamp = new VectorTimestamp(toCopy.vectorTimestamp);
        this.acked = true;
    }


    public RoomMulticastMessage(int clientID,
                                int roomID, VectorTimestamp vectorTimestamp, String payload) {
        super(clientID, MESSAGE_TYPE_ROOM_MESSAGE, roomID);
        this.payload = payload;
        this.vectorTimestamp = vectorTimestamp;
    }


    /**
     * @param o the object to be compared.
     * @return
     */

    public VectorTimestamp getTimestamp() {
        return vectorTimestamp;
    }

    /**
     * @return
     */
    @Override
    public Gson gson() {
        GsonBuilder builder = new GsonBuilder();
        return builder.create();
    }

    public String toChatString() {
        return "Client [" + (this.senderID == ChatClient.ID ? " me " : this.senderID) + "] wrote : [" + this.payload + "] - " + this.vectorTimestamp.toString() + "\n";
    }

    public boolean equals(Object o) {
        if (!(o instanceof RoomMulticastMessage)) return false;
        RoomMulticastMessage other = (RoomMulticastMessage) o;
        return this.senderID == other.senderID && this.getTimestamp().equals(other.getTimestamp());
    }

}

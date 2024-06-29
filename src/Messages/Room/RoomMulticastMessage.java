package Messages.Room;

import Messages.AbstractMessage;
import VectorTimestamp.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Comparator;

import static utils.Constants.MESSAGE_TYPE_ROOM_MESSAGE;

public class RoomMulticastMessage extends AbstractMessage {


    public static class RoomMulticastMessageComparator implements Comparator<RoomMulticastMessage> {

        @Override
        public int compare(RoomMulticastMessage o1, RoomMulticastMessage o2) {
            if (o1.getSenderID() != o2.getSenderID())
                throw new RuntimeException("Use Vector clock comparison for this");
            return Integer.compare(o1.getTimestamp().getValueAtPosition(o1.getSenderID()), o2.getTimestamp().getValueAtPosition(o2.getSenderID()));
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }


    public RoomMulticastMessage(int clientID, int roomID) {
        super(clientID, MESSAGE_TYPE_ROOM_MESSAGE, roomID);
        vectorTimestamp = null;
    }


    public RoomMulticastMessage(int clientID,
                                int roomID, VectorTimestamp vectorTimestamp,String payload) {
        super(clientID, MESSAGE_TYPE_ROOM_MESSAGE, roomID );
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
        return "Client [" + this.senderID + "] wrote : [" + this.payload + "]\n";
    }

}

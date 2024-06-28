package Messages.Room;

import Messages.AbstractMessage;
import VectorTimestamp.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static utils.Constants.MESSAGE_TYPE_ROOM_MESSAGE;

public class RoomMulticastMessage extends AbstractMessage {

    private final VectorTimestamp vectorTimestamp;

    private int sortAccordingToIndex = -1;

    public RoomMulticastMessage(int clientID, int roomID) {
        super(clientID, MESSAGE_TYPE_ROOM_MESSAGE, roomID);
        vectorTimestamp = null;
    }

    public void setSortAccordingToIndex(int partID) {
        this.sortAccordingToIndex = partID;
    }

    public RoomMulticastMessage(int clientID,
                                int roomID, VectorTimestamp vectorTimestamp) {
        super(clientID, MESSAGE_TYPE_ROOM_MESSAGE, roomID);
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
}

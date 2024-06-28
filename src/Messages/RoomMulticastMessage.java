package Messages;

import VectorTimestamp.*;

public class RoomMulticastMessage extends MulticastMessage  {

    private final VectorTimestamp vectorTimestamp;

    private int sortAccordingToIndex = -1;

    public RoomMulticastMessage(int clientID, int type, int roomID) {
        super(clientID, type, roomID);
        vectorTimestamp = null;
    }

    public void setSortAccordingToIndex(int partID) {
        this.sortAccordingToIndex = partID;
    }

    public RoomMulticastMessage(int clientID,
                                int type,
                                int roomID, VectorTimestamp vectorTimestamp) {
        super(clientID, type, roomID);
        this.vectorTimestamp = vectorTimestamp;
    }

    /**
     * @param o the object to be compared.
     * @return
     */

    public Timestamp getTimestamp() {
        return vectorTimestamp;
    }
}

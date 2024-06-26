package Messages;

public class RoomMulticastMessage extends MulticastMessage implements Comparable<RoomMulticastMessage> {

    private int[] vectorTimestamp;

    private int sortAccordingToIndex = -1;

    public RoomMulticastMessage(int clientID, int type, int roomID) {
        super(clientID, type, roomID);
    }

    public void setSortAccordingToIndex(int partID) {
        this.sortAccordingToIndex = partID;
    }

    public RoomMulticastMessage(int clientID,
                            int type,
                            int roomID, int[] vectorTimestamp) {
        super(clientID, type, roomID);
        this.vectorTimestamp = vectorTimestamp;
    }

    /**
     * @param o the object to be compared.
     * @return
     */

    @Override
    public int compareTo(RoomMulticastMessage o) {
        if (o == null) return -1;
        if (!(o instanceof RoomMulticastMessage)) throw new RuntimeException("Bad comparison");
        if (o.roomID != this.roomID || this.sortAccordingToIndex == -1 || o.sortAccordingToIndex != this.sortAccordingToIndex)
            throw new RuntimeException("Objects not comparable");
//        boolean lessOrEqual = false;
//        int strictlyGreater = 0, equal = 0;
//        for (int i = 0; i < this.vectorTimestamp.length; i++) {
//            if (this.vectorTimestamp[i] > o.vectorTimestamp[i]) {
//                strictlyGreater++;
//            }
//            if (this.vectorTimestamp[i] == o.vectorTimestamp[i]) {
//                equal++;
//            }
//        }
//        if (strictlyGreater >= 1) return strictlyGreater;
//        if (equal == 0) return -1; //this comes before
        return Integer.compare(vectorTimestamp[this.sortAccordingToIndex], o.vectorTimestamp[o.sortAccordingToIndex]);

    }
}

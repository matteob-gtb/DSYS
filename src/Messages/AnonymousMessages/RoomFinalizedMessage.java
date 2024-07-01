package Messages.AnonymousMessages;

import Messages.AbstractMessage;
import utils.Constants;

import java.util.Set;

public class RoomFinalizedMessage extends AbstractMessage {
    private String multicastAddress;
    private Set<Integer> participantIds;

    public RoomFinalizedMessage(int clientID, int roomID, Set<Integer> participantIds, String multicastAddress) {
        this.senderID = clientID;
        this.roomID = roomID;
        this.messageType = Constants.MESSAGE_TYPE_ROOM_FINALIZED;
        this.participantIds = participantIds;
        this.multicastAddress = multicastAddress;
    }

    public Set<Integer> getParticipantIds() {
        return participantIds;
    }

}

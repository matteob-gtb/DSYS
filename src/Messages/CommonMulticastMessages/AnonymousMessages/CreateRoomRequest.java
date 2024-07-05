package Messages.CommonMulticastMessages.AnonymousMessages;

import Messages.CommonMulticastMessages.AbstractMessage;
import utils.Constants;

public class CreateRoomRequest extends AbstractMessage {
    private String groupname;

    //TODO add participants

    public CreateRoomRequest(int userID,   int roomID, String roomGroupName) {
        super(userID, Constants.MESSAGE_TYPE_CREATE_ROOM, roomID);
        this.groupname = roomGroupName;
    }
    public String getGroupname() {
        return groupname;
    }
}

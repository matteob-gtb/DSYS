package Messages.AnonymousMessages;

import Messages.AbstractMessage;
import Messages.MulticastMessage;
import utils.Constants;

public class CreateRoomRequest extends AbstractMessage {
    private String groupname;



    public CreateRoomRequest(int userID,   int roomID, String roomGroupName) {
        super(userID, Constants.MESSAGE_TYPE_CREATE_ROOM, roomID);
        this.groupname = roomGroupName;
    }
    public String getGroupname() {
        return groupname;
    }
}

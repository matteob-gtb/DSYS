package Messages.AnonymousMessages;

import Messages.AbstractMessage;
import Messages.MulticastMessage;

public class CreateRoomRequest extends AbstractMessage {
    private String groupname;

    public CreateRoomRequest(int userID, int type, int roomID) {
        super(userID, type, roomID);
    }

    public CreateRoomRequest(int userID, int type, int roomID, String roomGroupName) {
        super(userID, type, roomID);
        this.groupname = roomGroupName;
    }
    public String getGroupname() {
        return groupname;
    }
}

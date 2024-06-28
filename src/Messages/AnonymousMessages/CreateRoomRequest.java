package Messages.AnonymousMessages;

import Messages.MulticastMessage;

public class CreateRoomRequest extends MulticastMessage {
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

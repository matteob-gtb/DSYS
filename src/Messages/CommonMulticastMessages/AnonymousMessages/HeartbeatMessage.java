package Messages.CommonMulticastMessages.AnonymousMessages;

import Messages.CommonMulticastMessages.AbstractMessage;

import static utils.Constants.MESSAGE_TYPE_HEARTBEAT;

public class HeartbeatMessage extends AbstractMessage {

    public HeartbeatMessage(int sender, int room) {
        super(sender, MESSAGE_TYPE_HEARTBEAT, room);
    }

    //do not queue them
    public boolean equals(Object o) {
        return o instanceof HeartbeatMessage;
    }

    //send only once
    public boolean canDelete() {
        return true;
    }


}

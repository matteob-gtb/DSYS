package Messages.CommonMulticastMessages.AnonymousMessages;

import Messages.CommonMulticastMessages.AbstractMessage;
import utils.Constants;

import static utils.Constants.MESSAGE_TYPE_HEARTBEAT;

public class HeartbeatMessage extends AbstractMessage {
    public int getAppVersion() {
        return appVersion;
    }

    private final int appVersion;

    public HeartbeatMessage(int sender, int room) {
        super(sender, MESSAGE_TYPE_HEARTBEAT, room);
        this.appVersion = Constants.APP_VERSION;
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

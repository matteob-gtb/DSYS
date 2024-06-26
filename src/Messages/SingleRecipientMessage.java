package Messages;

import com.google.gson.Gson;

public class SingleRecipientMessage extends AbstractMessage{
    /**
     * @return
     */
    private int messageType;
    private int senderID;

    //-1 if it's not meant to be a room message
    private int roomID;
    private int recipientID;
    private int[] vectorTimestamp;

    /**
     * @return
     */
    @Override
    public Gson gson() {
       throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String toJSONString() {
        return "";
    }

    /**
     * @return
     */
    @Override
    public boolean isSent() {
        return false;
    }

    /**
     * @param sent
     */
    @Override
    public void setSent(boolean sent) {

    }
}

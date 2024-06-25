package Messages;

public class SingleRecipientMessage implements MessageInterface{
    /**
     * @return
     */
    private int messageType;
    private int senderID;

    //-1 if it's not meant to be a room message
    private int roomID;
    private int recipientID;
    private int[] vectorTimestamp;

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

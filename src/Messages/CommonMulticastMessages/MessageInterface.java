package Messages.CommonMulticastMessages;

import java.net.InetAddress;

public interface MessageInterface {
    String toJSONString();


    void setPayload(String payload);

    void setSent(boolean sent);

    public String getUsername();

    public int getMessageType();

    public boolean canDelete();

    public boolean shouldRetransmit();

    public boolean isUnicast();

    public InetAddress getDestinationAddress();
}

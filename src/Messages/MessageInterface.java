package Messages;

public interface MessageInterface {
    String toJSONString();

    boolean isSent();

    void setSent(boolean sent);
}

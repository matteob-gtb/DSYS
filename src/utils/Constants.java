package utils;

public class Constants {


    public static final int GROUP_PORT = 7631;

    public static final String MESSAGE_PROPERTY_FIELD_CLIENTID = "ID";


    public static final int MESSAGE_TYPE_WELCOME = 2;
    //causally ordered
    public static final int MESSAGE_TYPE_ROOM_MESSAGE = 4;

    public static final int MESSAGE_TYPE_HELLO = 1;
    public static final int MESSAGE_TYPE_CREATE_ROOM = 5;
    public static final int MESSAGE_TYPE_ROOM_FINALIZED = 33;
    public static final int DEFAULT_GROUP_ROOMID = 99;
    public static final int MESSAGE_TYPE_JOIN_ROOM_ACCEPT = 6;
    public static final int MESSAGE_TYPE_JOIN_ROOM_REFUSE = 99;

    public static final int QUEUE_THREAD_SLEEP_MIN_MS = 500;
    public static final int CLIENT_SLEEP_MS = 15;
    public final static int MAX_ROOM_CREATION_WAIT_MILLI = 1 * 1000;
    public final static int MIN_SOCKET_RECONNECT_DELAY = 1 * 1000;

    public final static int MIN_RETRANSMIT_WAIT = 15 * 1000;

    public static final int MESSAGE_TYPE_ACK = 103;
    public static final int MESSAGE_TYPE_CONNECTION_PROBE = 1337;

    public static final String COMMON_GROUPNAME = "224.1.1.1";
}

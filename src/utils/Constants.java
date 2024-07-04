package utils;

public class Constants {


    public static final int GROUP_PORT = 7631;

    public static final int DEFAULT_GROUP_ROOMID = 99;

    public static final int MESSAGE_TYPE_WELCOME = 2;
    public static final int MESSAGE_TYPE_ROOM_MESSAGE = 4;
    public static final int MESSAGE_TYPE_HELLO = 1;
    public static final int MESSAGE_TYPE_CREATE_ROOM = 5;
    public static final int MESSAGE_TYPE_ROOM_FINALIZED = 33;
    public static final int MESSAGE_TYPE_JOIN_ROOM_ACCEPT = 6;
    public static final int MESSAGE_TYPE_JOIN_ROOM_REFUSE = 99;
    public static final int MESSAGE_TYPE_ACK = 103;
    public static final int MESSAGE_TYPE_CONNECTION_PROBE = 1337;
    public static final int MESSAGE_TYPE_DELETE_ROOM = 1333;

    public static final int QUEUE_THREAD_SLEEP_MIN_MS = 100;
    public static final int CLIENT_SLEEP_MS = 15;
    public final static int MAX_ROOM_CREATION_WAIT_MS = 5 * 1000;
    public final static int MIN_SOCKET_RECONNECT_DELAY_MS = 300;
    public final static int MIN_RETRANSMIT_WAIT_MS = 500;

    public static final String COMMON_GROUPNAME = "224.0.0.1";



}

package utils;

public class Constants {


    public static final int SOCKET_PORT_LOW = 2000, SOCKET_PORT_HIGH = 5000, GROUP_PORT = 7631;
    public static final int RCV_BUFFER_SIZE = 1024;
    public static final String MESSAGE_TYPE_FIELD_NAME = "MESSAGE_TYPE";
    public static final String MESSAGE_PROPERTY_FIELD_CLIENTID = "ID";

    public static final String ROOM_ID_PROPERTY_NAME = "roomID";
    public static final String ROOM_MULTICAST_GROUP_ADDRESS = "ROOM_ADDRESS";
    public static final String FIELD_ROOM_PARTICIPANTS = "PARTICIPANTS";


    public static final int MESSAGE_TYPE_WELCOME = 2;
    //causally ordered
    public static final int MESSAGE_TYPE_ROOM_MESSAGE = 4;

    public static final int MESSAGE_TYPE_HELLO = 1;
    public static final int MESSAGE_TYPE_CREATE_ROOM = 5;
    public static final int MESSAGE_TYPE_ROOM_FINALIZED = 33;
    public static final int DEFAULT_GROUP_ROOMID = 99;
    public static final int MESSAGE_TYPE_JOIN_ROOM_ACCEPT = 6;
    public static final int MESSAGE_TYPE_JOIN_ROOM_REFUSE = 99;
    public static final int QUEUE_THREAD_SLEEP_MIN_MS = 100;
    public static final int MESSAGE_TYPE_CONNECTION_PROBE = 1337;
    public static final int CLIENT_SLEEP_MS = 15;
    public static final String COMMON_GROUPNAME = "224.1.1.1";
}

package utils;

public class Constants {
    public static final int SOCKET_PORT_LOW = 2000, SOCKET_PORT_HIGH = 5000, GROUP_PORT = 5000;
    public static final int RCV_BUFFER_SIZE = 1024;
    public static final String MESSAGE_TYPE_FIELD_NAME = "MESSAGE_TYPE";
    public static final String MESSAGE_PROPERTY_FIELD_CLIENTID = "ID";
    public static final String MESSAGE_PROPERTY_FIELD_KNOWNCLIENTS = "PEERS";
    public static final String MESSAGE_INTENDED_RECIPIENT = "RECIPIENT";
    public static final String MESSAGE_INTENDED_RECIPIENTS = "RECIPIENTS";
    public static final String ROOM_ID_PROPERTY_NAME = "ROOM_ID";


    public static final int MESSAGE_TYPE_HELLO = 1;
    public static final int MESSAGE_TYPE_WELCOME = 2;
     //causally ordered
    public static final int MESSAGE_TYPE_ROOM_MESSAGE = 4;
    public static final int MESSAGE_TYPE_CREATE_ROOM = 5;
    public static final int MESSAGE_TYPE_JOIN_ROOM_ACCEPT = 6;
    public static final int MESSAGE_TYPE_JOIN_ROOM_REFUSE = 99;
    public static final int MESSAGE_TYPE_ANNOUNCE_LEAVE = 7;
    public static final int MESSAGE_TYPE_GENERIC_ACK = 15;
    public static final int ROOM_MESSAGE = 30;


    public static final String GROUPNAME = "228.5.6.254";
}

package utils;

import java.net.MulticastSocket;

public class Constants {
    public static final int SOCKET_PORT_LOW = 2000, SOCKET_PORT_HIGH = 5000, GROUP_PORT = 5000;
    public static final int RCV_BUFFER_SIZE = 1024;
    public static final String MESSAGE_TYPE_FIELD_NAME = "MESSAGE_TYPE";
    public static final String MESSAGE_PROPERTY_FIELD_CLIENTID = "ID";
    public static final String MESSAGE_PROPERTY_FIELD_KNOWNCLIENTS = "PEERS ";
    public static final int MESSAGE_TYPE_HELLO = 1;
    public static final int MESSAGE_TYPE_WELCOME = 2;

    public static final int MESSAGE_TYPE_CREATE_ROOM = 3;
    public static final int MESSAGE_TYPE_JOIN_ROOM = 4;
    public static final int MESSAGE_TYPE_ANNOUNCE_LEAVE = 5;

    public static final String GROUPNAME = "228.5.6.254";
}

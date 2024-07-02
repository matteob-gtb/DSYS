package Peer;

import Messages.Logger;
import Networking.MyMulticastSocketWrapper;

import java.nio.file.attribute.FileAttribute;

public class Main {
    public static boolean debug = true;

    public static void main(String[] args) throws Exception {

        Logger.openLogFile();
        MyMulticastSocketWrapper.setupInterfaces();
        ChatClient client = new ChatClient();
        client.announceSelf();
        client.mainLoop();

    }
}

package Peer;

import Messages.Logger;
import Network.MyMulticastSocketWrapper;

public class Main {

    public static void main(String[] args) throws Exception {
        Logger.openLogFile();
        MyMulticastSocketWrapper.setupInterfaces();
        ChatClient client = new ChatClient();
        client.announceSelf();
        client.mainLoop();

    }
}

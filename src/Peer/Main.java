package Peer;

import Networking.MyMulticastSocketWrapper;

public class Main {
    public static boolean debug = true;

    public static void main(String[] args) throws Exception {

        //MyMulticastSocketWrapper.setupInterfaces();
        ChatClient client = new ChatClient();
        client.announceSelf();
        client.mainLoop();

    }
}

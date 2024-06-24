package Peer;

import Messages.MyMulticastSocketWrapper;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws Exception {
        MyMulticastSocketWrapper.setupInterfaces();
        ChatClient client = new ChatClient();
        client.announceSelf();
        client.mainLoop();

    }
}

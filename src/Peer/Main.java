package Peer;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient();
        client.announceSelf();
        client.mainLoop();

    }
}

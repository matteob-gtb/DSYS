package Peer;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;

/*
Clients send an HELLO message to discover peers on the same LAN
everyone responds with HI, containing each their ID



 */


public class ChatClient {
    private static final int SOCKET_PORT_LOW = 2000, SOCKET_PORT_HIGH = 5000, GROUP_PORT = 5000;
    private static final int RCV_BUFFER_SIZE = 1024;
    private static final String GROUPNAME = "228.5.6.254";
    private InetAddress group;
    private MulticastSocket socket;
    private ArrayList<Integer> knownClients = new ArrayList<>();

    private int CLIENT_ID;

    public ChatClient() throws IOException {
        Random generator = new Random();
        this.CLIENT_ID = generator.nextInt(0, 6000);
        this.group = InetAddress.getByName(GROUPNAME);

        boolean socketCreated = false;
        while (!socketCreated) {
            System.out.println("Attempting to connect to " + this.group);
            try {
                socket = new MulticastSocket(GROUP_PORT);
                socket.joinGroup(group);
                socketCreated = true;
            } catch (SocketException e) {
                System.out.println(e);
            }
        }

        System.out.println("Client #" + this.CLIENT_ID + " online ");

    }

    public void stayIdleAndReceive throws IOException() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            socket.receive(recv);
            System.out.println("Client [" + this.CLIENT_ID + "] received a message ");
            String receivedMessage = new String(recv.getData(), 0, recv.getLength());
        }


    }


    //block until received from all or timer expires
    public void announceSelf() throws IOException {

        JsonObject messageObject = new JsonObject();
        messageObject.addProperty("ID", this.CLIENT_ID);
        messageObject.addProperty("MESSAGE_TYPE", "HELLO");
        String helloMessage = messageObject.toString();

        System.out.println(helloMessage);
        DatagramPacket packet = new DatagramPacket(helloMessage.getBytes(), helloMessage.length(), this.group, GROUP_PORT);
        socket.send(packet);


        boolean stopReceiving = false;
        byte[] buf = new byte[1000];

        while (!stopReceiving) {
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            socket.receive(recv);
            System.out.println("Client [" + this.CLIENT_ID + "] received a message ");
            String receivedMessage = new String(recv.getData(), 0, recv.getLength());
            // Parse string to JSON object
            JsonObject receivedJson = JsonParser.parseString(receivedMessage).getAsJsonObject();
            // Print the received JSON object
            System.out.println("Received JSON: " + receivedJson.toString());
            if (receivedJson.get("ID").getAsInt() == (this.CLIENT_ID)) {
                continue;
            } else {
                this.knownClients = new ArrayList<>();

            }


        }


    }

    public void createRoom() throws IOException {
        //Send Message CreateRoom

    }


}

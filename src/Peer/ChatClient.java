package Peer;


import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;

/*
Clients send an HELLO message to discover peers on the same LAN
everyone responds with HI, containing each their ID



 */

public class ChatClient {
    private static final String GROUPNAME = "228.5.6.254";
    private InetAddress group:
    private MulticastSocket socket;
    private ArrayList<Integer> knownClients = new ArrayList<>();

    private final long CLIENT_ID;

    public ChatClient() throws IOException {
        Random generator = new Random();
        this.CLIENT_ID = generator.nextLong();
        this.group = InetAddress.getByName(GROUPNAME);

        boolean socketCreated = false;
        while (!socketCreated) {
            try {
                socket = new MulticastSocket(generator.nextInt(100, 1000));
                socket.joinGroup(group);
                socketCreated = true;
            } catch (SocketException e) {

            }
        }


    }

    //block until received from all or timer expires
    public void announceSelf(){

        JsonObject messageObject = new JsonObject();
        messageObject.addProperty("ID", this.CLIENT_ID);
        messageObject.addProperty("MESSAGE_TYPE","HELLO");
        String helloMessage = messageObject.toString();

        System.out.println(helloMessage);

        socket.send(helloMessage);

        boolean stopReceiving;
        while(!stopReceiving){
            System.out.println("Client " + this.CLIENT_ID + "received a message ");


            byte[] buf = new byte[1000];
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            socket.receive(recv);

            //MESSAGE CONTENT TO STRING HERE


         }



    }

    public void createRoom() throws IOException {
        //Send Message CreateRoom

    }


}

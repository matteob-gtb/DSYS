package Peer;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;

/*
Clients send an HELLO message to discover peers on the same LAN
everyone responds with HI, containing each their ID



 */


public class ChatClient extends AbstractClient {


    public ChatClient() throws IOException {
        Random generator = new Random();
        this.CLIENT_ID = generator.nextInt(0, 6000);
        this.group = InetAddress.getByName(GROUPNAME);
        this.knownClients = new ArrayList<>();

        boolean socketCreated = false;
        while (!socketCreated) {
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

    public void stayIdleAndReceive() throws IOException {
        byte[] buf = new byte[RCV_BUFFER_SIZE];
        while (true) {
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            socket.receive(recv);
            System.out.println("Client [" + this.CLIENT_ID + "] received a message ");
            String receivedMessage = new String(recv.getData(), 0, recv.getLength());

            JsonObject receivedJson = JsonParser.parseString(receivedMessage).getAsJsonObject();
            int messageType = receivedJson.get(MESSAGE_TYPE_FIELD_NAME).getAsInt();

            switch (messageType) {
                case MESSAGE_TYPE_HELLO:
                    System.out.println("Client [" + this.CLIENT_ID + "] received a hello message, replying with WELCOME");
                    //Reply with a WELCOME Message announcing the clients we know of
                    JsonObject welcomeJSON = new JsonObject();
                    welcomeJSON.addProperty(MESSAGE_PROPERTY_FIELD_CLIENTID, this.CLIENT_ID);
                    welcomeJSON.addProperty(MESSAGE_PROPERTY_FIELD_KNOWNCLIENTS, this.knownClients.toString());
                    sendMessage(welcomeJSON);
            }

            if (receivedJson.get(MESSAGE_TYPE_FIELD_NAME).getAsInt() == MESSAGE_TYPE_CREATE_ROOM)
                break;

        }


    }


    //block until received from all or timer expires
    public void announceSelf() throws IOException {

        JsonObject messageObject = new JsonObject();
        messageObject.addProperty(MESSAGE_PROPERTY_FIELD_CLIENTID, this.CLIENT_ID);
        messageObject.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_TYPE_HELLO);
        sendMessage(messageObject);


        boolean stopReceiving = false;
        byte[] buf = new byte[1000];
        JsonObject receivedJson;
        String receivedMessage;
        while (!stopReceiving) {
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            socket.receive(recv);
            System.out.println("Client [" + this.CLIENT_ID + "] received a message ");
            receivedMessage = new String(recv.getData(), 0, recv.getLength());
            // Parse string to JSON object
            receivedJson = JsonParser.parseString(receivedMessage).getAsJsonObject();
            // Print the received JSON object
            System.out.println("Received JSON: " + receivedJson.toString());

            int messageType = receivedJson.get(MESSAGE_TYPE_FIELD_NAME).getAsInt();

            if (receivedJson.get(MESSAGE_PROPERTY_FIELD_CLIENTID).getAsInt() == (this.CLIENT_ID)) {
                continue;
            } else {
                switch (messageType) {
                    case MESSAGE_TYPE_WELCOME:
                        knownClients.add(receivedJson.get(MESSAGE_PROPERTY_FIELD_CLIENTID).getAsInt());
                        break;
                    case MESSAGE_TYPE_CREATE_ROOM:
                        System.out.println("Received a new room");
                        break;
                    default:
                        break;
                }


             }


        }


    }

    public void createRoom() throws IOException {
        //Send Message CreateRoom

    }


}

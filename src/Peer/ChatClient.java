package Peer;


import Messages.MessageMiddleware;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;

import utils.Constants;

import static utils.Constants.*;
/*
Clients send an HELLO message to discover peers on the same LAN
everyone responds with HI, containing each their ID



 */


public class ChatClient extends AbstractClient {


    public ChatClient() throws IOException {
        Random generator = new Random();
        this.CLIENT_ID = generator.nextInt(0, 6000);
        this.messageMiddleware = new MessageMiddleware(CLIENT_ID);
    }


    /**
     * @throws IOException
     */
    @Override
    public void stayIdleAndReceive() throws IOException {

    }

    //block until received from all or timer expires
    public void announceSelf() throws IOException {

        JsonObject messageObject = new JsonObject();
        messageObject.addProperty(MESSAGE_PROPERTY_FIELD_CLIENTID, this.CLIENT_ID);
        messageObject.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_TYPE_HELLO);
        messageMiddleware.sendMessage(messageObject);

    }

    public void createRoom() throws IOException {
        //Send Message CreateRoom

    }


}

package Peer;


import Messages.ChatRoom;
import Messages.MessageMiddleware;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;


import static utils.Constants.*;
/*
Clients send an HELLO message to discover peers on the same LAN
everyone responds with HI, containing each their ID
 */


public class ChatClient extends AbstractClient {
    private BufferedReader reader;
    boolean inRoom = false;
    //Avoid overlapping messages
    private final Semaphore consoleSemaphore = new Semaphore(1);
    private boolean messageWaitingForReply = false;
    private final SecureRandom random = new SecureRandom();
    private ChatRoom currentRoom = null;





    public String askUserCommand(String commandPrompt, String defaultChoice, String... choices) {
        try {
            consoleSemaphore.acquire();
            while (true) {
                System.out.println(commandPrompt);
                String response = reader.readLine().trim();
                System.out.println("Response line " + response);
                for (String choice : choices)
                    if (response.equalsIgnoreCase(choice)) {
                        System.out.println("You chose : " + response);
                        return choice;
                    }
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
        } finally {
            consoleSemaphore.release();
        }
        return defaultChoice;
    }

    public void print(String message) {
        try {
            consoleSemaphore.acquire();
            System.out.println(message);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } finally {
            consoleSemaphore.release();
        }
    }

    public ChatClient() throws IOException {
        Random generator = new Random();
        this.CLIENT_ID = generator.nextInt(0, 6000);
        this.messageMiddleware = new MessageMiddleware(this);
    }


    //block until received from all or timer expires
    public void announceSelf() throws IOException {
        JsonObject messageObject = new JsonObject();
        messageObject.addProperty(MESSAGE_PROPERTY_FIELD_CLIENTID, this.CLIENT_ID);
        messageObject.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_TYPE_HELLO);
        print("Client #" + this.CLIENT_ID + " online, sent HELLO");
        messageMiddleware.sendMessage(messageObject);
    }

    public void createRoom() throws IOException {
        //Send Message CreateRoom

    }


    public void mainLoop() throws Exception {
        String command;
        reader = new BufferedReader(new InputStreamReader(System.in));
        printAvailableCommands();
        Event currentEvent = null;
        while (true) {
            if (!eventsToProcess.isEmpty())
                currentEvent = eventsToProcess.remove(0);
            //poll and get not needed to be atomic, only 1 consumer and 1 producer

            if (currentEvent != null)
                print(currentEvent.eventPrompt());
            consoleSemaphore.acquire();
            command = reader.readLine().trim();
            consoleSemaphore.release();
            Optional<JsonObject> eventOutcome = Optional.empty();

            if (currentEvent != null) {
                while (eventOutcome.isEmpty())
                    eventOutcome = currentEvent.executeEvent(command);
                messageMiddleware.sendMessage(eventOutcome.get());
            }


            switch (command.toLowerCase()) {
                case "0":
                    print("Command 'List Commands' received.");
                    printAvailableCommands();
                    /*JsonObject msg = getBaseMessageStub();
                    msg.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_TYPE_WELCOME);
                    messageMiddleware.sendMessage(msg);*/
                    // Add logic to send message
                    break;
                case "1":
                    print("Command 'Join Room' received.");
                    int roomID = -1;
                    while (true) {
                        print("Enter the room ID or q to exit this prompt");
                        String nextLine = reader.readLine().trim();
                        if (nextLine.contains("q")) break;
                        try {
                            roomID = Integer.parseInt(nextLine);
                        } catch (Exception e) {

                        }
                        Optional<ChatRoom> room = messageMiddleware.getChatRoom(roomID);
                        if (room.isEmpty())
                            print("Room not found.");
                        else {
                            currentRoom = room.get();
                            print("Joining Chat #" + currentRoom.getChatID());
                            break;
                        }

                    }
                    // Add logic to send message
                    break;
                case "2":
                    print("Command 'Create room' received.");
                    JsonObject outgoingMessage = getBaseMessageStub();
                    outgoingMessage.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_TYPE_CREATE_ROOM);
                    JsonArray recipientsArray = new JsonArray(messageMiddleware.getOnlineClients().size());
                    //messageMiddleware.getOnlineClients().forEach(recipientsArray::add);
                    outgoingMessage.add(MESSAGE_INTENDED_RECIPIENTS, recipientsArray);
                    ChatRoom room = new ChatRoom(random.nextInt(0, 999999));
                    outgoingMessage.addProperty(ROOM_ID_PROPERTY_NAME, room.getChatID());
                    messageMiddleware.registerRoom(room);
                    currentRoom = room;
                    messageMiddleware.sendMessage(outgoingMessage);
                    print("Sent room creation request to online peers,waiting for responses...");
                    break;
                case "3":
                    print("Command 'delete room' received.");
                    // Add logic to delete room
                    break;
                case "4":
                    print("Command 'Leave room' received.");
                    // Add logic to delete room
                    break;
                case "5":
                    print("Command 'List Online Peers' received.");
                    if (messageMiddleware.getOnlineClients().isEmpty())
                        print("No online peer detected yet");
                    else
                        messageMiddleware.getOnlineClients().forEach(System.out::println);

                    break;
                case "7":
                    System.exit(0);
                    break;
                default:
                    print("Invalid Command");
                    break;
            }
        }
    }

    private void printAvailableCommands() {
        if (currentRoom == null)
            print("""
                    Available commands:
                    0. List Commands
                    1. Join Room
                    2. Create Room
                    3. Delete Room
                    4. Leave Room
                    5. List Online Peers
                    6. Discover Online Peers
                    7. Quit Application
                    Enter command:  """);
        else {
            print("Current participants " + Arrays.toString(currentRoom.getParticipants()));
            print("""
                    Type a message and hit Enter to send it in the current room #""" + currentRoom.getChatID());
        }

    }
}


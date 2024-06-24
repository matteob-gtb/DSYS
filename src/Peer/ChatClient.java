package Peer;


import Events.AbstractEvent;
import Messages.ChatRoom;
import Messages.MessageMiddleware;
import Messages.MyMulticastSocketWrapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Semaphore;


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
    private ArrayList<ChatRoom> rooms = new ArrayList<>();

    @Deprecated
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
        messageMiddleware.sendMulticastMessage(messageObject);
    }

    public void createRoom() throws IOException {
        //Send Message CreateRoom

    }


    public void mainLoop() throws Exception {
        String command;
        reader = new BufferedReader(new InputStreamReader(System.in));
        printAvailableCommands();
        AbstractEvent currentEvent = null;
        command = null;
        boolean waitingForInput = true;
        while (true) {
            waitingForInput = true;

            //poll and get not needed to be atomic, only 1 consumer and 1 producer
            /*
             * Busy wait not optimal --> can't block on buffered reader and still accept events
             * as soon as they come, can't be interrupted
             * */
            while (waitingForInput) {
                if (!eventsToProcess.isEmpty())
                    currentEvent = eventsToProcess.removeFirst();
                if (reader.ready()) {
                    command = reader.readLine().trim();
                    waitingForInput = false;
                }
                if (currentEvent != null) {
                    Optional<JsonObject> eventOutcome = Optional.empty();
                    if (currentEvent.isActionable()) {
                        while (eventOutcome.isEmpty()) {
                            System.out.println(currentEvent.eventPrompt());
                            command = reader.readLine().trim();
                            eventOutcome = currentEvent.executeEvent(command);
                        }
                        messageMiddleware.sendMulticastMessage(eventOutcome.get());
                        command = "x";
                        waitingForInput = false;
                    } System.out.println(currentEvent.eventPrompt());
                    currentEvent = null;
                }
                Thread.sleep(25);
            }
            switch (command.toLowerCase()) {
                case "x":
                    //Event executed, ignore previous value
                    break;
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
                    ChatRoom room = new ChatRoom(random.nextInt(0, 999999), MyMulticastSocketWrapper.getNewGroupName());

                    JsonObject outgoingMessage = getJsonObject(room);
                    messageMiddleware.registerRoom(room);
                    currentRoom = room;
                    messageMiddleware.sendMulticastMessage(outgoingMessage);
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

    private JsonObject getJsonObject(ChatRoom room) {
        JsonObject outgoingMessage = getBaseMessageStub();
        outgoingMessage.addProperty(MESSAGE_TYPE_FIELD_NAME, MESSAGE_TYPE_CREATE_ROOM);
        JsonArray recipientsArray = new JsonArray(messageMiddleware.getOnlineClients().size());
        //messageMiddleware.getOnlineClients().forEach(recipientsArray::add);
        outgoingMessage.add(MESSAGE_INTENDED_RECIPIENTS, recipientsArray);


        outgoingMessage.addProperty(ROOM_ID_PROPERTY_NAME, room.getChatID());
        return outgoingMessage;
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


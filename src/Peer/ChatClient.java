package Peer;


import ChatRoom.ChatRoom;
import Events.AbstractEvent;
import Messages.*;
import Networking.MyMulticastSocketWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import static utils.Constants.*;


public class ChatClient extends AbstractClient {
    private BufferedReader reader;
    //Avoid overlapping messages
    private ChatRoom currentRoom = null;
    private QueueManager queueManager;
    private String userName;
    private ChatRoom commonMulticastChannel;

    public void print(String message) {
        System.out.println(message);
    }


    public ChatClient() throws Exception {
        Random generator = new Random(System.currentTimeMillis());
        reader = new BufferedReader(new InputStreamReader(System.in));


        if (!Main.debug)
            while (true) {
                System.out.print("Enter your username: > ");
                this.userName = reader.readLine().trim();
                System.out.println("You chose : " + this.userName);
                System.out.print("Press Enter to accept, r to change your username >");
                String response = reader.readLine().trim();
                if (!response.contains("r")) break;
            }
        else userName = "asdvasdvwq";
        this.CLIENT_ID = generator.nextInt(0, 150000);
        commonMulticastChannel = new ChatRoom(DEFAULT_GROUP_ROOMID, COMMON_GROUPNAME);
        //Default room, no fixed participants
        commonMulticastChannel.setRoomFinalized(true);
        this.queueManager = new QueueThread(this, commonMulticastChannel);
        this.currentRoom = commonMulticastChannel;
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(queueManager);
    }


    //block until received from all or timer expires
    public void announceSelf() throws IOException {
        MulticastMessage welcomeMessage = new MulticastMessage(
                this.CLIENT_ID,
                MESSAGE_TYPE_HELLO,
                DEFAULT_GROUP_ROOMID
        );
        //username only in HELLO messages
        welcomeMessage.setUsername(this.userName);
        currentRoom.addOutgoingMessage(welcomeMessage);
    }

    public void stayInRoom(ChatRoom room) {
        currentRoom = room;
        print("Joining Chat #" + currentRoom.getChatID());
        room.printMessages();
        String response;
        while (true) {
            System.out.println("Type M to send a message or q to go back to main menu > ");
            try {
                response = reader.readLine();
                if (response.contains("q")) break;
                if (response.contains("M")) {
                    System.out.println("Type the message you want to send >");
                    response = reader.readLine();
//                    currentRoom.addOutgoingMessage(new MulticastMessage(
//                     ))
                }
                ;
            } catch (IOException e) {
                System.out.println("Unrecoverable I/O error,shutting down...");
                System.exit(1);
            }
        }
        currentRoom = this.commonMulticastChannel;
    }

    public void listRooms() {
        queueManager.getRooms().forEach(
                room -> {
                    System.out.println("Room #" + room.getChatID());
                    room.getParticipantIDs().forEach(
                            id -> System.out.println("Participant #" + id)
                    );
                }
        );
    }

    public void mainLoop() throws Exception {
        String command;
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
                    Optional<MulticastMessage> eventOutcome = Optional.empty();
                    if (currentEvent.isActionable()) {
                        while (eventOutcome.isEmpty()) {
                            System.out.println(currentEvent.eventPrompt());
                            command = reader.readLine().trim();
                            System.out.println("Read " + command + " from user");
                            eventOutcome = currentEvent.executeEvent(command);
                            if (eventOutcome.isPresent())
                                commonMulticastChannel.addOutgoingMessage(eventOutcome.get());
                        }
                        //TODO  fix currentroom
                        //   queueManager.sendMessage(eventOutcome.get(), currentRoom);
                        command = "x";
                        waitingForInput = false;
                    }
                    currentEvent = null;
                }
                Thread.sleep(25);
            }
            switch (command.toLowerCase()) {
                case "x":
                    break;
                case "0":
                    print("Command 'List Commands' received.");
                    printAvailableCommands();
                    break;
                case "1":
                    print("Command 'List Online Rooms' received.");
                    listRooms();
                    break;
                case "2":
                    print("Command 'Join Room' received.");
                    int roomID = -1;
                    while (true) {
                        print("Enter the room ID or q to exit this prompt");
                        String nextLine = reader.readLine().trim();
                        if (nextLine.contains("q")) break;
                        try {
                            roomID = Integer.parseInt(nextLine);
                            System.out.println("Asked to join room " + roomID);
                        } catch (Exception e) {
                            System.out.println("Not a number");
                        }
                        Optional<ChatRoom> room = queueManager.getChatRoom(roomID);
                        if (room.isEmpty())
                            print("Room not found.");
                        else if (!room.get().isRoomFinalized()) {
                            System.out.println("Room has not been finalized yet, wait for" + (System.currentTimeMillis() - room.get().getCreationTimestamp()) + " more milliseconds");
                        } else {
                            stayInRoom(room.get());
                            break;
                        }

                    }
                    // Add logic to send message
                    break;
                case "3":
                    print("Command 'Create room' received.");
                    boolean stopCreatingRoom = false;
                    int ID = -1;
                    while (ID == -1) {
                        System.out.println("Enter the room ID or q to exit this prompt >");
                        String nextLine = reader.readLine().trim();
                        if (nextLine.contains("q")) {
                            stopCreatingRoom = true;
                            break;
                        } else {
                            try {
                                ID = Integer.parseInt(nextLine);
                                ChatRoom room = new ChatRoom(ID, MyMulticastSocketWrapper.getNewGroupName());
                                System.out.println("Created room with id #" + room.getChatID());
                                MulticastMessage outMsg = new MulticastMessage(this.CLIENT_ID, MESSAGE_TYPE_CREATE_ROOM, room.getChatID());
                                queueManager.registerRoom(room);
                                // NO ™currentRoom = room;
                                currentRoom.addOutgoingMessage(outMsg);
                                print("Sent room creation request to online peers,waiting for responses...");
                            } catch (NumberFormatException e) {
                                System.out.println("Not a number");
                            } catch (RuntimeException e) {
                                System.out.println("This room number alredy exists, choose another one");
                                ID = -1;
                            }

                        }

                    }


                    break;
                case "5":
                    print("Command 'Leave room' received.");
                    int deleteID = -1;
                    Optional<ChatRoom> toDelete = Optional.empty();

                    while (true) {
                        System.out.println("Enter the ID of the room you wish to delete,q to exit this prompt");
                        String response = reader.readLine().trim();
                        if (response.contains("q"))
                            break;
                        try {
                            deleteID = Integer.parseInt(response);
                        } catch (NumberFormatException e) {
                            System.out.println("Not a number");
                        }
                        toDelete = queueManager.getChatRoom(deleteID);
                        if (toDelete.isEmpty())
                            System.out.println("Room not found");
                        else break;
                    }
                    queueManager.deleteRoom(toDelete.get());
                    printAvailableCommands();
                    break;
                case "6":
                    print("Command 'List Online Peers' received.");
                    if (queueManager.getOnlineClients().isEmpty())
                        print("No online peer detected yet");
                    else {
                        ArrayList<String> namesList = new ArrayList<>(queueManager.getOnlineClients().size());
                        queueManager.getOnlineClients().forEach(
                                id -> namesList.add(idUsernameMappings.get(id))
                        );
                        System.out.println("List of online peers: [" + Arrays.toString(namesList.toArray()) + "]");
                    }
                    break;
                case "7":
                    print("Command 'Discover online Peers' received.");
                    announceSelf();
                    print("Sent an HELLO in multicast, waiting for replies...");
                    break;
                case "8":
                    System.exit(0);
                    break;
                default:
                    print("Invalid Command");
                    break;
            }
        }
    }

    private void printAvailableCommands() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        print("User [" + this.userName + "] - ID [" + this.CLIENT_ID + "]");
        print("""
                Available commands:
                0. List Commands
                1. List Online Rooms
                2. Join Room
                3. Create Room
                4. Delete Room
                5. Leave Room
                6. List Online Peers
                7. Discover Online Peers
                8. Quit Application
                Enter command:""");
        //Flush console

    }

    public void showRoomInfo(ChatRoom room) {
        print("Current participants " + Arrays.toString(room.getParticipants()));
        print("Type a message and hit Enter to send it in the current room #" + room.getChatID());
    }

}


package Peer;


import ChatRoom.ChatRoom;
import Events.AbstractEvent;
import Events.ReplyToRoomRequestEvent;
import Messages.CommonMulticastMessages.AbstractMessage;
import Messages.CommonMulticastMessages.AnonymousMessages.CreateRoomRequest;
import Messages.CommonMulticastMessages.AnonymousMessages.HelloMessage;
import Messages.Handler.QueueManager;
import Messages.Handler.QueueThread;
import Networking.MyMulticastSocketWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static utils.Constants.*;


public class ChatClient extends AbstractClient {
    private BufferedReader reader;
    //Avoid overlapping messages
    private ChatRoom currentRoom = null;
    private QueueManager queueManager;
    private ChatRoom commonMulticastChannel;
    public static int ID = -1;


    public void print(String message) {
        System.out.println(message);
    }


    public ChatClient() throws Exception {
        Random generator = new Random(System.currentTimeMillis() ^ 125243526);
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
        else {
            userName = "test " + System.currentTimeMillis() % 100;
        }
        this.CLIENT_ID = generator.nextInt(0, 150000);
        ID = this.CLIENT_ID;
        commonMulticastChannel = new ChatRoom(this.CLIENT_ID, DEFAULT_GROUP_ROOMID, COMMON_GROUPNAME);

        //Default room, no fixed participants
        commonMulticastChannel.setRoomFinalized(true);
        this.queueManager = new QueueThread(this, commonMulticastChannel);
        this.currentRoom = commonMulticastChannel;
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(queueManager);
    }


    //block until received from all or timer expires
    public void announceSelf() throws IOException {
        AbstractMessage welcomeMessage = new HelloMessage(
                this.CLIENT_ID,
                DEFAULT_GROUP_ROOMID,
                this.userName
        );
        //username only in HELLO messages
        System.out.println("Sending HELLO in multicast...");
        currentRoom.addOutgoingMessage(welcomeMessage);
    }

    public void joinRoom(ChatRoom room) {
        currentRoom = room;
        print("Joining Chat #" + currentRoom.getRoomId());
        room.getMessages();
        String response;
        while (true) {
            System.out.println("Client #" + this.CLIENT_ID);
            System.out.print("1) Type a message and press Enter to send it\n2) Press Enter without typing anything to refresh the chat\n3) q to quit\n>  ");
            try {
                response = reader.readLine();
                if (response.equals("q")) break;
                if (response.length() != 0)
                    currentRoom.sendInRoomMessage(response, this.CLIENT_ID);
                flushConsole();
                if (room.isScheduledForDeletion()) {
                    System.out.println("This room is about to be deleted, exiting...");
                    break;
                }
                System.out.println(room.getMessages());
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
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
                    if (room.isRoomFinalized()) {
                        System.out.println("Room #" + room.getRoomId() + " - Status [" + room.getStatusString() + "] - Owner = " + room.getOwnerID());
                        room.getParticipantIDs().forEach(
                                id -> System.out.println("      Participant #" + id));
                    } else {
                        System.out.println("Room #" + room.getRoomId() + " - Not Finalized Yet");
                    }

                }
        );
    }

    public void mainLoop() throws Exception {
        String command;
        printAvailableCommands();
        AbstractEvent currentEvent = null;
        command = null;
        boolean waitingForInput = true;
        boolean flushAfter = false;

        while (true) {
            waitingForInput = true;
            flushAfter = false;
            //poll and get not needed to be atomic, only 1 consumer and 1 producer
            /*
             * Busy wait not optimal --> can't block on buffered reader and still accept events
             * as soon as they come, can't be interrupted
             * */
            while (waitingForInput) {
                if (!eventsToProcess.isEmpty()) {
                    currentEvent = eventsToProcess.remove(0);
                    if (currentEvent instanceof ReplyToRoomRequestEvent && System.currentTimeMillis() - currentEvent.getCreationTimestamp() > MAX_ROOM_CREATION_WAIT_MS)
                        currentEvent = null;//discard the event, MOST LIKELY the timeout has already passed
                }
                if (reader.ready()) {
                    command = reader.readLine().trim();
                    waitingForInput = false;
                }
                if (currentEvent != null) {
                    Optional<AbstractMessage> eventOutcome = Optional.empty();
                    if (currentEvent.isActionable()) {
                        while (eventOutcome.isEmpty()) {
                            System.out.print(currentEvent.eventPrompt());
                            command = reader.readLine().trim();
                            //System.out.println("Read " + command + " from user");
                            eventOutcome = currentEvent.executeEvent(command);
                            //events can only be broadcasted over the common channel
                            if (eventOutcome.isPresent()) {
                                commonMulticastChannel.addOutgoingMessage(eventOutcome.get());
                                if (currentEvent instanceof ReplyToRoomRequestEvent) {
                                    ChatRoom newRoom = ((ReplyToRoomRequestEvent) currentEvent).createRoomReference();
                                    queueManager.registerRoom(newRoom);
                                }
                            }
                        }
                        command = "x";
                        waitingForInput = false;
                    }
                    currentEvent = null;
                }
                Thread.sleep(CLIENT_SLEEP_MS);
            }
            flushConsole();
            switch (command.toLowerCase()) {
                case "x":
                    break;
                case "0":
                    print("Command 'List Commands' received.");
                    printAvailableCommands();
                    flushAfter = false;
                    break;
                case "1":
                    print("Command 'List Online Rooms' received.");
                    listRooms();
                    flushAfter = false;
                    break;
                case "2":
                    print("Command 'Join Room' received.");
                    int roomID = -1;
                    while (true) {
                        listRooms();
                        System.out.println();
                        print("Enter the room ID or q to exit this prompt");
                        System.out.print("> ");
                        String nextLine = reader.readLine().trim();
                        if (nextLine.contains("q")) {
                            printAvailableCommands();
                            break;
                        }
                        try {
                            roomID = Integer.parseInt(nextLine);
                            System.out.println("Asked to join room " + roomID);
                        } catch (Exception e) {
                            System.out.println("Not a number");
                        }
                        Optional<ChatRoom> room = queueManager.getChatRoom(roomID);
                        if (room.isEmpty())
                            if (roomID == getDefaultRoom().getRoomId())
                                System.out.println("The default room does not allow messages");
                            else
                                System.out.println("Room not found.");
                        else if (!room.get().isRoomFinalized()) {
                            System.out.println("Room has not been finalized yet, wait for" + (System.currentTimeMillis() - room.get().getCreationTimestamp()) + " more milliseconds");
                        } else {
                            joinRoom(room.get());
                            break;
                        }
                        flushAfter = false;
                    }
                    break;
                case "3":
                    print("Command 'Create room' received.");
                    int ID = -1;
                    while (ID == -1) {
                        System.out.println("Enter the room ID or q to exit this prompt >");
                        String nextLine = reader.readLine().trim();
                        if (nextLine.contains("q")) {
                            printAvailableCommands();
                            break;
                        } else {
                            try {
                                ID = Integer.parseInt(nextLine);
                                ChatRoom room = new ChatRoom(CLIENT_ID, ID, MyMulticastSocketWrapper.getNewGroupName());
                                room.addParticipant(this.CLIENT_ID);
                                System.out.println("Created room with id #" + room.getRoomId() + " groupname " + room.getRoomAddress());
                                AbstractMessage outMsg = new CreateRoomRequest(this.CLIENT_ID, room.getRoomId(), room.getRoomAddress());
                                queueManager.registerRoom(room);
                                currentRoom.addOutgoingMessage(outMsg);
                                print("Sent room creation request to online peers,waiting for responses...");
                            } catch (NumberFormatException e) {
                                System.out.println("Not a number");
                            } catch (RuntimeException e) {
                                System.out.println("This room number alredy exists, choose another one");
                                ID = -1;
                            }

                        }
                        Thread.sleep(1000);
                        printAvailableCommands();

                    }

                    break;
                case "4":
                    print("Command 'Delete room' received.");
                    listRooms();
                    System.out.println();
                    int deleteID = -1;
                    Optional<ChatRoom> toDelete = Optional.empty();

                    while (true) {
                        System.out.println("Enter the ID of the room you wish to delete,q to exit this prompt");
                        System.out.print("> ");
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
                            if (deleteID == getDefaultRoom().getRoomId())
                                System.out.println("Can't delete the default room");
                            else
                                System.out.println("Room not found");
                        else {
                            //delete room
                            ChatRoom room = toDelete.get();
                            if (room.canDelete(this.CLIENT_ID)) {
                                System.out.println("Sent a ROOM_DELETE message, waiting to empty the queues to delete the room");
                                room.scheduleDeletion(true);
                            } else System.out.println("You are not the owner");
                            break;
                        }
                    }
                    printAvailableCommands();
                    break;
                case "5":
                    print("Command 'List Online Peers' received.");
                    System.out.println(queueManager.getOnlineClients());
                    break;
                case "6":
                    print("Command 'Discover online Peers' received.");
                    announceSelf();
                    print("Sent an HELLO in multicast, waiting for replies...");
                    Thread.sleep(1000);
                    printAvailableCommands();
                    break;
                case "7":
                    System.exit(0);
                    break;
                default:
                    printAvailableCommands();
                    print("Invalid Command");
                    break;
            }

            System.out.print("\n>\t");
        }
    }

    private void flushConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void printAvailableCommands() {
        flushConsole();
        print("User [" + this.userName + "] - ID [" + this.CLIENT_ID + "]");
        print("""
                Available commands:
                0. List Commands
                1. List Online Rooms
                2. Join Room
                3. Create Room 
                4. Delete Room
                5. List Online Peers
                6. Discover Online Peers
                7. Quit Application
                Enter a command:""");
        //Flush console

    }


    public ChatRoom getDefaultRoom() {
        return commonMulticastChannel;
    }

}


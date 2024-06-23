package Messages;

import com.google.gson.JsonObject;

import javax.imageio.IIOException;
import javax.swing.plaf.basic.BasicOptionPaneUI;
import java.awt.desktop.QuitEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import utils.Constants;

import static utils.Constants.GROUPNAME;
import static utils.Constants.GROUP_PORT;

public abstract class Middleware {
    final Lock lock = new ReentrantLock();

    private int currMessageIndex = 0;

    protected ArrayList<JsonObject> incomingMessages = new ArrayList<>();
    protected ArrayList<JsonObject> outGoingMessages = new ArrayList<>();
    protected HashMap<Integer, ChatRoom> chatRooms = new HashMap<>();

    protected QueueThread queueThread;
    protected ArrayList<Integer> knownClients;
    protected MulticastSocket socket = null;
    protected InetAddress group;
    protected int CLIENT_ID;

    public Middleware(int CLIENT_ID) throws IOException {
        this.queueThread = new QueueThread(this, CLIENT_ID);
        new Thread(queueThread).start();

    }


    protected HashMap<Integer, ChatRoom> roomsMap;

    protected void enqueueMessage(JsonObject message) {
        try {
            lock.lock();
            incomingMessages.add(message);
        } finally {
            lock.unlock();
        }
    }

    protected boolean availableOutgoingMessages() {
        try {
            lock.lock();
            return !outGoingMessages.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    protected Optional<JsonObject> getFirstOutgoingMessages() {
        try {
            lock.lock();
            if (outGoingMessages.size() <= currMessageIndex)
                return Optional.empty();
            currMessageIndex++;
            return Optional.of(outGoingMessages.get(currMessageIndex - 1));
        } finally {
            lock.unlock();
        }
    }


    public void sendMessage(JsonObject msgObject) {
        lock.lock();
        try {
            System.out.println("Enqueued message");
            outGoingMessages.add(msgObject);
        } finally {
            lock.unlock();
        }
    }


    /*
     *
     * check messages in a given chat room or other kinds of messages
     * */

    public boolean pollMessage(Optional<Integer> chatRoomID) {
        try {
            lock.lock();
            if (chatRoomID.isPresent())
                return chatRooms.get(chatRoomID.get()).getMessageCount() > 0;
            return !incomingMessages.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    public abstract JsonObject getMessage(Optional<Integer> chatRoomID);


}

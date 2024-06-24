package Messages;

import Peer.AbstractClient;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public abstract class Middleware {
    final Lock lock = new ReentrantLock();

    private int currMessageIndex = 0;

    protected ArrayList<JsonObject> incomingMessages = new ArrayList<>();

    protected ArrayList<JsonObject> outGoingMessages = new ArrayList<>();

    protected ConcurrentHashMap<Integer, ChatRoom> chatRooms = new ConcurrentHashMap<>();

    protected QueueThread queueThread;
    protected int CLIENT_ID;
    protected AbstractClient client;


    public void addParticipantToRoom(int chatID, int clientID) {
        if (!chatRooms.containsKey(chatID))
            throw new RuntimeException("Chat room with id " + chatID + " does not exist");
        chatRooms.get(chatID).addParticipant(clientID);
    }

    public Optional<ChatRoom> getChatRoom(int chatID) {
        if (!chatRooms.containsKey(chatID)) return Optional.empty();
        return Optional.of(chatRooms.get(chatID));
    }

    public Middleware(AbstractClient client) throws IOException {
        this.client = client;
        this.queueThread = new QueueThread(this, CLIENT_ID);
        this.CLIENT_ID = client.getID();
        new Thread(queueThread).start();
    }

    public Set<Integer> getOnlineClients() {
        return queueThread.getOnlineClients();
    }

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

    public void registerRoom(ChatRoom room) {
        chatRooms.put(room.getChatID(), room);
        queueThread.addRoom(room);
        room.addParticipant(this.CLIENT_ID);
    }

    public void sendMessageTo(JsonObject msgObject, InetAddress recipient) {
        lock.lock();
        try {
            throw new UnsupportedOperationException();
        } finally {
            lock.unlock();
        }
    }

    public void sendMulticastMessage(JsonObject msgObject) {
        lock.lock();
        try {
            outGoingMessages.add(msgObject);
        } finally {
            lock.unlock();
        }
    }
    public void sendRoomMessage(JsonObject msgObject,ChatRoom roomID) {
        lock.lock();
        try {
            throw new UnsupportedOperationException();
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
                if (chatRooms.contains(chatRoomID.get()))
                    return chatRooms.get(chatRoomID.get()).getMessageCount() > 0;
                else return false;
            return !incomingMessages.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    public abstract JsonObject getMessage(Optional<Integer> chatRoomID);


    public AbstractClient getClient() {
        return client;
    }
}

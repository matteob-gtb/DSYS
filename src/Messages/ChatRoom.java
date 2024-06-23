package Messages;

import com.google.gson.JsonObject;
import com.sun.source.tree.Tree;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class ChatRoom {
    private final int chatID;
    private ArrayList<JsonObject> messageList;
    private Set<Integer> participantIDs = new TreeSet<Integer>();
    private int[] ownVectorTimestamp;

    public int getChatID() {
        return chatID;
    }

    public ChatRoom(int chatID) {
        this.chatID = chatID;
    }

    public int[] getParticipants() {
        return participantIDs.stream().mapToInt(i -> i).toArray();
    }

    public boolean addParticipant(Integer participantID) {
        return participantIDs.add(participantID);
    }

    public int getMessageCount() {
        return messageList.size();
    }

}

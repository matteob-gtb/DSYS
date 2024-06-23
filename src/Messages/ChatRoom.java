package Messages;

import com.google.gson.JsonObject;

import java.util.ArrayList;

public class ChatRoom {
    private final int chatID;
    private ArrayList<JsonObject> messageList;


    public ChatRoom(int chatID) {
        this.chatID = chatID;
    }

    public int getMessageCount() { return messageList.size(); }

}

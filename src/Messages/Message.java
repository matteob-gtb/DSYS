package Messages;

import com.google.gson.JsonObject;

public class Message {
    public int getRecipientID() {
        return recipientID;
    }

    public JsonObject getRawMessage() {
        return rawMessage;
    }

    public int[] getVectorTimestamp() {
        return vectorTimestamp;
    }

    private int recipientID;
    private JsonObject rawMessage;

    public Message(int senderID, JsonObject rawMessage) {
        this.recipientID = senderID;
        this.rawMessage = rawMessage;
    }

    public Message( JsonObject rawMessage) {
        this.recipientID = -1;
        this.rawMessage = rawMessage;
    }

    private int[] vectorTimestamp;

}

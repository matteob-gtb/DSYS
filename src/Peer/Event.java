package Peer;

import com.google.gson.JsonObject;

import java.util.Optional;

public interface Event {

    public Optional<JsonObject> executeEvent(String command);
    public String eventPrompt();
}

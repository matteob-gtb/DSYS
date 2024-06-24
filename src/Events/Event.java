package Events;

import com.google.gson.JsonObject;

import java.util.Optional;

public interface Event {

    public void executeEvent();

    public Optional<JsonObject> executeEvent(String command);

    public String eventPrompt();
}

package Events;

public abstract class AbstractEvent implements Event {

    public boolean isActionable() {
        return isActionable;
    }

    private final boolean isActionable;

    public AbstractEvent(boolean isActionable) {
        this.isActionable = isActionable;
    }


}

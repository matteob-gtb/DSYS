package Events;

public abstract class AbstractEvent implements Event {

    public boolean isActionable() {
        return isActionable;
    }

    private final boolean isActionable;

    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    private final Long creationTimestamp = System.currentTimeMillis();

    public AbstractEvent(boolean isActionable) {
        this.isActionable = isActionable;
    }


}

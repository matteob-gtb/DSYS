package VectorTimestamp;

public interface Timestamp {
    boolean lessThan(Timestamp other);

    boolean greaterThan(Timestamp other);

    boolean lessThanOrEqual(Timestamp other);

    boolean greaterThanOrEqual(Timestamp other);

    boolean equal(Timestamp other);

    //checks if it can be delivered given the current client's timestamp
    boolean comesBefore(Timestamp other);


    public int getValueAtPosition(int position);

}

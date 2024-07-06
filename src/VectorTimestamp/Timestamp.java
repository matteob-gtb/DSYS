package VectorTimestamp;

public interface Timestamp {
    boolean lessThan(Timestamp other);

    boolean greaterThan(Timestamp other);

    boolean lessThanOrEqual(Timestamp other);

    boolean greaterThanOrEqual(Timestamp other);

    //checks if it can be delivered given the current client's timestamp
    boolean canDeliver(Timestamp other,int index);

    boolean isConcurrent(Timestamp other);



    public int getValueAtPosition(int position);

}

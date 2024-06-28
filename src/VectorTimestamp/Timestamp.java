package VectorTimestamp;

public interface Timestamp {
    boolean lessThan(Timestamp other);

    boolean greaterThan(Timestamp other);

    boolean lessThanOrEqual(Timestamp other);

    boolean greaterThanOrEqual(Timestamp other);

    boolean equal(Timestamp other);
}

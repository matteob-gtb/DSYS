package VectorTimestamp;

import java.util.Arrays;
import java.util.stream.IntStream;

public class VectorTimestamp implements Timestamp {

    private final int[] rawTimestamp;

    public VectorTimestamp(int[] rawTimestamp) {
        this.rawTimestamp = rawTimestamp;
    }

    /**
     * @param other
     * @return
     */
    @Override
    public boolean lessThan(Timestamp other) {
        if (!(other instanceof VectorTimestamp)) throw new RuntimeException("Bad comparison");
        VectorTimestamp otherV = (VectorTimestamp) other;
        boolean foundGreaterOrEqual =
                IntStream.range(0, otherV.rawTimestamp.length).mapToObj(
                        i -> otherV.rawTimestamp[i] <= this.rawTimestamp[i]
                ).findFirst().isPresent();
        return !foundGreaterOrEqual;
    }

    /**
     * @param other
     * @return
     */
    @Override
    public boolean greaterThan(Timestamp other) {
        if (!(other instanceof VectorTimestamp)) throw new RuntimeException("Bad comparison");
        VectorTimestamp otherV = (VectorTimestamp) other;
        boolean foundLessOrEqual =
                IntStream.range(0, otherV.rawTimestamp.length).mapToObj(
                        i -> otherV.rawTimestamp[i] >= this.rawTimestamp[i]
                ).findFirst().isPresent();
        return !foundLessOrEqual;
    }

    /**
     * @param other
     * @return
     */
    @Override
    public boolean lessThanOrEqual(Timestamp other) {
        if (!(other instanceof VectorTimestamp)) throw new RuntimeException("Bad comparison");
        VectorTimestamp otherV = (VectorTimestamp) other;
        boolean foundStrictlyGreater =
                IntStream.range(0, otherV.rawTimestamp.length).mapToObj(
                        i -> otherV.rawTimestamp[i] < this.rawTimestamp[i]
                ).findFirst().isPresent();
        return !foundStrictlyGreater;
    }


    /**
     * @param other
     * @return
     */
    @Override
    public boolean greaterThanOrEqual(Timestamp other) {
        if (!(other instanceof VectorTimestamp)) throw new RuntimeException("Bad comparison");
        VectorTimestamp otherV = (VectorTimestamp) other;
        boolean foundStrictlyLess =
                IntStream.range(0, otherV.rawTimestamp.length).mapToObj(
                        i -> otherV.rawTimestamp[i] > this.rawTimestamp[i]
                ).findFirst().isPresent();
        return !foundStrictlyLess;
    }

    /**
     * @param other
     * @return
     */
    @Override
    public boolean equal(Timestamp other) {
        if (!(other instanceof VectorTimestamp)) throw new RuntimeException("Bad comparison");
        VectorTimestamp otherV = (VectorTimestamp) other;
        boolean foundDiff =
                IntStream.range(0, otherV.rawTimestamp.length).mapToObj(
                        i -> otherV.rawTimestamp[i] != this.rawTimestamp[i]
                ).findFirst().isPresent();
        return !foundDiff;
    }
}

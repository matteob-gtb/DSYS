package VectorTimestamp;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VectorTimestamp implements Timestamp {

    //immutable
    private final int[] rawTimestamp;


    //copy constructor
    public VectorTimestamp(VectorTimestamp toCopy) {
        this.rawTimestamp = Arrays.copyOf(toCopy.rawTimestamp, toCopy.rawTimestamp.length);
    }

    public VectorTimestamp(int[] rawTimestamp) {
        this.rawTimestamp = Arrays.copyOf(rawTimestamp, rawTimestamp.length);
    }

    public static VectorTimestamp merge(VectorTimestamp first, VectorTimestamp other) {
        VectorTimestamp merged = new VectorTimestamp(first.rawTimestamp);
        IntStream.range(0, first.rawTimestamp.length).forEach(i -> merged.rawTimestamp[i] = Math.max(other.rawTimestamp[i], merged.rawTimestamp[i]));
        return merged;
    }

    public VectorTimestamp increment(int clientIndex) {
        int[] newTimestamp = Arrays.copyOf(this.rawTimestamp, this.rawTimestamp.length);
        newTimestamp[clientIndex] += 1;
        return new VectorTimestamp(newTimestamp);
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

    public boolean equals(Object other) {
        if (!(other instanceof VectorTimestamp)) throw new RuntimeException("Bad comparison");
        VectorTimestamp otherV = (VectorTimestamp) other;
        return Arrays.equals(this.rawTimestamp, otherV.rawTimestamp);
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
//
//    /**
//     * @param other
//     * @return
//     */
//    @Override
//    public boolean equals(Timestamp other) {
//        if (!(other instanceof VectorTimestamp)) throw new RuntimeException("Bad comparison");
//        VectorTimestamp otherV = (VectorTimestamp) other;
//        boolean foundDiff =
//                IntStream.range(0, otherV.rawTimestamp.length).mapToObj(
//                        i -> otherV.rawTimestamp[i] != this.rawTimestamp[i]
//                ).findFirst().isPresent();
//        return !foundDiff;
//    }

    @Override
    public boolean canDeliver(Timestamp other) {
        if (!(other instanceof VectorTimestamp)) throw new RuntimeException("Bad comparison");
        VectorTimestamp otherV = (VectorTimestamp) other;
        if (this.rawTimestamp.length != otherV.rawTimestamp.length)
            throw new RuntimeException("Bad comparison, check room finalization");
        /*
         * Given client k (this) and client j (source of the message), check that the message
         * can be delivered to k's queue iff v_k[j] = v_j[j] - 1 e tutte le altre posizioni sono <=
         * */

        //check in how many positions this[k] = other[k] + 1, must be only 1 to accept

        var howManyPositionsPlusOne = IntStream.range(0, otherV.rawTimestamp.length).filter(
                index -> otherV.rawTimestamp[index] - 1 == this.rawTimestamp[index]
        ).boxed().toList();
        if (howManyPositionsPlusOne.size() != 1) return false;

        return IntStream.range(0, otherV.rawTimestamp.length).
                filter(index -> index != howManyPositionsPlusOne.get(0)).
                filter(index -> this.rawTimestamp[index] >= otherV.rawTimestamp[index]).
                count() == this.rawTimestamp.length - 1;

    }

    @Override
    public boolean isConcurrent(Timestamp other) {
        if (!(other instanceof VectorTimestamp)) throw new RuntimeException("Bad comparison");
        VectorTimestamp otherV = (VectorTimestamp) other;
        return !otherV.lessThan(this) && !this.lessThan(otherV);
    }

    @Override
    public int getValueAtPosition(int position) {
        return rawTimestamp[position];
    }

    public String toString() {
        return Arrays.toString(this.rawTimestamp);
    }


    public int hashCode() {
        return Arrays.hashCode(this.rawTimestamp);
    }
}

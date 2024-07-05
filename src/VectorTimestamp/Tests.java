package VectorTimestamp;

public class Tests {

    public static void main(String[] args) {

        VectorTimestamp m = new VectorTimestamp(new int[]{0, 0, 0});
        VectorTimestamp timestamp = new VectorTimestamp(new int[]{1, 1, 1});

        System.out.println(m);
        System.out.println(timestamp);

        System.out.println(m.greaterThan(timestamp));
        System.out.println(m.greaterThanOrEqual(timestamp));
        System.out.println(m.lessThan(timestamp));
        System.out.println(m.lessThanOrEqual(timestamp));



    }

}

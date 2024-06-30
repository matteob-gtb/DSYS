package VectorTimestamp;

public class Tests {

    public static void main(String[] args) {
        VectorTimestamp initial = new VectorTimestamp(new int[]{0, 0, 0});
        VectorTimestamp message = new VectorTimestamp(new int[]{0, 1, 0});
        VectorTimestamp reply = new VectorTimestamp(new int[]{1, 1, 0});


        System.out.println(initial.comesBefore(message));
        System.out.println(reply.comesBefore(message));
        System.out.println(message.comesBefore(reply));


    }

}

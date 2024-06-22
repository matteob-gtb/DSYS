package Messages;

import java.util.ArrayList;
import java.util.List;

public class VectorTimestamp {
    private List<Integer> vector;

    public VectorTimestamp(int howMany) {
        this.vector = new ArrayList<Integer>(howMany);
    }

}

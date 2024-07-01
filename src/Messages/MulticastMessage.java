package Messages;

import com.google.gson.*;


public class MulticastMessage extends AbstractMessage {



    /**
     * @return
     */
    @Override
    public Gson gson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setExclusionStrategies(new ExclusionStrategy() {
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                if (fieldAttributes.getName().equals("sent")) {
                    return true;
                }
                if ("roomID".equals(fieldAttributes.getName())) {
                    return roomID == -1;
                }
                return false;
            }

            public boolean shouldSkipClass(Class aClass) {
                return false;
            }
        });
        return builder.create();
    }


    ExclusionStrategy exclusionStrategy = new ExclusionStrategy() {
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            if (fieldAttributes.getName().equals("sent")) {
                return true;
            }
            if ("roomID".equals(fieldAttributes.getName())) {
                return roomID == -1;
            }
            return false;
        }

        public boolean shouldSkipClass(Class aClass) {
            return false;
        }
    };


}



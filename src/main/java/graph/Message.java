package graph;

import java.util.Arrays;
import java.util.Date;

public class Message {

    public final Byte[] data;
    public final Date date;

    public Message(Byte[] data) {
        this.data = data;
        this.date = new Date();
    }

    public Message(String text) {
        //TODO Use default constructor
        this.date = new Date();
    }

    public Message(double number) {
        //TODO Use default constructor
        this.date = new Date();
    }

    public String asText() {
        return Arrays.toString(this.data);
    }

    public double asDouble() {
        return 0.0;
    }
}

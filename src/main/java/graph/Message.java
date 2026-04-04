package graph;

import java.util.Date;

public class Message {
    public final byte[] data;
    public final String asText;
    public final double asDouble;
    public final Date date;

    private Message(byte[] data, String asText, double asDouble) {
        this.data = data;
        this.asText = asText;
        this.asDouble = asDouble;
        this.date = new Date();
    }

    public Message(String text) {
        this(text.getBytes(), text, parseDouble(text));
    }

    public Message(byte[] data) {
        this(data, new String(data), parseDouble(new String(data)));
    }

    public Message(double number) {
        this(Double.toString(number).getBytes(), Double.toString(number), number);
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
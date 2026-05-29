package graph;

import java.util.Date;

/**
 * A message exchanged between topics and agents.
 * The same payload is stored in three forms (bytes, text, double) so
 * agents can read whichever one they need. If the text is not a valid
 * number, asDouble is {@link Double#NaN}.
 */
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

    /** Returns NaN instead of throwing when the text is not a number. */
    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}

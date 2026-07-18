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

    /**
     * Creates a message from text. {@code asDouble} is {@link Double#NaN} if the text is
     * not a valid number.
     *
     * @param text the message payload as text
     */
    public Message(String text) {
        this(text.getBytes(), text, parseDouble(text));
    }

    /**
     * Creates a message from raw bytes, decoded as text using the platform charset.
     *
     * @param data the message payload as bytes
     */
    public Message(byte[] data) {
        this(data, new String(data), parseDouble(new String(data)));
    }

    /**
     * Creates a message from a number; its text and byte forms are the number's string value.
     *
     * @param number the message payload as a number
     */
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

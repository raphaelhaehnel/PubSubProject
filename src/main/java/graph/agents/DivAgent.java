package graph.agents;

import graph.Message;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;

/**
 * Publishes subs[0] / subs[1] on pubs[0].
 * subs[0] is the numerator, subs[1] is the denominator (order matters).
 * Division by zero is skipped (no message is published)
 */
public class DivAgent implements Agent {

    private Double numerator;
    private Double denominator;

    private final String[] subs;
    private final String[] pubs;
    private final TopicManager topicManager;

    /**
     * Subscribes to {@code subs[0]} (numerator) and {@code subs[1]} (denominator) and
     * registers as publisher of {@code pubs[0]}. Wires nothing if fewer than two inputs
     * are given.
     *
     * @param subs the input topic names; {@code subs[0]} numerator, {@code subs[1]} denominator
     * @param pubs the output topic names; the quotient is published on {@code pubs[0]}
     */
    public DivAgent(String[] subs, String[] pubs) {

        this.subs = subs;
        this.pubs = pubs;

        topicManager = TopicManagerSingleton.get();

        if (subs.length < 2) {
            return;
        }

        topicManager.getTopic(subs[0]).subscribe(this);
        topicManager.getTopic(subs[1]).subscribe(this);

        if (pubs.length < 1) {
            return;
        }

        topicManager.getTopic(pubs[0]).addPublisher(this);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Clears the stored numerator and denominator.
     */
    @Override
    public void reset() {
        this.numerator = 0.0;
        this.denominator = 0.0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Stores the numerator or denominator depending on which topic fired, and once both are
     * known publishes {@code numerator / denominator}. Non-numeric messages and division by
     * zero are skipped (nothing is published).
     */
    @Override
    public void callback(String topic, Message msg) {
        double messageValue = msg.asDouble;
        if (Double.isNaN(messageValue)) {
            return;
        }

        if (subs.length > 0 && topic.equals(subs[0])) {
            numerator = messageValue;
        } else if (subs.length > 1 && topic.equals(subs[1])) {
            denominator = messageValue;
        }

        if (numerator != null && denominator != null && pubs.length > 0) {
            if (denominator == 0.0) {
                System.err.println("DivAgent: division by zero, skipping publish.");
                return;
            }
            double result = numerator / denominator;
            topicManager.getTopic(pubs[0]).publish(new Message(result));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        if (subs.length > 0) {
            topicManager.getTopic(subs[0]).unsubscribe(this);
        }
        if (subs.length > 1) {
            topicManager.getTopic(subs[1]).unsubscribe(this);
        }
        if (pubs.length > 0) {
            topicManager.getTopic(pubs[0]).removePublisher(this);
        }
    }
}

package graph;

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

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void reset() {
        this.numerator = 0.0;
        this.denominator = 0.0;
    }

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

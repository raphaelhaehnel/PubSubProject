package graph.agents;

import graph.Message;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;

/**
 * Publishes subs[0] + 1 on pubs[0] every time it receives a message.
 */
public class IncAgent implements Agent {

    private final String[] subs;
    private final String[] pubs;
    private final TopicManager topicManager;

    /**
     * Subscribes to {@code subs[0]} and registers as publisher of {@code pubs[0]}.
     *
     * @param subs the input topic names; only {@code subs[0]} is read
     * @param pubs the output topic names; the incremented value is published on {@code pubs[0]}
     */
    public IncAgent(String[] subs, String[] pubs) {

        this.subs = subs;
        this.pubs = pubs;

        topicManager = TopicManagerSingleton.get();

        if (subs.length > 0) {
            topicManager.getTopic(subs[0]).subscribe(this);
        }
        if (pubs.length > 0) {
            topicManager.getTopic(pubs[0]).addPublisher(this);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * No-op: this agent keeps no state between messages.
     */
    @Override
    public void reset() {
    }

    /**
     * {@inheritDoc}
     * <p>
     * Publishes the received value plus {@code 1.0} on {@code pubs[0]}. Non-numeric messages
     * are ignored.
     */
    @Override
    public void callback(String topic, Message msg) {
        double messageValue = msg.asDouble;
        if (Double.isNaN(messageValue)) {
            return;
        }

        if (pubs.length > 0) {
            double result = messageValue + 1.0;
            topicManager.getTopic(pubs[0]).publish(new Message(result));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        if (subs.length > 0) {
            topicManager.getTopic(subs[0]).unsubscribe(this);
        }
        if (pubs.length > 0) {
            topicManager.getTopic(pubs[0]).removePublisher(this);
        }
    }
}

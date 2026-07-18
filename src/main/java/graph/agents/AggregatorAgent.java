package graph.agents;

import graph.Message;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;

import java.util.Arrays;

/**
 * Base class for agents that combine N input topics into a single
 * output value (PlusAgent, MulAgent, AvgAgent, ...).
 * <p>
 * It handles all the common logic: subscribing to every input,
 * remembering the latest value of each one, waiting until every
 * input has been received at least once, and publishing the result.
 * Subclasses only have to implement {@link #aggregate(Double[])}.
 */
public abstract class AggregatorAgent implements Agent {

    protected final String[] subs;
    protected final String[] pubs;
    protected final Double[] values;
    protected final TopicManager topicManager;

    /**
     * Subscribes to every input topic and registers as a publisher of {@code pubs[0]}.
     * If {@code subs} is empty the agent wires nothing.
     *
     * @param subs the input topic names
     * @param pubs the output topic names; only {@code pubs[0]} is published to
     */
    protected AggregatorAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;
        this.values = new Double[subs.length];
        this.topicManager = TopicManagerSingleton.get();

        if (subs.length == 0) {
            return;
        }

        for (String s : subs) {
            topicManager.getTopic(s).subscribe(this);
        }

        if (pubs.length > 0) {
            topicManager.getTopic(pubs[0]).addPublisher(this);
        }
    }

    /**
     * Combines the latest values from every input into a single output.
     * Called only once every slot has been filled at least once.
     *
     * @param values the latest value of each input topic, in {@code subs} order
     * @return the aggregated result to publish
     */
    protected abstract double aggregate(Double[] values);

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Resets every stored input value to {@code 0.0}.
     */
    @Override
    public void reset() {
        Arrays.fill(values, 0.0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Stores the value on every matching input slot (so a topic listed more than once
     * counts each time), and publishes {@link #aggregate(Double[])} once every input has
     * received at least one value. Non-numeric messages are ignored.
     */
    @Override
    public void callback(String topic, Message msg) {
        double messageValue = msg.asDouble;
        if (Double.isNaN(messageValue)) {
            return;
        }

        // Fill every slot whose name matches. Doing every match (instead
        // of breaking on the first) lets configs like ["A","A","B"]
        // correctly use A twice.
        boolean matched = false;
        for (int i = 0; i < subs.length; i++) {
            if (subs[i].equals(topic)) {
                values[i] = messageValue;
                matched = true;
            }
        }
        if (!matched) {
            return;
        }

        // Wait until every input has been received at least once.
        for (Double d : values) {
            if (d == null) {
                return;
            }
        }

        double result = aggregate(values);

        if (pubs.length > 0) {
            topicManager.getTopic(pubs[0]).publish(new Message(result));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        for (String s : subs) {
            topicManager.getTopic(s).unsubscribe(this);
        }
        if (pubs.length > 0) {
            topicManager.getTopic(pubs[0]).removePublisher(this);
        }
    }
}

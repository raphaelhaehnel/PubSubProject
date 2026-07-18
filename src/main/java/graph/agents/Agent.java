package graph.agents;

import graph.Message;

/**
 * Anything that can publish messages to topics and/or subscribe to them
 */
public interface Agent {

    /** @return the agent's name, used to identify it in the graph view */
    String getName();

    /** Reset the agent's internal state (stored operands, counters, ...). */
    void reset();

    /**
     * Called by a topic this agent subscribes to when a new message arrives.
     *
     * @param topic the name of the topic that published the message
     * @param msg   the published message
     */
    void callback(String topic, Message msg);

    /** Unsubscribe from all topics and release any resources. */
    void close();
}

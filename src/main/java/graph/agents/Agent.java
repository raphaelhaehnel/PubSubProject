package graph.agents;

import graph.Message;

/**
 * Anything that can publish messages to topics and/or subscribe to them
 */
public interface Agent {

    String getName();

    /** Reset the agent's internal state (stored operands, counters, ...) */
    void reset();

    /** Called by a topic this agent subscribes to when a new message arrives */
    void callback(String topic, Message msg);

    /** Unsubscribe from all topics and release any resources */
    void close();
}

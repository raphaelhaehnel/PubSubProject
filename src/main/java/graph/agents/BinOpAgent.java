package graph.agents;

import java.util.function.BinaryOperator;

import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;

/**
 * Generic two-input agent that applies a {@link BinaryOperator} on the
 * two inputs and publishes the result.
 */
public class BinOpAgent implements Agent {

    private final BinaryOperator<Double> operation;
    private final String agentName;
    private final Topic firstTopic;
    private final Topic secondTopic;
    private final Topic outputTopic;
    private Double firstInput;
    private Double secondInput;

    /**
     * Subscribes to the two input topics and registers as publisher of the output topic.
     *
     * @param agentName       the agent's display name
     * @param firstTopicName  the first input topic name
     * @param secondTopicName the second input topic name
     * @param outputTopicName the output topic name
     * @param operation       the binary operation applied to the two inputs
     */
    public BinOpAgent(String agentName, String firstTopicName, String secondTopicName, String outputTopicName, BinaryOperator<Double> operation) {
        this.operation = operation;
        this.agentName = agentName;
        TopicManagerSingleton.TopicManager topicManager = TopicManagerSingleton.get();

        firstTopic = topicManager.getTopic(firstTopicName);
        secondTopic = topicManager.getTopic(secondTopicName);
        outputTopic = topicManager.getTopic(outputTopicName);

        firstTopic.subscribe(this);
        secondTopic.subscribe(this);
        outputTopic.addPublisher(this);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return agentName;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Clears both stored inputs.
     */
    @Override
    public void reset() {
        firstInput = 0.0;
        secondInput = 0.0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Stores the matching input and, once both are known, publishes the operation's result
     * on the output topic.
     */
    @Override
    public void callback(String topic, Message msg) {
        if (topic.equals(firstTopic.name)) {
            firstInput = msg.asDouble;
        } else if (topic.equals(secondTopic.name)) {
            secondInput = msg.asDouble;
        }

        if (firstInput != null && secondInput != null) {
            double result = operation.apply(firstInput, secondInput);
            outputTopic.publish(new Message(result));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        firstTopic.unsubscribe(this);
        secondTopic.unsubscribe(this);
        outputTopic.removePublisher(this);
    }
}

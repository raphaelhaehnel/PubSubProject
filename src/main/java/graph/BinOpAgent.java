package graph;

import java.util.function.BinaryOperator;

public class BinOpAgent implements Agent {

    private final BinaryOperator<Double> operation;
    private final String agentName;
    private final TopicManagerSingleton.TopicManager topicManager;
    private final Topic firstTopic;
    private final Topic secondTopic;
    private final Topic outputTopic;
    private Double firstInput;
    private Double secondInput;

    public BinOpAgent(String agentName, String firstTopicName, String secondTopicName, String outputTopicName, BinaryOperator<Double> operation) {
        this.operation = operation;
        this.agentName = agentName;
        this.topicManager = TopicManagerSingleton.get();

        firstTopic = topicManager.getTopic(firstTopicName);
        secondTopic = topicManager.getTopic(secondTopicName);
        outputTopic = topicManager.getTopic(outputTopicName);

        firstTopic.subscribe(this);
        secondTopic.subscribe(this);
        outputTopic.addPublisher(this);
    }

    @Override
    public String getName() {
        return agentName;
    }

    @Override
    public void reset() {
        firstInput = 0.0;
        secondInput = 0.0;
    }

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

    @Override
    public void close() {
        firstTopic.unsubscribe(this);
        secondTopic.unsubscribe(this);
        outputTopic.removePublisher(this);
    }
}

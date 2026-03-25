package test;

import test.TopicManagerSingleton.TopicManager;

public class PlusAgent implements Agent {


    private Double firstOperand;
    private Double secondOperand;
    private final String[] subs;
    private final String[] pubs;
    private Double result;
    private TopicManager topicManager;

    public PlusAgent(String[] subs, String[] pubs) {

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
        this.firstOperand = 0.0;
        this.secondOperand = 0.0;
    }

    @Override
    public void callback(String topic, Message msg) {
        double messageValue = msg.asDouble;

        if (Double.isNaN(messageValue)) {
            return;
        }

        if (subs.length > 0 && topic.equals(subs[0])) {
            firstOperand = messageValue;
        } else if (subs.length > 1 && topic.equals(subs[1])) {
            secondOperand = messageValue;
        }

        if (firstOperand != null && secondOperand != null && pubs.length > 0) {
            result = firstOperand + secondOperand;
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

package test;

import test.TopicManagerSingleton.TopicManager;

public class IncAgent implements Agent {

    private final String[] subs;
    private final String[] pubs;
    private Double result;
    private final TopicManager topicManager;

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

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void reset() {
    }

    @Override
    public void callback(String topic, Message msg) {
        double messageValue = msg.asDouble;

        if (Double.isNaN(messageValue)) {
            return;
        }

        if (pubs.length > 0) {
            result = messageValue + 1.0;
            topicManager.getTopic(pubs[0]).publish(new Message(result));
        }

    }

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


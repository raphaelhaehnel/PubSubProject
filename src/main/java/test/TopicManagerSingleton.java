package test;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class TopicManagerSingleton {

    public static class TopicManager {

        private static final TopicManager instance = new TopicManager();

        private final ConcurrentHashMap<String, Topic> topics;

        private TopicManager() {
            topics = new ConcurrentHashMap<>();
        }

        public Topic getTopic(String topicName) {
            return topics.computeIfAbsent(topicName, key -> new Topic(topicName));
        }

        public Collection<Topic> getTopics() {
            return topics.values();
        }

        public void clear() {
            topics.clear();
        }
    }

    public static TopicManager get() {
        return TopicManager.instance;
    }

}

package graph;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton holder for every Topic in the system.
 * Using a singleton makes sure every component (agents, servlets, graph
 * builder) shares the same set of topics.
 */
public class TopicManagerSingleton {

    public static class TopicManager {

        private static final TopicManager instance = new TopicManager();

        private final ConcurrentHashMap<String, Topic> topics = new ConcurrentHashMap<>();

        private TopicManager() {
        }

        /** Returns the matching topic, creating it on the fly if it does not exist. */
        public Topic getTopic(String topicName) {
            return topics.computeIfAbsent(topicName, key -> new Topic(topicName));
        }

        public Collection<Topic> getTopics() {
            return topics.values();
        }

        /** Removes every topic. Called when a new configuration is loaded. */
        public void clear() {
            topics.clear();
        }
    }

    private TopicManagerSingleton() {}

    public static TopicManager get() {
        return TopicManager.instance;
    }
}

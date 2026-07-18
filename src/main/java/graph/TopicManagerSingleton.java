package graph;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton holder for every Topic in the system.
 * Using a singleton makes sure every component (agents, servlets, graph
 * builder) shares the same set of topics.
 */
public class TopicManagerSingleton {

    /**
     * The thread-safe registry of every {@link Topic}, backed by a {@link ConcurrentHashMap}.
     * Accessed through the enclosing class's {@link #get()} method.
     */
    public static class TopicManager {

        private static final TopicManager instance = new TopicManager();

        private final ConcurrentHashMap<String, Topic> topics = new ConcurrentHashMap<>();

        private TopicManager() {
        }

        /**
         * Returns the matching topic, creating it on the fly if it does not exist.
         *
         * @param topicName the topic name
         * @return the existing or newly created topic
         */
        public Topic getTopic(String topicName) {
            return topics.computeIfAbsent(topicName, key -> new Topic(topicName));
        }

        /** @return every topic currently registered */
        public Collection<Topic> getTopics() {
            return topics.values();
        }

        /** Removes every topic. Called when a new configuration is loaded. */
        public void clear() {
            topics.clear();
        }
    }

    private TopicManagerSingleton() {}

    /** @return the shared {@link TopicManager} instance */
    public static TopicManager get() {
        return TopicManager.instance;
    }
}

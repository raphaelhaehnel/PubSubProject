package graph;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import graph.agents.Agent;

/**
 * One channel of the pub-sub system: agents subscribe to receive its
 * messages, others register as publishers.
 * Topics are created through {@link TopicManagerSingleton}, never directly.
 */
public class Topic {
    public final String name;

    // CopyOnWriteArrayList: subscribers list is read much more often than
    // written and may be iterated while another thread modifies it.
    public final CopyOnWriteArrayList<Agent> subs;
    public final CopyOnWriteArrayList<Agent> pubs;

    private volatile Message lastMessage;

    /**
     * Package-private: topics are created only through {@link TopicManagerSingleton}.
     *
     * @param name the topic name
     */
    Topic(String name) {
        this.name = name;
        this.subs = new CopyOnWriteArrayList<>();
        this.pubs = new CopyOnWriteArrayList<>();
    }

    /**
     * Subscribes an agent so it receives this topic's messages. No-op if already subscribed.
     *
     * @param agent the agent to subscribe
     */
    public void subscribe(Agent agent) {
        subs.addIfAbsent(agent);
    }

    /**
     * Removes an agent's subscription.
     *
     * @param agent the agent to unsubscribe
     */
    public void unsubscribe(Agent agent) {
        subs.remove(agent);
    }

    /**
     * Notifies every subscriber and remembers the message as the latest one.
     *
     * @param msg the message to publish
     */
    public void publish(Message msg) {
        lastMessage = msg;
        subs.forEach(publisher -> publisher.callback(this.name, msg));
    }

    /**
     * Registers an agent as a publisher of this topic. No-op if already registered.
     *
     * @param agent the publishing agent
     */
    public void addPublisher(Agent agent) {
        pubs.addIfAbsent(agent);
    }

    /**
     * Removes an agent from this topic's publishers.
     *
     * @param agent the publisher to remove
     */
    public void removePublisher(Agent agent) {
        pubs.remove(agent);
    }

    /** @return the agents subscribed to this topic */
    public List<Agent> getSubscribers() {
        return subs;
    }

    /** @return the agents publishing to this topic */
    public List<Agent> getPublishers() {
        return pubs;
    }

    /** @return the most recently published message, or {@code null} if nothing has been published */
    public Message getLastMessage() {
        return lastMessage;
    }
}
